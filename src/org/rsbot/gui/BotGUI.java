package org.rsbot.gui;

public final class BotGUI {
	/**
	 * Opens an url.
	 *
	 * @param url The URL to open.
	 * @see org.rsbot.gui.Chrome#openURL(String)
	 * @deprecated Class name changed.
	 */
	@Deprecated
	public static void openURL(final String url) {
		Chrome.openURL(url);
	}
}
