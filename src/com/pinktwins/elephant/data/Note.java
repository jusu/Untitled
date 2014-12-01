package com.pinktwins.elephant.data;

import java.io.File;

public class Note {
	private File file;
	private String fileName = "";

	public Note(File f) {
		file = f;
		readInfo();
	}
	
	private void readInfo() {
		fileName = file.getName();
	}

	public String name() {
		return fileName;
	}
}
