package com.pinktwins.elephant.eventbus;

import java.io.File;

import com.pinktwins.elephant.Elephant;

public class NotebookEvent {
	static public enum Kind {
		noteMoved, noteCreated, noteRenamed
	};

	public Kind kind;

	public File source, dest;

	public NotebookEvent(Kind k) {
		this.kind = k;
	}

	public static void post(final Kind kind, final File source, final File dest) {
		NotebookEvent e = new NotebookEvent(kind);
		e.source = source;
		e.dest = dest;
		Elephant.eventBus.post(e);
	}
}
