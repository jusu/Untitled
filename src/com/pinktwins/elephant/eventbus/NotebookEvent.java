package com.pinktwins.elephant.eventbus;

import java.io.File;

public class NotebookEvent {
	static public enum Kind {
		noteMoved, noteCreated
	};

	public Kind kind;

	public File source, dest;

	public NotebookEvent(Kind k) {
		this.kind = k;
	}
}
