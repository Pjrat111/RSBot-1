package org.rsbot.script.randoms;

import org.rsbot.script.Random;
import org.rsbot.script.ScriptManifest;

import java.util.HashMap;
import java.util.Map;

/**
 * Closes interfaces that scripts may open by mistake.
 */
@ScriptManifest(authors = {"Jacmob", "HeyyamaN", "Pervy Shuya"}, name = "InterfaceCloser", version = 1.9)
public class CloseAllInterface extends Random {
	private final Map<Integer, Integer> components = new HashMap<Integer, Integer>();

	{
		components.put(14, 11); // Pin settings
		components.put(17, 13); // Death items
		components.put(18, 37); // "Get Ready to Respawn" Confirm button.
		//components.put(109, 14); // Grand exchange collection
		components.put(149, 226); //Level 10 membership solicitation
		components.put(157, 13); // Quick chat help
		components.put(206, 13); // Price check
		components.put(266, 1); // Tombstone
		components.put(266, 11); // Grove
		components.put(275, 8); // Quest
		components.put(276, 76); // Soul Wars Rewards
		components.put(499, 29); // Stats
		components.put(594, 48); // Report Abuse
		components.put(667, 75); // Equipment Bonus
		components.put(732, 208); // Fist of Guthx Reward Shop
		components.put(742, 18); // Graphic
		components.put(743, 20); // Audio
		components.put(755, 44); // World Map
		components.put(764, 18); // Objectives
		components.put(767, 10); // Bank of RuneScape - Help
		components.put(895, 19); // Advisor
		components.put(917, 73); // Task List
		components.put(1011, 51); // Pest Control Rewards ( Commendation Rewards )
		components.put(1083, 181); // Livid Farm Rewards
		components.put(1107, 174); // Clan Vexillum
		components.put(1127, 16); //Hybrid Gloves selection ( Fist of Guthix )
	}

	@Override
	public boolean activateCondition() {
		if (game.isLoggedIn()) {
			for (Map.Entry<Integer, Integer> c : components.entrySet()) {
				if (interfaces.getComponent(c.getKey(), c.getValue()).isValid()) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public int loop() {
		sleep(random(500, 900));
		for (final Map.Entry<Integer, Integer> c : components.entrySet()) {
			if (interfaces.getComponent(c.getKey(), c.getValue()).isValid()) {
				if (interfaces.getComponent(c.getKey(), c.getValue()).doClick()) {
					sleep(random(900, 1100));
					if (random(0, 4) == 0) {
						mouse.moveSlightly();
					}
					break;
				}
			}
		}
		return -1;
	}
}