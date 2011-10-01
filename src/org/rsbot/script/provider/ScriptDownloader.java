package org.rsbot.script.provider;

import org.rsbot.Configuration;
import org.rsbot.util.StringUtil;
import org.rsbot.util.io.HttpClient;
import org.rsbot.util.io.IOHelper;
import org.rsbot.util.io.JavaCompiler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Paris
 */
public class ScriptDownloader {
	private static final Logger log = Logger.getLogger(ScriptDownloader.class.getName());

	public static void save(String url) {
		// check the source URL is valid
		url = url.trim();
		if (url.startsWith("https:")) {
			url = "http" + url.substring(5);
		}
		if (!url.startsWith("http://")) {
			log.warning("Invalid URL");
			return;
		}
		if (url.contains("pastie") && url.endsWith("/text")) {
			url = url.substring(0, url.toCharArray().length - 5);
		}
		url = normalisePastebin(url);

		if (url == null || url.length() == 0) {
			log.warning("Unknown pastebin");
			return;
		}

		// download the file
		log.info("Downloading script: " + url);
		final byte[] data;
		try {
			data = HttpClient.downloadBinary(new URL(url));
		} catch (final IOException e) {
			log.warning("Could not download script");
			return;
		}

		// if file is a .class then move as precompiled script
		final ByteArrayInputStream in = new ByteArrayInputStream(data);
		String className = classFileName(in);
		if (className != null) {
			final File classFile = new File(Configuration.Paths.getScriptsPrecompiledDirectory());
			IOHelper.write(in, classFile);
			if (classFile.exists()) {
				log.info("Saved precompiled script " + className);
			} else {
				log.warning("Could not save precompiled script " + className);
			}
			return;
		}

		// otherwise read file as plaintext
		String source = StringUtil.newStringUtf8(data);
		if (source == null || source.length() == 0) {
			log.severe("Could not read downloaded file");
			return;
		}

		// parse out any html
		if (source.startsWith("<html") || source.startsWith("<!")) {
			final int z = source.indexOf("<body");
			if (z != -1) {
				source = source.substring(z);
			}
			source = source.replaceAll("\\<br\\s*\\/?\\s*\\>", "\r\n");
			source = StringUtil.stripHtml(source);
			source = source.replaceAll("&nbsp;", " ");
			source = source.replaceAll("&quot;", "\"");
			source = source.replaceAll("&lt;", "<");
			source = source.replaceAll("&gt;", ">");
			source = source.replaceAll("&amp;", "&");
		}

		// check that the text represents java code for a script
		final Matcher def = Pattern.compile("public\\s+class\\s+(\\w+)\\s+extends\\s+Script").matcher(source);
		if (!def.find()) {
			log.severe("Specified URL is not a script");
			return;
		}
		className = def.group(1);
		final File dir = new File(Configuration.Paths.getScriptsSourcesDirectory());
		if (!dir.exists()) {
			dir.mkdirs();
		}

		// save the code in the folder for source scripts
		final File classFile = new File(dir, className + ".java");
		IOHelper.write(source, classFile);
		if (!classFile.exists()) {
			log.severe("Could not save script " + className);
			return;
		}

		// compile the script
		boolean result = false;
		if (JavaCompiler.isAvailable()) {
			String compileClassPath;
			if (Configuration.RUNNING_FROM_JAR) {
				compileClassPath = Configuration.Paths.getRunningJarPath();
			} else {
				compileClassPath = new File(Configuration.Paths.ROOT + File.separator + "bin").getAbsolutePath();
			}
			result = new JavaCompiler(classFile, compileClassPath).call();
		}

		// notify user of result
		if (result) {
			log.info("Compiled script " + className);
		} else {
			log.warning("Could not compile script " + className);
		}
		System.gc();
	}

	private static String classFileName(final InputStream in) {
		final int magic = 0xCAFEBABE;
		int header;
		try {
			header = in.read();
			if (header != magic) {
				return null;
			}
			for (int i = 1; i < 16; i++) {
				in.read();
			}
			StringBuilder name = new StringBuilder(32);
			int r;
			while ((r = in.read()) != -1) {
				if (r == 0x7) {
					return name.length() == 0 ? null : name.toString();
				}
				if (name.append((char) r).length() > 0x10000) { // obviously in after over 9000
					return null;
				}
			}
		} catch (IOException ignored) {
			return null;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ignored) {
				}
			}
		}
		return null;
	}

	private static String normalisePastebin(String sourceURL) {
		if (sourceURL.contains("gist.github.com")) {
			return gistRaw(sourceURL);
		}
		final HashMap<String, String> map = new HashMap<String, String>(8);
		map.put("pastebin\\.com/(\\w+)", "pastebin.com/raw.php?i=$1");
		map.put("pastie\\.org/(?:pastes/)?(\\d+)", "pastie.org/pastes/$1/text");
		map.put("pastebin\\.ca/(\\d+)", "pastebin.ca/raw/$1");
		map.put("sprunge\\.us/(\\w+)(?:\\?.*)?", "sprunge.us/$1");
		map.put("codepad\\.org/(\\w+)", "codepad.org/$1/raw.txt");
		for (final Entry<String, String> entry : map.entrySet()) {
			sourceURL = sourceURL.replaceAll(entry.getKey(), entry.getValue());
		}
		return sourceURL;
	}

	private static String gistRaw(final String gistURL) {
		Matcher m = Pattern.compile("gist\\.github\\.com/(\\d+)", Pattern.CASE_INSENSITIVE).matcher(gistURL);
		if (!m.find()) {
			return gistURL;
		}
		final String id = m.group(1);
		String meta;
		try {
			meta = HttpClient.downloadAsString(new URL("http://gist.github.com/api/v1/json/" + id));
		} catch (final Exception ignored) {
			return gistURL;
		}
		m = Pattern.compile("\"files\":\\s*\\[\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE).matcher(meta);
		if (!m.find()) {
			return gistURL;
		}
		final String file = m.group(1);
		try {
			return HttpClient.getFinalURL(new URL("http://gist.github.com/raw/" + id + "/" + file)).toString();
		} catch (final IOException ignored) {
			return null;
		}
	}
}
