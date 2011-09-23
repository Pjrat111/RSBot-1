package org.rsbot.script.methods;

import org.rsbot.gui.AccountManager;
import org.rsbot.script.wrappers.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Obtains information on tradeable items from the Grand Exchange website and
 * Grand Exchange in-game interaction.
 *
 * @author Javaskill
 * @author Aion
 * @author Boolean
 * @author Debauchery
 * @author kyleshay
 */
public class GrandExchange extends MethodProvider {
	private static final String HOST = "http://services.runescape.com";
	private static final String GET = "/m=itemdb_rs/viewitem.ws?obj=";
	private final static String WIKIHOST = "http://runescape.wikia.com/wiki/";
	private final static String WIKIBASE = "Exchange:";

	public static final int INTERFACE_GRAND_EXCHANGE = 105;
	public static final int INTERFACE_BUY_SEARCH_BOX = 389;
	public static final int[] INTERFACE_GRAND_EXCHANGE_BUY_BUTTON = {31, 47, 63, 82, 101, 120};
	public static final int[] INTERFACE_GRAND_EXCHANGE_SELL_BUTTON = {32, 48, 64, 83, 102, 121};
	public static final int[] INTERFACE_GRAND_EXCHANGE_OFFER_BOXES = {19, 35, 51, 67, 83, 99};

	public static final int GRAND_EXCHANGE_COLLECT_BOX_ONE = 206;
	public static final int GRAND_EXCHANGE_COLLECT_BOX_TWO = 208;

	public static final int[] GRAND_EXCHANGE_CLERK = {6528, 6529,
			1419, 2240, 2241, 2593};

	GrandExchange(final MethodContext ctx) {
		super(ctx);
	}

	/**
	 * Gets the Grand Exchange interface.
	 *
	 * @return The grand exchange <code>RSInterface</code>.
	 */
	public RSInterface getInterface() {
		return methods.interfaces.get(INTERFACE_GRAND_EXCHANGE);
	}

	/**
	 * Checks Grand Exchange slot and returns ID
	 *
	 * @param slot The slot to check
	 * @return The item name as a string equal to the item being sold/brought
	 *         Will return null if no items are being sold.
	 */
	public String checkSlot(final int slot) {
		try {
			final int slotComponent = INTERFACE_GRAND_EXCHANGE_OFFER_BOXES[slot];
			if (isOpen()) {
				if (methods.interfaces.getComponent(INTERFACE_GRAND_EXCHANGE, slotComponent).getComponent(10).isValid()) {
					return methods.interfaces.getComponent(INTERFACE_GRAND_EXCHANGE, slotComponent).getComponent(10).getText();
				} else {
					return null;
				}
			} else {
				return null;
			}
		} catch (final Exception e) {
			return null;
		}
	}

	/**
	 * Checks Grand Exchange slots for an item.
	 *
	 * @param name The name of the item to check for.
	 * @return An int of the corresponding slot.
	 *         0 = Not found.
	 */
	public int findItem(final String name) {
		for (int i = 1; i <= 6; i++) {
			if (isOpen()) {
				if (checkSlotIsEmpty(i)) {
					final int slotComponent = INTERFACE_GRAND_EXCHANGE_OFFER_BOXES[i];
					final String s = methods.interfaces.getComponent(INTERFACE_GRAND_EXCHANGE, slotComponent).getComponent(18).getText();
					if (s.equals(name)) {
						return i;
					}
				}
			}
		}
		return 0;
	}

	/**
	 * Finds first empty slot.
	 *
	 * @return An int of the corresponding slot.
	 *         0 = No empty slots.
	 */
	public int freeSlot() {
		for (int i = 1; i <= 6; i++) {
			if (checkSlotIsEmpty(i)) {
				return i;
			}
		}
		return 0;
	}

	public final RSArea GE_AREA = new RSArea(3157, 3477, 3176, 3501);
	public final RSTile GE_TILE = new RSTile(3164, 3484);

	/**
	 * Closes the GE interface
	 *
	 * @return True if the G.E. was Closed
	 */
	public boolean closeGe() {
		if (isOpen()) {
			RSComponent exit = methods.interfaces.get(105).getComponent(14);
			return exit.doClick();
		}
		return true;
	}

