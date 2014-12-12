package com.pinktwins.elephant;

import java.awt.Color;

import javax.swing.JLabel;

public class Tags extends BackgroundPanel {
	private ElephantWindow window;
	
	public Tags(ElephantWindow w) {
		this.window = w;

		JLabel wip = new JLabel("Tags coming soon.", JLabel.CENTER);
		setBackground(Color.decode("#d5d3d5"));
		add(wip);
	}
}
