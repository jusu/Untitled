package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.data.Notebook;
import com.pinktwins.elephant.data.RecentNotes;
import com.pinktwins.elephant.data.Settings;
import com.pinktwins.elephant.data.Shortcuts;
import com.pinktwins.elephant.eventbus.RecentNotesChangedEvent;
import com.pinktwins.elephant.eventbus.ShortcutsChangedEvent;
import com.pinktwins.elephant.util.Images;

public class Sidebar extends BackgroundPanel {

	private static final long serialVersionUID = 5100779924945307084L;

	public static final String ACTION_NOTES = "a:notes";
	public static final String ACTION_NOTEBOOKS = "a:notebooks";
	public static final String ACTION_TAGS = "a:tags";

	private ElephantWindow window;
	private static Image tile, sidebarDivider;

	private Shortcuts shortcuts = new Shortcuts();
	private RecentNotes recentNotes = new RecentNotes();

	SideBarList shortcutList, recentList, navigationList;

	public static enum RecentNotesModes {
		SHOW,
		HIDE
	};
	
	private RecentNotesModes recentMode = RecentNotesModes.SHOW;
	
	public void toggleRecentNotes(){
		switch (recentMode) {
		case SHOW:
			recentList.setVisible(false);
			recentMode = RecentNotesModes.HIDE;
			break;
		case HIDE:
			recentList.setVisible(true);
			recentMode = RecentNotesModes.SHOW;
			break;
		}
		
		Elephant.settings.set(Settings.Keys.RECENT_SHOW, recentMode.toString());
	}
	
	static {
		Iterator<Image> i = Images.iterator(new String[] { "sidebar", "sidebarDivider" });
		tile = i.next();
		sidebarDivider = i.next();
	}

	public Sidebar(ElephantWindow w) {
		super(tile);
		window = w;

		Elephant.eventBus.register(this);

		shortcutList = new SideBarList(window, "SHORTCUTS", true);
		shortcutList.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
		shortcutList.load(shortcuts.load());
		shortcutList.setItemModifier(shortcuts);

		recentList = new SideBarList(window, "RECENT NOTES", false);
		recentList.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
		recentList.load(recentNotes.list());

		BackgroundPanel div = new BackgroundPanel(sidebarDivider);
		div.setStyle(BackgroundPanel.SCALED_X);
		div.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
		div.setMaximumSize(new Dimension(1920, 2));

		navigationList = new SideBarList(window, "", false);
		navigationList.addNavigation();
		navigationList.setOpaque(false);
		navigationList.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
		navigationList.highlightSelection = true;

		JPanel p1 = new JPanel(new BorderLayout());
		JPanel p2 = new JPanel(new BorderLayout());
		JPanel p3 = new JPanel(new BorderLayout());
		p1.setOpaque(false);
		p2.setOpaque(false);
		p3.setOpaque(false);

		add(shortcutList, BorderLayout.NORTH);
		p1.add(recentList, BorderLayout.NORTH);
		p2.add(div, BorderLayout.NORTH);
		p3.add(navigationList, BorderLayout.NORTH);

		add(p1, BorderLayout.CENTER);
		p1.add(p2, BorderLayout.CENTER);
		p2.add(p3, BorderLayout.CENTER);
		
		// recent notes toggle settings
		recentMode = Elephant.settings.getRecentNotesMode();
		
		switch (recentMode) {
		case SHOW:
			recentList.setVisible(true);
			break;
		case HIDE:
			recentList.setVisible(false);
			break;
		}
	}

	public void selectNavigation(int n) {
		navigationList.select(n);
	}

	@Subscribe
	public void handleShortcutsChanged(ShortcutsChangedEvent event) {
		shortcutList.load(shortcuts.list());
		revalidate();
	}

	@Subscribe
	public void handleRecentNotesChanged(RecentNotesChangedEvent event) {
		recentList.load(recentNotes.list());
		revalidate();
	}

	public void addToShortcuts(Note note) {
		shortcuts.addNote(note);
	}

	public void addToShortcuts(Notebook nb) {
		shortcuts.addNotebook(nb);
	}

	public void addToShortcuts(String s) {
		shortcuts.add(s);
	}
}
