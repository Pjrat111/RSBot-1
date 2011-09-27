package org.rsbot.script.randoms;

import org.rsbot.script.Random;
import org.rsbot.script.ScriptManifest;
import org.rsbot.script.util.Timer;
import org.rsbot.script.wrappers.RSComponent;
import org.rsbot.script.wrappers.RSInterface;
import org.rsbot.script.wrappers.RSNPC;
import org.rsbot.script.wrappers.RSObject;

import java.awt.*;

@ScriptManifest(authors = "endoskeleton", name = "CapnArnav", version = 1)
public class CapnArnav extends Random {

	private static final int[] ARNAV_CHEST = {42337, 42338};
	private static final int ARNAV_ID = 2308;
	private static final int EXIT_PORTAL = 11369;
	private static final int[][] INTERFACE_SOLVE_IDS = {{7, 14, 21}, // BOWL
			{5, 12, 19}, // RING
			{6, 13, 20}, // COIN
			{8, 15, 22} // BAR
	};
	private static int index = -1;
	private static final int[][] ARROWS = {{2, 3}, {9, 10}, {16, 17}};
	private static final int TALK_INTERFACE = 228;
	private static final int CHEST_INTERFACE_PARENT = 185;
	private static final int CHEST_INTERFACE_UNLOCK = 28;
	private static final int CHEST_INTERFACE_CENTER = 23;

	private static enum STATE {
		OPEN_CHEST, SOLVE, TALK, EXIT
	}


	@Override
	public boolean activateCondition() {
		final RSNPC captain = npcs.getNearest(ARNAV_ID);
		final RSObject portal = objects.getNearest(EXIT_PORTAL);
		return portal != null && captain != null;
	}

	@Override
	public void onFinish() {
		index = -1;
	}

	private STATE getState() {
		if (objects.getNearest(ARNAV_CHEST[1]) != null) {
			return STATE.EXIT;
		} else if (interfaces.canContinue() || interfaces.get(TALK_INTERFACE).isValid()) {
			return STATE.TALK;
		} else if (!interfaces.get(CHEST_INTERFACE_PARENT).isValid()) {
			return STATE.OPEN_CHEST;
		} else {
			return STATE.SOLVE;
		}
	}

	@Override
	public int loop() {
		if (bank.isDepositOpen() || bank.isOpen()) {
			bank.close();
		}
		if (!activateCondition()) {
			return -1;
		}
		if (getMyPlayer().isMoving()) {
			return random(700, 1200);
		}
		switch (getState()) {
			case EXIT:
				final RSObject portal = objects.getNearest(EXIT_PORTAL);
				if (portal != null) {
					if (!portal.isOnScreen()) {
						if (random(0, 2) == 1) {
							camera.turnTo(portal);
						} else {
							if (walking.walkTileMM(portal.getLocation())) {
								sleep(800, 1000);
							}
						}
					}
					if (portal.interact("Enter")) {
						return random(3000, 3500);
					}
				}
				break;

			case OPEN_CHEST:
				final RSObject chest = objects.getNearest(ARNAV_CHEST);
				if (chest != null) {
					if (chest.interact("Open")) {
						return random(1000, 1300);
					}
				}
				break;

			case TALK:
				if (interfaces.clickContinue()) {
					return random(1000, 1500);
				}
				final RSComponent okay = interfaces.getComponent(TALK_INTERFACE, 3);
				if (okay.isValid()) {
					if (okay.doClick()) {
						return random(200, 500);
					}
				}
				break;

			case SOLVE:
				final RSInterface solver = interfaces.get(CHEST_INTERFACE_PARENT);
				if (solver.isValid()) {
					if (solved()) {
						if (solver.getComponent(CHEST_INTERFACE_UNLOCK).doClick()) {
							return random(600, 900);
						}
						return 0;
					}
					final RSComponent container = solver.getComponent(CHEST_INTERFACE_CENTER);
					final String s = solver.getComponent(32).getText();
					if (s.contains("Bowl")) {
						index = 0;
					} else if (s.contains("Ring")) {
						index = 1;
					} else if (s.contains("Coin")) {
						index = 2;
					} else if (s.contains("Bar")) {
						index = 3;
					}
					for (int i = 0; i < 3; i++) {
						final RSComponent target = solver.getComponent(INTERFACE_SOLVE_IDS[index][i]);
						final int y = target.getRelativeY();
						int direction;
						if (y < 50 && y > -50) {
							direction = 0;
						} else if (y >= 50) {
							direction = 1;
						} else {
							direction = random(0, 2);
						}
						final RSComponent arrow = solver.getComponent(ARROWS[i][direction]);
						while (container.isValid() && target.isValid() && arrow.isValid() &&
								!container.getArea().contains(target.getCenter()) && new Timer(10000).isRunning()) {
							if (arrow.doClick()) {
								sleep(random(800, 1200));
							}
						}
					}
				}
		}
		return random(500, 800);
	}

	private boolean solved() {
		if (index == -1) {
			return false;
		}
		final RSInterface solver = interfaces.get(CHEST_INTERFACE_PARENT);
		if (solver != null && solver.isValid()) {
			final RSComponent container = solver.getComponent(CHEST_INTERFACE_CENTER);
			final Rectangle centerArea = container.getArea();
			final Point p1 = solver.getComponent(INTERFACE_SOLVE_IDS[index][0]).getCenter();
			final Point p2 = solver.getComponent(INTERFACE_SOLVE_IDS[index][1]).getCenter();
			final Point p3 = solver.getComponent(INTERFACE_SOLVE_IDS[index][2]).getCenter();
			return centerArea.contains(p1) && centerArea.contains(p2) && centerArea.contains(p3);
		}
		return false;
	}
}