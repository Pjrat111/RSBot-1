package org.rsbot.loader;

import org.rsbot.Configuration;
import org.rsbot.loader.asm.ClassReader;
import org.rsbot.loader.script.ModScript;
import org.rsbot.loader.script.ParseException;
import org.rsbot.util.io.HttpClient;
import org.rsbot.util.io.IOHelper;
import org.rsbot.util.io.IniParser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Paris
 */
public class ClientLoader {
	private final static ClientLoader instance = new ClientLoader();
	private final String id = ClientLoader.class.getName();
	private final Logger log = Logger.getLogger(id);
	private static final File manifest = new File(Configuration.Paths.getCacheDirectory(), "client.ini");
	private static final File cache = new File(Configuration.Paths.getCacheDirectory(), "client.jar");
	private static final File localms = new File(Configuration.Paths.getCacheDirectory(), "modscript");
	public final static int PORT_CLIENT = 43594;
	private int[] version = {-1, -1, -1};
	private Map<String, byte[]> classes;

	private ClientLoader() {
	}

	public static ClientLoader getInstance() {
		return instance;
	}

	public static File getClientManifest() {
		return manifest;
	}

	private boolean isCacheClean() {
		if (!(manifest.exists() && cache.exists()) || (localms.exists() && !Configuration.RUNNING_FROM_JAR)) {
			return false;
		}
		final Map<String, String> info;
		try {
			info = IniParser.deserialise(manifest).get(id);
		} catch (final IOException ignored) {
			ignored.printStackTrace();
			return false;
		}
		if (info != null && info.containsKey("v1") && info.containsKey("t")) {
			try {
				version[1] = Integer.parseInt(info.get("v1"));
			} catch (final NumberFormatException ignored) {
				return false;
			}
			try {
				version[2] = getRemoteVersion(version[1]);
			} catch (final IOException ignored) {
				return false;
			}
			boolean notModified = false;
			try {
				notModified = HttpClient.isModifiedSince(new URL(Configuration.Paths.URLs.CLIENTPATCH), Long.parseLong(info.get("t")));
			} catch (final IOException ignored) {
			} catch (final NumberFormatException ignored) {
			}
			return version[1] == version[2] && notModified;
		} else {
			return false;
		}
	}

	public synchronized void load() throws IOException, ParseException {
		if (classes != null) {
			return;
		}
		classes = new HashMap<String, byte[]>();

		if (isCacheClean()) {
			log.info("Reading game client from cache");
			final JarFile jar = new JarFile(cache);
			final Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				final JarEntry entry = entries.nextElement();
				String name = entry.getName();
				if (name.endsWith(".class")) {
					name = name.substring(0, name.length() - 6).replace('/', '.');
					classes.put(name, IOHelper.read(jar.getInputStream(entry)));
				}
			}
			jar.close();
		} else {
			final ModScript script;
			if (localms.exists() && !Configuration.RUNNING_FROM_JAR) {
				log.info("Loading game client against local patch");
				script = new ModScript(IOHelper.ungzip(IOHelper.read(localms)));
			} else {
				log.info("Downloading new game client");
				script = new ModScript(HttpClient.downloadBinary(new URL(Configuration.Paths.URLs.CLIENTPATCH)));
			}
			version[0] = script.getVersion();
			if (version[2] > version[0]) {
				throw new ParseException("Patch outdated (" + version[2] + " > " + version[0] + ")");
			}
			final JarFile loader = getJar(true), client = getJar(false);
			final List<String> replace = Arrays.asList(script.getAttribute("replace").split(" "));

			for (final JarFile jar : new JarFile[]{loader, client}) {
				final Enumeration<JarEntry> entries = jar.entries();
				while (entries.hasMoreElements()) {
					final JarEntry entry = entries.nextElement();
					String name = entry.getName();
					if (name.endsWith(".class")) {
						name = name.substring(0, name.length() - 6).replace('/', '.');
						if (jar == client || replace.contains(name)) {
							classes.put(name, IOHelper.read(jar.getInputStream(entry)));
						}
					}
				}
				jar.close();
			}

			for (final Map.Entry<String, byte[]> entry : classes.entrySet()) {
				entry.setValue(script.process(entry.getKey(), entry.getValue()));
			}

			final ClassReader reader = new ClassReader(new ByteArrayInputStream(classes.get("client")));
			final VersionVisitor vv = new VersionVisitor();
			reader.accept(vv, ClassReader.SKIP_FRAMES);
			version[1] = vv.getVersion();

			final ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(cache));
			zip.setMethod(ZipOutputStream.STORED);
			zip.setLevel(0);
			for (final Entry<String, byte[]> item : classes.entrySet()) {
				final ZipEntry entry = new ZipEntry(item.getKey() + ".class");
				entry.setMethod(ZipEntry.STORED);
				final byte[] data = item.getValue();
				entry.setSize(data.length);
				entry.setCompressedSize(data.length);
				entry.setCrc(IOHelper.crc32(data));
				zip.putNextEntry(entry);
				zip.write(item.getValue());
				zip.closeEntry();
			}
			zip.close();

			final Map<String, Map<String, String>> data = new HashMap<String, Map<String, String>>(1);
			final Map<String, String> info = new HashMap<String, String>();
			info.put("v1", Integer.toString(version[1]));
			info.put("t", Long.toString(System.currentTimeMillis() / 1000L));
			data.put(id, info);
			IniParser.serialise(data, manifest);
		}
	}

	public Map<String, byte[]> getClasses() {
		final Map<String, byte[]> copy = new HashMap<String, byte[]>(classes.size());
		for (final Entry<String, byte[]> item : classes.entrySet()) {
			copy.put(item.getKey(), item.getValue().clone());
		}
		return copy;
	}

	public boolean isOutdated() {
		return version[2] > version[1];
	}

	public static int getRemoteVersion(final int start) throws IOException {
		final String host = getTargetHost();
		for (int i = start; i < start + 50; i++) {
			final Socket sock = new Socket(host, PORT_CLIENT);
			final byte[] payload = new byte[]{15, 0, 0, (byte) (i >> 8), (byte) i};
			sock.getOutputStream().write(payload, 0, payload.length);
			if (sock.getInputStream().read() == 0) {
				sock.close();
				return i;
			} else {
				sock.close();
			}
		}
		return -1;
	}

	private static JarFile getJar(final boolean loader) {
		while (true) {
			try {
				final StringBuilder sb = new StringBuilder(50);
				sb.append("jar:http://").append(getTargetHost()).append("/").append(loader ? "loader" : getTargetName()).append(".jar!/");
				final JarURLConnection juc = (JarURLConnection) new URL(sb.toString()).openConnection();
				juc.setConnectTimeout(5000);
				return juc.getJarFile();
			} catch (final Exception ignored) {
			}
		}
	}

	public static String getTargetName() {
		String target = Configuration.Paths.URLs.GAME;
		return target.substring(0, target.indexOf('.'));
	}

	public static String getTargetHost() {
		return getTargetHost(Integer.toString(1 + new Random().nextInt(169)));
	}

	public static String getTargetHost(final String world) {
		return "world" + world + "." + Configuration.Paths.URLs.GAME;
	}
}
