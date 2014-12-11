package com.pinktwins.elephant;

import javax.swing.JLabel;

public class Tags extends BackgroundPanel {
	private ElephantWindow window;
	
	public Tags(ElephantWindow w) {
		this.window = w;
		
		JLabel wip = new JLabel("coming soon", JLabel.CENTER);
		add(wip);
	}
}
