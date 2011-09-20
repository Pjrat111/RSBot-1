package org.rsbot.script.provider;

import org.rsbot.Configuration;
import org.rsbot.script.Script;
import org.rsbot.script.provider.FileScriptSource.FileScriptDefinition;
import org.rsbot.util.io.HttpClient;
import org.rsbot.util.io.IOHelper;
import org.rsbot.util.io.IniParser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * @author Paris
 */
public class ScriptDeliveryNetwork implements ScriptSource, Runnable {
	private static final Logger log = Logger.getLogger("ScriptDelivery");
	private final static String DELIMITER = ",";
	private static ScriptDeliveryNetwork instance;
	private URL base;
	private final File manifest = new File(Configuration.Paths.getCacheDirectory(), "sdn-manifests.txt");
	private final FileScriptSource fileSource;

	private ScriptDeliveryNetwork() {
		fileSource = new FileScriptSource(new File(Configuration.Paths.getScriptsNetworkDirectory()));
	}

	public static ScriptDeliveryNetwork getInstance() {
		if (instance == null) {
			instance = new ScriptDeliveryNetwork();
		}
		return instance;
	}

	private static void parseManifests(final Map<String, Map<String, String>> entries, final List<ScriptDefinition> defs) {
		for (final Entry<String, Map<String, String>> entry : entries.entrySet()) {
			final Map<String, String> values = entry.getValue();
			if (!values.containsKey("name") || !values.containsKey("authors")) {
				continue;
			}
			final ScriptDefinition def = new ScriptDefinition();
			def.path = entry.getKey();
			def.name = values.get("name");
			def.authors = values.get("authors").split(DELIMITER);
			if (values.containsKey("id")) {
				def.id = Integer.parseInt(values.get("id"));
			}
			if (values.containsKey("crc32")) {
				def.crc32 = Long.parseLong(values.get("crc32"));
			}
			if (values.containsKey("version")) {
				def.version = Double.parseDouble(values.get("version"));
			}
			if (values.containsKey("description")) {
				def.description = values.get("description");
			}
			if (values.containsKey("keywords")) {
				def.keywords = values.get("keywords").split(DELIMITER);
			}
			if (values.containsKey("website")) {
				def.website = values.get("website");
			}
			defs.add(def);
		}
	}

	public synchronized void refresh(final boolean force) {
		if (force || !manifest.exists() || base == null) {
			try {
				base = HttpClient.download(new URL(Configuration.Paths.URLs.SDN_MANIFEST), manifest).getURL();
			} catch (final IOException ignored) {
				log.warning("Unable to load scripts from the network");
			}
		}
		if (base == null) {
			log.warning("Attempting to use cached network scripts");
		}
	}

	public List<ScriptDefinition> list() {
		if (base == null) {
			return fileSource.list();
		}
		final ArrayList<ScriptDefinition> defs = new ArrayList<ScriptDefinition>();
		refresh(false);
		try {
			parseManifests(IniParser.deserialise(manifest), defs);
		} catch (final IOException ignored) {
			log.warning("Error reading network script manifests");
		}
		for (final ScriptDefinition def : defs) {
			def.source = this;
		}
		return defs;
	}

	public Script load(final ScriptDefinition def) {
		if (base == null) {
			try {
				return fileSource.load(def);
			} catch (final Exception ignored) {
				log.severe("Unable to load cached script");
			}
		}
		final File cache = new File(Configuration.Paths.getScriptsNetworkDirectory(), def.path);
		final LinkedList<ScriptDefinition> defs = new LinkedList<ScriptDefinition>();
		try {
			if (!cache.exists() || IOHelper.crc32(cache) != def.crc32) {
				log.info("Downloading script " + def.getName() + "...");
				HttpClient.download(new URL(base, def.path), cache);
			}
			FileScriptSource.load(cache, defs, null);
			return FileScriptSource.load((FileScriptDefinition) defs.getFirst());
		} catch (final Exception ignored) {
			log.severe("Unable to load script");
		}
		return null;
	}

	@Override
	public void run() {
		refresh(true);
	}

	public boolean isAvailable() {
		return base != null;
	}
}
