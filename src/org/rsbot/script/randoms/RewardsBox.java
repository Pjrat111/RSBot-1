package org.rsbot.script.randoms;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import org.rsbot.gui.AccountManager;
import org.rsbot.script.Random;
import org.rsbot.script.ScriptManifest;
import org.rsbot.script.methods.Game.Tab;
import org.rsbot.script.wrappers.RSComponent;
import org.rsbot.script.wrappers.RSItem;

@ScriptManifest (authors = {"xMunch", "Emeleo"}, version = 2.0, name = "ImprovedRewardsBox")
public class RewardsBox extends Random {

    private Reward SELECTED_REWARD = null;
    private BufferedImage IMG = null;
    private Point[] XP_POINT = null;

    private interface Values {

        int INTERFACE_MAIN_BOX = 202;
        int INTERFACE_BOX_CONFIRM_BUTTON = 28;
        int INTERFACE_BOX_SELECTION = 15;
        int INTERFACE_BOX_SCROLLBAR = 24;
        int INTERFACE_XP_SELECTION = 1139;
        int INTERFACE_XP_CONFIRM_ID = 2;
        int BOX_ID = 14664;
        int REWARD_BOX_ID = 14197;
        int BOOK_KNOWLEDGE_ID = 11640;
        int LAMP_ID = 2528;
        int MYSTERY_BOX_ID = 6199;
        int ACTIVATION_ITEMS[] = {BOX_ID, BOOK_KNOWLEDGE_ID, LAMP_ID, MYSTERY_BOX_ID};
        Reward DEFAULT_REWARD = Reward.Cash, DEFAULT_XP_REWARD = Reward.Woodcutting;
    }

    private enum Reward {

        Cash, Runes, Coal, Essence, Ore, Bars, Gems,
        Herbs, Seeds, Charms, Surprise, Emote, Costume,
        Attack(30), Defence(32), Strength(31), Constitution(35), Range(33),
        Prayer(34), Magic(36), Cooking(46), Woodcutting(47), Fletching(41),
        Fishing(44), Firemaking(45), Crafting(40), Smithing(43), Mining(42),
        Herblore(38), Agility(37), Thieving(39), Slayer(49), Farming(51),
        Runecrafting(48), Hunter(50), Construction(52), Summoning(53),
        Dungeoneering(54);

        private Reward() {
            this(-1);
        }

        final int XP_SELECTION;

        private Reward(int id) {
            XP_SELECTION = id;
        }

        @Override
        public String toString() {
            return XP_SELECTION != -1 ? "xp item" : super.toString().toLowerCase();
        }

    }

    private enum State {

        OPEN_RANDOM, HANDLE_BOX, HANDLE_XP_REWARD, ERROR
    }

    private State getState() {
        if (interfaces.get(Values.INTERFACE_MAIN_BOX).isValid()) {
            return State.HANDLE_BOX;
        } else if (interfaces.get(Values.INTERFACE_XP_SELECTION).isValid()) {
            return State.HANDLE_XP_REWARD;
        } else if (inventoryContains(Values.ACTIVATION_ITEMS)) {
            return State.OPEN_RANDOM;
        }
        return State.ERROR;
    }

    private RSComponent getRewardInterface(Reward r) {
        RSComponent mainBox = interfaces.getComponent(Values.INTERFACE_MAIN_BOX, Values.INTERFACE_BOX_SELECTION);
        if (mainBox == null) {
            return null;
        }
        RSComponent[] selection = mainBox.getComponents();
        for (int i = 0; i < selection.length; i++) {
            if (selection[i].getText().toLowerCase().contains(r.toString())) {
                return selection[i - 6];
            }
        }
        return null;
    }

    private Color getColorAt(int x, int y) {
        if (IMG == null) {
            DISABLE_PAINT = true;
            sleep(100, 350);
            IMG = env.takeScreenshot(false);
            DISABLE_PAINT = false;
        }
        return new Color(IMG.getRGB(x, y));
    }

