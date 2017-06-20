package com.pinktwins.elephant.data;

import java.io.File;
import java.util.List;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.Elephant;
import com.pinktwins.elephant.eventbus.ElephantEvent;
import com.pinktwins.elephant.eventbus.NotebookEvent;
import com.pinktwins.elephant.eventbus.UndoRedoStateUpdateRequest;
import com.pinktwins.elephant.util.Factory;

// Keep a queue of elephant actions such as delete / move to notebook, for undoing them.
// Editor undo/redo is handled by java.swing.undo.UndoManager.

public class ElephantUndoManager {

	public ElephantUndoManager() {
		Elephant.eventBus.register(this);
	}

	private final List<ElephantEvent> events = Factory.newArrayList();
	private File performingActionOnFile = null;

	public boolean hasEvents() {
		return events.size() > 0;
	}

	public void clear() {
		events.clear();
	}
	
	private String getTitle(File f) {
		return new Note(f).getMeta().title();
	}

	public String getActionText() {
		if (!hasEvents()) {
			return "";
		}

		ElephantEvent event = events.get(events.size() - 1);
		if (event instanceof NotebookEvent) {
			NotebookEvent e = (NotebookEvent) event;
			switch (e.kind) {
			case noteMoved:
				if (Note.findContainingNotebook(e.dest).isTrash()) {
					return "Restore note \"" + getTitle(e.dest) + "\"";
				}
				return "Undo Move of \"" + getTitle(e.dest) + "\"";
			default:
				break;
			}
		}

		return "";
	}

	@Subscribe
	public void handleNotebookEvent(NotebookEvent event) {
		if (performingActionOnFile != null && performingActionOnFile == event.source) {
			return;
		}

		performingActionOnFile = null;

		switch (event.kind) {
		case noteMoved:
			events.add(event);
			new UndoRedoStateUpdateRequest(event).post();
			break;
		default:
			break;
		}
	}

	public void performUndo() {
		if (!hasEvents()) {
			return;
		}

		ElephantEvent event = events.get(events.size() - 1);
		if (event instanceof NotebookEvent) {
			NotebookEvent e = (NotebookEvent) event;
			switch (e.kind) {
			case noteMoved:
				Note n = new Note(e.dest);
				Notebook nb = Note.findContainingNotebook(e.source);

				performingActionOnFile = e.dest;
				n.moveTo(nb.folder());
				nb.refresh();

				break;
			default:
				break;
			}
		}

		events.remove(events.size() - 1);
		new UndoRedoStateUpdateRequest(event).post();
	}

}
