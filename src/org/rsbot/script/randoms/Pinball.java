package org.rsbot.script.randoms;

import org.rsbot.script.Random;
import org.rsbot.script.ScriptManifest;
import org.rsbot.script.wrappers.RSComponent;
import org.rsbot.script.wrappers.RSObject;

/*
* Updated by Iscream(Feb 3, 10)
* Updated by Twistedmind(Feb 4, 10) Small camera turning issue...
* Updated by Iscream(Feb 5, 10)
* Updated by TwistedMind(Feb 7, '10) "What have you guys been smoking??? I cleaned the code and it worked again... Why atTile if there's atObject?"
*/
@ScriptManifest(authors = {"Aelin", "LM3", "IceCandle", "Taha", "Twistedmind", "Iscream"}, name = "Pinball", version = 2.7)

public class Pinball extends Random {

    private final int[] OBJ_PILLARS = {15000, 15002, 15004, 15006, 15008};

    @Override
    public boolean activateCondition() {
        return (game.isLoggedIn() && (getPillar() != null)) || (objects.getNearest(OBJ_PILLARS) != null) || (objects.getNearest(15007) != null);
    }


    private RSObject getPillar() {
        return objects.getNearest(OBJ_PILLARS);
    }

    private int getScore() {
        int IFACE_PINBALL=263;RSComponent score = interfaces.getComponent(IFACE_PINBALL, 1);
        try {
            return Integer.parseInt(score.getText().split(" ")[1]);
        } catch (java.lang.ArrayIndexOutOfBoundsException t) {
            return 10;
        }
    }

    @Override
    public int loop() {
        if (!activateCondition()) {
            return -1;
        }
        if (interfaces.canContinue()) {
            interfaces.clickContinue();
            return random(1000, 1200);
        }
        if (getMyPlayer().isMoving() || (getMyPlayer().getAnimation() != -1)) {
            return random(1000, 1600);
        }
        if (getScore() >= 10) {
            int OBJ_EXIT=15010;RSObject exit = objects.getNearest(OBJ_EXIT);
            if (exit != null) {
                if (calc.tileOnScreen(exit.getLocation()) && tiles.doAction(exit.getLocation(), "Exit")) {
                    sleep(random(2000, 2200));
                    exit.doAction("Exit");
                    return random(1000, 1200);
                } else {
                    camera.setCompass('s');
                    walking.walkTileOnScreen(exit.getLocation());
                    return random(1400, 1500);
                }
            }
        }
        if (objects.getNearest(OBJ_PILLARS) != null) {
            if (!calc.tileOnScreen(objects.getNearest(OBJ_PILLARS).getLocation())) {
                walking.walkTileOnScreen(objects.getNearest(OBJ_PILLARS).getLocation());
                return random(500, 600);
            }
            sleep(random(400, 500));
            objects.getNearest(OBJ_PILLARS).doAction("Tag");
            for (int i = 0; i < 2; i++) {
                if (getMyPlayer().getInteracting() != null) {
                    i = 0;
                }
                sleep(random(936, 1259));//Randomness ftw, I was bored :P
            }
            return random(1000, 1300);
        }
        return random(200, 400);
    }

}