package org.rsbot.script.randoms;

import org.rsbot.script.Random;
import org.rsbot.script.ScriptManifest;
import org.rsbot.script.wrappers.*;

import java.util.ArrayList;

//Checked 3/7/10

/**
 * Updated by aman (Nov 14, 09) Updated by Equilibrium (Dec 13, 09) Updated by
 * Fred (Dec 25, 09) Updated by Iscream (Jan 31, 10) Updated by Iscream (Feb 20,
 * 10) Updated by Jacmob (Oct 24, 10) Updated by Arbiter (Nov 11, 10)
 *
 * @author PwnZ
 */
@ScriptManifest(authors = {"PwnZ"}, name = "Molly", version = 1.9)
public class Molly extends Random {

	private static final int CLAW_ID = 14976;
	private static final int CONTROL_PANEL_ID = 14978;
	private static final int DOOR_ID = 14982;
	private static final int MOLLY_CHATBOX_INTERFACEGROUP = 228;
	private static final int MOLLY_CHATBOX_NOTHANKS = 3;
	private static final int MOLLY_CHATBOX_SKIP = 2;
	private static final int CONTROL_INTERFACEGROUP = 240;
	private static final int CONTROLS_GRAB = 28;
	private static final int CONTROLS_UP = 29;
	private static final int CONTROLS_DOWN = 30;
	private static final int CONTROLS_LEFT = 31;
	private static final int CONTROLS_RIGHT = 32;

	private static RSNPC molly;
	private static RSObject controlPanel;
	private static int mollyID = -1;
	private static boolean talkedToMolly;
	private static boolean finished;
	private static long delayTime;

	@Override
	public void onFinish() {
		mollyID = -1;
		talkedToMolly = false;
		finished = false;
		delayTime = -1;
	}

	@Override
	public boolean activateCondition() {
		molly = npcs.getNearest("Molly");
		controlPanel = objects.getNearest(Molly.CONTROL_PANEL_ID);
		return molly != null && molly.isInteractingWithLocalPlayer() || controlPanel != null;
	}

	@Override
	public int loop() {
		if (!activateCondition()) {
			log("Molly random finished!");
			sleep(500);
			if (!activateCondition()) {
				return -1;
			}
		}
		controlPanel = objects.getNearest(Molly.CONTROL_PANEL_ID);
		if (getMyPlayer().isMoving() || getMyPlayer().getAnimation() != -1) {
			return random(800, 1300);
		}
		if (interfaces.clickContinue()) {
			setCamera();
			return random(500, 800);
		}
		if (mollyID == -1) {
			mollyID = molly.getID();
			log("Molly ID: " + Integer.toString(mollyID));
			log("Evil Twin ID:" + Integer.toString(mollyID - 40));
		}
		final RSComponent skipInterface = interfaces.get(
				Molly.MOLLY_CHATBOX_INTERFACEGROUP).getComponent(Molly.MOLLY_CHATBOX_SKIP);
		if (skipInterface != null && skipInterface.isValid()
				&& skipInterface.getAbsoluteY() > 5 && skipInterface.containsText("Yes, I")) {
			skipInterface.doClick();
			return random(600, 1000);
		}
		final RSComponent noThanksInterface = interfaces.get(
				Molly.MOLLY_CHATBOX_INTERFACEGROUP).getComponent(Molly.MOLLY_CHATBOX_NOTHANKS);
		if (noThanksInterface != null && noThanksInterface.isValid()
				&& noThanksInterface.getAbsoluteY() > 5) {
			setCamera();
			sleep(random(800, 1200));
			if (noThanksInterface.doClick()) {
				talkedToMolly = true;
			}
			return random(600, 1000);
		}
		if (camera.getPitch() < 95 && !inControlInterface()) {
			camera.setPitch(true);
			return random(300, 500);
		}
		if (finished) {
			if (!inControlRoom()) {
				if (!calc.tileOnScreen(molly.getLocation())) {
					walking.walkTileOnScreen(molly.getLocation());
					return random(1000, 2000);
				}
				molly.interact("Talk");
				return random(1000, 1200);
			} else {
				if (openDoor()) {
					return random(2000, 2500);
				}
				return random(400, 600);
			}
		}
		if (!inControlRoom()) {
			if (talkedToMolly && !finished
					&& (interfaces.get(Molly.MOLLY_CHATBOX_INTERFACEGROUP) == null || interfaces
					.get(Molly.MOLLY_CHATBOX_INTERFACEGROUP).getComponent(0).getAbsoluteY() < 2)
					&& (interfaces.get(Molly.MOLLY_CHATBOX_NOTHANKS) == null || interfaces
					.get(Molly.MOLLY_CHATBOX_NOTHANKS).getComponent(0).getAbsoluteY() < 2)) {
				if (openDoor()) {
					sleep(random(800, 1200));
				}
			} else {
				molly.interact("Talk");
				talkedToMolly = true;
				return random(1000, 2000);
			}
		} else {
			if (npcs.getNearest("Molly") != null) {
				finished = true;
				sleep(random(800, 1200));
			} else {
				if (!inControlInterface()) {
					if (controlPanel != null) {
						if (controlPanel.isOnScreen()) {
							if (controlPanel.interact("Use")) {
								sleep(random(1200, 2000));
							}
						} else {
							camera.setPitch(true);
							camera.turnTo(controlPanel.getLocation());
							walking.walkTileOnScreen(controlPanel.getLocation());
						}
					} else {
						camera.setPitch(false);
						camera.setCompass('e');
					}
				} else {
					navigateClaw();
					delayTime = System.currentTimeMillis();
					while (!interfaces.canContinue() && System.currentTimeMillis() - delayTime < 12000) {
						// Waiting for the cut scene to finish
					}
					sleep(random(300, 400));
				}
			}
		}
		return random(200, 400);
	}

