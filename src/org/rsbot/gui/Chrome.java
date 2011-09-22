package org.rsbot.gui;

import org.rsbot.Configuration;
import org.rsbot.bot.Bot;
import org.rsbot.gui.component.*;
import org.rsbot.log.TextAreaLogHandler;
import org.rsbot.script.internal.ScriptHandler;
import org.rsbot.script.internal.event.ScriptListener;
import org.rsbot.script.methods.Environment;
import org.rsbot.script.provider.ScriptDeliveryNetwork;
import org.rsbot.script.provider.ScriptDownloader;
import org.rsbot.script.provider.ScriptUserList;
import org.rsbot.script.task.LoopTask;
import org.rsbot.script.util.WindowUtil;
import org.rsbot.service.Preferences;
import org.rsbot.service.TwitterUpdates;
import org.rsbot.util.UpdateChecker;
import org.rsbot.util.io.ScreenshotUtil;

import javax.swing.*;

import java.awt.*;
import java.awt.TrayIcon.MessageType;
import java.awt.event.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author Paris
 */
public class Chrome extends JFrame implements ActionListener, ScriptListener {
	public static final int PANEL_WIDTH = 765, PANEL_HEIGHT = 503;
	public static final int MAX_BOTS = 6;
	private static final long serialVersionUID = -5411033752001988794L;
	private static final Logger log = Logger.getLogger(Chrome.class.getName());
	private BotPanel panel;
	private BotToolBar toolBar;
	private BotMenuBar menuBar;
	private JScrollPane textScroll;
	protected static final List<Bot> bots = new ArrayList<Bot>();
	private TrayIcon tray = null;