	/**
	 * Sells an item, at the price of 5% * upButtonNumber + normalPrice
	 *
	 * @param item           The RSItem to sell
	 * @param quantity       The quantity to Sell CAN CONTAIN "m", "k", and "b"
	 * @param upButtonNumber The amount of times to press the %5 up button
	 * @return If the item was sold.
	 */
	public boolean sellUp(RSItem item, String quantity, int upButtonNumber) {
		boolean success = false;
		int slot;
		if (item == null) {
			return false;
		}
		if (!isOpen()) {
			open();
			sleep(random(450, 750));
		}
		if (isOpen()) {
			if (isBuyOpen()) {
				clickBackButton();
			}
			for (int i = 0; i < 4 && !isSellOpen(); i++) {
				slot = getFree();
				if (slot == -1) {
					return false;
				}
				openSell(slot);
				sleep(random(350, 650));
			}
			if (isSellOpen()) {
				while (!isSellItemChosen()) {
					if (item.doClick(true)) {
						sleep(random(350, 650));
					} else {
						return false;
					}
				}
				if (isSellItemChosen()) {
					if (quantity.equals("0")) {
						sellAll();
						sleep(random(250, 500));
					}
					if (!quantity.equals("1") || !quantity.equals("0")) {
						setQuantity(quantity);
						sleep(random(250, 500));
					}
					if (upButtonNumber > 0) {
						for (int i = 0; i < upButtonNumber; i++) {
							fivePercentUp();
						}
					}
					success = clickConfirm();
				}
			}
		}
		return success;
	}

	/**
	 * Sells an item, at the price of 5% * downButtonNumer + normalPrice
	 *
	 * @param item             The RSItem to sell
	 * @param quantity         The quantity to Sell CAN CONTAIN "m", "k", and "b"
	 * @param downButtonNumber The amount of times to press the %5 up button
	 * @return True on Success
	 */
	public boolean sellDown(RSItem item, String quantity, int downButtonNumber) {
		boolean success = false;
		int slot;
		if (item == null || item.getID() == 0 || !methods.inventory.contains(item.getID())) {
			return false;
		}
		if (!isOpen()) {
			open();
			sleep(random(450, 750));
		}
		sleep(random(900, 1300));
		if (isOpen()) {
			if (isBuyOpen()) {
				clickBackButton();
			}
			for (int i = 0; i < 4 && !isSellOpen(); i++) {
				slot = getFree();
				if (slot == -1) {
					return false;
				}
				slot = getFree();
				openSell(slot);
				sleep(random(350, 650));
			}
			if (isSellOpen()) {
				while (!isSellItemChosen() && isSellOpen()) {
					if (methods.inventory.contains(item.getID())) {
						item.doClick(true);
					} else {
						return false;
					}
					sleep(random(350, 650));
				}
				if (isSellItemChosen()) {
					if (quantity.equals("0")) {
						sellAll();
						sleep(random(250, 500));
					}
					if (!quantity.equals("0")) {
						setQuantity(quantity);
						sleep(random(250, 500));
					}
					if (downButtonNumber > 0) {
						for (int i = 0; i < downButtonNumber; i++) {
							fivePercentDown();
							sleep(random(350, 650));
						}
					}
					success = clickConfirm();
				}
			}
		}

		return success;
	}

	/**
	 * Sells an item at a specific price
	 *
	 * @param item     The RSItem to sell
	 * @param quantity The quantity to Sell CAN CONTAIN "m", "k", and "b"
	 * @param price    The price to sell the item at
	 * @return True on Success
	 */
	public boolean sellItemAt(RSItem item, String quantity, String price) {
		boolean success = false;
		int slot;
		if (item == null || item.getID() == 0) {
			return false;
		}
		if (!isOpen()) {
			open();
			sleep(random(450, 750));
		}
		if (isOpen()) {
			if (isBuyOpen()) {
				clickBackButton();
			}
			for (int i = 0; i < 4 && !isSellOpen(); i++) {
				slot = getFree();
				if (slot == -1) {
					return false;
				}
				openSell(slot);
				sleep(random(350, 650));
			}
			if (isSellOpen()) {
				while (!isSellItemChosen()) {
					if (methods.inventory.contains(item.getID())) {
						item.doClick(true);
					} else {
						return false;
					}
					sleep(random(350, 650));
				}
				if (isSellItemChosen()) {
					if (quantity.equals("0")) {
						sellAll();
						sleep(random(250, 500));
					}
					if (!quantity.equals("0")) {
						setQuantity(quantity);
						sleep(random(350, 500));
					}
					setPrice(price);
					sleep(random(450, 650));
					success = clickConfirm();
				}
			}
		}

		return success;
	}

