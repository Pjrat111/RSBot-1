package org.rsbot.script.methods;

import org.rsbot.Configuration;
import org.rsbot.script.util.Filter;
import org.rsbot.script.wrappers.RSComponent;
import org.rsbot.script.wrappers.RSInterface;
import org.rsbot.util.io.HttpClient;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Methods for lobby interface
 *
 * @author Debauchery
 */
public class Lobby extends MethodProvider {
	public static final int INTERFACE_LOBBY = 906;
	public static final int TAB_WORLDS = 22;
	public static final int TAB_FRIENDS = 21;
	public static final int TAB_CLAN = 20;
	public static final int TAB_OPTIONS = 19;
	public static final int TAB_FRIENDS_CHAT = 265;
	public static final int TAB_PLAYERS = 215;
	public static final int INTERFACE_LOBBY_BUTTON_PLAY = 171;
	public static final int INTERFACE_LOBBY_ALERT_TEXT = 235;
	public static final int INTERFACE_LOBBY_ALERT_CLOSE = 242;
	public static final int INTERFACE_LOBBY_BUTTON_LOGOUT = 195;
	public final static int INTERFACE_LOBBY_WORLD_SELECT = 910;
	public final static int INTERFACE_WORLD_SELECT_CURRENT_WORLD = 11;
	public final static int INTERFACE_WORLD_SELECT_WORLD_LIST = 77;
	public final static int INTERFACE_WORLD_SELECT_WORLD_NAME = 69;
	public final static int INTERFACE_WORLD_SELECT_AMOUNT_OF_PLAYERS = 71;
	public final static int INTERFACE_WORLD_SELECT_WORLD_ACTIVITY = 72;
	public final static int INTERFACE_WORLD_SELECT_WORLD_TYPE = 74;
	public final static int INTERFACE_WORLD_SELECT_WORLD_PING = 76;
	public final static int INTERFACE_WORLD_SELECT_SCROLL_AREA = 86;
	public final static int INTERFACE_WORLD_SELECT_SCROLL_BAR = 1;

	public static final Filter<World> ALL_FILTER = new Filter<World>() {
		public boolean accept(final World w) {
			return true;
		}
	};

	public Lobby(final MethodContext ctx) {
		super(ctx);
	}

	/**
	 * Checks that current game is in lobby.
	 *
	 * @return <tt>true</tt> if the tab is opened.
	 */
	public boolean inLobby() {
		return methods.game.getClientState() == Game.INDEX_LOBBY_SCREEN;
	}

	public class World {
		private final String world;
		private final String players;
		private final String activity;
		private final String lootShare;
		private final String type;

		public World(String world, String players, String activity, String lootShare, String type) {
			this.world = world;
			this.players = players;
			this.activity = activity;
			this.lootShare = lootShare;
			this.type = type;
		}

		public int getWorldID() {
			return Integer.parseInt(this.world);
		}

		public int getAmountOfPlayers() {
			return Integer.parseInt(this.players);
		}

		public String getActivity() {
			return this.activity;
		}

		public boolean isLootShare() {
			return (this.lootShare.equals("Yes"));
		}

		public boolean isOnline() {
			return (!this.players.equals("OFFLINE"));
		}

		public boolean isMembers() {
			return (this.type.equals("Members"));
		}
	}

	public Object[][] getWorldObjects() {
		String HTML = null;
		try {
			HTML = HttpClient.downloadAsString((new URL("http://www." + Configuration.Paths.URLs.GAME + "/slu.ws?order=WPMLA")));
		} catch (IOException ignored) {
		}

		try {
			HTML = HTML.split("Type[^<]</td>[^<]</tr>")[1];
			HTML = HTML.split("</table>")[0].trim();
		} catch (Exception e) {
			return new Object[0][0];
		}

		ArrayList<Object[]> worldData = new ArrayList<Object[]>();

		try {
			Pattern regex = Pattern.compile("[^ ]World ([0-9[^ |^<|^\n|^ (]]*)[^0-9|^OF]*([0-9|A-Z]*)[^=]*[^>]*>([^<]*)"
					+ "[^Y|^N]*(Y|N)[^F|^M]*(Members|Free)", Pattern.UNICODE_CASE);
			Matcher regexMatcher = regex.matcher(HTML);
			while (regexMatcher.find()) {
				worldData.add(new Object[]{regexMatcher.group(1), regexMatcher.group(2), regexMatcher.group(3),
						(regexMatcher.group(4).equals("Y") ? "Yes" : "No"), regexMatcher.group(5)});
			}
		} catch (Exception ignored) {
		}

		Object[][] result = new Object[worldData.size()][5];
		for (int i = 0; i < result.length; i++) {
			result[i] = worldData.get(i);
		}
		return result;
	}

	public World[] getAll(final Filter<World> filter) {
		List<World> worlds = new ArrayList<World>();
		for (Object[] worldData : getWorldObjects()) {
			if (worldData.length == 5) {
				World world = (new World((String) worldData[0], (String) worldData[1], (String) worldData[2],
						(String) worldData[3], (String) worldData[4]));
				if (world != null && filter.accept(world)) {
					worlds.add(world);
				}
			}
		}
		return (World[]) worlds.toArray();
	}

	public World[] getAllMembersIncluded(final boolean members) {
		return getAll(new Filter<World>() {
			public boolean accept(final World world) {
				return world != null && (world.isMembers() == members);
			}
		});
	}

