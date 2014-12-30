package com.pinktwins.elephant.eventbus;

import javax.swing.undo.UndoManager;

public class UndoRedoStateUpdateRequest {
	public UndoManager manager;

	public UndoRedoStateUpdateRequest(UndoManager m) {
		manager = m;
	}
}
