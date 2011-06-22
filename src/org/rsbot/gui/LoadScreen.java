package org.rsbot.gui;

import org.rsbot.Configuration;
import org.rsbot.locale.Messages;
import org.rsbot.log.LabelLogHandler;
import org.rsbot.log.LogOutputStream;
import org.rsbot.log.SystemConsoleHandler;
import org.rsbot.script.provider.ScriptDeliveryNetwork;
import org.rsbot.security.RestrictedSecurityManager;
import org.rsbot.util.UpdateChecker;
import org.rsbot.util.io.HttpClient;
import org.rsbot.util.io.IOHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoadScreen extends JDialog {
	private final static Logger log = Logger.getLogger(LoadScreen.class.getName());
	private static final long serialVersionUID = 5520543482560560389L;
	private final boolean error;
	private static LoadScreen instance = null;

	private LoadScreen() {
		JDialog.setDefaultLookAndFeelDecorated(true);
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (final Exception ignored) {
		}
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				System.exit(1);
			}
		});
		setTitle(Configuration.NAME);
		setIconImage(Configuration.getImage(Configuration.Paths.Resources.ICON));
		final JPanel panel = new JPanel(new GridLayout(2, 1));
		final int pad = 10;
		panel.setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));
		final JProgressBar progress = new JProgressBar();
		progress.setPreferredSize(new Dimension(350, progress.getPreferredSize().height));
		progress.setIndeterminate(true);
		panel.add(progress);
		final LabelLogHandler handler = new LabelLogHandler();
		Logger.getLogger("").addHandler(handler);
		handler.label.setBorder(BorderFactory.createEmptyBorder(pad, 0, 0, 0));
		final Font font = handler.label.getFont();
		handler.label.setFont(new Font(font.getFamily(), Font.BOLD, font.getSize()));
		handler.label.setPreferredSize(new Dimension(progress.getWidth(), handler.label.getPreferredSize().height + pad));
		panel.add(handler.label);
		log.info("Loading");
		add(panel);
		pack();
		setLocationRelativeTo(getOwner());
		setResizable(false);
		setVisible(true);
		setModal(true);
		setAlwaysOnTop(true);

		log.info("Language: " + Messages.LANGUAGE);

		log.info("Registering logs");
		bootstrap();

		log.info("Extracting resources");
		try {
			extractResources();
		} catch (final IOException ignored) {
		}

		log.info("Creating directories");
		Configuration.createDirectories();

		log.info("Enforcing security policy");
		if (Configuration.GOOGLEDNS) {
			System.setProperty("sun.net.spi.nameservice.nameservers", RestrictedSecurityManager.DNSA + "," + RestrictedSecurityManager.DNSB);
			System.setProperty("sun.net.spi.nameservice.provider.1", "dns,sun");
		}
		System.setProperty("java.io.tmpdir", Configuration.Paths.getGarbageDirectory());
		System.setSecurityManager(new RestrictedSecurityManager());

		final String downloading = "Downloading resources";
		log.info(downloading);
		for (final Entry<String, File> item : Configuration.Paths.getCachableResources().entrySet()) {
			try {
				log.fine(downloading + " (" + item.getValue().getName() + ")");
				HttpClient.download(new URL(item.getKey()), item.getValue());
			} catch (final IOException ignored) {
			}
		}

		log.info("Downloading network scripts");
		ScriptDeliveryNetwork.getInstance().sync();

		if (Configuration.isSkinAvailable()) {
			log.info("Setting theme");
			final Component instance = this;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					try {
						UIManager.setLookAndFeel(Configuration.SKIN);
						SwingUtilities.updateComponentTreeUI(instance);
					} catch (final Exception ignored) {
					}
				}
			});
		}

		log.info("Checking for updates");

		String error = null;

		if (UpdateChecker.isError()) {
			error = "Unable to obtain latest version information";
		} else if (Configuration.RUNNING_FROM_JAR) {
			try {
				if (UpdateChecker.isDeprecatedVersion()) {
					error = "Please update at " + Configuration.Paths.URLs.DOWNLOAD_SHORT;
				}
			} catch (final IOException ignored) {
			}
		} else {
			error = null;
		}

		if (error == null) {
			this.error = false;
			log.info("Loading bot");
			Configuration.registerLogging();
			Logger.getLogger("").removeHandler(handler);
		} else {
			this.error = true;
			progress.setIndeterminate(false);
			log.severe(error);
		}
	}

	public static boolean showDialog() {
		instance = new LoadScreen();
		return !instance.error;
	}

	public static void quit() {
		if (instance != null) {
			instance.dispose();
		}
	}

	private static void bootstrap() {
		Logger.getLogger("").setLevel(Configuration.RUNNING_FROM_JAR ? Level.INFO : Level.FINE);
		Logger.getLogger("").addHandler(new SystemConsoleHandler());
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			private final Logger log = Logger.getLogger("EXCEPTION");

			public void uncaughtException(final Thread t, final Throwable e) {
				final String ex = "Exception", msg = t.getName() + ": ";
				if (Configuration.RUNNING_FROM_JAR) {
					Logger.getLogger(ex).severe(msg + e.toString());
				} else {
					log.logp(Level.SEVERE, ex, "", msg, e);
				}
			}
		});
		if (!Configuration.RUNNING_FROM_JAR) {
			System.setErr(new PrintStream(new LogOutputStream(Logger.getLogger("STDERR"), Level.SEVERE), true));
		}
	}

	private static void extractResources() throws IOException {
		final String[] extract;
		if (Configuration.getCurrentOperatingSystem() == Configuration.OperatingSystem.WINDOWS) {
			extract = new String[]{Configuration.Paths.Resources.COMPILE_SCRIPTS_BAT, Configuration.Paths.Resources.COMPILE_FIND_JDK};
		} else {
			extract = new String[]{Configuration.Paths.Resources.COMPILE_SCRIPTS_SH};
		}
		for (final String item : extract) {
			IOHelper.write(Configuration.getResourceURL(item).openStream(), new File(Configuration.Paths.getHomeDirectory(), new File(item).getName()));
		}
	}
}
