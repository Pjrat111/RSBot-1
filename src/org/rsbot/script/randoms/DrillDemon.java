package org.rsbot.script.randoms;

import org.rsbot.script.Random;
import org.rsbot.script.ScriptManifest;
import org.rsbot.script.wrappers.RSArea;
import org.rsbot.script.wrappers.RSNPC;
import org.rsbot.script.wrappers.RSObject;

import java.util.HashMap;

/*
 * Last Updated 9/24/11
 */

@ScriptManifest(authors = {"Keilgo"}, name = "DrillDemon", version = 0.2)
public class DrillDemon extends Random {

	private final static int demonID = 2790;
	private final static HashMap<Integer, Integer> exerciseMats = new HashMap<Integer, Integer>();
	private final static HashMap<Integer, int[]> setting = new HashMap<Integer, int[]>();

	static {
		exerciseMats.put(10949, 0); //Star jumps
		exerciseMats.put(10946, 1); //Push ups
		exerciseMats.put(10948, 2); //Sit ups
		exerciseMats.put(10947, 3); //Jog
	}

	static {
		setting.put(668, new int[]{0, 1, 2, 3});
		setting.put(675, new int[]{1, 0, 2, 3});
		setting.put(724, new int[]{0, 2, 1, 3});
		setting.put(738, new int[]{2, 0, 1, 3});
		setting.put(787, new int[]{1, 2, 0, 3});
		setting.put(794, new int[]{2, 1, 0, 3});
		setting.put(1116, new int[]{0, 1, 3, 2});
		setting.put(1123, new int[]{1, 0, 3, 2});
		setting.put(1228, new int[]{0, 3, 1, 2});
		setting.put(1249, new int[]{3, 0, 1, 2});
		setting.put(1291, new int[]{1, 3, 0, 2});
		setting.put(1305, new int[]{3, 1, 0, 2});
		setting.put(1620, new int[]{0, 2, 3, 1});
		setting.put(1634, new int[]{2, 0, 3, 1});
		setting.put(1676, new int[]{0, 3, 2, 1});
		setting.put(1697, new int[]{3, 0, 2, 1});
		setting.put(1802, new int[]{2, 3, 0, 1});
		setting.put(1809, new int[]{3, 2, 0, 1});
		setting.put(2131, new int[]{1, 2, 3, 0});
		setting.put(2138, new int[]{2, 1, 3, 0});
		setting.put(2187, new int[]{1, 3, 2, 0});
		setting.put(2201, new int[]{3, 1, 2, 0});
		setting.put(2250, new int[]{2, 3, 1, 0});
		setting.put(2257, new int[]{3, 2, 1, 0});
	}

	@Override
	public boolean activateCondition() {
		return new RSArea(3159, 4818, 3167, 4822).contains(getMyPlayer().getLocation());
	}

	@Override
	public int loop() {
		if (!activateCondition()) {
			return -1;
		}
		camera.setPitch(true);
		camera.setCompass('N');
		if (getMyPlayer().isMoving()) {
			for (int i = 0; i < 50; i++) {
				if (!getMyPlayer().isMoving()) {
					break;
				}
				sleep(75, 80);
			}
			return random(1800, 2000);
		}
		if (getMyPlayer().getAnimation() != -1) {
			for (int i = 0; i < 50; i++) {
				if (getMyPlayer().getAnimation() == -1) {
					break;
				}
				sleep(60, 90);
			}
			for (int i = 0; i < 50; i++) {
				if (interfaces.getComponent(241, 0).isValid()) {
					break;
				}
				sleep(30, 40);
			}
			return random(400, 700);
		}
		if (interfaces.clickContinue()) {
			return random(1000, 1500);
		}
		if (interfaces.get(148).isValid()) {
			final int compare = settings.getSetting(531);
			final int compID = interfaces.getComponent(148, 0).getComponentID();
			for (int i = 0; i < setting.get(compare).length; i++) {
				if (setting.get(compare)[i] == exerciseMats.get(compID)) {
					if (findAndUseMat(i)) {
						return 800;
					}
				}
			}
		}
		if (!interfaces.clickContinue() && getMyPlayer().getAnimation() == -1) {
			final RSNPC demon = npcs.getNearest(demonID);
			demon.interact("Talk-to");
		}
		return random(2000, 2500);
	}

	public boolean findAndUseMat(final int signID) {
		final RSObject[] mats = {objects.getNearest(10076), objects.getNearest(10077),
				objects.getNearest(10078), objects.getNearest(10079)};
		if (mats[signID] != null) {
			if (!mats[signID].isOnScreen()) {
				if (walking.walkTileMM(mats[signID].getLocation())) {
					sleep(900);
				}
			} else {
				if (getMyPlayer().getAnimation() == -1) {
					if (mats[signID].interact("Use")) {
						sleep(800);
						return true;
					}
				}
			}
		}
		return false;
	}
}