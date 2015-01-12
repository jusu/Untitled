package com.pinktwins.elephant;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JTextPane;

import org.apache.commons.lang3.StringUtils;

import com.pinktwins.elephant.util.Factory;
import com.pinktwins.elephant.util.Images;

public class NoteAttachments {

	private HashMap<Object, File> attachments = Factory.newHashMap();
	private String loadMark;

	public Set<Object> keySet() {
		return new HashSet<Object>(attachments.keySet());
	}

	public void put(Object o, File f) {
		attachments.put(o, f);
	}

	public File get(Object o) {
		return attachments.get(o);
	}

	public void remove(Object o) {
		attachments.remove(o);
	}

	void insertFileIntoNote(NoteEditor editor, File f, int position) {
		JTextPane notePane = editor.editor.getTextPane();

		int caret = notePane.getCaretPosition();

		if (Images.isImage(f)) {
			try {
				Image i = ImageIO.read(f);
				if (i != null) {
					if (editor.getWidth() > 0) {
						i = editor.imageAttachmentImageScaler.scale(i, f);
					} else {
						throw new AssertionError();
					}

					ImageIcon ii = new ImageIcon(i);

					if (position > notePane.getDocument().getLength()) {
						position = 0;
					}

					try {
						notePane.setCaretPosition(position);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					}

					notePane.insertIcon(ii);

					attachments.put(ii, f);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			FileAttachment aa = new FileAttachment(f, editor.editorWidthScaler, editor.editorController);

			notePane.setCaretPosition(position);
			notePane.insertComponent(aa);

			attachments.put(aa, f);
		}

		notePane.setCaretPosition(caret);
	}

	private String getAttachmentString() {
		List<String> files = new ArrayList<String>();

		for (File f : attachments.values()) {
			files.add(f.getAbsolutePath());
		}

		Collections.sort(files);
		return StringUtils.join(files, ":");
	}

	public void loaded() {
		loadMark = getAttachmentString();
	}

	public boolean didChange() {
		if (loadMark == null) {
			return false;
		}

		return !loadMark.equals(getAttachmentString());
	}
}
