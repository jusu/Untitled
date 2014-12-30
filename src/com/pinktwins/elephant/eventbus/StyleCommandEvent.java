package com.pinktwins.elephant.eventbus;

import java.awt.event.ActionEvent;

public class StyleCommandEvent {
	public ActionEvent event;

	public StyleCommandEvent(ActionEvent e) {
		event = e;
	}
}
