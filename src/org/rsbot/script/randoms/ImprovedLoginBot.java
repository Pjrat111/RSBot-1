package org.rsbot.script.randoms;

import org.rsbot.gui.AccountManager;
import org.rsbot.script.AccountStore;
import org.rsbot.script.Random;
import org.rsbot.script.ScriptManifest;
import org.rsbot.script.methods.Game;
import org.rsbot.script.methods.Lobby;
import org.rsbot.script.util.Timer;
import org.rsbot.script.wrappers.RSComponent;

import java.awt.*;

/**
 * A task to login to the game of Runescape.
 *
 * @author Timer
 */
@ScriptManifest(authors = {"Timer"}, name = "Improved Login", version = 0.2)
public class ImprovedLoginBot extends Random {
	public static final int INTERFACE_LOGIN_SCREEN = 596;
	public static final int INTERFACE_LOGIN_SCREEN_ENTER_GAME = 60;
	public static final int INTERFACE_LOGIN_SCREEN_USERNAME_TEXT = 73;
	public static final int INTERFACE_LOGIN_SCREEN_PASSWORD_TEXT = 79;
	public static final int INTERFACE_LOGIN_SCREEN_ALERT_TEXT = 14;
	public static final int INTERFACE_LOGIN_SCREEN_ALERT_BACK = 68;
	public static final int INTERFACE_LOGIN_SCREEN_FACEBOOK = 51;
	public static final int INTERFACE_LOGIN_SCREEN_ABORT_LOGIN = 19;
	public static final int INTERFACE_GRAPHICS_NOTICE = 976;
	public static final int INTERFACE_GRAPHICS_LEAVE_ALONE = 6;
	public static final int INTERFACE_LOBBY_HIGH_RISK_WORLD_TEXT = 98;

	private int world = -1;
	private static final int timeout = 12000; // for fbconnecting.
	public static final int INTERFACE_LOBBY_HIGH_RISK_WORLD_LOGIN_BUTTON = 104;

	private enum LoginEvent {
		NO_REPLY("no reply from login server", 1500),
		GAME_UPDATE("update", -1),
		BANNED("disable", -1),
		SERVER_LAG_1("your account has not logged out", 5000),
		INVALID_LOGIN_1("invalid", 0),
		INVALID_LOGIN_2("incorrect", 0),
		ERROR("error connecting", 0),
		LIMIT_1("login limit exceeded", 0),

		BAD_WORLD("total skill level of", -1),
		LIMIT_2("login limit exceeded", 0),
		SERVER_LAG_2("your account has not logged out", 5000),
		NON_MEMBER("member", -1),

		WAIT_1("world", 1500),
		WAIT_2("performing login", 1500);

		public final String message;
		public final int sleep;

		LoginEvent(final String message, final int sleep) {
			this.message = message;
			this.sleep = sleep;
		}
	}

