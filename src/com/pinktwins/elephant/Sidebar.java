package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.io.File;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.data.RecentNotes;
import com.pinktwins.elephant.data.Vault;
import com.pinktwins.elephant.eventbus.RecentNotesChangedEvent;
import com.pinktwins.elephant.util.Images;

public class Sidebar extends BackgroundPanel {

	private static final long serialVersionUID = 5100779924945307084L;

	public static final String ACTION_NOTES = "a:notes";
	public static final String ACTION_NOTEBOOKS = "a:notebooks";
	public static final String ACTION_TAGS = "a:tags";

	private ElephantWindow window;
	private static Image tile, sidebarDivider;

	private RecentNotes recentNotes = new RecentNotes();

	SideBarList shortcuts, recent, navigation;

	static {
		Iterator<Image> i = Images.iterator(new String[] { "sidebar", "sidebarDivider" });
		tile = i.next();
		sidebarDivider = i.next();
	}

	public Sidebar(ElephantWindow w) {
		super(tile);
		window = w;

		Elephant.eventBus.register(this);

		shortcuts = new SideBarList(window, "SHORTCUTS");
		shortcuts.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
		shortcuts.load(new File(Vault.getInstance().getHome() + File.separator + ".shortcuts"));

		recent = new SideBarList(window, "RECENT NOTES");
		recent.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
		recent.load(recentNotes.list());

		BackgroundPanel div = new BackgroundPanel(sidebarDivider);
		div.setStyle(BackgroundPanel.SCALED_X);
		div.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
		div.setMaximumSize(new Dimension(1920, 2));

		navigation = new SideBarList(window, "");
		navigation.addNavigation();
		navigation.setOpaque(false);
		navigation.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
		navigation.highlightSelection = true;

		JPanel p1 = new JPanel(new BorderLayout());
		JPanel p2 = new JPanel(new BorderLayout());
		JPanel p3 = new JPanel(new BorderLayout());
		p1.setOpaque(false);
		p2.setOpaque(false);
		p3.setOpaque(false);

		add(shortcuts, BorderLayout.NORTH);
		p1.add(recent, BorderLayout.NORTH);
		p2.add(div, BorderLayout.NORTH);
		p3.add(navigation, BorderLayout.NORTH);
		
		add(p1, BorderLayout.CENTER);
		p1.add(p2, BorderLayout.CENTER);
		p2.add(p3, BorderLayout.CENTER);
	}

	public void selectNavigation(int n) {
		navigation.select(n);
	}

	@Subscribe
	public void handleRecentNotesChanged(RecentNotesChangedEvent event) {
		recent.load(recentNotes.list());
		revalidate();
	}
}
