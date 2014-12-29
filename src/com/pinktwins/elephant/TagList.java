package com.pinktwins.elephant;

import java.awt.Color;

import javax.swing.JLabel;

public class TagList extends BackgroundPanel {
	private ElephantWindow window;
	
	public TagList(ElephantWindow w) {
		window = w;
		if (window != null) {
		}

		JLabel wip = new JLabel("You can add tags to notes and search with them. This view coming soon.", JLabel.CENTER);
		wip.setFont(ElephantWindow.fontStart);
		wip.setForeground(ElephantWindow.colorTitleButton);
		setBackground(Color.decode("#d5d3d5"));
		add(wip);
	}
}
