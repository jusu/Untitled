package com.pinktwins.elephant.data;

public class NotebookEvent {
	enum Kind { noteMoved, noteCreated };
	
	Kind kind;
	
	public NotebookEvent(Kind k) {
		this.kind = k;
	}
}
