package org.rsbot.script.wrappers;

import org.rsbot.client.Model;
import org.rsbot.client.RSAnimable;
import org.rsbot.script.methods.MethodContext;
import org.rsbot.script.methods.MethodProvider;

import java.awt.*;


public class RSObject extends MethodProvider {

	public static enum Type {
		INTERACTABLE, FLOOR_DECORATION, BOUNDARY, WALL_DECORATION
	}

	private final org.rsbot.client.RSObject obj;
	private final Type type;
	private final int plane;

	public RSObject(final MethodContext ctx,
	                final org.rsbot.client.RSObject obj, final Type type,
	                final int plane) {
		super(ctx);
		this.obj = obj;
		this.type = type;
		this.plane = plane;
	}

	/**
     * Performs the specified action on this object.
     *
     * @param action the menu item to search and click
     * @return returns true if clicked, false if object does not contain the
     *         desired action
     */
    public boolean doAction(final String action) {
    	return doAction(action, null);
    }

	/**
     * Performs the specified action on this object.
     *
     * @param action the action of the menu item to search and click
     * @param option the option of the menu item to search and click
     * @return returns true if clicked, false if object does not contain the
     *         desired action
     */
    public boolean doAction(final String action, final String option) {
    	final RSModel model = getModel();
    	if (model != null) {
    		return model.doAction(action, option);
    	}
    	return methods.tiles.doAction(getLocation(), action, option);
    }

	/**
     * Left-clicks this object.
     *
     * @return <tt>true</tt> if clicked.
     */
    public boolean doClick() {
    	return doClick(true);
    }

	/**
     * Clicks this object.
     *
     * @param leftClick <tt>true</tt> to left-click; <tt>false</tt> to right-click.
     * @return <tt>true</tt> if clicked.
     */
    public boolean doClick(final boolean leftClick) {
    	final RSModel model = getModel();
    	if (model != null) {
    		return model.doClick(leftClick);
    	} else {
    		Point p = methods.calc.tileToScreen(getLocation());
    		if (methods.calc.pointOnScreen(p)) {
    			methods.mouse.move(p);
    			if (methods.calc.pointOnScreen(p)) {
    				methods.mouse.click(leftClick);
    				return true;
    			} else {
    				p = methods.calc.tileToScreen(getLocation());
    				if (methods.calc.pointOnScreen(p)) {
    					methods.mouse.move(p);
    					methods.mouse.click(leftClick);
    					return true;
    				}
    			}
    		}
    		return false;
    	}
    }

	/**
     * Moves the mouse over this object.
     */
    public void doHover() {
    	final RSModel model = getModel();
    	if (model != null) {
    		model.hover();
    	} else {
    		final Point p = methods.calc.tileToScreen(getLocation());
    		if (methods.calc.pointOnScreen(p)) {
    			methods.mouse.move(p);
    		}
    	}
    }

	/**
     * Gets the area of tiles covered by this object.
     *
     * @return The RSArea containing all the tiles on which this object can be
     *         found.
     */
    public RSArea getArea() {
    	if (obj instanceof RSAnimable) {
    		final RSAnimable a = (RSAnimable) obj;
    		final RSTile sw = new RSTile(methods.client.getBaseX() + a.getX1(),
    				methods.client.getBaseY() + a.getY1());
    		final RSTile ne = new RSTile(methods.client.getBaseX() + a.getX2(),
    				methods.client.getBaseY() + a.getY2());
    		return new RSArea(sw, ne, plane);
    	}
    	final RSTile loc = getLocation();
    	return new RSArea(loc, loc, plane);
    }

	/**
     * Gets the object definition of this object.
     *
     * @return The RSObjectDef if available, otherwise <code>null</code>.
     */
    public RSObjectDef getDef() {
    	final org.rsbot.client.Node ref = methods.nodes.lookup(
    			methods.client.getRSObjectDefLoader(), getID());
    	if (ref != null) {
    		if (ref instanceof org.rsbot.client.HardReference) {
    			return new RSObjectDef(
    					(org.rsbot.client.RSObjectDef) ((org.rsbot.client.HardReference) ref)
    							.get());
    		} else if (ref instanceof org.rsbot.client.SoftReference) {
    			final Object def = ((org.rsbot.client.SoftReference) ref)
    					.getReference().get();
    			if (def != null) {
    				return new RSObjectDef((org.rsbot.client.RSObjectDef) def);
    			}
    		}
    	}
    	return null;
    }

	/**
     * Gets the ID of this object.
     *
     * @return The ID.
     */
    public int getID() {
    	return obj.getID();
    }

	/**
	 * Gets the RSTile on which this object is centered. An RSObject may cover
	 * multiple tiles, in which case this will return the floored central tile.
	 *
	 * @return The central RSTile.
	 * @see #getArea()
	 */
	public RSTile getLocation() {
		return new RSTile(methods.client.getBaseX() + obj.getX() / 512,
				methods.client.getBaseY() + obj.getY() / 512, plane);
	}

	/**
     * Gets the Model of this object.
     *
     * @return The RSModel, or null if unavailable.
     */
    public RSModel getModel() {
    	try {
    		final Model model = obj.getModel();
    		if (model != null && model.getXPoints() != null) {
    			return new RSObjectModel(methods, model, obj);
    		}
    	} catch (final AbstractMethodError ignored) {
    	}
    	return null;
    }

	/**
     * Returns the name of the object.
     *
     * @return The object name if the definition is available; otherwise "".
     */
    public String getName() {
    	final RSObjectDef objectDef = getDef();
    	return objectDef != null ? objectDef.getName() : "";
    }

	/**
	 * Returns the name of the object.
	 *
	 * @param object The object to look up.
	 * @return The object name if the definition is available; otherwise "".
	 */
	@Deprecated
	public String getName(final RSObject object) {
		return object.getName();
	}

	/**
     * Returns this object's type.
     *
     * @return The type of the object.
     */
    public Type getType() {
    	return type;
    }

	/**
	 * Determines whether or not this object is on the game screen.
	 *
	 * @return <tt>true</tt> if the object is on screen.
	 */
	public boolean isOnScreen() {
		final RSModel model = getModel();
		if (model == null) {
			return methods.calc.tileOnScreen(getLocation());
		} else {
			return methods.calc.pointOnScreen(model.getPoint());
		}
	}

	@Override
	public boolean equals(final Object o) {
		return o instanceof RSObject && ((RSObject) o).obj == obj;
	}

	@Override
	public int hashCode() {
		return obj.hashCode();
	}

}