	public RSInterface getInterface() {
		return methods.interfaces.get(INTERFACE_LOBBY);
	}

	public RSComponent getComponent(int index) {
		RSInterface face = getInterface();
		if (face != null && face.isValid()) {
			return face.getComponent(index);
		}
		return null;
	}

	public boolean clickPlay() {
		RSComponent playComp = getComponent(INTERFACE_LOBBY_BUTTON_PLAY);
		return playComp != null && playComp.isValid() && playComp.doClick();
	}

	public int getSelectedTab() {
		int[] ids = new int[]{TAB_PLAYERS, TAB_WORLDS, TAB_FRIENDS, TAB_CLAN, TAB_OPTIONS, TAB_FRIENDS_CHAT};
		for (int id : ids) {
			final RSComponent c = getComponent(id);
			if (c != null && c.isValid() && c.getTextureID() == 4671) {
				return id;
			}
		}
		return -1;
	}

	public boolean open(final int tab) {
		if (getSelectedTab() == tab) {
			return true;
		}
		final RSComponent c = getComponent(tab);
		if (c != null && c.isValid()) {
			c.doClick();
		}
		return getSelectedTab() == tab;
	}

	public int getSelectedWorld() {
		if (!inLobby()) {
			return -1;
		}
		open(TAB_WORLDS);
		if (methods.interfaces.getComponent(INTERFACE_LOBBY_WORLD_SELECT, INTERFACE_WORLD_SELECT_CURRENT_WORLD).isValid()) {
			final String worldText = methods.interfaces.getComponent(INTERFACE_LOBBY_WORLD_SELECT,
					INTERFACE_WORLD_SELECT_CURRENT_WORLD).getText().trim().substring(methods.interfaces.getComponent(
					INTERFACE_LOBBY_WORLD_SELECT, INTERFACE_WORLD_SELECT_CURRENT_WORLD).getText().trim().indexOf("World ") + 6);
			return Integer.parseInt(worldText);
		}
		return -1;
	}

	/**
	 * Enters a world from the lobby.
	 *
	 * @param world The world to switch to.
	 * @param enter To enter the world or not.
	 * @return <tt>true</tt> If correctly entered the world else <tt>false</tt>
	 * @see org.rsbot.script.methods.Game switchWorld(int world)
	 */
	public boolean switchWorlds(final int world, final boolean enter) {
		if (!inLobby() || methods.game.getClientState() == 9 || methods.game.getClientState() == 11) {
			return false;
		}
		if (!methods.interfaces.get(INTERFACE_LOBBY_WORLD_SELECT).isValid() || getSelectedTab() != TAB_WORLDS) {
			open(TAB_WORLDS);
			sleep(random(600, 800));
		}
		if (getSelectedWorld() == world) {
			if (enter) {
				methods.interfaces.getComponent(INTERFACE_LOBBY, INTERFACE_LOBBY_BUTTON_PLAY).doClick();
			}
			return true;
		}
		final RSComponent comp = getWorldComponent(world);
		if (comp != null) {
			methods.interfaces.scrollTo(comp, methods.interfaces.getComponent(INTERFACE_LOBBY_WORLD_SELECT, INTERFACE_WORLD_SELECT_SCROLL_AREA));
			comp.doClick();
			sleep(random(500, 800));
			if (getSelectedWorld() == world) {
				if (enter) {
					methods.interfaces.getComponent(INTERFACE_LOBBY, INTERFACE_LOBBY_BUTTON_PLAY).doClick();
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Enters a world from the lobby.
	 *
	 * @param world The world to switch to.
	 * @return <tt>true</tt> If correctly entered the world else <tt>false</tt>
	 * @see org.rsbot.script.methods.Game switchWorld(int world)
	 */
	public boolean switchWorlds(final int world) {
		return switchWorlds(world, true);
	}

	/**
	 * Gets the component of any world on the lobby interface
	 *
	 * @param world The world to get the component of.
	 * @return The component corresponding to the world.
	 */
	public RSComponent getWorldComponent(final int world) {
		if (!inLobby()) {
			return null;
		}
		if (!methods.interfaces.get(INTERFACE_LOBBY_WORLD_SELECT).isValid()) {
			open(TAB_WORLDS);
		}
		for (int i = 0; i < methods.interfaces.getComponent(INTERFACE_LOBBY_WORLD_SELECT, INTERFACE_WORLD_SELECT_WORLD_NAME).getComponents().length; i++) {
			final RSComponent comp = methods.interfaces.getComponent(INTERFACE_LOBBY_WORLD_SELECT, INTERFACE_WORLD_SELECT_WORLD_NAME).getComponents()[i];
			if (comp != null) {
				final String number = comp.getText();
				if (Integer.parseInt(number) == world) {
					return methods.interfaces.getComponent(INTERFACE_LOBBY_WORLD_SELECT, INTERFACE_WORLD_SELECT_WORLD_LIST).getComponents()[i];
				}
			}
		}
		return null;
	}

	/**
	 * Used for logging out if in lobby
	 *
	 * @return <tt>true</tt> if correctly logged out else false
	 */
	public boolean logout() {
		if (inLobby()) {
			methods.interfaces.getComponent(INTERFACE_LOBBY, INTERFACE_LOBBY_BUTTON_LOGOUT).doClick();
		}
		return !methods.game.isLoggedIn();
	}
}