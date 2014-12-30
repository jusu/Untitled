package com.pinktwins.elephant.eventbus;

public class UIEvent {
	static public enum Kind {
		editorWillChangeNote
	};

	public Kind kind;

	public UIEvent(Kind kind) {
		this.kind = kind;
	}
}
