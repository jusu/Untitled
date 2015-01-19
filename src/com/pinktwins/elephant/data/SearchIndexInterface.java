package com.pinktwins.elephant.data;

import java.util.Set;

public interface SearchIndexInterface {

	public abstract void digestWord(Note n, String text);

	public abstract Set<Note> search(String text);

	public abstract void purgeNote(Note note);

	public abstract void debug();

	public abstract void commit();

}