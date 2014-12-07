package com.pinktwins.elephant;

public class UIEvent {
	static public enum Kind {
		editorWillChangeNote
	};

	Kind kind;

	public UIEvent(Kind kind) {
		this.kind = kind;
	}
}
