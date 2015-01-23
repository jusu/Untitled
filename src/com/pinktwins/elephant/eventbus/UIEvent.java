package com.pinktwins.elephant.eventbus;

import com.pinktwins.elephant.Elephant;

public class UIEvent {
	public static enum Kind {
		editorWillChangeNote
	};

	public Kind kind;

	public UIEvent(Kind kind) {
		this.kind = kind;
	}

	public static void post(Kind kind) {
		Elephant.eventBus.post(new UIEvent(kind));
	}
}