	@Override
	public int loop() {
		if (lobby.inLobby()) {
			if (lobby.getSelectedTab() != Lobby.TAB_PLAYERS) {
				lobby.open(Lobby.TAB_PLAYERS);
				return random(500, 800);
			}
			if (world != -1 && lobby.getSelectedWorld() != world) {
				lobby.switchWorlds(world, false);
				return random(500, 800);
			}
			if (lobby.clickPlay()) {
				for (int i = 0; i < 4 && game.getClientState() == 6; i++) {
					sleep(500);
				}
				final String returnText = interfaces.getComponent(Lobby.INTERFACE_LOBBY, Lobby.INTERFACE_LOBBY_ALERT_TEXT).getText().toLowerCase().trim();
				if (interfaces.getComponent(Lobby.INTERFACE_LOBBY, Lobby.INTERFACE_LOBBY_ALERT_CLOSE).isValid()) {
					interfaces.getComponent(Lobby.INTERFACE_LOBBY, Lobby.INTERFACE_LOBBY_ALERT_CLOSE).doClick();
				}
				for (final LoginEvent event : LoginEvent.values()) {
					if (returnText.contains(event.message.toLowerCase())) {
						log("Handling login event: " + event.name());
						if (event.sleep == -1) {
							stopScript(false);
							return -1;
						}
						return event.sleep;
					}
				}
			}
		}
		if (game.getClientState() == Game.INDEX_LOGGING_IN && account.getName().equalsIgnoreCase(AccountStore.FACEBOOK_ACCOUNT.getUsername())) {
			if (interfaces.getComponent(INTERFACE_LOGIN_SCREEN, INTERFACE_LOGIN_SCREEN_ALERT_TEXT).getText().toLowerCase().trim().contains("logging in - please wait")) {
				log.info("Waiting for facebook connection");
				Timer t = new Timer(timeout);
				while (t.isRunning() && game.getClientState() == Game.INDEX_LOGGING_IN) {
					sleep(100);
				}
				if (game.getClientState() == Game.INDEX_LOGGING_IN) {
					return interfaces.getComponent(INTERFACE_LOGIN_SCREEN, INTERFACE_LOGIN_SCREEN_ABORT_LOGIN).doClick() ? 500 : 0;
				}
				return 500;
			}
		} else if (game.getClientState() == Game.INDEX_LOGIN_SCREEN) {
			if (interfaces.getComponent(INTERFACE_GRAPHICS_NOTICE, INTERFACE_GRAPHICS_LEAVE_ALONE).isValid()) {
				interfaces.getComponent(INTERFACE_GRAPHICS_NOTICE, INTERFACE_GRAPHICS_LEAVE_ALONE).doClick();
				return random(500, 600);
			}
			final String returnText = interfaces.getComponent(INTERFACE_LOGIN_SCREEN, INTERFACE_LOGIN_SCREEN_ALERT_TEXT).getText().toLowerCase().trim();
			if (interfaces.getComponent(INTERFACE_LOGIN_SCREEN, INTERFACE_LOGIN_SCREEN_ALERT_BACK).isValid()) {
				interfaces.getComponent(INTERFACE_LOGIN_SCREEN, INTERFACE_LOGIN_SCREEN_ALERT_BACK).doClick();
			}
			if (interfaces.getComponent(Lobby.INTERFACE_LOBBY, INTERFACE_LOBBY_HIGH_RISK_WORLD_TEXT).getText().toLowerCase().trim().contains("high-risk wilderness world")) {
				return interfaces.getComponent(Lobby.INTERFACE_LOBBY, INTERFACE_LOBBY_HIGH_RISK_WORLD_LOGIN_BUTTON).doClick() ? 500 : 0;
			}
			for (final LoginEvent event : LoginEvent.values()) {
				if (returnText.contains(event.message.toLowerCase())) {
					log("Handling login event: " + event.name());
					if (event.sleep == -1) {
						stopScript(false);
						return -1;
					}
					return event.sleep;
				}
			}
			if (account.getName().equalsIgnoreCase(AccountStore.FACEBOOK_ACCOUNT.getUsername())) {
				return interfaces.getComponent(INTERFACE_LOGIN_SCREEN, INTERFACE_LOGIN_SCREEN_FACEBOOK).doClick() ? 500 : 0;
			}
			if (isUsernameCorrect() && isPasswordValid()) {
				attemptLogin();
				return random(1200, 1500);
			}
			if (!isUsernameCorrect()) {
				final String username = account.getName().toLowerCase().trim();
				atLoginInterface(interfaces.getComponent(INTERFACE_LOGIN_SCREEN, INTERFACE_LOGIN_SCREEN_USERNAME_TEXT));
				sleep(random(500, 700));
				final int textLength = interfaces.getComponent(INTERFACE_LOGIN_SCREEN, INTERFACE_LOGIN_SCREEN_USERNAME_TEXT).getText().length();
				if (textLength > 0) {
					for (int i = 0; i <= textLength + random(1, 5); i++) {
						keyboard.sendText("\b", false);
						if (random(0, 2) == 1) {
							sleep(random(25, 100));
						}
					}
					return random(500, 600);
				}
				keyboard.sendText(username, false);
				return random(500, 600);
			}
			if (isUsernameCorrect() && !isPasswordValid()) {
				atLoginInterface(interfaces.getComponent(INTERFACE_LOGIN_SCREEN, INTERFACE_LOGIN_SCREEN_PASSWORD_TEXT));
				sleep(random(500, 700));
				final int textLength = interfaces.getComponent(INTERFACE_LOGIN_SCREEN, INTERFACE_LOGIN_SCREEN_PASSWORD_TEXT).getText().length();
				if (textLength > 0) {
					for (int i = 0; i <= textLength + random(1, 5); i++) {
						keyboard.sendText("\b", false);
						if (random(0, 2) == 1) {
							sleep(random(25, 100));
						}
					}
					return random(500, 600);
				}
				String passWord = AccountManager.getPassword(account.getName());
				keyboard.sendText(passWord, false);
				return random(500, 600);
			}
		}
		return game.isLoggedIn() ? -1 : random(100, 500);
	}

