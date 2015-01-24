package com.pinktwins.elephant.eventbus;

import com.pinktwins.elephant.data.Notebook;

public class VaultEvent extends ElephantEvent {
	public static enum Kind {
		notebookCreated, notebookListChanged, notebookRefreshed
	}

	public final Kind kind;
	public final Notebook ref;

	public VaultEvent(Kind kind, Notebook ref) {
		this.kind = kind;
		this.ref = ref;
	}
}
