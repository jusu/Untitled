package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Image;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.pinktwins.elephant.data.Vault;

public class Sidebar extends BackgroundPanel {

	private static final long serialVersionUID = 5100779924945307084L;

	private ElephantWindow window;
	private static Image tile;

	static {
		try {
			tile = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/sidebar.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	SideBarList shortcuts;

	public Sidebar(ElephantWindow w) {
		super(tile);
		
		window = w;

		shortcuts = new SideBarList(window);
		shortcuts.load(new File(Vault.getInstance().getHome() + File.separator + ".shortcuts"));

		shortcuts.setOpaque(false);
		add(shortcuts, BorderLayout.NORTH);
	}



}