	private void navigateClaw() {
		if (!inControlInterface() || mollyID < 1) {
			return;
		}
		RSObject claw;
		RSNPC suspect;
		while ((claw = objects.getNearest(Molly.CLAW_ID)) != null && (suspect = npcs.getNearest(mollyID - 40)) != null) {
			final RSTile clawLoc = claw.getLocation();
			final RSTile susLoc = suspect.getLocation();
			final ArrayList<Integer> options = new ArrayList<Integer>();
			if (susLoc.getX() > clawLoc.getX()) {
				options.add(Molly.CONTROLS_LEFT);
			}
			if (susLoc.getX() < clawLoc.getX()) {
				options.add(Molly.CONTROLS_RIGHT);
			}
			if (susLoc.getY() > clawLoc.getY()) {
				options.add(Molly.CONTROLS_DOWN);
			}
			if (susLoc.getY() < clawLoc.getY()) {
				options.add(Molly.CONTROLS_UP);
			}
			if (options.isEmpty()) {
				options.add(Molly.CONTROLS_GRAB);
			}
			final RSInterface i = interfaces.get(Molly.CONTROL_INTERFACEGROUP);
			if (i != null && i.isValid()) {
				i.getComponent(options.get(random(0, options.size()))).doClick();
			}
			delayTime = System.currentTimeMillis();
			while (!hasClawMoved(clawLoc) && System.currentTimeMillis() - delayTime < 3500) {
				sleep(10);
			}
		}
	}

	private boolean hasClawMoved(final RSTile prevClawLoc) {
		final RSObject claw = objects.getNearest(Molly.CLAW_ID);
		if (claw == null) {
			return false;
		}
		final RSTile currentClawLoc = claw.getLocation();
		return !prevClawLoc.equals(currentClawLoc);
	}

	private boolean openDoor() {
		final RSObject door = objects.getNearest(Molly.DOOR_ID);
		if (door != null) {
			if (!calc.tileOnScreen(door.getLocation())) {
				walking.walkTileOnScreen(door.getLocation());
				sleep(random(1000, 2000));
				return false;
			}
			if (door.interact("Open")) {
				return true;
			}
		}
		return false;
	}

	private boolean inControlRoom() {
		final RSObject o = objects.getNearest(DOOR_ID);
		return o != null && getMyPlayer().getLocation().getX() > o.getLocation().getX();
	}

	private boolean inControlInterface() {
		final RSInterface i = interfaces.get(Molly.CONTROL_INTERFACEGROUP);
		return i != null && i.isValid();
	}

	private void setCamera() {
		if (random(0, 6) == 3) {
			camera.setPitch(true);
		}
	}
}