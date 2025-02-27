package org.rsbot.util.io;

import org.rsbot.Configuration;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 * @author Paris
 */
public class HttpClient {
	private static final Logger log = Logger.getLogger(HttpClient.class.getName());
	public static final String HTTP_USERAGENT_FAKE, HTTP_USERAGENT_REAL;

	static {
		final boolean x64 = System.getProperty("sun.arch.data.model").equals("64");
		final StringBuilder s = new StringBuilder(70);
		s.append("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; ");
		if (x64) {
			s.append("WOW64; ");
			s.append("Trident/5.0)");
		}
		HTTP_USERAGENT_FAKE = s.toString();
		s.setLength(0);
		s.append(Configuration.NAME);
		s.append('/');
		s.append(Configuration.getVersion());
		s.append(" (");
		s.append(System.getProperty("os.name"));
		s.append(System.getProperty("os.version"));
		s.append("; ");
		s.append(System.getProperty("os.arch"));
		s.append("; ");
		s.append(System.getProperty("java.vendor"));
		s.append('/');
		s.append(System.getProperty("java.version"));
		s.append(')');
		HTTP_USERAGENT_REAL = s.toString();
	}

	public static String getHttpUserAgent(final URL url) {
		return url.getHost().equalsIgnoreCase(Configuration.Paths.URLs.GAME) || url.getHost().toLowerCase().endsWith(Configuration.Paths.URLs.GAME) ? HTTP_USERAGENT_FAKE : HTTP_USERAGENT_REAL;
	}

	public static HttpURLConnection getHttpConnection(final URL url) throws IOException {
		final HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		con.addRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
		con.addRequestProperty("Accept-Encoding", "gzip");
		con.addRequestProperty("Accept-Language", "en-us,en;q=0.5");
		con.addRequestProperty("Host", url.getHost());
		con.addRequestProperty("User-Agent", getHttpUserAgent(url));
		con.setConnectTimeout(10000);
		return con;
	}

	private static HttpURLConnection getConnection(final URL url) throws IOException {
		final HttpURLConnection con = getHttpConnection(url);
		con.setUseCaches(true);
		return con;
	}

	public static URL getFinalURL(final URL url) throws IOException {
		return getFinalURL(url, true);
	}

	private static URL getFinalURL(final URL url, final boolean httpHead) throws IOException {
		final HttpURLConnection con = getConnection(url);
		con.setInstanceFollowRedirects(false);
		if (httpHead) {
			con.setRequestMethod("HEAD");
		}
		con.connect();
		switch (con.getResponseCode()) {
			case HttpURLConnection.HTTP_MOVED_PERM:
			case HttpURLConnection.HTTP_MOVED_TEMP:
			case HttpURLConnection.HTTP_SEE_OTHER:
				return getFinalURL(new URL(con.getHeaderField("Location")), true);
			case HttpURLConnection.HTTP_BAD_METHOD:
				return getFinalURL(url, false);
			default:
				return url;
		}
	}

	private static HttpURLConnection cloneConnection(final HttpURLConnection con) throws IOException {
		final HttpURLConnection cloned = (HttpURLConnection) con.getURL().openConnection();
		for (final Entry<String, List<String>> prop : con.getRequestProperties().entrySet()) {
			final String key = prop.getKey();
			for (final String value : prop.getValue()) {
				cloned.addRequestProperty(key, value);
			}
		}
		return cloned;
	}

	public static boolean isModifiedSince(URL url, long date) {
		try {
			url = getFinalURL(url);
			date -= TimeZone.getDefault().getOffset(date);
			final HttpURLConnection con = getConnection(url);
			con.setRequestMethod("HEAD");
			con.connect();
			final int resp = con.getResponseCode();
			con.disconnect();
			return resp != HttpURLConnection.HTTP_NOT_MODIFIED;
		} catch (final IOException ignored) {
			return true;
		}
	}

	public static HttpURLConnection download(final URL url, final File file) throws IOException {
		return download(getConnection(getFinalURL(url)), file);
	}

	public static HttpURLConnection download(final HttpURLConnection con, final File file) throws IOException {
		if (file.exists()) {
			final HttpURLConnection head = cloneConnection(con);
			final int offset = TimeZone.getDefault().getOffset(file.lastModified());
			head.setIfModifiedSince(file.lastModified() - offset);
			head.setRequestMethod("HEAD");
			head.connect();
			if (head.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
				log.fine("Using " + file.getName() + " from cache");
				con.disconnect();
				head.disconnect();
				return head;
			}
		}

		log.fine("Downloading new " + file.getName());

		final byte[] buffer = downloadBinary(con);

		if (!file.exists()) {
			file.createNewFile();
		}
		if (file.exists() && (!file.canRead() || file.canWrite())) {
			file.setReadable(true);
			file.setWritable(true);
		}
		if (file.exists() && file.canRead() && file.canWrite()) {
			final FileOutputStream fos = new FileOutputStream(file);
			fos.write(buffer);
			fos.flush();
			fos.close();
		}

		if (con.getLastModified() != 0L) {
			final int offset = TimeZone.getDefault().getOffset(con.getLastModified());
			file.setLastModified(con.getLastModified() + offset);
		}

		con.disconnect();
		return con;
	}

	public static String downloadAsString(final URL url) throws IOException {
		return downloadAsString(getConnection(url));
	}

	public static String downloadAsString(final HttpURLConnection con) throws IOException {
		final byte[] buffer = downloadBinary(con);
		return new String(buffer);
	}

	public static byte[] downloadBinary(final URL url) throws IOException {
		return downloadBinary(getConnection(url));
	}

	private static byte[] downloadBinary(final URLConnection con) throws IOException {
		final DataInputStream di = new DataInputStream(con.getInputStream());
		byte[] buffer;
		final int len = con.getContentLength();
		if (len == -1) {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			int b;
			while ((b = di.read()) != -1) {
				out.write(b);
			}
			buffer = out.toByteArray();
		} else {
			buffer = new byte[con.getContentLength()];
			di.readFully(buffer);
		}
		di.close();
		if (buffer != null) {
			buffer = IOHelper.ungzip(buffer);
		}
		return buffer;
	}
}
