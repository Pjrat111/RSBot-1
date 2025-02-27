package org.rsbot.script.methods;

import org.rsbot.script.util.Timer;
import org.rsbot.script.wrappers.RSCharacter;
import org.rsbot.script.wrappers.RSObject;
import org.rsbot.script.wrappers.RSTile;

import java.awt.event.KeyEvent;

/**
 * Camera related operations.
 */
public class Camera extends MethodProvider {
	Camera(final MethodContext ctx) {
		super(ctx);
	}

	/**
	 * Turns to a RSCharacter (RSNPC or RSPlayer).
	 *
	 * @param c The RSCharacter to turn to.
	 */
	public void turnTo(final RSCharacter c) {
		final int angle = getCharacterAngle(c);
		setAngle(angle);
	}

	/**
	 * Turns to within a few degrees of an RSCharacter (RSNPC or RSPlayer).
	 *
	 * @param c   The RSCharacter to turn to.
	 * @param dev The maximum difference in the angle.
	 */
	public void turnTo(final RSCharacter c, final int dev) {
		int angle = getCharacterAngle(c);
		angle = random(angle - dev, angle + dev + 1);
		setAngle(angle);
	}

	/**
	 * Turns to an RSObject.
	 *
	 * @param o The RSObject to turn to.
	 */
	public void turnTo(final RSObject o) {
		final int angle = getObjectAngle(o);
		setAngle(angle);
	}

	/**
	 * Turns to within a few degrees of an RSObject.
	 *
	 * @param o   The RSObject to turn to.
	 * @param dev The maximum difference in the turn angle.
	 */
	public void turnTo(final RSObject o, final int dev) {
		int angle = getObjectAngle(o);
		angle = random(angle - dev, angle + dev + 1);
		setAngle(angle);
	}

	/**
	 * Turns to a specific RSTile.
	 *
	 * @param tile Tile to turn to.
	 */
	public void turnTo(final RSTile tile) {
		final int angle = getTileAngle(tile);
		setAngle(angle);
	}

	/**
	 * Turns within a few degrees to a specific RSTile.
	 *
	 * @param tile Tile to turn to.
	 * @param dev  Maximum deviation from the angle to the tile.
	 */
	public void turnTo(final RSTile tile, final int dev) {
		int angle = getTileAngle(tile);
		angle = random(angle - dev, angle + dev + 1);
		setAngle(angle);
	}

	/**
	 * Sets the altitude to max or minimum.
	 *
	 * @param up True to go up. False to go down.
	 * @return <tt>true</tt> if the altitude was changed.
	 */
	public boolean setPitch(final boolean up) {
		if (up) {
			return setPitch(100);
		}
		return setPitch(0);
	}

	/**
	 * Set the camera to a certain percentage of the maximum pitch. Don't rely
	 * on the return value too much - it should return whether the camera was
	 * successfully set, but it isn't very accurate near the very extremes of
	 * the height.
	 * <p/>
	 * <p/>
	 * This also depends on the maximum camera angle in a region, as it changes
	 * depending on situation and surroundings. So in some areas, 68% might be
	 * the maximum altitude. This method will do the best it can to switch the
	 * camera altitude to what you want, but if it hits the maximum or stops
	 * moving for any reason, it will return.
	 * <p/>
	 * <p/>
	 * <p/>
	 * Mess around a little to find the altitude percentage you like. In later
	 * versions, there will be easier-to-work-with methods regarding altitude.
	 *
	 * @param percent The percentage of the maximum pitch to set the camera to.
	 * @return true if the camera was successfully moved; otherwise false.
	 */
	public boolean setPitch(final int percent) {
		int curAlt = getPitch();
		int lastAlt = 0;
		if (curAlt == percent) {
			return true;
		} else if (curAlt < percent) {
			methods.inputManager.pressKey((char) KeyEvent.VK_UP);
			long start = System.currentTimeMillis();
			while (curAlt < percent && System.currentTimeMillis() - start < random(50, 100)) {
				if (lastAlt != curAlt) {
					start = System.currentTimeMillis();
				}
				lastAlt = curAlt;
				sleep(random(5, 10));
				curAlt = getPitch();
			}
			methods.inputManager.releaseKey((char) KeyEvent.VK_UP);
			return true;
		} else {
			methods.inputManager.pressKey((char) KeyEvent.VK_DOWN);
			long start = System.currentTimeMillis();
			while (curAlt > percent && System.currentTimeMillis() - start < random(50, 100)) {
				if (lastAlt != curAlt) {
					start = System.currentTimeMillis();
				}
				lastAlt = curAlt;
				sleep(random(5, 10));
				curAlt = getPitch();
			}
			methods.inputManager.releaseKey((char) KeyEvent.VK_DOWN);
			return true;
		}
	}