    private Point[] getXPPoint(Reward r) {
        ArrayList<Point> points = new ArrayList<Point>();
        final int red = 210;
        if (r.XP_SELECTION != -1 && interfaces.get(Values.INTERFACE_XP_SELECTION).isValid()) {
            Rectangle area = interfaces.getComponent(Values.INTERFACE_XP_SELECTION, r.XP_SELECTION).getArea();
            for (int x = area.x; x < area.width + area.x; x++) {
                for (int y = area.y; y < area.height + area.y; y++) {
                    if (getColorAt(x, y).getRed() <= red) {
                        points.add(new Point(x, y));
                    }
                }
            }
        } else if (interfaces.get(Values.INTERFACE_XP_SELECTION).isValid()) {
            return getXPPoint(Values.DEFAULT_XP_REWARD);
        }
        return points.isEmpty() ? null : points.toArray(new Point[points.size()]);
    }

    private Reward getSelectedReward() {
        String reward = account.getName() == null ? null : AccountManager.getReward(account.getName());
        if (reward != null) {
            for (Reward r : Reward.values()) {
                if (r.name().equalsIgnoreCase(reward) && (interfaces.get(Values.INTERFACE_MAIN_BOX).isValid() ? (getRewardInterface(r).isValid() || r.XP_SELECTION != -1) : true)) {
                    return r;
                }
            }
        }
        return Values.DEFAULT_REWARD;
    }

    private boolean scrollTo(Reward r) {
        return interfaces.scrollTo(getRewardInterface(r), interfaces.get(Values.INTERFACE_MAIN_BOX).getComponent(Values.INTERFACE_BOX_SCROLLBAR));
    }

    @Override
    public boolean activateCondition() {
        return game.isLoggedIn() && !bank.isOpen() && !getMyPlayer().isInCombat() && inventoryContains(Values.ACTIVATION_ITEMS);
    }

    public boolean inventoryContains(int... ids) {
        for (RSItem i : inventory.getItems(true)) {
            for (int a : ids) {
                if (i.getID() == a) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected int loop() {
        if (!activateCondition()) {
            return -1;
        }
        if (SELECTED_REWARD == null) {
            SELECTED_REWARD = getSelectedReward();
            log.info("Selecting " + SELECTED_REWARD.name() + " as reward.");
        }
        game.openTab(Tab.INVENTORY, random(0, 5) == 3);
        switch (getState()) {
            case OPEN_RANDOM:
                RSItem item = inventory.getItem(Values.ACTIVATION_ITEMS);
                return item != null && item.doClick(true) ? random(3000, 3500) : 10;
            case HANDLE_BOX:
                /** validate selected reward*/
                for (final RSComponent child : interfaces.get(137).getComponents()) {
                    if (!SELECTED_REWARD.equals(Reward.Emote)) {
                        break;
                    }
                    if (child.containsText("You've already unlocked") && child.containsText("emotes")
                            && !child.containsText("<col=0000ff>")) {
                        SELECTED_REWARD = Values.DEFAULT_REWARD;
                        break;
                    }
                }
                /** scroll to reward interface */
                if (scrollTo(SELECTED_REWARD)) {
                    sleep(300, 700);
                }
                RSComponent REWARD_INTERFACE = getRewardInterface(SELECTED_REWARD),
                 CONFIRM = interfaces.getComponent(Values.INTERFACE_MAIN_BOX, Values.INTERFACE_BOX_CONFIRM_BUTTON);
                if (REWARD_INTERFACE == null || CONFIRM == null) {
                    return 10;
                }
                /** Click reward & Confirm */
                return REWARD_INTERFACE.doClick() && CONFIRM.doClick() ? random(500, 1500) : 10;
            case HANDLE_XP_REWARD:
                if (XP_POINT != null && XP_POINT.length > 0) {
                    mouse.click(XP_POINT[random(0, XP_POINT.length)], true);
                    sleep(100, 350);
                    return interfaces.getComponent(Values.INTERFACE_XP_SELECTION, Values.INTERFACE_XP_CONFIRM_ID).doClick() ? random(1900, 3500) : 10;
                } else {
                    XP_POINT = getXPPoint(SELECTED_REWARD);
                }
                return 100;
            case ERROR:
                return 100;
        }
        return -1;
    }

    @Override
    public void onFinish() {
        DISABLE_PAINT = false;
        SELECTED_REWARD = null;
    }

}