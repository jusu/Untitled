package com.pinktwins.elephant.eventbus;

import java.io.File;

public class NotebookEvent extends ElephantEvent {
	public static enum Kind {
		noteMoved, noteCreated, noteRenamed, noteDeleted
	};

	public final Kind kind;
	public final File source, dest;
	
	public NotebookEvent(Kind kind, File source) {
		this.kind = kind;
		this.source = source;
		this.dest = null;
	}

	public NotebookEvent(Kind kind, File source, File dest) {
		this.kind = kind;
		this.source = source;
		this.dest = dest;
	}
}
