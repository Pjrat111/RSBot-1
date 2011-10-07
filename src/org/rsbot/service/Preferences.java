package org.rsbot.service;

import org.rsbot.Configuration;
import org.rsbot.util.io.IniParser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Preferences {
	private final Logger log = Logger.getLogger(Preferences.class.getName());
	private static Preferences instance;
	private final File store;

	public boolean hideAds = false;
	public String sdnUser = "";
	public boolean localScripts = true;
	public boolean theme = true;

	private Preferences(final File store) {
		this.store = store;
	}

	public static Preferences getInstance() {
		if (instance == null) {
			instance = new Preferences(new File(Configuration.Paths.getSettingsDirectory(), "preferences.ini"));
		}
		return instance;
	}

	public void load() {
		Map<String, String> keys = null;
		try {
			if (!store.exists()) {
				if (!store.createNewFile()) {
					throw new IOException("Could not create a new file.");
				}
			}
			keys = IniParser.deserialise(store).get(IniParser.EMPTYSECTION);
		} catch (final IOException ignored) {
			log.severe("Failed to load preferences");
		}
		if (keys == null || keys.isEmpty()) {
			return;
		}
		if (keys.containsKey("hideAds")) {
			hideAds = IniParser.parseBool(keys.get("hideAds"));
		}
		if (keys.containsKey("sdnUser")) {
			sdnUser = keys.get("sdnUser");
		}
		if (keys.containsKey("localScripts")) {
			localScripts = IniParser.parseBool(keys.get("localScripts"));
		}
		if (keys.containsKey("theme")) {
			theme = IniParser.parseBool(keys.get("theme"));
		}
	}

	public void save() {
		final Map<String, String> keys = new HashMap<String, String>(11);
		keys.put("hideAds", Boolean.toString(hideAds));
		keys.put("sdnUser", sdnUser);
		keys.put("localScripts", Boolean.toString(localScripts));
		keys.put("theme", Boolean.toString(theme));
		final Map<String, Map<String, String>> data = new HashMap<String, Map<String, String>>(1);
		data.put(IniParser.EMPTYSECTION, keys);
		try {
			IniParser.serialise(data, store);
		} catch (final IOException ignored) {
			log.severe("Could not save preferences");
		}
	}
}