	/**
	 * Buys an item, at the price of 5% * upButtonNumber + normalPrice
	 *
	 * @param item           The itemID to search for
	 * @param quantity       The quantity to Sell CAN CONTAIN "m", "k", and "b"
	 * @param upButtonNumber The amount of times to press the %5 up button
	 * @return True on Success
	 */
	public boolean buyUp(int item, String quantity, int upButtonNumber) {
		boolean success = false;
		int slot;
		if (!isOpen()) {
			open();
			sleep(random(450, 750));
		}
		if (isOpen()) {
			if (isSellOpen()) {
				clickBackButton();
			}
			for (int i = 0; i < 4 && !isBuyOpen(); i++) {
				slot = getFree();
				if (slot == -1) {
					return false;
				}
				openBuy(slot);
				sleep(random(350, 650));
			}
			if (isBuyOpen()) {
				boolean skip = false;
				for (int i = 0; i < 4 && !skip; i++) {
					if (chooseBuyingItem(getItemName(item))) {
						skip = true;
						sleep(random(350, 650));
					}
				}
				if (skip) {
					if (quantity.equals("1") && quantity.equals("0")) {
						setQuantity(quantity);
						sleep(random(250, 500));
					}
					if (upButtonNumber > 0) {
						for (int i = 0; i < upButtonNumber; i++) {
							fivePercentUp();
						}
					}
					success = clickConfirm();
				}
			}
		}
		return success;
	}

	/**
	 * Buys an item, at the price of (normal - 5% * downButtonNumber)
	 *
	 * @param item             The itemID to search for
	 * @param quantity         The quantity to Sell CAN CONTAIN "m", "k", and "b"
	 * @param downButtonNumber The amount of times to press the %5 down button
	 * @return True on Success
	 */
	public boolean buyDown(int item, String quantity, int downButtonNumber) {
		boolean success = false;
		int slot;
		if (!isOpen()) {
			open();
			sleep(random(450, 750));
		}
		if (isOpen()) {
			if (isSellOpen()) {
				clickBackButton();
			}
			for (int i = 0; i < 4 && !isBuyOpen(); i++) {
				slot = getFree();
				if (slot == -1) {
					return false;
				}
				openBuy(slot);
				sleep(random(350, 650));
			}
			if (isBuyOpen()) {
				boolean skip = false;
				for (int i = 0; i < 4 && !skip; i++) {
					if (chooseBuyingItem(getItemName(item))) {
						skip = true;
						sleep(random(350, 650));
					}
				}
				if (skip) {
					if (!quantity.equals("0")) {
						setQuantity(quantity);
						sleep(random(250, 500));
					}
					if (downButtonNumber > 0) {
						for (int i = 0; i < downButtonNumber; i++) {
							fivePercentDown();
						}
					}
					success = clickConfirm();
				}
			}
		}
		return success;
	}

	/**
	 * Buys an item at a specific price
	 *
	 * @param item     The item to buy
	 * @param quantity The amount to buy
	 * @param price    The price to buy at
	 * @return If the item was bought.
	 */
	public boolean buyItemAt(int item, String quantity, String price) {
		boolean success = false;
		int slot;
		for (int i = 0; i < 4 && !isOpen(); i++) {
			open();
			while (methods.players.getMyPlayer().isMoving()) {
				sleep(random(750, 950));
			}
			sleep(random(650, 1100));
		}
		if (isOpen()) {
			if (isSellOpen()) {
				clickBackButton();
				sleep(random(250, 350));
			}
			for (int i = 0; i < 4 && !isBuyOpen(); i++) {
				slot = getFree();
				if (slot == -1) {
					return false;
				}
				slot = getFree();
				openBuy(slot);
				sleep(random(350, 650));
			}
			if (isBuyOpen()) {
				boolean skip = false;
				for (int i = 0; i < 4 && !skip; i++) {
					if (chooseBuyingItem(getItemName(item))) {
						skip = true;
						sleep(random(350, 650));
					}
				}
				if (skip) {
					if (!quantity.equals("0")) {
						sleep(random(450, 650));
						setQuantity(quantity);
						sleep(random(450, 500));
					}
					sleep(random(350, 500));
					setPrice(price);
					sleep(random(200, 350));
					success = clickConfirm();
				}
			}
		}

		return success;
	}

