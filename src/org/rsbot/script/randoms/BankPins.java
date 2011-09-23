package org.rsbot.script.randoms;

import org.rsbot.script.Random;
import org.rsbot.script.ScriptManifest;

@ScriptManifest(authors = {"Holo", "Gnarly", "Salty_Fish", "Pervy Shuya", "Doout"}, name = "BankPin", version = 3.0)
public class BankPins extends Random {

	@Override
	public boolean activateCondition() {
		return interfaces.get(13).isValid() || (interfaces.get(14).isValid() && interfaces.getComponent(14, 34).getText().contains("Yes, I really want a Bank PIN"));
	}

	void enterPin(String pin) {
		final int[] pinComponents = {6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
		int state = settings.getSetting(563);
		if (!interfaces.get(13).isValid() || !interfaces.get(759).isValid() || state == 4) {
			return;
		}
		String pinNumber = String.valueOf(pin.charAt(state));
		if (interfaces.getComponent(13, pinComponents[Integer.valueOf(pinNumber)]).doClick()) {
			sleep(800, 1600);
		}
	}

	@Override
	public int loop() {
		if (interfaces.get(14).isValid() && interfaces.getComponent(14, 34).getText().contains("Yes, I really want a Bank PIN")) {
			interfaces.getComponent(14, 34).doClick();
			return 500;
		} else {
			final String pin = account.getPin();
			if (pin == null || pin.length() != 4) {
				log.severe("You must add a bank pin to your account.");
				stopScript(false);
			}
			enterPin(pin);
			if (interfaces.get(211).isValid()) {
				interfaces.get(211).getComponent(3).doClick();
			} else if (interfaces.get(217).isValid()) {
				sleep(random(10500, 12000));
			}
		}
		return activateCondition() ? 500 : -1;
	}
}