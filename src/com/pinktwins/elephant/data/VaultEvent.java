package com.pinktwins.elephant.data;

public class VaultEvent {
	public enum Kind { notebookCreated, notebookListChanged };
	
	public Kind kind;
	
	public VaultEvent(Kind kind) {
		this.kind = kind;
	}
}
