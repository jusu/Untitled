package com.pinktwins.elephant.eventbus;

import com.pinktwins.elephant.data.Note;

public class NoteChangedEvent {
	public Note note;

	public NoteChangedEvent(Note note) {
		this.note = note;
	}
}
