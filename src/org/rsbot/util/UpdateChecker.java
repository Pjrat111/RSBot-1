package org.rsbot.util;

import org.rsbot.Configuration;
import org.rsbot.util.io.HttpClient;
import org.rsbot.util.io.IOHelper;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * @author Paris
 */
public final class UpdateChecker {
	private static int latest = -1;

	public static int getLatestVersion() {
		if (latest != -1) {
			return latest;
		}
		latest = Configuration.getVersion();
		try {
			final File cache = new File(Configuration.Paths.getCacheDirectory(), "version-latest.txt");
			HttpClient.download(new URL(Configuration.Paths.URLs.VERSION), cache);
			latest = Integer.parseInt(IOHelper.readString(cache).trim());
		} catch (final IOException ignored) {
		} catch (final NumberFormatException ignored) {
		} catch (final NullPointerException ignored) {
		}
		return latest;
	}

	public static String downloadLatest() {
		final File cache = new File(Configuration.Paths.getCacheDirectory(), Configuration.NAME + "-" + Integer.toString(getLatestVersion()) + ".jar");
		try {
			HttpClient.download(new URL(Configuration.Paths.URLs.DOWNLOAD), cache);
			return cache.getCanonicalPath();
		} catch (final IOException ignored) {
		}
		return null;
	}
}
