package org.rsbot.script.internal;

import org.rsbot.bot.Bot;
import org.rsbot.script.Script;
import org.rsbot.script.internal.event.ScriptListener;
import org.rsbot.script.randoms.*;
import org.rsbot.script.task.LoopTask;
import org.rsbot.script.task.TaskContainer;

import java.awt.*;
import java.util.*;

public class ScriptHandler extends TaskContainer {
	private final ArrayList<org.rsbot.script.Random> randoms = new ArrayList<org.rsbot.script.Random>();
	public final static String THREAD_GROUP_NAME = "Scripts";
	private final ThreadGroup scriptThreadGroup = new ThreadGroup(THREAD_GROUP_NAME);

	private final Set<ScriptListener> listeners = Collections.synchronizedSet(new HashSet<ScriptListener>());

	private final Bot bot;

	public ScriptHandler(final Bot bot) {
		this.bot = bot;
	}

	public void init() {
		setContext(bot.getMethodContext());
		try {
			bot.setLoginBot(new ImprovedLoginBot());
			randoms.add(bot.getLoginBot());
			randoms.add(new BankPins());
			randoms.add(new BeehiveSolver());
			randoms.add(new CapnArnav());
			randoms.add(new Certer());
			randoms.add(new CloseAllInterface());
			randoms.add(new DrillDemon());
			randoms.add(new FreakyForester());
			randoms.add(new FrogCave());
			randoms.add(new GraveDigger());
			randoms.add(new ImprovedRewardsBox());
			randoms.add(new LostAndFound());
			randoms.add(new Maze());
			randoms.add(new Mime());
			randoms.add(new Molly());
			randoms.add(new Exam());
			randoms.add(new Pillory());
			randoms.add(new Pinball());
			randoms.add(new Prison());
			randoms.add(new QuizSolver());
			randoms.add(new SandwhichLady());
			randoms.add(new ScapeRuneIsland());
			randoms.add(new TeleotherCloser());
			randoms.add(new FirstTimeDeath());
			randoms.add(new LeaveSafeArea());
			randoms.add(new SystemUpdate());
		} catch (final Exception e) {
			e.printStackTrace();
		}
		for (final org.rsbot.script.Random r : randoms) {
			r.init(bot.getMethodContext());
		}
	}

	public void addScriptListener(final ScriptListener l) {
		listeners.add(l);
	}

	public void removeScriptListener(final ScriptListener l) {
		listeners.remove(l);
	}

	private int addScriptToPool(final Script ss) {
		return pool(ss);
	}

	public Bot getBot() {
		return bot;
	}

	public Collection<org.rsbot.script.Random> getRandoms() {
		return randoms;
	}

	public Map<Integer, LoopTask> getRunningScripts() {
		return Collections.unmodifiableMap(getTasks());
	}

	public void pauseScript(final int id) {
		final LoopTask s = getTasks().get(id);
		s.setPaused(!s.isPaused());
		if (s.isPaused()) {
			for (final ScriptListener l : listeners) {
				l.scriptPaused(this);
			}
		} else {
			for (final ScriptListener l : listeners) {
				l.scriptResumed(this);
			}
		}
	}

	public void stopScript(final int id) {
		final LoopTask script = getTasks().get(id);
		if (script != null) {
			script.stop();
			remove(id);
			for (final ScriptListener l : listeners) {
				l.scriptStopped(this);
			}
		}
	}

	public int runScript(final Script script) {
		for (final ScriptListener l : listeners) {
			l.scriptStarted(this);
		}
		final int id = pool(script);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				invoke(id);
			}
		});
		return id;
	}

	public int runPassiveScript(final Script script) {
		final int id = pool(script);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				invoke(id);
			}
		});
		return id;
	}

	public void stopAllScripts() {
		stop();
	}

	public void updateInput(final Bot bot, final int mask) {
		for (final ScriptListener l : listeners) {
			l.inputChanged(bot, mask);
		}
	}
}