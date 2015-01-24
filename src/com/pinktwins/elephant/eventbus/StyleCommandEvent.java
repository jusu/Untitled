package com.pinktwins.elephant.eventbus;

import java.awt.event.ActionEvent;

public class StyleCommandEvent extends ElephantEvent {
	public final ActionEvent event;

	public StyleCommandEvent(ActionEvent e) {
		event = e;
	}
}
