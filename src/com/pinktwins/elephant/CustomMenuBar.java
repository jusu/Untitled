package com.pinktwins.elephant;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.apache.commons.lang3.SystemUtils;

public class CustomMenuBar extends JMenuBar {

	private static boolean isOpen = false;

	public static boolean isOpen() {
		return isOpen;
	}

	@Override
	public JMenu add(JMenu c) {
		super.add(c);

		// On windows and linux, opening a menu will unfocus editor, causing
		// undo/redo to become unavailable. Make CustomMenuBar.isOpen()
		// indicate if the menubar is open, and if so, avoid unfocusing the editor
		// on these systems. Mac works without this.
		if (SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_LINUX) {
			c.addMenuListener(new MenuListener() {
				@Override
				public void menuSelected(MenuEvent e) {
					CustomMenuBar.isOpen = true;
				}

				@Override
				public void menuDeselected(MenuEvent e) {
					CustomMenuBar.isOpen = false;
				}

				@Override
				public void menuCanceled(MenuEvent e) {
					CustomMenuBar.isOpen = false;
				}
			});
		}

		return c;
	}
}
