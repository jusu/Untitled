package com.pinktwins.elephant.ui;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Utilities;

public class TabAction extends GenericTabAction {

	/**
	 * If there is no bullet, default behavior applies: insert 4 spaces
	 */
	@Override
	void defaultBehavior(Document doc, int caretPosition) throws BadLocationException {
		doc.insertString(caretPosition, "    ", null);
	}

	/**
	 * If there is a bullet, insert two spaces at the beginning of the line
	 */
	@Override
	void bulletListBehavior(JTextPane comp, int caretPosition) throws BadLocationException {
		int start = Utilities.getRowStart(comp, caretPosition);
		Document doc = comp.getDocument();
		// assumption: COUNT_OF_SPACES_FOR_LIST_INDENT == 4;
		doc.insertString(start, "    ", null);
	}

	@Override
	int getMinSpaceCount() {
		return 0;
	}

}
