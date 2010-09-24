package org.rsbot.script.randoms;

import org.rsbot.event.events.ServerMessageEvent;
import org.rsbot.event.listeners.ServerMessageListener;
import org.rsbot.script.Random;
import org.rsbot.script.ScriptManifest;
import org.rsbot.script.wrappers.RSObject;
import org.rsbot.script.wrappers.RSTile;

import java.awt.*;

/**
 * @author Illusion + Pwnaz0r
 * 
 * Last Updated 9-23-10 Arbiter
 */
@ScriptManifest(authors = {"Arbiter", "illusion", "Pwnaz0r"}, name = "Pillory", version = 3.8)
public class Pillory extends Random implements ServerMessageListener {

    public int fail = 0;
    private final int GameInterface = 189;
    public boolean inCage = false;
    public RSTile myLoc;
    private String pilloryMessage = "Solve the Pillory";

    public RSTile South = new RSTile(2606, 3105);
    RSTile cagetiles[] = {new RSTile(2608, 3105), new RSTile(2606, 3105), new RSTile(2604, 3105), new RSTile(3226, 3407), new RSTile(3228, 3407), new RSTile(3230, 3407), new RSTile(2685, 3489), new RSTile(2683, 3489), new RSTile(2681, 3489)};

    @Override
    public boolean activateCondition() {
        if (!game.isLoggedIn()) {
            return false;
        }
        for (int i = 0; i < cagetiles.length; i++) {
            if (getMyPlayer().getLocation().equals(cagetiles[i])) {
                return true;
            }
        }
        if (!inCage) {
            inCage = interfaces.getComponent(372, 3).getText().contains(
                    "Solve the pillory");
        }
        if (!inCage) {
            inCage = interfaces.getComponent(372, 3).getText().contains(
                    "swinging");
        }
        return inCage;
    }

    public RSObject findMYObject(final int... ids) {
        // Changed to find the nearest, reachable!
        // fixed, lol, objects.getAt want a real xy not this one
        RSObject cur = null;
        int dist = -1;
        for (int x = 0; x < 104; x++) {
            for (int y = 0; y < 104; y++) {
                final RSObject[] objs = objects.getAllAt(new RSTile(x + game.getBaseX(), y + game.getBaseY()));
                if (objs.length > 0) {
                	RSObject o = objs[0];
                    boolean isObject = false;
                    for (final int id : ids) {
                        if (o.getID() == id) {
                            isObject = true;
                            break;
                        }
                    }
                    if (isObject) {
                        final int distTmp = calc.distanceTo(o.getLocation());
                        if (distTmp != -1) {
                            if (cur == null) {
                                dist = distTmp;
                                cur = o;
                            } else if (distTmp < dist) {
                                cur = o;
                                dist = distTmp;
                            }
                        }
                    }
                }
            }
        }
        return cur;
    }

    private int getKey() {
        int key = 0;
        log.info("\tKey needed :");
        switch (interfaces.get(GameInterface).getComponent(4).getModelID()) {
            case 9753:
                key = 9749;
                log.info("\t   Diamond");
                break;
            case 9754:
                key = 9750;
                log.info("\t   Square");
                break;
            case 9755:
                key = 9751;
                log.info("\t   Circle");
                break;
            case 9756:
                key = 9752;
                log.info("\t   Triangle");
                break;
        }
        if (interfaces.get(GameInterface).getComponent(5).getModelID() == key)
            return 1;
        if (interfaces.get(GameInterface).getComponent(6).getModelID() == key)
            return 2;
        if (interfaces.get(GameInterface).getComponent(7).getModelID() == key)
            return 3;
        return -1;
    }


    @Override
    public int loop() {
        camera.setAltitude(true);
        if (calc.distanceTo(South) <= 10) {
            camera.setAngle(180);
        } else {
            camera.setAngle(360);
        }
        if (fail > 20) {
            //log.info("Failed Wayyy to many Times..");
            //log.info("Report this to illusion so He can Fix the Pillory.");
            shutdown();
        }
        if (myLoc == null) {
            //log.info("getting Location..");
            myLoc = getMyPlayer().getLocation();
            //log.info(myLoc.getX() + "," + myLoc.getY());
            return random(1000, 2000);
        }
        if (!getMyPlayer().getLocation().equals(myLoc)) {
            //log.info(getMyPlayer().getLocation().toString());
            log.info("Solved It.");
            pilloryMessage = null;
            inCage = false;
            return -1;
        }
        if (!interfaces.get(GameInterface).isValid()) {
            final Point ObjectPoint = new Point(calc.tileToScreen(myLoc));
            final Point Lock = new Point((int) ObjectPoint.getX() + 10, (int) ObjectPoint.getY() - 30);
            mouse.click(Lock.x, Lock.y + random(0, 15), false);
            sleep(random(600, 800));
            if (menu.doAction("unlock")) {
                log.info("Successfully opened the lock!");
                return random(1000, 2000);
            } else {
                fail++;
            }
        }
        if (interfaces.get(GameInterface).isValid()) {
            final int key = getKey();
            log.info(String.valueOf(key));
            switch (key) {
                case 1:
                    mouse.click(interfaces.get(GameInterface).getComponent(5).getArea().getLocation().x + random(10, 13), interfaces.get(GameInterface).getComponent(5).getArea().getLocation().y + random(46, 65), true);
                    break;
                case 2:
                    mouse.click(interfaces.get(GameInterface).getComponent(6).getArea().getLocation().x + random(10, 13), interfaces.get(GameInterface).getComponent(6).getArea().getLocation().y + random(46, 65), true);
                    break;
                case 3:
                    mouse.click(interfaces.get(GameInterface).getComponent(7).getArea().getLocation().x + random(10, 13), interfaces.get(GameInterface).getComponent(7).getArea().getLocation().y + random(46, 65), true);
                    break;
                default:
                    log.info("Bad Combo?");
                    fail++;
                    break;
            }
            return random(1000, 1600);
        }
        return -1;
    }

    public void serverMessageRecieved(final ServerMessageEvent e) {
        final String serverString = e.getMessage();
        if (serverString.contains(pilloryMessage)) {
            inCage = true;
        }

    }

    public void shutdown() {
        System.exit(1);
    }
}