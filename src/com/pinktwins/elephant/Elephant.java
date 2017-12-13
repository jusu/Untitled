package com.pinktwins.elephant;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.UIManager;

import com.google.common.eventbus.EventBus;
import com.pinktwins.elephant.data.ElephantUndoManager;
import com.pinktwins.elephant.data.Settings;
import com.pinktwins.elephant.data.Vault;

public class Elephant {

	public static final int VERSION = 43;

	private static final Logger LOG = Logger.getLogger(Elephant.class.getName());

	public static final EventBus eventBus = new EventBus();
	public static final Settings settings = new Settings();
	public static final ElephantUndoManager undoManager = new ElephantUndoManager();

	public static String[] args;

	public static void main(String[] args) {
		Elephant.args = args;

		try {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Elephant");
			System.setProperty("awt.useSystemAAFontSettings", "on");
			System.setProperty("swing.aatext", "true");

			UIManager.put("ScrollBar.width", 15);
			UIManager.put("ScrollBar.incrementButtonGap", 0);
			UIManager.put("ScrollBar.decrementButtonGap", 0);

			UIManager.put("ScrollBarUI", "com.pinktwins.elephant.ui.CustomScrollBarUI");

			// No system look-and-feel. Some systems (eg. ubuntu) draw additional borders etc
			// that we do not want.
			// UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			// irrelevant // e.printStackTrace();
		}

		String vaultPath = settings.getString(Settings.Keys.VAULT_FOLDER);
		Vault.getInstance().setLocation(vaultPath);

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				ElephantWindow w = new ElephantWindow();
				w.setVisible(true);
			}
		});
	}

	// http://stackoverflow.com/questions/4159802/how-can-i-restart-a-java-application
	public static boolean restartApplication() {
		final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
		try {
			File currentJar = new File(Elephant.class.getProtectionDomain().getCodeSource().getLocation().toURI());

			final ArrayList<String> command = new ArrayList<String>();

			if (currentJar.getName().endsWith(".jar")) {
				command.add(javaBin);
				command.add("-jar");
				command.add(currentJar.getPath());
			}

			if (currentJar.getName().endsWith(".exe")) {
				command.add(currentJar.getPath());
			}

			if (command.isEmpty()) {
				return false;
			}

			final ProcessBuilder builder = new ProcessBuilder(command);
			builder.start();
			System.out.println("Restarting...");
			System.exit(0);
		} catch (URISyntaxException e) {
			LOG.severe("Fail: " + e);
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}

		return false;
	}
}
