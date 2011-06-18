package org.rsbot.gui;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;

import org.rsbot.locale.Messages;

/**
 * @author Mothma
 */
public class KeyboardShortcuts {
	KeyboardFocusManager manager;
	BotGUI botgui;
	public static final HashMap<Object, String> SHORTCUT_MAP = new HashMap<Object, String>();
	
	static {
		SHORTCUT_MAP.put(KeyEvent.VK_N, Messages.FILE + "." + Messages.NEWBOT);
		SHORTCUT_MAP.put(KeyEvent.VK_W, Messages.FILE + "." + Messages.CLOSEBOT);
		SHORTCUT_MAP.put(KeyEvent.VK_A, Messages.FILE + "." + Messages.ADDSCRIPT);
		SHORTCUT_MAP.put(KeyEvent.VK_R, Messages.FILE + "." + Messages.RUNSCRIPT);
		SHORTCUT_MAP.put(KeyEvent.VK_S, Messages.FILE + "." + Messages.STOPSCRIPT);
		SHORTCUT_MAP.put(KeyEvent.VK_P, Messages.FILE + "." + Messages.PAUSESCRIPT);
		SHORTCUT_MAP.put(KeyEvent.VK_I, Messages.FILE + "." + Messages.SAVESCREENSHOT);
		SHORTCUT_MAP.put(KeyEvent.VK_H, Messages.FILE + "." + Messages.HIDEBOT);
		SHORTCUT_MAP.put(KeyEvent.VK_Q, Messages.FILE + "." + Messages.EXIT);
		SHORTCUT_MAP.put(KeyEvent.VK_U, Messages.EDIT + "." + Messages.ACCOUNTS);
		SHORTCUT_MAP.put(KeyEvent.VK_O, Messages.TOOLS + "." + Messages.OPTIONS);		
	}
	
	KeyboardShortcuts(KeyboardFocusManager manager, BotGUI botgui) {
		this.manager = manager;
		this.botgui = botgui;
		manager.addKeyEventDispatcher(new KeyDispatcher(manager,botgui));
	}
	
	public class KeyDispatcher  implements KeyEventDispatcher {
		KeyboardFocusManager manager;
		BotGUI botgui;
		
		KeyDispatcher (KeyboardFocusManager manager, BotGUI botgui) {
			System.out.println("Dispatcher created.");
			this.manager = manager;
			this.botgui = botgui;
		}
		
		@Override
		public boolean dispatchKeyEvent(KeyEvent e) {
			if (e.isControlDown()) {
				if (SHORTCUT_MAP.containsKey(e.getKeyCode())) {
					if (e.getID() == KeyEvent.KEY_PRESSED) {						
						botgui.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, SHORTCUT_MAP.get(e.getKeyCode())));												
					}
				}
				return false;
			} else {
			manager.dispatchKeyEvent(e);			
			}
			return true;
		}		
		
	}
	
}


