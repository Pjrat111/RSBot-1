package org.rsbot.script.randoms;

import org.rsbot.script.Random;
import org.rsbot.script.ScriptManifest;
import org.rsbot.script.wrappers.RSComponent;

@ScriptManifest(authors = {"Holo", "Gnarly"}, name = "Bank pins manager", version = 2.1)
public class BankPins extends Random {

    private String pin;

    @Override
    public boolean activateCondition() {
        pin = account.getPin();
        if (interfaces.get(13).isValid()) {
            if ((pin == null) || (pin.length() != 4)) {
                log.severe("You must add a bank pin to your account.");
                stopScript();
            } else {
                return true;
            }
        }
        return false;
    }

    public void enterCode(final String pin) {
        if (!interfaces.get(13).isValid())
            return;
        final RSComponent[] children = interfaces.get(13).getComponents();
        int state = 0;
        for (int i = 21; i < 25; i++) {
            if (children[i].containsText("?")) {
                state++;
            }
        }
        state = 4 - state;
        for (int i = 11; i < 21; i++) {
            if (children[i].containsText(pin.substring(state, state + 1))) {
            	children[i - 10].doClick();
                sleep(random(500, 1000));
                break;
            } else {
                if (random(0, 5) == 0) {
                    mouse.moveSlightly();
                }
            }
        }
    }

    @Override
    public int loop() {
        if (interfaces.get(14).isValid() || !interfaces.get(13).isValid()) {
        	interfaces.get(14).getComponent(3).doClick();
            return -1;
        }
        enterCode(pin);
        if (interfaces.get(211).isValid()) {
        	interfaces.get(211).getComponent(3).doClick();
        } else if (interfaces.get(217).isValid()) {
            sleep(random(10500, 12000));
        }
        return 500;
    }
}