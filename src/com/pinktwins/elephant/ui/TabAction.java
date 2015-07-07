package com.pinktwins.elephant.ui;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Utilities;
import static com.pinktwins.elephant.ui.UIConstants.COUNT_OF_SPACES_FOR_LIST_INDENT;

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
		String spaces;
		switch (COUNT_OF_SPACES_FOR_LIST_INDENT) {
		case 2:
			spaces = "  ";
			break;
		default:
			// even if COUNT_OF_SPACES_FOR_LIST_INDENT is not 4, fall back to four spaces
			spaces = "    ";
			break;
		}
		doc.insertString(start, spaces, null);
	}

	@Override
	int getMinSpaceCount() {
		// two spaces
		return COUNT_OF_SPACES_FOR_LIST_INDENT;
	}

}
