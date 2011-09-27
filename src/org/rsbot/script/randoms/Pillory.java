package org.rsbot.script.randoms;

import org.rsbot.script.Random;
import org.rsbot.script.ScriptManifest;
import org.rsbot.script.wrappers.RSTile;

import java.util.HashMap;
import java.util.Map;

/*
 * Last Update: 9/23/11
 */

@ScriptManifest(authors = {"illusion", "Pwnaz0r"}, name = "Pillory", version = 3.8)
public class Pillory extends Random {

	private static final int lockInterface = 189;
	private static final RSTile[] cageTiles = {new RSTile(2608, 3105), new RSTile(2606, 3105), new RSTile(2604, 3105),
			new RSTile(3226, 3407), new RSTile(3228, 3407), new RSTile(3230, 3407),
			new RSTile(2685, 3489), new RSTile(2683, 3489), new RSTile(2681, 3489)};
	private static final HashMap<Integer, String> keys = new HashMap<Integer, String>();

	static {
		keys.put(9753, "Diamond");
		keys.put(9754, "Square");
		keys.put(9755, "Circle");
		keys.put(9756, "Triangle");
	}

	@Override
	public boolean activateCondition() {
		if (game.isLoggedIn() && game.getClientState() != 12) {
			for (final RSTile cageTile : cageTiles) {
				if (getMyPlayer().getLocation().equals(cageTile)) {
					return true;
				}
			}
		}
		return false;
	}

	private int getKey() {
		for (Map.Entry<Integer, String> key : keys.entrySet()) { // Look through the keys
			if (interfaces.getComponent(lockInterface, 4).getModelID() == key.getKey()) { //lock model == key model
				log.info("Key needed: " + key.getValue());
				for (int i = 5; i < 8; i++) {
					if (interfaces.getComponent(lockInterface, i).getModelID() == key.getKey() - 4) {
						log("It is the " + (i - 4) + " key");
						return i; // return the key's component
					}
				}
			}
		}
		return -1;
	}

	@Override
	public int loop() {
		if (!activateCondition()) {
			return -1;
		}
		if (!interfaces.get(lockInterface).isValid()) {
			if (objects.getNearest("Cage") != null) {
				if (objects.getNearest("Cage").interact("unlock")) {
					return random(1000, 1500);
				}
			}
		} else {
			final int key = getKey();
			if (key > 4 && key < 8) {
				if (interfaces.getComponent(lockInterface, (key + 3)).interact("Select")) {
					return random(1300, 2500);
				}
				return 200;
			} else {
				log.info("We couldn't find correct the key, We're going to guess. The key returned " + key);
				if (interfaces.getComponent(lockInterface, random(5, 8)).interact("Select")) {
					return random(1300, 2500);
				}
				return random(500, 900);
			}
		}
		return -1;
	}
}