	public Chrome() {
		init();
		pack();
		setTitle(null);
		setLocationRelativeTo(getOwner());
		setMinimumSize(new Dimension((int) (getSize().width * .8), (int) (getSize().height * .8)));
		setResizable(true);
		toolBar.runScriptButton.setEnabled(false);
		menuBar.getMenuItem(Messages.RUNSCRIPT).setEnabled(false);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame.setDefaultLookAndFeelDecorated(true);
				JPopupMenu.setDefaultLightWeightPopupEnabled(false);
				ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
				SwingUtilities.updateComponentTreeUI(Chrome.this);
				addBot();
				updateScriptControls();
				ExecutorService pool = Executors.newCachedThreadPool();
				if (Configuration.Twitter.ENABLED) {
					pool.execute(new TwitterUpdates());
				}
				if (!Preferences.getInstance().hideAds) {
					pool.execute(new SplashAd(Chrome.this));
				}
				pool.execute(ScriptDeliveryNetwork.getInstance());
				pool.execute(ScriptUserList.getInstance());
				pool.shutdown();
				try {
					pool.awaitTermination(15, TimeUnit.SECONDS);
				} catch (final InterruptedException ignored) {
					log.warning("Unable to complete startup tasks");
				}
				menuBar.getMenuItem(Messages.RUNSCRIPT).setEnabled(true);
				toolBar.runScriptButton.setEnabled(true);
				setVisible(true);
				LoadScreen.quit();
				System.gc();
			}
		});
	}

	@Override
	public void setTitle(final String title) {
		String t = Configuration.NAME + " v" + Configuration.getVersionFormatted();
		final int v = Configuration.getVersion(), l = UpdateChecker.getLatestVersion();
		if (v > l) {
			t += " beta";
		}
		if (title != null) {
			t = title + " - " + t;
		}
		super.setTitle(t);
	}

	public void actionPerformed(final ActionEvent evt) {
		final String action = evt.getActionCommand();
		final String menu, option;
		final int z = action.indexOf('.');
		if (z == -1) {
			menu = action;
			option = "";
		} else {
			menu = action.substring(0, z);
			option = action.substring(z + 1);
		}
		if (menu.equals(Messages.CLOSEBOT)) {
			final int idx = Integer.parseInt(option);
			removeBot(bots.get(idx));
		} else if (menu.equals(Messages.FILE)) {
			if (option.equals(Messages.NEWBOT)) {
				addBot();
			} else if (option.equals(Messages.CLOSEBOT)) {
				removeBot(getCurrentBot());
			} else if (option.equals(Messages.ADDSCRIPT)) {
				final String pretext = "";
				final String key = (String) JOptionPane.showInputDialog(this, "Enter the script URL e.g. pastebin link or direct compiled file:",
						option, JOptionPane.QUESTION_MESSAGE, null, null, pretext);
				if (!(key == null || key.trim().isEmpty())) {
					ScriptDownloader.save(key);
				}
			} else if (option.equals(Messages.RUNSCRIPT)) {
				final Bot current = getCurrentBot();
				if (current != null) {
					showScriptSelector(current);
				}
			} else if (option.equals(Messages.STOPSCRIPT)) {
				final Bot current = getCurrentBot();
				if (current != null) {
					showStopScript(current);
				}
			} else if (option.equals(Messages.PAUSESCRIPT)) {
				final Bot current = getCurrentBot();
				if (current != null) {
					pauseScript(current);
				}
			} else if (option.equals(Messages.SAVESCREENSHOT)) {
				final Bot current = getCurrentBot();
				if (current != null && current.getMethodContext() != null) {
					ScreenshotUtil.saveScreenshot(current, current.getMethodContext().game.isLoggedIn());
				}
			} else if (option.equals(Messages.HIDE)) {
				setTray();
			} else if (option.equals(Messages.EXIT)) {
				cleanExit();
			}
		} else if (menu.equals(Messages.EDIT)) {
			if (option.equals(Messages.ACCOUNTS)) {
				AccountManager.getInstance().showGUI();
			} else {
				final Bot current = getCurrentBot();
				if (current != null) {
					if (option.equals(Messages.FORCEINPUT)) {
						current.overrideInput = ((JCheckBoxMenuItem) evt.getSource()).isSelected();
						updateScriptControls();
					} else if (option.equals(Messages.LESSCPU)) {
						current.disableRendering = ((JCheckBoxMenuItem) evt.getSource()).isSelected();
					} else if (option.equals(Messages.DISABLECANVAS)) {
						current.disableGraphics = ((JCheckBoxMenuItem) evt.getSource()).isSelected();
					} else if (option.equals(Messages.EXTDVIEWS)) {
						menuBar.setExtendedView(((JCheckBoxMenuItem) evt.getSource()).isSelected());
					} else if (option.equals(Messages.DISABLEANTIRANDOMS)) {
						current.disableRandoms = ((JCheckBoxMenuItem) evt.getSource()).isSelected();
					} else if (option.equals(Messages.DISABLEAUTOLOGIN)) {
						current.disableAutoLogin = ((JCheckBoxMenuItem) evt.getSource()).isSelected();
					} else if (option.equals(Messages.DISABLETHEME)) {
						Preferences.getInstance().theme = !((JCheckBoxMenuItem) evt.getSource()).isSelected();
						log.info("Themes will be " + (Preferences.getInstance().theme ? "enabled" : "disabled") + " next time you run the application");
					}
				}
			}
		} else if (menu.equals(Messages.VIEW)) {
			final Bot current = getCurrentBot();
			final boolean selected = ((JCheckBoxMenuItem) evt.getSource()).isSelected();
			if (option.equals(Messages.HIDETOOLBAR)) {
				toggleViewState(toolBar, selected);
			} else if (option.equals(Messages.HIDELOGPANE)) {
				toggleViewState(textScroll, selected);
			} else if (current != null) {
				if (option.equals(Messages.ALLDEBUGGING)) {
					for (final String key : BotMenuBar.DEBUG_MAP.keySet()) {
						final Class<?> el = BotMenuBar.DEBUG_MAP.get(key);
						if (menuBar.getCheckBox(key).isVisible()) {
							final boolean wasSelected = menuBar.getCheckBox(key).isSelected();
							menuBar.getCheckBox(key).setSelected(selected);
							if (selected) {
								if (!wasSelected) {
									current.addListener(el);
								}
							} else {
								if (wasSelected) {
									current.removeListener(el);
								}
							}
						}
					}
				} else {
					final Class<?> el = BotMenuBar.DEBUG_MAP.get(option);
					menuBar.getCheckBox(option).setSelected(selected);
					if (selected) {
						current.addListener(el);
					} else {
						menuBar.getCheckBox(Messages.ALLDEBUGGING).setSelected(false);
						current.removeListener(el);
					}
				}
			}
		} else if (menu.equals(Messages.TOOLS)) {
			if (option.equals(Messages.LICENSES)) {
				log.warning("License manager coming soon");
			}
		} else if (menu.equals(Messages.HELP)) {
			if (option.equals(Messages.SITE)) {
				openURL(Configuration.Paths.URLs.SITE);
			} else if (option.equals(Messages.PROJECT)) {
				openURL(Configuration.Paths.URLs.PROJECT);
			} else if (option.equals(Messages.LICENSE)) {
				openURL(Configuration.Paths.URLs.LICENSE);
			} else if (option.equals(Messages.ABOUT)) {
				JOptionPane.showMessageDialog(this, new String[]{
						"An open source bot developed by the community.",
						"",
						"RuneScape® is a trademark of Jagex © 1999 - 2011 Jagex, Ltd.",
						"RuneScape content and materials are trademarks and copyrights of Jagex or its licensees.",
						"This program is issued with no warranty and is not affiliated with Jagex Ltd., nor do they endorse usage of our software.",
						"",
						"Visit " + Configuration.Paths.URLs.SITE + "/ for more information."},
						option,
						JOptionPane.INFORMATION_MESSAGE);
			}
		} else if (menu.equals("Tab")) {
			final Bot curr = getCurrentBot();
			menuBar.setBot(curr);
			panel.setBot(curr);
			panel.repaint();
			toolBar.setHome(curr == null);
			setTitle(curr == null ? null : curr.getAccountName());
			updateScriptControls();
		}
	}

	public void updateScriptControls() {
		boolean idle = true, paused = false;
		final Bot bot = getCurrentBot();

		if (bot != null) {
			final Map<Integer, LoopTask> scriptMap = bot.getScriptHandler().getRunningScripts();
			if (scriptMap.size() > 0) {
				idle = false;
				paused = scriptMap.values().iterator().next().isPaused();
			} else {
				idle = true;
			}
		}

		menuBar.getMenuItem(Messages.RUNSCRIPT).setVisible(idle);
		menuBar.getMenuItem(Messages.STOPSCRIPT).setVisible(!idle);
		menuBar.getMenuItem(Messages.PAUSESCRIPT).setEnabled(!idle);
		menuBar.setPauseScript(paused);
		toolBar.setInputButtonVisible(!idle);
		menuBar.setEnabled(Messages.FORCEINPUT, !idle);

		if (idle) {
			toolBar.setOverrideInput(false);
			menuBar.setOverrideInput(false);
			toolBar.setInputState(Environment.INPUT_KEYBOARD | Environment.INPUT_MOUSE);
			toolBar.setScriptButton(BotToolBar.RUN_SCRIPT);
		} else {
			toolBar.setOverrideInput(bot.overrideInput);
			menuBar.setOverrideInput(bot.overrideInput);
			toolBar.setInputState(bot.inputFlags);
			toolBar.setScriptButton(paused ? BotToolBar.RESUME_SCRIPT : BotToolBar.PAUSE_SCRIPT);
		}

		toolBar.updateInputButton();
		repaint();
	}

	public BotPanel getPanel() {
		return panel;
	}

	public Bot getBot(final Object o) {
		final ClassLoader cl = o.getClass().getClassLoader();
		for (final Bot bot : bots) {
			if (cl == bot.getLoader().getClient().getClass().getClassLoader()) {
				panel.offset();
				return bot;
			}
		}
		return null;
	}

	public void addBot() {
		if (bots.size() > MAX_BOTS) {
			return;
		}
		final Bot bot = new Bot();
		bots.add(bot);
		toolBar.addTab();
		toolBar.setAddTabVisible(bots.size() < MAX_BOTS);
		bot.getScriptHandler().addScriptListener(this);
		new Thread(new Runnable() {
			public void run() {
				bot.start();
			}
		}).start();
	}

	public void removeBot(final Bot bot) {
		final int idx = bots.indexOf(bot);
		bot.getScriptHandler().stopAllScripts();
		bot.getScriptHandler().removeScriptListener(this);
		if (idx >= 0) {
			toolBar.removeTab(idx);
		}
		bots.remove(idx);
		toolBar.setAddTabVisible(bots.size() < MAX_BOTS);
		new Thread(new Runnable() {
			public void run() {
				bot.stop();
				System.gc();
			}
		}).start();
	}

	void pauseScript(final Bot bot) {
		for (final Entry<Integer, LoopTask> script : bot.getScriptHandler().getRunningScripts().entrySet()) {
			bot.getScriptHandler().pauseScript(script.getKey());
		}
	}

	private Bot getCurrentBot() {
		final int idx = toolBar.getCurrentTab();
		if (idx > -1 && idx < bots.size()) {
			return bots.get(idx);
		}
		return null;
	}

	private void showScriptSelector(final Bot bot) {
		if (AccountManager.getAccountNames() == null || AccountManager.getAccountNames().length == 0) {
			log.warning("Please save an account before loading a script");
			AccountManager.getInstance().showGUI();
		}
		if (bot.getMethodContext() == null) {
			log.warning("The client is still loading");
		} else if (AccountManager.getAccountNames() != null && AccountManager.getAccountNames().length != 0) {
			new ScriptSelector(this, bot).showGUI();
		}
	}

	private void showStopScript(final Bot bot) {
		final ScriptHandler sh = bot.getScriptHandler();
		final Map<Integer, LoopTask> running = sh.getRunningScripts();
		if (running.size() > 0) {
			final int result = JOptionPane.showConfirmDialog(this, "Would you like to stop the script?", "Script", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (result == JOptionPane.OK_OPTION) {
				sh.stopAllScripts();
				bot.getMethodContext().web.unloadWebScripts();
				updateScriptControls();
			}
		}
	}

	private void toggleViewState(final Component component, final boolean visible) {
		final Dimension size = getSize();
		size.height += component.getSize().height * (visible ? -1 : 1);
		component.setVisible(!visible);
		setMinimumSize(size);
		if ((getExtendedState() & Frame.MAXIMIZED_BOTH) != Frame.MAXIMIZED_BOTH) {
			pack();
		}
	}

	private void lessCpu(boolean on) {
		final Bot bot = getCurrentBot();
		if (bot != null) {
			disableRendering(on || menuBar.isTicked(Messages.LESSCPU));
			disableGraphics(on || menuBar.isTicked(Messages.DISABLECANVAS));
		}
	}

	public void disableRendering(final boolean mode) {
		for (final Bot bot : bots) {
			bot.disableRendering = mode;
		}
	}

	public void disableGraphics(final boolean mode) {
		for (final Bot bot : bots) {
			bot.disableGraphics = mode;
		}
	}

	private void init() {
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				cleanExit();
			}
		});
		addWindowStateListener(new WindowStateListener() {
			public void windowStateChanged(final WindowEvent arg0) {
				switch (arg0.getID()) {
					case WindowEvent.WINDOW_ICONIFIED:
						lessCpu(true);
						break;
					case WindowEvent.WINDOW_DEICONIFIED:
						lessCpu(false);
						break;
				}
			}
		});
		setIconImage(Configuration.getImage(Configuration.Paths.Resources.ICON));
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		WindowUtil.setFrame(this);
		panel = new BotPanel();
		menuBar = new BotMenuBar(this);
		toolBar = new BotToolBar(this, menuBar);
		panel.setFocusTraversalKeys(0, new HashSet<AWTKeyStroke>());
		new BotKeyboardShortcuts(KeyboardFocusManager.getCurrentKeyboardFocusManager(), this, this);
		menuBar.setBot(null);
		setJMenuBar(menuBar);
		textScroll = new JScrollPane(TextAreaLogHandler.TEXT_AREA, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		textScroll.setBorder(null);
		textScroll.setPreferredSize(new Dimension(PANEL_WIDTH, 120));
		textScroll.setVisible(true);
		JScrollPane scrollableBotPanel = new JScrollPane(panel);
		add(toolBar, BorderLayout.NORTH);
		add(scrollableBotPanel, BorderLayout.CENTER);
		add(textScroll, BorderLayout.SOUTH);
	}

	public void scriptStarted(final ScriptHandler handler) {
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				final Bot bot = handler.getBot();
				bot.inputFlags = Environment.INPUT_KEYBOARD;
				bot.overrideInput = false;
				final String acct = bot.getAccountName();
				toolBar.setTabLabel(bots.indexOf(bot), acct == null ? Messages.TABDEFAULTTEXT : acct);
				if (bot == getCurrentBot()) {
					updateScriptControls();
					setTitle(acct);
				}
			}
		});
	}

	public void scriptStopped(final ScriptHandler handler) {
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				final Bot bot = handler.getBot();
				bot.inputFlags = Environment.INPUT_KEYBOARD | Environment.INPUT_MOUSE;
				bot.overrideInput = false;
				toolBar.setTabLabel(bots.indexOf(bot), Messages.TABDEFAULTTEXT);
				if (bot == getCurrentBot()) {
					updateScriptControls();
					setTitle(null);
				}
			}
		});
	}

	public void scriptResumed(final ScriptHandler handler) {
		if (handler.getBot() == getCurrentBot()) {
			updateScriptControls();
		}
	}

	public void scriptPaused(final ScriptHandler handler) {
		if (handler.getBot() == getCurrentBot()) {
			updateScriptControls();
		}
	}

	public void inputChanged(final Bot bot, final int mask) {
		bot.inputFlags = mask;
		updateScriptControls();
	}

	public static void openURL(final String url) {
		final Configuration.OperatingSystem os = Configuration.getCurrentOperatingSystem();
		try {
			if (os == Configuration.OperatingSystem.MAC) {
				final Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
				final Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[]{String.class});
				openURL.invoke(null, url);
			} else if (os == Configuration.OperatingSystem.WINDOWS) {
				Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
			} else {
				final String[] browsers = {"firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape", "google-chrome", "chromium-browser"};
				String browser = null;
				for (int count = 0; count < browsers.length && browser == null; count++) {
					if (Runtime.getRuntime().exec(new String[]{"which", browsers[count]}).waitFor() == 0) {
						browser = browsers[count];
					}
				}
				if (browser == null) {
					throw new Exception("Could not find web browser");
				} else {
					Runtime.getRuntime().exec(new String[]{browser, url});
				}
			}
		} catch (final Exception e) {
			log.warning("Unable to open " + url);
		}
	}

	public void cleanExit() {
		setVisible(false);
		dispose();
		Preferences.getInstance().save();
		System.exit(0);
	}

	public void setTray() {
		boolean showTip = false;
		if (tray == null) {
			showTip = true;
			final Image image = Configuration.getImage(Configuration.Paths.Resources.ICON);
			tray = new TrayIcon(image, Configuration.NAME, null);
			tray.setImageAutoSize(true);
			tray.addMouseListener(new MouseListener() {
				public void mouseClicked(MouseEvent arg0) {
				}

				public void mouseEntered(MouseEvent arg0) {
				}

				public void mouseExited(MouseEvent arg0) {
				}

				public void mouseReleased(MouseEvent arg0) {
				}

				public void mousePressed(MouseEvent arg0) {
					SystemTray.getSystemTray().remove(tray);
					setVisible(true);
					lessCpu(false);
				}
			});
		}
		try {
			SystemTray.getSystemTray().add(tray);
			if (showTip) {
				tray.displayMessage(Configuration.NAME + " Hidden", "Bots are still running in the background.\nClick this icon to restore the window.", MessageType.INFO);
			}
		} catch (Exception ignored) {
			log.warning("Unable to hide window");
		}
		setVisible(false);
		lessCpu(true);
	}
}