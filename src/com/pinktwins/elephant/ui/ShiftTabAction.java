package com.pinktwins.elephant.ui;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Utilities;
import static com.pinktwins.elephant.ui.UIConstants.COUNT_OF_SPACES_FOR_LIST_INDENT;

public class ShiftTabAction extends GenericTabAction {

	/**
	 * If there is no bullet, default behavior applies: do nothing
	 */
	@Override
	void defaultBehavior(Document doc, int caretPosition) throws BadLocationException {
	}

	/**
	 * If there is a bullet, remove two spaces at the beginning of the line
	 */
	@Override
	void bulletListBehavior(JTextPane comp, int caretPosition) throws BadLocationException {
		int start = Utilities.getRowStart(comp, caretPosition);
		Document doc = comp.getDocument();
		String text = doc.getText(start, 1);
		if (" ".equals(text)) {
			// indent made by spaces
			doc.remove(start, COUNT_OF_SPACES_FOR_LIST_INDENT);
		} else {
			// indent made by tabs
			doc.remove(start, 1);
		}
	}

	@Override
	int getMinSpaceCount() {
		// four spaces (in the case of COUNT_OF_SPACES_FOR_LIST_INDENT==2)
		// in case there were two spaces only, the resulting bullet list entry was not a valid entry, because of the missing leading two spaces 
		return 2*COUNT_OF_SPACES_FOR_LIST_INDENT;
	}

}
