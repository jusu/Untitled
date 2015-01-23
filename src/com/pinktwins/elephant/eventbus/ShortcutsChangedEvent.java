package com.pinktwins.elephant.eventbus;

import com.pinktwins.elephant.Elephant;

public class ShortcutsChangedEvent {
	public static void post() {
		Elephant.eventBus.post(new ShortcutsChangedEvent());
	}
}
