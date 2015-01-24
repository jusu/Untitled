package com.pinktwins.elephant.eventbus;

import javax.swing.undo.UndoManager;

public class UndoRedoStateUpdateRequest extends ElephantEvent {
	public final UndoManager manager;

	public UndoRedoStateUpdateRequest(UndoManager m) {
		manager = m;
	}
}
