package com.pinktwins.elephant.ui;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Utilities;

public class ActionUtils {

	public static String getTextUntilCursor(JTextPane comp) throws BadLocationException {
		int caretPosition = comp.getCaretPosition();
		int start = Utilities.getRowStart(comp, caretPosition);
		Document doc = comp.getDocument();
		String str = doc.getText(start, caretPosition - start);
		return str;
	}

}
