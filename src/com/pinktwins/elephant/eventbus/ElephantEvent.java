package com.pinktwins.elephant.eventbus;

import com.pinktwins.elephant.Elephant;

public class ElephantEvent {
	public void post() {
		Elephant.eventBus.post(this);
	}
}
