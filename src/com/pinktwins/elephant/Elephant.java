package com.pinktwins.elephant;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import javax.swing.UIManager;

import com.google.common.eventbus.EventBus;
import com.pinktwins.elephant.data.Settings;
import com.pinktwins.elephant.data.Vault;

public class Elephant {

	public final static EventBus eventBus = new EventBus();
	public final static Settings settings = new Settings();

	public static String[] args;

	public static void main(String args[]) {
		Elephant.args = args;

		try {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Elephant");
			System.setProperty("awt.useSystemAAFontSettings", "on");
			System.setProperty("swing.aatext", "true");
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
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
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}
}
