package com.pinktwins.elephant;

import java.awt.EventQueue;

import javax.swing.UIManager;

import com.pinktwins.elephant.data.Vault;

public class Elephant {
	public static void main(String args[]) {
		try {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Elephant");
			System.setProperty("awt.useSystemAAFontSettings", "on");
			System.setProperty("swing.aatext", "true");
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			// irrelevant // e.printStackTrace();
		}

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				ElephantWindow w = new ElephantWindow();
				w.setVisible(true);
			}
		});
	}
}
