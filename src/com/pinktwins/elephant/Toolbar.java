package com.pinktwins.elephant;

import java.awt.Image;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Toolbar extends BackgroundPanel {

	private static final long serialVersionUID = -8186087241529191436L;

	ElephantWindow window;

	private static Image toolbarBg, toolbarBgInactive;

	static {
		try {
			toolbarBg = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/toolbarBg.png"));
			toolbarBgInactive = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/toolbarBgInactive.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Toolbar(ElephantWindow w) {
		super(toolbarBg);
		window = w;
	}
	
	public void focusGained() {
		setImage(toolbarBg);
	}
	
	public void focusLost() {
		setImage(toolbarBgInactive);
	}
}
