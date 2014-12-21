package com.pinktwins.elephant;

import javax.swing.undo.UndoManager;

public class UndoRedoStateUpdateRequest {
	public UndoManager manager;

	public UndoRedoStateUpdateRequest(UndoManager m) {
		manager = m;
	}
}
