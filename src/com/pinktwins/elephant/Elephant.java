package com.pinktwins.elephant;

import java.awt.EventQueue;
import java.awt.event.KeyEvent;

import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.text.DefaultEditorKit;

import com.google.common.eventbus.EventBus;
import com.pinktwins.elephant.data.Settings;

public class Elephant {

	public final static EventBus eventBus = new EventBus();
	public final static Settings settings = new Settings();

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
