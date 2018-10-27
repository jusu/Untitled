package com.pinktwins.elephant.eventbus;

import javax.swing.undo.UndoManager;

public class UndoRedoStateUpdateRequest extends ElephantEvent {
	public final UndoManager manager;
	public final ElephantEvent event;

	public UndoRedoStateUpdateRequest(UndoManager m) {
		manager = m;
		event = null;
	}

	public UndoRedoStateUpdateRequest(ElephantEvent e) {
		event = e;
		manager = null;
	}
}
