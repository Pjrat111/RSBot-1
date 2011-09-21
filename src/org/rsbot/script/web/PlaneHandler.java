package org.rsbot.script.web;

import org.rsbot.script.methods.MethodContext;
import org.rsbot.script.methods.MethodProvider;

import java.util.ArrayList;
import java.util.List;

public class PlaneHandler extends MethodProvider {
	final List<PlaneTraverse> traverseList = new ArrayList<PlaneTraverse>();

	public PlaneHandler(final MethodContext ctx) {
		super(ctx);
		Traverse traverse = new Traverse();
		traverseList.add(traverse.example);
	}

	private class Traverse {
		public final PlaneTraverse example = new PlaneTraverse(0, null, null, methods);
	}

	public PlaneTraverse[] get(final int plane) {
		List<PlaneTraverse> collectedTraverseList = new ArrayList<PlaneTraverse>();
		for (PlaneTraverse planeTraverse : traverseList) {
			if (planeTraverse.plane() == plane) {
				collectedTraverseList.add(planeTraverse);
			}
		}
		return collectedTraverseList.toArray(new PlaneTraverse[collectedTraverseList.size()]);
	}
}
