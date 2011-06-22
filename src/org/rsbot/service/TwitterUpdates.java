package org.rsbot.service;

import org.rsbot.Configuration;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TwitterUpdates {
	public static void loadTweets(final int count) {
		final Logger log = Logger.getLogger("@" + Configuration.Twitter.NAME);
		final Level level = Level.INFO;
		final Object[] param = new Object[]{new Color(29, 131, 174)};

		final StringBuilder url = new StringBuilder();
		url.append("http://api.twitter.com/1/statuses/user_timeline.xml?screen_name=");
		url.append(Configuration.Twitter.NAME);
		url.append("&count=");
		url.append(count * 25);

		InputStreamReader stream = null;
		BufferedReader reader = null;

		try {
			stream = new InputStreamReader(new URL(url.toString()).openStream());
			reader = new BufferedReader(stream);
			String s = null;
			int c = 0;
			while ((s = reader.readLine()) != null) {
				final String a = "<text>", b = "</text>";
				int x = s.indexOf(a);
				if (x == -1) {
					continue;
				}
				x += a.length();
				final int y = s.indexOf(b, x);
				if (y == -1) {
					continue;
				}
				String msg = s.substring(x, y).trim();
				if (!msg.contains(Configuration.Twitter.HASHTAG)) {
					continue;
				}
				if (msg.endsWith(Configuration.Twitter.HASHTAG)) {
					msg = msg.substring(0, msg.length() - Configuration.Twitter.HASHTAG.length()).trim();
				}
				if (msg.isEmpty()) {
					continue;
				}
				log.log(level, msg, param);
				if (++c == count) {
					break;
				}
			}
		} catch (final IOException ioe) {
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
				if (stream != null) {
					stream.close();
				}
			} catch (final IOException ioe1) {
			}
		}
	}
}