	private boolean atLoginInterface(final RSComponent i) {
		if (!i.isValid()) {
			return false;
		}
		final Rectangle pos = i.getArea();
		if (pos.x == -1 || pos.y == -1 || pos.width == -1 || pos.height == -1) {
			return false;
		}
		final int dy = (int) (pos.getHeight() - 4) / 2;
		final int maxRandomX = (int) (pos.getMaxX() - pos.getCenterX());
		final int midx = (int) pos.getCenterX();
		final int midy = (int) (pos.getMinY() + pos.getHeight() / 2);
		if (i.getIndex() == INTERFACE_LOGIN_SCREEN_PASSWORD_TEXT) {
			mouse.click(minX(i), midy + random(-dy, dy), true);
		} else {
			mouse.click(midx + random(1, maxRandomX), midy + random(-dy, dy), true);
		}
		return true;
	}

	private int minX(final RSComponent a) {
		int x = 0;
		final Rectangle pos = a.getArea();
		final int dx = (int) (pos.getWidth() - 4) / 2;
		final int midx = (int) (pos.getMinX() + pos.getWidth() / 2);
		if (pos.x == -1 || pos.y == -1 || pos.width == -1 || pos.height == -1) {
			return 0;
		}
		for (int i = 0; i < interfaces.getComponent(INTERFACE_LOGIN_SCREEN, INTERFACE_LOGIN_SCREEN_PASSWORD_TEXT).getText().length(); i++) {
			x += 11;
		}
		if (x > 44) {
			return (int) (pos.getMinX() + x + 15);
		} else {
			return midx + random(-dx, dx);
		}
	}

	private void attemptLogin() {
		if (random(0, 2) == 0) {
			keyboard.sendKey('\n');
		} else {
			interfaces.getComponent(INTERFACE_LOGIN_SCREEN, INTERFACE_LOGIN_SCREEN_ENTER_GAME).doClick();
		}
	}

	private boolean isUsernameCorrect() {
		final String userName = account.getName().toLowerCase().trim();
		return interfaces.getComponent(INTERFACE_LOGIN_SCREEN, INTERFACE_LOGIN_SCREEN_USERNAME_TEXT).getText().toLowerCase().equalsIgnoreCase(userName);
	}

	private boolean isPasswordValid() {
		String passWord = AccountManager.getPassword(account.getName());
		return interfaces.getComponent(INTERFACE_LOGIN_SCREEN, INTERFACE_LOGIN_SCREEN_PASSWORD_TEXT).getText().length() == (passWord == null ? 0 : passWord.length());
	}

	public void setWorld(final int world) {
		this.world = world;
	}

	@Override
	public boolean activateCondition() {
		final int idx = game.getClientState();
		return (idx == Game.INDEX_LOGIN_SCREEN || idx == Game.INDEX_LOBBY_SCREEN) && account.getName() != null;
	}
}