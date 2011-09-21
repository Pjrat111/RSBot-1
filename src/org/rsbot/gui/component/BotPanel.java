package org.rsbot.gui.component;

import org.rsbot.bot.Bot;
import org.rsbot.event.EventManager;
import org.rsbot.gui.Chrome;
import org.rsbot.script.methods.Mouse;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import static org.rsbot.script.methods.Environment.INPUT_KEYBOARD;
import static org.rsbot.script.methods.Environment.INPUT_MOUSE;

/**
 */
public class BotPanel extends JPanel {
	private static final long serialVersionUID = 2269767882075468055L;

	private class HomeUpdater implements Runnable {
		private boolean running;

		public void run() {
			synchronized (this) {
				if (running) {
					throw new IllegalStateException("Already running!");
				}
				running = true;
			}
			while (true) {
				synchronized (this) {
					if (!running) {
						break;
					}
				}
				repaint();
				try {
					Thread.sleep(70);
				} catch (final Exception ex) {
					break;
				}
			}
			synchronized (this) {
				running = false;
			}
		}

		public void stop() {
			synchronized (this) {
				running = false;
			}
		}
	}

	private Bot bot;
	private final HomeUpdater updater;
	private Point offset;
	private boolean present;

	public BotPanel() {
		updater = new HomeUpdater();
		offset = new Point(0, 0);
		setSize(new Dimension(Chrome.PANEL_WIDTH, Chrome.PANEL_HEIGHT));
		setMinimumSize(new Dimension(Chrome.PANEL_WIDTH, Chrome.PANEL_HEIGHT));
		setPreferredSize(new Dimension(Chrome.PANEL_WIDTH, Chrome.PANEL_HEIGHT));
		setBackground(Color.BLACK);
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(final ComponentEvent evt) {
				if (bot != null) {
					bot.resize(getWidth(), getHeight());
					offset();
				}
				requestFocus();
			}
		});
		addMouseListener(new MouseListener() {
			public void mouseClicked(final MouseEvent e) {
				redispatch(e);
				if (!hasFocus()) {
					requestFocus();
				}
			}

			public void mouseEntered(final MouseEvent e) {
			}

			public void mouseExited(final MouseEvent e) {
				redispatch(e);
			}

			public void mousePressed(final MouseEvent e) {
				redispatch(e);
			}

			public void mouseReleased(final MouseEvent e) {
				redispatch(e);
			}
		});
		addMouseMotionListener(new MouseMotionListener() {
			public void mouseDragged(final MouseEvent e) {
				redispatch(e);
			}

			public void mouseMoved(final MouseEvent e) {
				redispatch(e);
			}
		});
		addMouseWheelListener(new MouseWheelListener() {
			public void mouseWheelMoved(final MouseWheelEvent e) {
				redispatch(e);
			}
		});
		addKeyListener(new KeyListener() {
			public void keyPressed(final KeyEvent e) {
				redispatch(e);
			}

			public void keyReleased(final KeyEvent e) {
				redispatch(e);
			}

			public void keyTyped(final KeyEvent e) {
				redispatch(e);
			}
		});
	}

	public void offset() {
		if (bot.getCanvas() != null) {
			offset.x = (getWidth() - bot.getCanvas().getWidth()) / 2;
			offset.y = (getHeight() - bot.getCanvas().getHeight()) / 2;
		}
	}

	public void setBot(final Bot bot) {
		if (this.bot != null) {
			this.bot.setPanel(null);
		} else {
			updater.stop();
		}
		this.bot = bot;
		if (bot != null) {
			bot.setPanel(this);
			offset();
		} else {
			new Thread(updater).start();
		}
	}

	@Override
	public void paintComponent(final Graphics g) {
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, getWidth(), getHeight());
		if (bot != null) {
			g.drawImage(bot.getImage(), offset.x, offset.y, null);
		}
	}

	private void redispatch(final MouseEvent e) {
		if (bot != null && bot.getLoader().getComponentCount() > 0) {
			final Mouse mouse = bot.getMethodContext().mouse;
			if (mouse == null) {
				return; // client cannot currently accept events
			}
			final boolean present = mouse.isPresent();
			final Component c = bot.getLoader().getComponent(0);
			// account for horizontal offset
			e.translatePoint(-offset.x, -offset.y);
			// fire human mouse event for scripts
			dispatchHuman(c, e);
			if (!bot.overrideInput && (bot.inputFlags & INPUT_MOUSE) == 0) {
				return;
			}
			if (e.getX() > 0 && e.getX() < c.getWidth() && e.getY() < c.getHeight() && e.getID() != MouseEvent.MOUSE_EXITED) {
				if (present) {
					if (e instanceof MouseWheelEvent) {
						final MouseWheelEvent mwe = (MouseWheelEvent) e;
						c.dispatchEvent(new MouseWheelEvent(c, e.getID(), System.currentTimeMillis(), 0, e.getX(), e.getY(), 0, e.isPopupTrigger(), mwe.getScrollType(), mwe.getScrollAmount(), mwe.getWheelRotation()));
					} else {
						c.dispatchEvent(new MouseEvent(c, e.getID(), System.currentTimeMillis(), 0, e.getX(), e.getY(), 0, e.isPopupTrigger(), e.getButton()));
					}
				} else {
					c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, e.getX(), e.getY(), 0, false));
				}
			} else if (present) {
				c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, e.getX(), e.getY(), 0, false));
			}
		}
	}

	private void redispatch(final KeyEvent e) {
		if (bot != null) {
			final EventManager m = bot.getEventManager();
			if (m != null) {
				m.dispatchEvent(e);
			}
			if ((bot.overrideInput || (bot.inputFlags & INPUT_KEYBOARD) != 0) && bot.getLoader().getComponentCount() > 0) {
				final Component c = bot.getLoader().getComponent(0);
				c.dispatchEvent(e);
			}
		}
	}

	private void dispatchHuman(final Component c, final MouseEvent e) {
		if (e.getX() > 0 && e.getX() < c.getWidth() && e.getY() < c.getHeight() && e.getID() != MouseEvent.MOUSE_EXITED) {
			if (present) {
				bot.getEventManager().dispatchEvent(e);
			} else {
				present = true;
				bot.getEventManager().dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, e.getX(), e.getY(), 0, false));
			}
		} else if (present) {
			present = false;
			bot.getEventManager().dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, e.getX(), e.getY(), 0, false));
		}
	}
}