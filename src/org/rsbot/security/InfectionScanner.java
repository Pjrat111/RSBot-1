package org.rsbot.security;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.rsbot.Configuration;
import org.rsbot.script.util.WindowUtil;
import org.rsbot.util.Win32;

public class InfectionScanner implements Runnable {
	private final static String[] SUSPECT_PROCESSNAMES = { "javaw.exe", "java.exe" };
	private final static String[] SUSPECT_FILENAMES = { "jagex", "runescape", "casper", "gh0st" };
	int selectedOption;
	List<File> suspectFiles;

	@Override
	public void run() {
		if (Configuration.getCurrentOperatingSystem() != Configuration.OperatingSystem.WINDOWS) {
			return;
		}
		if (isInfected() && userConfirmedRemoval()) {
			terminateProcesses();
			removeSuspectFiles();
		}
	}

	private boolean isInfected() {
		scanStartupFiles();
		return suspectFiles != null && suspectFiles.size() != 0;
	}

	private boolean userConfirmedRemoval() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					selectedOption = JOptionPane.showConfirmDialog(WindowUtil.getBotGUI(), new String[] {
							"Malicious software has been detected on your computer.",
							"Would you like to preform an automatic virus removal?" }, "Security", JOptionPane.YES_NO_OPTION);
				}
			});
		} catch (final InterruptedException ignored) {
		} catch (final InvocationTargetException ignored) {
		}
		return selectedOption == JOptionPane.YES_OPTION;
	}

	private void scanStartupFiles() {
		final String startup = "\\Microsoft\\Windows\\Start Menu\\Programs\\Startup";
		final String[] paths = {
				System.getenv("APPDATA"),
				System.getenv("APPDATA") + startup,
				System.getenv("ProgramData") + startup,
		};
		suspectFiles = new ArrayList<File>();
		for (final String name : paths) {
			final File dir = new File(name);
			if (!dir.isDirectory()) {
				continue;
			}
			for (final File file : dir.listFiles()) {
				if (isFileSuspect(file)) {
					suspectFiles.add(file);
				}
			}
		}
	}

	private boolean isFileSuspect(final File file) {
		if (new File(Configuration.Paths.getAccountsFile()).getAbsolutePath().equals(file.getAbsolutePath()) || !file.isFile()) {
			return false;
		}
		for (final String check : SUSPECT_FILENAMES) {
			if (file.getName().contains(check)) {
				return true;
			}
		}
		return false;
	}

	private void removeSuspectFiles() {
		if (suspectFiles == null) {
			return;
		}
		for (final File item : suspectFiles) {
			if (!item.delete()) {
				item.deleteOnExit();
			}
		}
	}

	private void terminateProcesses() {
		final int p = Win32.getCurrentProcessId();
		for (final int pid : Win32.EnumProcesses()) {
			if (pid == 0 || pid == p) {
				continue;
			}
			final String name = Win32.QueryFullProcessImageName(pid);
			if (name == null) {
				continue;
			}
			for (final String check : SUSPECT_PROCESSNAMES) {
				if (name.contains(check)) {
					Win32.TerminateProcess(pid);
					break;
				}
			}
		}
	}
}
