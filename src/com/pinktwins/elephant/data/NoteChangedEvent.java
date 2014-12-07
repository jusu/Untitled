package com.pinktwins.elephant.data;

public class NoteChangedEvent {
	public Note note;

	public NoteChangedEvent(Note note) {
		this.note = note;
	}
}
