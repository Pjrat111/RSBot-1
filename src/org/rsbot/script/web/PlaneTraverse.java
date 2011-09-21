package org.rsbot.script.web;

import org.rsbot.script.methods.MethodContext;
import org.rsbot.script.methods.MethodProvider;
import org.rsbot.script.wrappers.RSTile;

public class PlaneTraverse extends MethodProvider {
	private final int plane;
	private RSTile start = null;
	private final RSTile walkTo, dest;
	private Route walkingRoute = null;

	public PlaneTraverse(final int plane, final RSTile walkTo, final RSTile dest, final MethodContext ctx) {
		super(ctx);
		this.plane = plane;
		this.walkTo = walkTo;
		this.dest = dest;
	}

	public int plane() {
		return plane;
	}

	public int destPlane() {
		return dest.getZ();
	}

	public void set(final RSTile tile) {
		start = tile;
	}

	public RSTile walkTo() {
		return walkTo;
	}

	public RSTile dest() {
		return dest;
	}

	public Route getRoute() {
		return walkingRoute == null ? walkingRoute = methods.web.planeRoute(start, dest, null) : walkingRoute;
	}

	public boolean applicable() {
		if (walkingRoute == null) {
			walkingRoute = methods.web.planeRoute(start, dest, null);
		}
		return start != null && walkingRoute != null;
	}
}