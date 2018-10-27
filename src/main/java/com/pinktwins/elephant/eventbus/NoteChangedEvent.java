package com.pinktwins.elephant.eventbus;

import com.pinktwins.elephant.data.Note;

public class NoteChangedEvent extends ElephantEvent {
	public Note note;
	public boolean contentChanged;

	public NoteChangedEvent(Note note, boolean contentChanged) {
		this.note = note;
		this.contentChanged = contentChanged;
	}
}
