package org.rsbot.security;

import org.rsbot.Application;
import org.rsbot.Configuration;
import org.rsbot.Configuration.OperatingSystem;
import org.rsbot.gui.Chrome;
import org.rsbot.gui.LoadScreen;
import org.rsbot.script.AccountStore;
import org.rsbot.script.Script;
import org.rsbot.script.internal.ScriptHandler;
import org.rsbot.script.methods.Web;
import org.rsbot.script.task.Containable;
import org.rsbot.script.task.LoopTask;
import org.rsbot.script.task.executor.ScriptPool;
import org.rsbot.util.io.JavaCompiler;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetAddress;
import java.security.Permission;
import java.util.logging.Logger;

/**
 * @author Paris
 */
public class RestrictedSecurityManager extends SecurityManager {
	private static Logger log = Logger.getLogger("Security");
	public static final String SCRIPTCLASS = "org.rsbot.script.Script";

	private String getCallingClass() {
		final String prefix = Application.class.getPackage().getName() + ".";
		for (final Class<?> c : getClassContext()) {
			final String name = c.getName();
			if (name.startsWith(prefix) && !name.equals(RestrictedSecurityManager.class.getName())) {
				return name;
			}
		}
		return "";
	}

	public boolean isCallerScript() {
		return getThreadGroup().getName().equals(ScriptHandler.THREAD_GROUP_NAME) || Thread.currentThread().getName().startsWith(ScriptHandler.THREAD_GROUP_NAME + "-") ||
				getCallingClass().startsWith(SCRIPTCLASS);
	}

	public static void assertNonScript() {
		final SecurityManager sm = System.getSecurityManager();
		if (sm == null || !(sm instanceof RestrictedSecurityManager) || ((RestrictedSecurityManager) sm).isCallerScript()) {
			throw new SecurityException();
		}
	}

	@Override
	public void checkAccept(final String host, final int port) {
		throw new SecurityException();
	}

	@Override
	public void checkAccess(final Thread t) {
		checkAccess(t.getThreadGroup());
	}

	@Override
	public void checkAccess(final ThreadGroup g) {
		if (g.getName().equals(ScriptHandler.THREAD_GROUP_NAME)) {
			final String calling = getCallingClass();
			boolean pass = false;
			for (final Class<?> clazz : new Class<?>[] { ScriptPool.class, LoopTask.class, Containable.class, Script.class, Web.class }) {
				if (calling.equals(clazz.getName())) {
					pass = true;
					break;
				}
			}
			if (!pass) {
				throw new SecurityException();
			}
		}
	}

	@Override
	public void checkDelete(final String file) {
		checkFilePath(file, false);
		super.checkDelete(file);
	}

	@Override
	public void checkExec(final String cmd) {
		final String calling = getCallingClass();
		for (final Class<?> c : new Class<?>[]{Chrome.class, Configuration.class, JavaCompiler.class, LoadScreen.class}) {
			if (calling.startsWith(c.getName())) {
				super.checkExec(cmd);
				return;
			}
		}
		throw new SecurityException();
	}

	@Override
	public void checkExit(final int status) {
		final String calling = getCallingClass();
		if (calling.equals(Chrome.class.getName()) || calling.equals(Application.class.getName()) || calling.startsWith(LoadScreen.class.getName())) {
			super.checkExit(status);
		} else {
			throw new SecurityException();
		}
	}

	@Override
	public void checkListen(final int port) {
		if (port != 0) {
			throw new SecurityException();
		}
	}

	@Override
	public void checkMulticast(final InetAddress maddr) {
		throw new SecurityException();
	}

	@Override
	public void checkMulticast(final InetAddress maddr, final byte ttl) {
		throw new SecurityException();
	}

	@Override
	public void checkPermission(final Permission perm) {
		if (perm instanceof RuntimePermission) {
			if (perm.getName().equals("setSecurityManager")) {
				throw new SecurityException();
			}
		} else if (isCallerScript()) {
			if (perm.getName().equals("java.home") && !perm.getActions().equals("read")) {
				throw new SecurityException();
			} else if (perm.getName().equals("accessDeclaredMembers")) {
				throw new SecurityException();
			}
		}
	}

