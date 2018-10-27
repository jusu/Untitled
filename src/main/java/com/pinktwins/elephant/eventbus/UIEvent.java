package com.pinktwins.elephant.eventbus;

public class UIEvent extends ElephantEvent {
	public static enum Kind {
		editorWillChangeNote
	};

	public final Kind kind;

	public UIEvent(Kind kind) {
		this.kind = kind;
	}
}
