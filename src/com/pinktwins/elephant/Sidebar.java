package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

import com.pinktwins.elephant.data.Vault;

public class Sidebar extends BackgroundPanel {

	private static final long serialVersionUID = 5100779924945307084L;

	public static final String ACTION_NOTES = "a:notes";
	public static final String ACTION_NOTEBOOKS = "a:notebooks";
	public static final String ACTION_TAGS = "a:tags";

	private ElephantWindow window;
	private static Image tile, sidebarDivider;

	static {
		try {
			tile = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/sidebar.png"));
			sidebarDivider = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/sidebarDivider.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	SideBarList shortcuts, navigation;

	public Sidebar(ElephantWindow w, String header) {
		super(tile);
		window = w;

		shortcuts = new SideBarList(window, header);
		shortcuts.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
		shortcuts.load(new File(Vault.getInstance().getHome() + File.separator + ".shortcuts"));

		BackgroundPanel div = new BackgroundPanel(sidebarDivider);
		div.setOpaque(false);
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
		p1.setOpaque(false);
		p2.setOpaque(false);

		add(shortcuts, BorderLayout.NORTH);
		p1.add(div, BorderLayout.NORTH);
		p2.add(navigation, BorderLayout.NORTH);

		add(p1, BorderLayout.CENTER);
		p1.add(p2, BorderLayout.CENTER);
	}

	public void selectNavigation(int n) {
		navigation.select(n);
	}

}
