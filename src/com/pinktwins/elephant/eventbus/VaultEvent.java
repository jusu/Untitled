package com.pinktwins.elephant.eventbus;

import com.pinktwins.elephant.data.Notebook;

public class VaultEvent {
	public enum Kind {
		notebookCreated, notebookListChanged, notebookRefreshed
	};

	public Kind kind;
	public Notebook ref;

	public VaultEvent(Kind kind, Notebook ref) {
		this.kind = kind;
		this.ref = ref;
	}
}
