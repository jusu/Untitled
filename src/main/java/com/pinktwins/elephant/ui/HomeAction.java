package com.pinktwins.elephant.ui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Utilities;

public class HomeAction extends AbstractAction {

	@Override
	public void actionPerformed(ActionEvent e) {
		JTextPane comp = (JTextPane) e.getSource();
		Document doc = comp.getDocument();

		if (!comp.isEditable()) {
			return;
		}

		try {
			int caretPosition = comp.getCaretPosition();

			// we need the text form the beginning until the first bullet plus space plus first character
			// it is easier to just read the complete line
			int start = Utilities.getRowStart(comp, caretPosition);
			String lineUntilCaret = doc.getText(start, caretPosition-start);
			int leadingWhiteSpaceWidth = AutoIndentAction.getLeadingWhiteSpaceWidth(lineUntilCaret);
			if (caretPosition <= start + leadingWhiteSpaceWidth) {
				// caret is at the first white spaces
				// --> jump to beginning of line
				comp.setCaretPosition(start);
			} else {
				// caret is somewhere in the text
				// --> jump to beginning of text
				comp.setCaretPosition(start + leadingWhiteSpaceWidth);
			}
		} catch (BadLocationException ex) {
			// ignore
		}

	}

}
