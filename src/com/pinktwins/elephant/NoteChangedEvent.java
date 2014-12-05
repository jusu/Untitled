package com.pinktwins.elephant;

import com.pinktwins.elephant.data.Note;

public class NoteChangedEvent {
	Note note;

	public NoteChangedEvent(Note note) {
		this.note = note;
	}
}
