package com.pinktwins.elephant.data;

import java.io.File;

public class NotebookEvent {
	enum Kind { noteMoved, noteCreated };
	
	Kind kind;

	public File source, dest;

	public NotebookEvent(Kind k) {
		this.kind = k;
	}
}
