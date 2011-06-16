package org.rsbot.script.util;

import org.rsbot.Configuration;
import org.rsbot.script.wrappers.RSItem;
import org.rsbot.util.io.IniParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;

/**
 * Bank cache class. Used for web.
 *
 * @author Timer
 */
public class BankCache {
	public final static String CACHE = Configuration.Paths.getCacheDirectory() + File.separator + "bank.dat";
	private final static File cacheFile = new File(CACHE);
	private final static HashMap<String, HashMap<String, String>> data = new HashMap<String, HashMap<String, String>>();
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
			final FileWriter fw = new FileWriter(cacheFile, false);
			final BufferedWriter bw = new BufferedWriter(fw);
			HashMap<String, String> newData = BankCache.genMap(name, items);
			if (data.containsKey(name.toLowerCase())) {
				data.get(name.toLowerCase()).putAll(newData);
			} else {
				data.put(name.toLowerCase(), newData);
			}
			IniParser.serialise(data, bw);
			bw.close();
		}
	}

	private static HashMap<String, String> genMap(final String name, final RSItem[] items) {
		synchronized (lock) {
			final HashMap<String, String> newData = new HashMap<String, String>();
			if (data.containsKey(name.toLowerCase())) {
				final HashMap<String, String> oldData = data.get(name.toLowerCase());
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

	public static void Load() throws Exception {
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
					final HashMap<String, String> userData = data.get(name);
					return userData.containsKey(o) || userData.containsValue(o);
				}
			} catch (final Exception ignored) {
			}
			return false;
		}
	}
}
