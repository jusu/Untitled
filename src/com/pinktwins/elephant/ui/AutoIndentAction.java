package com.pinktwins.elephant.ui;

/** 
 * MySwing: Advanced Swing Utilites 
 * Copyright (C) 2005  Santhosh Kumar T 
 * <p/> 
 * This library is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public 
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version. 
 * <p/> 
 * This library is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU 
 * Lesser General Public License for more details. 
 */

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Utilities;

/**
 * @author Santhosh Kumar T
 * @email santhosh@fiorano.com
 *
 *        Updated from JTextArea to JTextPane
 */
public class AutoIndentAction extends AbstractAction {
	public void actionPerformed(ActionEvent ae) {
		JTextPane comp = (JTextPane) ae.getSource();
		Document doc = comp.getDocument();

		if (!comp.isEditable())
			return;
		try {
			int caretPosition = comp.getCaretPosition();
			String str = getTextUntilCursor(comp);
			String whiteSpace = getLeadingWhiteSpace(str);
			String strAtCaret = doc.getText(caretPosition, 1);
			String additionalBullet;
			if ("*-+".contains(strAtCaret)) {
				// special case: cursor right before bullet sign --> do not insert additional bullet sign
				additionalBullet = "";
			} else {
				additionalBullet = determineAdditionalBullet(str);
			}

			// pressing enter twice removes the extraneous bullet sign
			if (caretPosition > 1) {
				String strBeforeCaret = doc.getText(caretPosition - 2, 1);
				if ("*-+".contains(strBeforeCaret) && str.length() == whiteSpace.length() + 2 && str.charAt(str.length() - 1) == ' ') {
					doc.remove(caretPosition - whiteSpace.length() - 2, whiteSpace.length() + 2);
					whiteSpace = "";
				}
			}

			doc.insertString(comp.getCaretPosition(), '\n' + whiteSpace + additionalBullet, null);
		} catch (BadLocationException ex) {
			try {
				doc.insertString(comp.getCaretPosition(), "\n", null);
			} catch (BadLocationException ignore) {
				// ignore
			}
		}
	}

	/**
	 * Checks if the given string begins with "* ", "- ", or "+ " (after removing the white spaces before) If yes, that
	 * string is returned If not, "" is returned
	 * 
	 * @param str
	 * @return
	 */
	private String determineAdditionalBullet(String str) {
		String trimmed = str.trim();
		if (trimmed.startsWith("* ")) {
			return "* ";
		} else if (trimmed.startsWith("- ")) {
			return "- ";
		} else if (trimmed.startsWith("+ ")) {
			return "+ ";
		} else {
			return "";
		}
	}

	/**
	 * Returns leading white space characters in the specified string.
	 */
	private String getLeadingWhiteSpace(String str) {
		return str.substring(0, getLeadingWhiteSpaceWidth(str));
	}

	/**
	 * Returns the number of leading white space characters in the specified string.
	 */
	private int getLeadingWhiteSpaceWidth(String str) {
		int whitespace = 0;
		while (whitespace < str.length()) {
			char ch = str.charAt(whitespace);
			if (ch == ' ' || ch == '\t')
				whitespace++;
			else
				break;
		}
		return whitespace;
	}

	private static String getTextUntilCursor(JTextPane comp) throws BadLocationException {
		int caretPosition = comp.getCaretPosition();
		int start = Utilities.getRowStart(comp, caretPosition);
		Document doc = comp.getDocument();
		String str = doc.getText(start, caretPosition - start);
		return str;
	}
}
