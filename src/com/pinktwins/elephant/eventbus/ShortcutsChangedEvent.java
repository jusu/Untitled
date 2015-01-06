package com.pinktwins.elephant.eventbus;

import com.pinktwins.elephant.Elephant;

public class ShortcutsChangedEvent {
	static public void post() {
		Elephant.eventBus.post(new ShortcutsChangedEvent());
	}
}
