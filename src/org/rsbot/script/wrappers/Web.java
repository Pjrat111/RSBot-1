package org.rsbot.script.wrappers;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.rsbot.script.methods.MethodContext;
import org.rsbot.util.GlobalConfiguration;

/**
 * The web generation and wrapper control.
 *
 * @author Timer
 * @author Aut0r
 */

public class Web extends WebSkeleton {

	/**
	 * The end tile.
	 */
	private final RSTile from, to;

	/**
	 * The WebPath variable.
	 */
	public WebPath path = null;

	/**
	 * The WebMap allocation.
	 */
	private WebMap map = null;

	/**
	 * @param ctx   The MethodContext.
	 * @param start The tile you wish to start the web at.
	 * @param end   The end tile you wish to result at.
	 */
	public Web(final MethodContext ctx, final RSTile start, final RSTile end) {
		super(ctx);
		from = start;
		to = end;
		getPath();
	}

	/**
	 * Returns if the map needs set.
	 *
	 * @return <tt>true</tt> if the map needs set, otherwise false.
	 */
	public boolean mapNeedsSet() {
		return map == null;
	}

	/**
	 * Sets the map up.
	 */
	public void setMap() {
		try {
			final List<WebTile> teTiles = new ArrayList<WebTile>();
			final File mapData = new File(GlobalConfiguration.Paths.getWebCache());
			if (mapData.exists() && mapData.canRead()) {
				final int xOff = 2045;
				final int yOff = 4168;
				final FileInputStream fis = new FileInputStream(mapData);
				final DataInputStream dis = new DataInputStream(fis);
				final BufferedReader br = new BufferedReader(new InputStreamReader(dis));
				String strLine = "";
				while ((strLine = br.readLine()) != null) {
					try {
						final String[] strArr = strLine.split("=");
						if (strArr != null && strArr.length == 3) {
							final String[] spl = strArr[2].split(",");
							final int[] nA = new int[spl.length];
							int i = 0;
							for (final String iSPL : spl) {
								if (iSPL.length() > 0) {
									nA[i] = Integer.parseInt(iSPL);
									i++;
								}
							}
							final int[] nAA = new int[i];
							for (final int na : nA) {
								i--;
								nAA[i] = na;
								if (i == 0) {
									break;
								}
							}
							teTiles.add(new WebTile(new RSTile(xOff + Integer
									.parseInt(strArr[0]), yOff - Integer
									.parseInt(strArr[1])), nAA, null));
						} else if (strArr != null && strArr.length == 5) {
							final String[] spl = strArr[4].split(",");
							final int[] nA = new int[spl.length];
							int i = 0;
							for (final String iSPL : spl) {
								if (iSPL.length() > 0) {
									nA[i] = Integer.parseInt(iSPL);
									i++;
								}
							}
							final int[] nAA = new int[i];
							for (final int na : nA) {
								i--;
								nAA[i] = na;
								if (i == 0) {
									break;
								}
							}
							teTiles.add(new WebTile(new RSTile(xOff + Integer
									.parseInt(strArr[0]), yOff - Integer
									.parseInt(strArr[1])), nAA, null));
						}
					} catch (final Exception e) {
						e.printStackTrace();
						map = null;
						return;
					}
				}
				map = new WebMap(teTiles.toArray(new WebTile[teTiles.size()]));
				br.close();
				return;
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
		map = null;
	}

	/**
	 * Traverses on the web path.
	 *
	 * @param options The flags to take into account while traversing the path.
	 * @return <tt>true</tt> if walked, otherwise false.
	 */
	@Override
	public boolean traverse(final EnumSet<TraversalOption> options) {
		return path != null ? path.traverse(options) : false;
	}

	/**
	 * Gets the path between two tiles.
	 */
	public void getPath() {
		try {
			if (mapNeedsSet()) {
				setMap();
			}
			if (from == null || to == null || map == null) {
				path = null;
				return;
			}
			final WebTile start = map.getWebTile(from);
			final WebTile end = map.getWebTile(to);
			if (start == null || end == null) {
				path = null;
				return;
			}
			if (start.equals(end)) {
				path = new WebPath(methods, new WebTile[]{end});
				return;
			}
			final HashSet<WebTile> open = new HashSet<WebTile>();
			final HashSet<WebTile> closed = new HashSet<WebTile>();
			WebTile curr = start;
			final WebTile dest = end;
			curr.f = map.heuristic(curr, dest);
			open.add(curr);
			while (!open.isEmpty()) {
				curr = lowest_f(open);
				if (curr.equals(dest)) {
					path = new WebPath(methods, path(curr));
					return;
				}
				open.remove(curr);
				closed.add(curr);
				for (final int iNext : curr.connectingIndex()) {
					final WebTile next = map.getWebTile(iNext);
					if (next.req != null) {
						if (!next.req.canDo()) {
							continue;
						}
					}
					if (!closed.contains(next)) {
						final double t = curr.g + map.dist(curr, next);
						boolean use_t = false;
						if (!open.contains(next)) {
							open.add(next);
							use_t = true;
						} else if (t < next.g) {
							use_t = true;
						}
						if (use_t) {
							next.parent = curr;
							next.g = t;
							next.f = t + map.heuristic(next, dest);
						}
					}
				}
			}
			path = null;
			return;
		} catch (final Exception e) {
			e.printStackTrace();
			path = null;
			return;
		}
	}

	/**
	 * Gets the lowest f score.
	 *
	 * @param open The set of web tiles.
	 * @return The lowest f score tile.
	 */
	private WebTile lowest_f(final Set<WebTile> open) {
		WebTile best = null;
		for (final WebTile t : open) {
			if (best == null || t.f < best.f) {
				best = t;
			}
		}
		return best;
	}

	/**
	 * Returns the array of tiles in the web.
	 *
	 * @return The array of tiles.
	 */
	private RSTile[] path() {
		return path.getTiles();
	}

	/**
	 * Returns a map.
	 *
	 * @return The web map.
	 */
	public WebMap map() {
		if (mapNeedsSet()) {
			setMap();
		}
		return map;
	}

	/**
	 * Reconstructs the path.
	 *
	 * @param end The final web tile.
	 * @return The path.
	 */
	private WebTile[] path(final WebTile end) {
		final LinkedList<RSTile> path = new LinkedList<RSTile>();
		WebTile p = end;
		while (p != null) {
			path.addFirst(p);
			p = p.parent;
		}
		return path.toArray(new WebTile[path.size()]);
	}

	/**
	 * Gets the next start tile.
	 *
	 * @return The start tile.
	 */
	public RSTile getStart() {
		return path() != null && path().length > 0 ? path()[0] : null;
	}

	/**
	 * The end tile.
	 *
	 * @return The end tile.
	 */
	public RSTile getEnd() {
		final RSTile[] path = path();
		return path != null && path.length > 0 ? path[path.length - 1] : null;
	}

	/**
	 * Returns if you're at your destination.
	 *
	 * @return <tt>true</tt> if at destination, otherwise false.
	 */
	public boolean atDestination() {
		return getEnd() != null ? methods.calc.distanceTo(getEnd()) < 8 : false;
	}

}
