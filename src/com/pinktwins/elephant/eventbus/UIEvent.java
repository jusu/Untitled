package com.pinktwins.elephant.eventbus;

import com.pinktwins.elephant.Elephant;

public class UIEvent {
	static public enum Kind {
		editorWillChangeNote
	};

	public Kind kind;

	public UIEvent(Kind kind) {
		this.kind = kind;
	}

	static public void post(Kind kind) {
		Elephant.eventBus.post(new UIEvent(kind));
	}
}
