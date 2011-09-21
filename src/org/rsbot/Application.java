package org.rsbot;

import org.rsbot.bot.Bot;
import org.rsbot.gui.Chrome;
import org.rsbot.gui.LoadScreen;

import javax.swing.*;
import java.awt.*;

public class Application {
	private static Chrome gui;

	public static void main(final String[] args) {
		JFrame.setDefaultLookAndFeelDecorated(true);
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (final Exception ignored) {
		}
		if (LoadScreen.showDialog()) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					gui = new Chrome();
				}
			});
		}
	}

	public static Bot getBot(final Object o) {
		return gui.getBot(o);
	}

	public static Dimension getPanelSize() {
		return gui.getPanel().getSize();
	}
}