	@Override
	public void checkPermission(final Permission perm, final Object context) {
		checkPermission(perm);
	}

	@Override
	public void checkPrintJobAccess() {
		throw new SecurityException();
	}

	@Override
	public void checkRead(final String file) {
		checkFilePath(file, true);
		super.checkRead(file);
	}

	@Override
	public void checkRead(final String file, final Object context) {
		checkRead(file);
	}

	@Override
	public void checkSystemClipboardAccess() {
		if (isCallerScript()) {
			throw new SecurityException();
		}
	}

	@Override
	public boolean checkTopLevelWindow(final Object window) {
		return super.checkTopLevelWindow(window);
	}

	@Override
	public void checkWrite(final FileDescriptor fd) {
		super.checkWrite(fd);
	}

	@Override
	public void checkWrite(final String file) {
		checkFilePath(file, false);
		super.checkWrite(file);
	}

	private void checkFilePath(final String pathRaw, final boolean readOnly) {
		final String path = new File(pathRaw).getAbsolutePath();
		if (isCallerScript()) {
			if (!path.startsWith(Configuration.Paths.getScriptCacheDirectory())) {
				boolean fail = true;
				if (!Configuration.RUNNING_FROM_JAR) {
					// allow project resource directory if not running from JAR (i.e. in eclipse)
					String check = new File(Configuration.Paths.ROOT).getAbsolutePath();
					try {
						check = new File(check).getCanonicalPath();
					} catch (final IOException ignored) {
					}
					fail = !path.startsWith(check);
				} else {
					if (readOnly && path.equals(Configuration.Paths.getRunningJarPath())) {
						fail = false;
					}
				}
				for (final String prefix : new String[]{Configuration.Paths.getScreenshotsDirectory(), Configuration.Paths.getScriptsDirectory()}) {
					if (path.startsWith(prefix)) {
						fail = false;
						break;
					}
				}
				for (final String prefix : new String[]{Configuration.Paths.getWebDatabase(), Configuration.Paths.getBankCache()}) {
					if (path.equalsIgnoreCase(prefix)) {
						fail = false;
						break;
					}
				}
				final String jre = System.getProperty("java.home");
				if (readOnly && jre != null && !jre.isEmpty() && path.startsWith(jre)) {
					fail = false;
				}
				if (fail && readOnly) {
					final String[] fonts;
					switch (Configuration.getCurrentOperatingSystem()) {
						case WINDOWS:
							fonts = new String[]{System.getenv("SystemRoot") + "\\Fonts"};
							break;
						case MAC:
							fonts = new String[]{"/Library/Fonts", "/System/Library/Fonts", System.getenv("HOME") + "/Library/Fonts"};
							break;
						default:
							fonts = new String[]{"/usr/share/fonts/", "/usr/local/share/fonts", System.getenv("HOME") + "/.fonts"};
							break;
					}
					for (int i = 0; i < fonts.length; i++) {
						if (path.startsWith(fonts[i])) {
							fail = false;
							break;
						}
					}
				}
				if (Configuration.getCurrentOperatingSystem() == OperatingSystem.WINDOWS) {
					final String sysroot = System.getenv("SystemRoot");
					if (sysroot != null && sysroot.length() > 0 && path.startsWith(sysroot)) {
						fail = !readOnly;
					}
					if (path.endsWith(".ttf") && readOnly) {
						fail = false;
					}
				} else {
					if (readOnly && path.equals("/etc/resolv.conf")) {
						fail = false;
					}
				}
				if (fail) {
					log.warning((readOnly ? "Read" : "Write") + " access denied: " + path);
					throw new SecurityException();
				}
			}
		}
		if (path.equalsIgnoreCase(new File(Configuration.Paths.getAccountsFile()).getAbsolutePath())) {
			for (final StackTraceElement s : Thread.currentThread().getStackTrace()) {
				final String name = s.getClassName();
				if (name.equals(AccountStore.class.getName())) {
					return;
				}
			}
			throw new SecurityException();
		}
	}
}
