package org.rsbot.script.util;

import org.rsbot.Configuration;
import org.rsbot.script.wrappers.RSItem;
import org.rsbot.util.io.IniParser;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Bank cache class. Used for web.
 *
 * @author Timer
 */
public class BankCache {
	private final static File cacheFile = new File(Configuration.Paths.getWebDatabase());
	private final static Map<String, Map<String, String>> data = new HashMap<String, Map<String, String>>();
	private static final Object lock = new Object();

	/**
	 * Saves a bank cache for a user.
	 *
	 * @param name  The name of the character.
	 * @param items The array of items in the bank.
	 * @throws java.lang.Exception
	 */
	public static void Save(final String name, final RSItem[] items) throws Exception {
		synchronized (lock) {
			Load();// For multiple bot instances.
			Map<String, String> newData = BankCache.genMap(name, items);
			if (data.containsKey(name.toLowerCase())) {
				data.get(name.toLowerCase()).putAll(newData);
			} else {
				data.put(name.toLowerCase(), newData);
			}
			IniParser.serialise(data, cacheFile);
		}
	}

	private static Map<String, String> genMap(final String name, final RSItem[] items) {
		synchronized (lock) {
			final Map<String, String> newData = new HashMap<String, String>();
			if (data.containsKey(name.toLowerCase())) {
				final Map<String, String> oldData = data.get(name.toLowerCase());
				for (final RSItem i : items) {
					if (i != null) {
						if (oldData.containsKey(i.getName())) {
							if (!(Integer.parseInt(oldData.get(i.getName())) == i.getID())) {
								data.get(name.toLowerCase()).remove(i.getName());
								newData.put(i.getName(), i.getID() + "");
							}
						} else {
							newData.put(i.getName(), i.getID() + "");
						}
					}
				}
			} else {
				for (final RSItem i : items) {
					if (i != null) {
						newData.put(i.getName(), i.getID() + "");
					}
				}
			}
			return newData;
		}
	}

	private static void Load() throws Exception {
		synchronized (lock) {
			if (!cacheFile.exists()) {
				if (!cacheFile.createNewFile()) {
					return;
				}
			}
			data.clear();
			data.putAll(IniParser.deserialise(cacheFile));
		}
	}

	/**
	 * Checks the bank cache for an item.
	 *
	 * @param name Character name.
	 * @param o    The object to look for.
	 * @return <tt>true</tt> if the bank cache contains it.
	 */
	public static boolean Contains(final String name, final Object o) {
		synchronized (lock) {
			try {
				Load();// For multiple bot instances.
				if (data.containsKey(name)) {
					final Map<String, String> userData = data.get(name);
					return userData.containsKey(o) || userData.containsValue(o);
				}
			} catch (final Exception ignored) {
			}
			return false;
		}
	}
}
