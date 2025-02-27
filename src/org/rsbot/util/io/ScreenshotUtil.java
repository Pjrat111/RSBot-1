package org.rsbot.util.io;

import org.rsbot.Configuration;
import org.rsbot.bot.Bot;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScreenshotUtil {

	private static final Logger log = Logger.getLogger(ScreenshotUtil.class.getName());
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-hhmmss");

	public static void saveScreenshot(final Bot bot, final boolean hideUsername) {
		final String name = ScreenshotUtil.dateFormat.format(new Date()) + ".png";
		final File dir = new File(Configuration.Paths.getScreenshotsDirectory());
		if (dir.isDirectory() || dir.mkdirs()) {
			ScreenshotUtil.saveScreenshot(bot, new File(dir, name), "png", hideUsername);
		}
	}

	public static void saveScreenshot(final Bot bot, final boolean hideUsername, String filename) {
		if (!filename.endsWith(".png")) {
			filename = filename.concat(".png");
		}

		final File dir = new File(Configuration.Paths.getScreenshotsDirectory());
		if (dir.isDirectory() || dir.mkdirs()) {
			ScreenshotUtil.saveScreenshot(bot, new File(dir, filename), "png", hideUsername);
		}
	}

	private static void saveScreenshot(final Bot bot, final File file, final String type, final boolean hideUsername) {
		try {
			final BufferedImage image = takeScreenshot(bot, hideUsername);

			ImageIO.write(image, type, file);
			ScreenshotUtil.log.info("Screenshot saved to: " + file.getPath());
		} catch (final Exception e) {
			ScreenshotUtil.log.log(Level.SEVERE, "Could not take screenshot.", e);
		}
	}

	public static BufferedImage takeScreenshot(final Bot bot, final boolean hideUsername) {
		final BufferedImage source = bot.getImage();
		final WritableRaster raster = source.copyData(null);
		final BufferedImage bufferedImage = new BufferedImage(source.getColorModel(), raster,
				source.isAlphaPremultiplied(), null);

		if (hideUsername) {
			final Graphics graphics = bufferedImage.createGraphics();
			graphics.setColor(Color.black);
			graphics.fillRect(9, 457, 100, 15);
			graphics.dispose();
		}
		return bufferedImage;
	}
}
