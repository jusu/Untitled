package com.pinktwins.elephant;

import java.awt.Image;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Sidebar extends BackgroundPanel {

	private static final long serialVersionUID = 5100779924945307084L;

	private static Image tile;

	static {
		try {
			tile = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/sidebar.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Sidebar() {
		super(tile);
	}



}
