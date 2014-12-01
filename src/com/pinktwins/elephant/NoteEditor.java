package com.pinktwins.elephant;

import java.awt.Image;
import java.io.IOException;

import javax.imageio.ImageIO;

public class NoteEditor extends BackgroundPanel {

	private static final long serialVersionUID = 5649274177360148568L;
	private static Image tile;

	static {
		try {
			tile = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/noteeditor.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public NoteEditor() {
		super(tile);
	}
}
