package org.rsbot.script.randoms;

import org.rsbot.script.Random;
import org.rsbot.script.ScriptManifest;
import org.rsbot.script.wrappers.RSComponent;
import org.rsbot.script.wrappers.RSObject;

/**
 * Updated by Arbiter 9/22/10: Replaced tile clicking with model clicking. :)
 * Updated by Dunnkers 28-09-2011: Fixed a condition where the random is
 * completed, but the component still contains the text "Abyssal Service".
 * Updated by Dunnkers 1/10/2011; Fixed a problem that not clicking "Click here to continue" bug.
 */
@ScriptManifest(authors = {"Garrett"}, name = "LostAndFound", version = 1.1)
public class LostAndFound extends Random {
	
	private final static int appendN = 8995;
	private final static int appendE = 8994;
	private final static int appendS = 8997;
	private final static int appendW = 8996;
	private final static int setting = 531;

	private final static int[] allAppendages = {appendN, appendE, appendS, appendW};
	private final static int[] answerN = {32, 64, 135236, 67778, 135332, 34017, 202982, 101443, 101603, 236743, 33793, 67682, 135172,
			236743, 169093, 33889, 202982, 67714, 101539};
	private final static int[] answerE = {4, 6, 101474, 101473, 169124, 169123, 67648, 135301, 135298, 67651, 169121, 33827, 67652,
			236774, 101479, 33824, 202951};
	private final static int[] answerS = {4228, 32768, 68707, 167011, 38053, 230433, 164897, 131072, 168068, 65536, 35939, 103589,
			235718, 204007, 100418, 133186, 99361, 136357, 1057, 232547};
	private final static int[] answerW = {105571, 37921, 131204, 235751, 1024, 165029, 168101, 68674, 203974, 2048, 100451, 6144,
			39969, 69698, 32801, 136324};
	private final static int[][] allAnswers = {answerN, answerE, answerS, answerW};

	@Override
	public boolean activateCondition() {
		final RSComponent component = interfaces.getComponent(210, 1);
		return game.isLoggedIn() && 
				(objects.getNearest(allAppendages) != null || 
				component.containsText("Abyssal Service") &&
				component.containsText("apologises for the inconvenience"));
	}

	private int getOddAppendage() {
		final int answer = settings.getSetting(setting);
		try {
			for (int i = 0; i < allAnswers.length; i++) {
				for (int j = 0; j < allAnswers[i].length; j++) {
					if (answer == allAnswers[i][j]) {
						return allAppendages[i];
					}
				}
			}
		} catch (final Exception ignored) {
		}
		return allAppendages[random(0, allAppendages.length)];
	}

	@Override
	public int loop() {
		if (getMyPlayer().isMoving()) {
			return random(200, 300);
		}
		if (!activateCondition()) {
			return -1;
		}
		if (interfaces.clickContinue()) {
			return 100;
		}
		final int appendage = getOddAppendage();
		try {
			final RSObject obj = objects.getNearest(appendage);
			if (!obj.isOnScreen()) {
				if (walking.walkTileMM(obj.getLocation())) {
					sleep(500, 750);
				}
			} else {
				if (obj.interact("Operate")) {
					sleep(1000, 1500);
				}
			}
		} catch (final Exception ignored) {
		}
		return random(1000, 2000);
	}
}