	/**
	 * Moves the camera in a random direction for a given time.
	 *
	 * @param timeOut The maximum time in milliseconds to move the camera for.
	 */
	public void moveRandomly(final int timeOut) {
		final Timer timeToHold = new Timer(timeOut);
		final int highest = random(75, 100);
		final int lowest = random(0, 25);
		final int vertical = Math.random() < Math.random() ? KeyEvent.VK_UP : KeyEvent.VK_DOWN;
		final int horizontal = Math.random() < Math.random() ? KeyEvent.VK_LEFT : KeyEvent.VK_RIGHT;
		boolean verticalKeyDown = false;
		boolean horizontalKeyDown = false;
		if (random(0, 10) < 8) {
			methods.inputManager.pressKey((char) vertical);
			verticalKeyDown = true;
		}
		if (random(0, 10) < 8) {
			methods.inputManager.pressKey((char) horizontal);
			horizontalKeyDown = true;
		}
		while (timeToHold.isRunning() && (verticalKeyDown || horizontalKeyDown)) {
			if (getPitch() >= highest && vertical == KeyEvent.VK_UP ||
					getPitch() <= lowest && vertical == KeyEvent.VK_DOWN) {
				methods.inputManager.releaseKey((char) vertical);
				verticalKeyDown = false;
			}
			sleep(10);
		}
		if (verticalKeyDown) {
			methods.inputManager.releaseKey((char) vertical);
		}
		if (horizontalKeyDown) {
			methods.inputManager.releaseKey((char) horizontal);
		}
	}

	/**
	 * Rotates the camera to a specific angle in the closest direction.
	 *
	 * @param degrees The angle to rotate to.
	 */
	public void setAngle(final int degrees) {
		if (getAngleTo(degrees) > 5) {
			methods.inputManager.pressKey((char) KeyEvent.VK_LEFT);
			while (getAngleTo(degrees) > 5  && methods.game.getClientState() == 11) {
				sleep(10);
			}
			methods.inputManager.releaseKey((char) KeyEvent.VK_LEFT);
		} else if (getAngleTo(degrees) < -5) {
			methods.inputManager.pressKey((char) KeyEvent.VK_RIGHT);
			while (getAngleTo(degrees) < -5  && methods.game.getClientState() == 11) {
				sleep(10);
			}
			methods.inputManager.releaseKey((char) KeyEvent.VK_RIGHT);
		}
	}

	/**
	 * Rotates the camera to the specified cardinal direction.
	 *
	 * @param direction The char direction to turn the map. char options are w,s,e,n
	 *                  and defaults to north if character is unrecognized.
	 */
	public void setCompass(final char direction) {
		switch (direction) {
			case 'n':
				setAngle(359);
				break;
			case 'w':
				setAngle(89);
				break;
			case 's':
				setAngle(179);
				break;
			case 'e':
				setAngle(269);
				break;
			default:
				setAngle(359);
				break;
		}
	}

	/**
	 * Uses the compass component to set the camera to face north.
	 */
	public void setNorth() {
		methods.interfaces.getComponent(methods.gui.getCompass().getID()).doClick();
	}

	/**
	 * Returns the camera angle at which the camera would be facing a certain
	 * character.
	 *
	 * @param n the RSCharacter
	 * @return The angle
	 */
	public int getCharacterAngle(final RSCharacter n) {
		return getTileAngle(n.getLocation());
	}

	/**
	 * Returns the camera angle at which the camera would be facing a certain
	 * object.
	 *
	 * @param o The RSObject
	 * @return The angle
	 */
	public int getObjectAngle(final RSObject o) {
		return getTileAngle(o.getLocation());
	}

	/**
	 * Returns the camera angle at which the camera would be facing a certain
	 * tile.
	 *
	 * @param t The target tile
	 * @return The angle in degrees
	 */
	public int getTileAngle(final RSTile t) {
		final int a = (methods.calc.angleToTile(t) - 90) % 360;
		return a < 0 ? a + 360 : a;
	}

	/**
	 * Returns the angle between the current camera angle and the given angle in
	 * degrees.
	 *
	 * @param degrees The target angle.
	 * @return The angle between the who angles in degrees.
	 */
	public int getAngleTo(final int degrees) {
		int ca = getAngle();
		if (ca < degrees) {
			ca += 360;
		}
		int da = ca - degrees;
		if (da > 180) {
			da -= 360;
		}
		return da;
	}

	/**
	 * Returns the current compass orientation in degrees, with North at 0,
	 * increasing counter-clockwise to 360.
	 *
	 * @return The current camera angle in degrees.
	 */
	public int getAngle() {
		// the client uses fixed point radians 0 - 2^14
		// degrees = yaw * 360 / 2^14 = yaw / 45.5111...
		return (int) (methods.client.getCameraYaw() / 45.51);
	}

	/**
	 * Returns the current percentage of the maximum pitch of the camera in an
	 * open area.
	 *
	 * @return The current camera altitude percentage.
	 */
	public int getPitch() {
		return (int) ((methods.client.getCameraPitch() - 1024) / 20.48);
	}

	/**
	 * Returns the current x position of the camera.
	 *
	 * @return The x position.
	 */
	public int getX() {
		return methods.client.getCamPosX();
	}

	/**
	 * Returns the current y position of the camera.
	 *
	 * @return The y position.
	 */
	public int getY() {
		return methods.client.getCamPosY();
	}

	/**
	 * Returns the current z position of the camera.
	 *
	 * @return The z position.
	 */
	public int getZ() {
		return methods.client.getCamPosZ();
	}
}
