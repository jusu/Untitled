package com.pinktwins.elephant.eventbus;

public class VaultEvent {
	public enum Kind { notebookCreated, notebookListChanged };
	
	public Kind kind;
	
	public VaultEvent(Kind kind) {
		this.kind = kind;
	}
}
