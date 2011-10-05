package org.rsbot.script.randoms;

import org.rsbot.script.Random;
import org.rsbot.script.ScriptManifest;
import org.rsbot.script.methods.Game;
import org.rsbot.script.wrappers.*;

/*
 * Updated by Arbiter (Oct 19, 2010)
 * Updated by Jacmob (Oct 22, 2010)
 * Updated by Jacmob (Oct 24, 2010)
 */
@ScriptManifest(authors = "Arbiter", name = "ScapeRuneIsland", version = 2.2)
public class ScapeRuneIsland extends Random {

	private final int[] STATUE_IDS = {8992, 8993, 8990, 8991};
	private final RSTile CENTER_TILE = new RSTile(3421, 4777);

	private RSObject direction;
	private boolean finished;
	private boolean fishing;
	private boolean forceTalk;

	@Override
	public boolean activateCondition() {
		return calc.distanceTo(CENTER_TILE) < 50;
	}

	@Override
	public void onFinish() {
		direction = null;
		finished = false;
		fishing = false;
		forceTalk = false;
	}

	@Override
	public int loop() {
		final RSObject statue1 = objects.getNearest(STATUE_IDS[0]);
		final RSObject statue2 = objects.getNearest(STATUE_IDS[1]);
		final RSObject statue3 = objects.getNearest(STATUE_IDS[2]);
		final RSObject statue4 = objects.getNearest(STATUE_IDS[3]);
		final RSObject[] statues = {statue1, statue2, statue3, statue4};
		if (!activateCondition()) {
			return -1;
		}
		if (getMyPlayer().isMoving() || getMyPlayer().getAnimation() != -1) {
			return random(100, 200);
		}
		if (interfaces.getComponent(241, 4).isValid() && interfaces.getComponent(241, 4).getText().contains("catnap") ||
				interfaces.getComponent(64, 4).isValid() && interfaces.getComponent(64, 4).getText().contains("fallen asleep")) {
			finished = true;
		}
		if (interfaces.getComponent(242, 4).isValid() && interfaces.getComponent(242, 4).getText().contains("Wait! Before")) {
			forceTalk = true;
		}
		if (interfaces.clickContinue()) {
			return random(500, 1000);
		}
		if (forceTalk) {
			RSNPC servant = npcs.getNearest(2481);
			if (servant != null && direction == null && settings.getSetting(344) == 0) {
				if (!calc.tileOnScreen(servant.getLocation())) {
					walking.walkTileMM(servant.getLocation());
					return 700;
				}
				if (servant.interact("Talk-to")) {
					forceTalk = false;
				}
				return random(1000, 2000);
			}
			if (servant == null) {
				servant = npcs.getNearest(2481);
				if (servant == null) {
					walking.walkTileMM(CENTER_TILE);
					return random(1000, 2000);
				}
				return random(50, 100);
			}
		}
		if (finished) {
			final RSObject portal = objects.getNearest(8987);
			if (portal != null) {
				if (!calc.tileOnScreen(portal.getLocation())) {
					walking.walkTileMM(portal.getLocation());
					return random(500, 1000);
				} else {
					if (portal.interact("Enter")) {
						return random(6000, 7000);
					}
					return random(500, 1000);
				}
			} else {
				walking.walkTileMM(CENTER_TILE);
			}
		}
		if (bank.isDepositOpen() && bank.getBoxCount() - bank.getBoxCount(6209, 6202, 6200) >= 27) {
			final RSComponent randomItem = interfaces.getComponent(11, 17).getComponent(random(16, 26));
			final int randomID = randomItem.getComponentID();
			if (randomID < 0) {
				return random(50, 100);
			}
			if (bank.deposit(randomID, 0)) {
				log("Item with ID " + randomID + " was deposited.");
				return random(500, 1000);
			}
			return random(50, 100);
		}
		if (bank.isDepositOpen() && bank.getBoxCount() - bank.getBoxCount(6209, 6202, 6200) < 27) {
			bank.close();
			return random(500, 1000);
		}
		if (inventory.getCountExcept(6209, 6202, 6200) >= 27) {
			bank.openDepositBox();
			return random(500, 1000);
		}
		if (inventory.contains(6202)) {
			final RSObject pot = objects.getNearest(8985);
			if (pot != null) {
				if (!calc.tileOnScreen(pot.getLocation())) {
					walking.walkTileMM(pot.getLocation());
					return random(400, 800);
				}
				if (useItem(inventory.getItem(6202), pot)) {
					sleep(1200, 1600);
				}
				return random(2000, 2400);
			} else {
				walking.walkTileMM(CENTER_TILE);
			}
		}
		if (fishing && inventory.getCount(6209) == 0) {
			final RSGroundItem net = groundItems.getNearest(6209);
			if (net != null) {
				if (!calc.tileOnScreen(net.getLocation())) {
					walking.walkTileMM(net.getLocation());
					return random(800, 1000);
				} else {
					if (net.interact("Take")) {
						return random(800, 1000);
					}
					return 200;
				}
			} else {
				walking.walkTileMM(CENTER_TILE);
			}
		}
		if (interfaces.getComponent(246, 5).containsText("contains") && settings.getSetting(334) == 1 && direction == null) {
			sleep(2000);
			log("Checking direction");
			for (RSObject statue : statues) {
				if (statue != null) {
					if (statue.isOnScreen()) {
						direction = statue;
						fishing = true;
						break;
					}
				}
			}
			return random(2000, 3000);
		}
		if (direction != null && inventory.getCount(6200) < 1) {
			sleep(1000, 1200);
			if (!calc.tileOnScreen(direction.getLocation())) {
				walking.walkTileMM(direction.getLocation());
				return random(400, 600);
			}
			final RSObject spot = objects.getNearest(8986);
			if (spot != null) {
				if (!calc.tileOnScreen(spot.getLocation())) {
					camera.turnTo(spot.getLocation());
				}
				if (!calc.tileOnScreen(spot.getLocation()) && walking.walkTileMM(spot.getLocation())) {
					sleep(1000, 2000);
					if (!calc.tileOnScreen(spot.getLocation())) {
						sleep(1000);
					}
				}
				if (spot.interact("Net")) {
					return random(2000, 2500);
				}
				return 400;
			} else {
				walking.walkTileMM(CENTER_TILE);
			}
		}
		if (inventory.getCount(6200) > 0) {
			final RSNPC cat = npcs.getNearest(2479);
			if (cat != null) {
				if (!calc.tileOnScreen(cat.getLocation())) {
					walking.walkTileMM(cat.getLocation());
					return 500;
				}
				if (useItem(inventory.getItem(6200), cat)) {
					return random(2300, 3000);
				}
				return 500;
			} else {
				walking.walkTileMM(CENTER_TILE);
			}
			return random(1900, 2200);
		}
		RSNPC servant = npcs.getNearest(2481);
		if (servant != null && direction == null && settings.getSetting(344) == 0) {
			if (!calc.tileOnScreen(servant.getLocation())) {
				walking.walkTileMM(servant.getLocation());
				return 700;
			}
			servant.interact("Talk-to");
			return random(1000, 2000);
		}
		if (servant == null) {
			servant = npcs.getNearest(2481);
			if (servant == null) {
				walking.walkTileMM(CENTER_TILE);
				return random(1000, 2000);
			}
			return random(50, 100);
		}
		log("Setting 344: " + settings.getSetting(344));
		return random(800, 1200);
	}

	public boolean useItem(final RSItem item, final RSObject targetObject) {
		game.openTab(Game.Tab.INVENTORY);
		return inventory.selectItem(item) && targetObject.interact("Use " + item.getName() + " -> " + targetObject.getName());
	}

	public boolean useItem(final RSItem item, final RSNPC targetNPC) {
		game.openTab(Game.Tab.INVENTORY);
		return inventory.selectItem(item) && targetNPC.interact("Use " + item.getName() + " -> " + targetNPC.getName());
	}
}