	/**
	 * Searches the G.E. for the item
	 *
	 * @param item The item to search for
	 * @return True if the item was found, and clicked
	 */
	public boolean searchItem(int item) {
		return chooseBuyingItem(getItemName(item));
	}

	private boolean chooseBuyingItem(String item) {
		int infID = -1;
		if (!methods.interfaces.getComponent(105, 142).getText().equalsIgnoreCase(item)) {
			if (methods.interfaces.getComponent(105, 189).doClick()) {
				sleep(random(2400, 2800));
				methods.keyboard.sendText(item, false);
				sleep(random(1500, 2200));
				RSComponent[] components = methods.interfaces.getComponent(389, 4).getComponents();
				for (int i = 1; i < components.length; i++) {
					if (components[i].getText().equalsIgnoreCase(item)) {
						infID = i;
						break;
					}
				}
				if (infID != -1) {
					if (methods.interfaces.getComponent(389, 4).getComponent(infID).doClick()) {
						sleep(random(900, 1700));
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Checks if the item under the name itemName is selected
	 *
	 * @param itemName The name of the item to check
	 * @return True if the selected Item's name is equal to itemName
	 */
	public boolean isItemSelected(String itemName) {
		RSComponent c = getInterface().getComponent(142);
		return isOpen() && c.isValid() && isSellOpen() && !c.getText().contains(itemName);
	}

	/**
	 * Clicks the Choose Item button
	 *
	 * @return True on success
	 */
	public boolean clickChooseItem() {
		RSComponent c = getInterface().getComponent(139);
		return c != null && isOpen() && c.doClick();
	}

	/**
	 * Checks for the amount of completed slots, and then collects items.
	 */
	public void collectIfFinished() {
		if (!isOpen()) {
			open();
		}
		int numberOfBoxes = 2;
		if (AccountManager.isMember(methods.account.getName())) {
			numberOfBoxes = 6;
		}
		for (int i = 1; i <= numberOfBoxes; i++) {
			if (!checkSlotIsEmpty(i)) {
				openBox(i);
				if (!methods.interfaces.getComponent(105, 200).containsAction("Abort Offer")) {
					sleep(random(450, 650));
					getItem();
				} else {
					clickBackButton();
				}
			}
		}
	}

	/**
	 * Checks Grand Exchange slots for an any activity (1-6)
	 *
	 * @param slot An int for the corresponding slot.
	 * @return <tt>True</tt> if the slot is free from activity.
	 */
	public boolean checkSlotIsEmpty(final int slot) {
		try {
			final int slotComponent = INTERFACE_GRAND_EXCHANGE_OFFER_BOXES[slot - 1];
			return isOpen() && methods.interfaces.getComponent(105, slotComponent).getComponent(10).containsText("Empty");
		} catch (final Exception e) {
			return false;
		}
	}

	/**
	 * Checks if a specific slot is complete.
	 *
	 * @param slot The slot to check.
	 * @return True if the slot is finish
	 */
	public boolean isSlotFinished(int slot) {
		if (slot == 0) {
			return false;
		}
		final int slotComponent = INTERFACE_GRAND_EXCHANGE_OFFER_BOXES[slot - 1];
		return !methods.interfaces.getComponent(105, slotComponent).containsAction("Abort Offer");
	}

	/**
	 * Opens a specific finished slot
	 *
	 * @param slot The slot number to open.
	 * @return True on success
	 */
	public boolean openBox(int slot) {
		boolean success = false;
		RSComponent c = methods.interfaces.get(105).getComponent(
				INTERFACE_GRAND_EXCHANGE_OFFER_BOXES[slot - 1]);
		if (c != null) {
			if (c.doClick(true)) {
				success = true;
			}
			sleep(random(250, 500));
		}
		return success;
	}

	/**
	 * Collects items in the open interface
	 */
	public void getItem() {
		int number = 0;
		if (!isOpen()) {
			open();
		}
		if (isOpen()) {
			sleep(random(700, 1200));
			RSComponent i = methods.interfaces.get(105).getComponent(
					GRAND_EXCHANGE_COLLECT_BOX_TWO);
			try {
				if (i != null) {
					if (i.isValid()) {
						if (i.containsAction("Collect")) {
							i.doClick();
							sleep(random(400, 900));
						} else {
							number += 1;
						}
					}
				}
			} catch (Exception ignored) {
			}
			RSComponent c = methods.interfaces.get(105).getComponent(GRAND_EXCHANGE_COLLECT_BOX_ONE);
			try {
				if (c != null) {
					if (c.isValid()) {
						if (c.containsAction("Collect")) {
							c.doClick();
							sleep(random(400, 900));
							return;
						} else {
							number += 1;
						}
					}
				}
			} catch (Exception ignored) {
			}
		}
		if (number == 2) {
			clickBackButton();
			return;
		}
		if (isSellOpen() || isBuyOpen()) {
			clickBackButton();
		}
	}

	/**
	 * Detects if you an item has been selected to sell
	 *
	 * @return True if an Item has been selected to be sold
	 */
	public boolean isSellItemChosen() {
		RSComponent c = getInterface().getComponent(142);
		return isOpen() && c.isValid() && isSellOpen() && !c.getText().contains("Choose an item");
	}

	/**
	 * Detects if you an item has been selected to Buy
	 *
	 * @return True if an Item has been selected to be sold
	 */
	public boolean isBuyItemChosen() {
		RSComponent c = methods.interfaces.getComponent(105).getComponent(142);
		return isOpen() && isBuyOpen() && !c.getText().contains("Choose an item");
	}

	/**
	 * Opens A specific Buy Slot
	 *
	 * @param slot The slot number numbers(1 - 6)
	 * @return True on Click
	 */
	public boolean openBuy(final int slot) {
		return openSlot(slot, true);
	}

	/**
	 * Opens A specific Sell Slot
	 *
	 * @param slot The slot number (1 - 6)
	 * @return True on Click
	 */
	public boolean openSell(final int slot) {
		return openSlot(slot, false);
	}

	/**
	 * Opens A specific Buy or Sell Slot
	 *
	 * @param slot The slot number numbers(1 - 6)
	 * @param buy  True to open Buy slot, False to open Sell Slot
	 * @return True on Click
	 */
	private boolean openSlot(final int slot, boolean buy) {
		if (!isOpen()) {
			return false;
		}
		int c = buy ? INTERFACE_GRAND_EXCHANGE_BUY_BUTTON[slot - 1] : INTERFACE_GRAND_EXCHANGE_SELL_BUTTON[slot - 1];
		return buy ?
				methods.interfaces.getComponent(105, c).interact("Make Buy Offer") :
				methods.interfaces.getComponent(105, c).interact("Make Sell Offer");
	}

	/**
	 * Calculates if the slot is free
	 *
	 * @param slot The G.E. Slot, 1-6
	 * @return True if the slot was opened
	 */
	public boolean isFree(int slot) {
		final int slotComponent = INTERFACE_GRAND_EXCHANGE_OFFER_BOXES[slot - 1];
		return isOpen() && methods.interfaces.getComponent(105, slotComponent).getComponent(10).containsText("Empty");
	}

	/**
	 * Gets the first free slot.
	 *
	 * @return The Slot number(1-6), -1 if none is free or if interface is
	 *         not open
	 */
	public int getFree() {
		int maxSlots = 6;
		if (!AccountManager.isMember(methods.account.getName())) {
			maxSlots = 2;
		}
		for (int i = 1; i <= maxSlots; i++) {
			if (isFree(i)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Sets the quantity
	 *
	 * @param quantity The amount
	 */
	public void setQuantity(String quantity) {
		RSComponent c = getInterface().getComponent(168);
		if (c != null) {
			if (c.doClick()) {
				sleep(random(1250, 1550));
				methods.keyboard.sendText(quantity, true);
				sleep(random(650, 850));
			}
		}
	}

	/**
	 * Calculates if the G.E. is open
	 *
	 * @return True if the G.E. is open
	 */
	public boolean isOpen() {
		try {
			RSInterface[] interfacesI = methods.interfaces.getAll();
			for (RSInterface i : interfacesI) {
				if (i != null) {
					if (i.getIndex() == 105) {
						return i.isValid();
					}
				}
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Opens the GE interface
	 *
	 * @return True if the GE was opened
	 */
	public boolean open() {
		if (isOpen()) {
			return true;
		}
		RSNPC clerk = methods.npcs.getNearest(GRAND_EXCHANGE_CLERK);
		if (clerk != null) {
			if (clerk.isOnScreen()) {
				clerk.interact("Exchange " + clerk.getName());
			} else {
				while (!clerk.isOnScreen()) {
					methods.walking.walkTo(clerk.getLocation());
				}
				return open();
			}
		} else {
			while (!atGe()) {
				walkTo();
			}
			RSNPC npc = methods.npcs.getNearest(GRAND_EXCHANGE_CLERK);
			if (atGe() && npc.isOnScreen()) {
				return open();
			}

			if (atGe() && !npc.isOnScreen()) {
				while (!npc.isOnScreen()) {
					methods.walking.walkTo(npc.getLocation());
				}
				return open();
			}
		}
		while (methods.players.getMyPlayer().isMoving()) {
			sleep(random(450, 850));
		}
		sleep(random(350, 550));
		return isOpen();
	}

	/**
	 * Walks to the GE. Method Should be used in a loop
	 */
	public void walkTo() {
		RSWeb webWalking = methods.web.getWeb(methods.players.getMyPlayer().getLocation(), GE_TILE);
		webWalking.step();
	}

	/**
	 * Checks if your player is at the GE
	 *
	 * @return True if you at the G.E
	 */
	public boolean atGe() {
		return GE_AREA.contains(methods.players.getMyPlayer().getLocation());
	}

	/**
	 * Clicks the sell all button
	 *
	 * @return True if the button is clicked
	 */
	public boolean sellAll() {
		RSComponent c = methods.interfaces.get(105).getComponent(166);
		return c != null && c.isValid() && c.doClick(true);
	}

	/**
	 * Enters the amount (quantity) of the item to sell
	 *
	 * @param text The Number to send, as String to shorten nums sellX("1k")
	 * @return True on Success
	 */
	public boolean sellX(String text) {
		RSComponent c = methods.interfaces.get(105).getComponent(166);
		if (c != null && c.isValid() && c.doClick()) {
			sleep(random(250, 500));
			methods.keyboard.sendText(text, true);
			sleep(random(175, 250));
			return true;
		}
		return false;
	}

	/**
	 * Presses the %5 DOWN button
	 *
	 * @return True on Success
	 */
	public boolean fivePercentDown() {
		RSComponent c = methods.interfaces.get(105).getComponent(181);
		if (c != null && c.isValid() && c.doClick()) {
			sleep(random(175, 225));
			return true;
		}
		sleep(random(250, 500));
		return false;
	}

	/**
	 * Presses the %5 UP button
	 *
	 * @return True on Success
	 */
	public boolean fivePercentUp() {
		RSComponent c = methods.interfaces.get(105).getComponent(179);
		if (c != null && c.isValid() && c.doClick()) {
			sleep(random(175, 225));
			return true;
		}
		sleep(random(250, 500));
		return false;
	}

	/**
	 * Sets the price of the item to Sell
	 *
	 * @param price The price to set Can include Symbols like "m" and "k"
	 * @return True on Success
	 */
	public boolean setPrice(String price) {
		RSComponent c = methods.interfaces.get(105).getComponent(177);
		if (c != null && c.isValid() && c.doClick()) {
			sleep(random(1250, 1550));
			methods.keyboard.sendText(price, true);
			sleep(random(250, 500));
			return true;
		}
		sleep(random(250, 500));
		return false;
	}

	/**
	 * Clicks the confirm Button on the G.E Interface
	 *
	 * @return True on Success
	 */
	public boolean clickConfirm() {
		RSComponent c = methods.interfaces.get(105).getComponent(186);
		if (c != null && c.isValid() && c.doClick()) {
			sleep(random(175, 225));
			return true;
		}
		sleep(random(250, 500));
		return false;
	}

	/**
	 * Clicks the back Button on the G.E order form
	 *
	 * @return True on Success
	 */
	public boolean clickBackButton() {
		RSComponent c = methods.interfaces.get(105).getComponent(128);
		if (c != null && c.isValid()) {
			sleep(random(150, 225));
			c.doHover();
			String[] actions = methods.menu.getItems();
			for (String a : actions) {
				if (a.contains("Back")) {
					c.doClick();
					return true;
				}
			}
		}
		sleep(random(250, 500));
		return false;
	}

	/**
	 * Calculates if the G.E offer interface is a buy Interface
	 *
	 * @return True if the interface is open
	 */
	public boolean isBuyOpen() {
		RSComponent c = methods.interfaces.get(105).getComponent(134);
		RSComponent bb = methods.interfaces.get(105).getComponent(128);
		if (c.isValid() && c.getText().contains("Buy Offer")) {
			if (bb != null) {
				if (bb.isValid()) {
					bb.doHover();
					String[] actions = methods.menu.getItems();
					for (String a : actions) {
						if (a.contains("Back")) {
							methods.mouse.moveRandomly(random(40, 150));
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Calculates if the G.E offer interface is a Sell Interface
	 *
	 * @return True if the interface is open
	 */
	public boolean isSellOpen() {
		RSComponent c = methods.interfaces.get(105).getComponent(134);
		RSComponent bb = methods.interfaces.get(105).getComponent(128);
		if (c != null && c.isValid() && c.getText().contains("Sell Offer")) {
			if (bb != null) {
				if (bb.isValid()) {
					bb.doHover();
					String[] actions = methods.menu.getItems();
					for (String a : actions) {
						if (a.contains("Back")) {
							methods.mouse.moveRandomly(random(40, 150));
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Will check a slot for to see if an item has completed.
	 *
	 * @param slot The slot to check.
	 * @return <tt>true</tt> if Complete, otherwise <tt>false</tt>
	 */
	public boolean checkCompleted(final int slot) {
		if (!checkSlotIsEmpty(slot)) {
			if (slot != 0) {
				final int slotComponent = INTERFACE_GRAND_EXCHANGE_OFFER_BOXES[slot];
				return !methods.interfaces.getComponent(INTERFACE_GRAND_EXCHANGE, slotComponent).containsAction("Abort Offer");
			}
		}
		return false;
	}

	/**
	 * Gets any items that may be in the offer.
	 * TODO; Add a collect from bank.
	 *
	 * @param slot An int for the corresponding slot, of which to check
	 */
	public void getItem(final int slot) {
		if (isOpen()) {
			open();
		}
		if (isOpen()) {
			final int slotComponent = INTERFACE_GRAND_EXCHANGE_OFFER_BOXES[slot];
			methods.interfaces.getComponent(INTERFACE_GRAND_EXCHANGE, slotComponent).containsAction("View Offer");
			sleep(random(700, 1200));
			if (methods.interfaces.getComponent(INTERFACE_GRAND_EXCHANGE, GRAND_EXCHANGE_COLLECT_BOX_TWO).containsAction("Collect")) {
				methods.interfaces.getComponent(INTERFACE_GRAND_EXCHANGE, GRAND_EXCHANGE_COLLECT_BOX_TWO).interact("Collect");
				sleep(random(400, 900));
			}
			if (methods.interfaces.getComponent(INTERFACE_GRAND_EXCHANGE, GRAND_EXCHANGE_COLLECT_BOX_ONE).containsAction("Collect")) {
				methods.interfaces.getComponent(INTERFACE_GRAND_EXCHANGE, GRAND_EXCHANGE_COLLECT_BOX_ONE).interact("Collect");
				sleep(random(400, 900));
			}
		}
	}

	/**
	 * Clicks the buy button for specified slot.
	 *
	 * @param slot An int for the corresponding slot.
	 * @return <tt>true</tt> on click.
	 */
	public boolean openBuySlot(final int slot) {
		return openSlot(slot, true);
	}

	/**
	 * Clicks the sell button for specified slot.
	 *
	 * @param slot An int for the corresponding slot.
	 * @return <tt>true</tt> on click.
	 */
	public boolean openSellSlot(final int slot) {
		return openSlot(slot, false);
	}

	/**
	 * Gets the name of the given item ID. Should not be used.
	 *
	 * @param itemID The item ID to look for.
	 * @return The name of the given item ID or an empty String if unavailable.
	 * @see GrandExchange#lookup(int)
	 */
	public String getItemName(final int itemID) {
		final GEItem geItem = lookup(itemID);
		if (geItem != null) {
			return geItem.getName();
		}
		return "";
	}

	/**
	 * Gets the ID of the given item name. Should not be used.
	 *
	 * @param itemName The name of the item to look for.
	 * @return The ID of the given item name or -1 if unavailable.
	 * @see GrandExchange#lookup(java.lang.String)
	 */
	public int getItemID(final String itemName) {
		final GEItem geItem = lookup(itemName);
		if (geItem != null) {
			return geItem.getID();
		}
		return -1;
	}

	/**
	 * Collects data for a given item ID from the Grand Exchange website.
	 *
	 * @param itemID The item ID.
	 * @return An instance of GrandExchange.GEItem; <code>null</code> if unable
	 *         to fetch data.
	 */
	public GEItem lookup(final int itemID) {
		try {
			final URL url = new URL(GrandExchange.HOST + GrandExchange.GET + itemID);
			final BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
			String input;
			boolean exists = false;
			int i = 0;
			final double[] values = new double[4];
			String name = "", examine = "";
			while ((input = br.readLine()) != null) {
				if (input.contains("<div class=\"brown_box main_ge_page") && !exists) {
					if (!input.contains("vertically_spaced")) {
						return null;
					}
					exists = true;
					br.readLine();
					br.readLine();
					name = br.readLine();
				} else if (input.contains("<img id=\"item_image\" src=\"")) {
					examine = br.readLine();
				} else if (input.matches("(?i).+ (price|days):</b> .+")) {
					values[i] = parse(input);
					i++;
				} else if (input.matches("<div id=\"legend\">")) {
					break;
				}
			}
			return new GEItem(name, examine, itemID, values);
		} catch (final IOException ignore) {
		}
		return null;
	}

	/**
	 * Collects data for a given item name from the Grand Exchange website.
	 *
	 * @param itemName The name of the item.
	 * @return An instance of GrandExchange.GEItem; <code>null</code> if unable
	 *         to fetch data.
	 */
	public GEItem lookup(String itemName) {
		try {
			final URL url = new URL(WIKIHOST + WIKIBASE +makeNameEdits(itemName));
			final BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
			String input;
			while ((input = br.readLine()) != null) {
				if (input.contains("Lookup current price")) {
					String[] inputs = input.split("\"");
					int itemID = Integer.parseInt(inputs[1].split("obj=")[1]);
					return lookup(itemID);
				}
			}
		} catch (final IOException ignored) {
		}
		return null;
	}
	
	private String makeNameEdits(String itemName) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String currString: itemName.trim().split(" ")) {
			if (first || currString.length() < 1) {
				sb.append(currString);
				first = false;
				continue;
			}
			sb.append('_');
			sb.append(currString.substring(0, 1).toLowerCase() + currString.substring(1));
		}
		return sb.toString();
	}

	private double parse(String str) {
		if (str != null && !str.isEmpty()) {
			str = stripFormatting(str);
			str = str.substring(str.indexOf(58) + 2, str.length());
			str = str.replace(",", "");
			str = str.trim();
			if (!str.endsWith("%")) {
				if (!str.endsWith("k") && !str.endsWith("m") && !str.endsWith("b")) {
					return Double.parseDouble(str);
				}
				return Double.parseDouble(str.substring(0, str.length() - 1)) * (str.endsWith("b") ? 1000000000 : str.endsWith("m") ? 1000000 : 1000);
			}
			final int k = str.startsWith("+") ? 1 : -1;
			str = str.substring(1);
			return Double.parseDouble(str.substring(0, str.length() - 1)) * k;
		}
		return -1D;
	}

	private String stripFormatting(final String str) {
		if (str != null && !str.isEmpty()) {
			return str.replaceAll("(^[^<]+>|<[^>]+>|<[^>]+$)", "");
		}
		return "";
	}

	/**
	 * Provides access to GEItem Information.
	 */
	public static class GEItem {
		private final String name;
		private final String examine;

		private final int id;

		private final int guidePrice;

		private final double change30;
		private final double change90;
		private final double change180;

		GEItem(final String name, final String examine, final int id, final double[] values) {
			this.name = name;
			this.examine = examine;
			this.id = id;
			guidePrice = (int) values[0];
			change30 = values[1];
			change90 = values[2];
			change180 = values[3];
		}

		/**
		 * Gets the change in price for the last 30 days of this item.
		 *
		 * @return The change in price for the last 30 days of this item.
		 */
		public double getChange30Days() {
			return change30;
		}

		/**
		 * Gets the change in price for the last 90 days of this item.
		 *
		 * @return The change in price for the last 90 days of this item.
		 */
		public double getChange90Days() {
			return change90;
		}

		/**
		 * Gets the change in price for the last 180 days of this item.
		 *
		 * @return The change in price for the last 180 days of this item.
		 */
		public double getChange180Days() {
			return change180;
		}

		/**
		 * Gets the ID of this item.
		 *
		 * @return The ID of this item.
		 */
		public int getID() {
			return id;
		}

		/**
		 * Gets the market price of this item.
		 *
		 * @return The market price of this item.
		 */
		public int getGuidePrice() {
			return guidePrice;
		}

		/**
		 * Gets the name of this item.
		 *
		 * @return The name of this item.
		 */
		public String getName() {
			return name;
		}

		/**
		 * Gets the description of this item.
		 *
		 * @return The description of this item.
		 */
		public String getDescription() {
			return examine;
		}
	}
}