package org.rsbot.script.randoms;

import org.rsbot.script.Random;
import org.rsbot.script.ScriptManifest;
import org.rsbot.script.wrappers.RSTile;

/**
 * Last Updated 9-23-10 Arbiter
 *
 * @author Illusion
 * @author Pwnaz0r
 */
@ScriptManifest(authors = {"illusion", "Pwnaz0r"}, name = "Pillory", version = 3.8)
public class Pillory extends Random {

	private static int fail = 0;
	private static final int GameInterface = 189;
	private static boolean inCage;
	private static RSTile myLoc;

	private static final RSTile[] cageTiles = {
			new RSTile(2608, 3105), new RSTile(2606, 3105), new RSTile(2604, 3105),
			new RSTile(3226, 3407), new RSTile(3228, 3407), new RSTile(3230, 3407),
			new RSTile(2685, 3489), new RSTile(2683, 3489), new RSTile(2681, 3489)};
	private static final int MODEL_IDS[] = {9753, 9754, 9755, 9756};
	private static final String MODEL_NAMES[] = {"Diamond", "Square", "Circle", "Triangle"};

	@Override
	public void onFinish() {
		fail = 0;
		inCage = false;
		myLoc = null;
	}

	@Override
	public boolean activateCondition() {
		if (!game.isLoggedIn()) {
			return false;
		}
		for (final RSTile cageTile : cageTiles) {
			if (getMyPlayer().getLocation().equals(cageTile) && game.getClientState() != 12) {
				return true;
			}
		}
		if (!inCage) {
			inCage = interfaces.getComponent(372, 3).getText().contains("Solve the pillory") ||
					interfaces.getComponent(372, 3).getText().contains("swinging");
		}
		return inCage;
	}

	private int getKey() {
		int key = 0;
		final int lockModelID = interfaces.getComponent(GameInterface, 4).getModelID();
		for (int i = 0; i < MODEL_IDS.length; i++) {
			if (MODEL_IDS[i] == lockModelID) {
				key = MODEL_IDS[i] - 4;
				log.info("Key needed: " + MODEL_NAMES[i]);
				break;
			}
		}
		for (int i = 5; i < 8; i++) {
			if (interfaces.getComponent(GameInterface, i).getModelID() == key) {
				log("It is the " + (i - 4) + " key");
				return i;
			}
		}
		return -1;
	}

	@Override
	public int loop() {
		if (!activateCondition()) {
			return -1;
		}
		if (fail > 20) {
			stopScript(false);
		}
		if (myLoc == null) {
			myLoc = getMyPlayer().getLocation();
			return random(1000, 2000);
		}
		if (!interfaces.get(GameInterface).isValid() && getMyPlayer().getAnimation() == -1) {
			if (objects.getNearest("Cage") != null) {
				if (objects.getNearest("Cage").interact("unlock")) {
					return random(1000, 1500);
				} else {
					fail++;
				}
			}
		}
		if (interfaces.get(GameInterface).isValid()) {
			int key = getKey();
			if (key <= 7) {
				if (interfaces.getComponent(GameInterface, (key + 3)).interact("Select")) {
					return random(1300, 2500);
				}
				return 200;
			} else {
				log.info("Bad combo?");
				fail++;
				return random(500, 900);
			}
		}
		return -1;
	}
}