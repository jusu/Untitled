package com.pinktwins.elephant.data;

import java.util.Set;

public interface SearchIndexInterface {

	public abstract void digestText(Note n, String text);
	
	public abstract void digestDate(Note note, long dateValue);

	public abstract Set<Note> search(String text);

	public abstract void purgeNote(Note note);

	public abstract void debug();

	public abstract void commit();

}