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
 *        * Updated from JTextArea to JTextPane * Added bullet list handling
 */
public class AutoIndentAction extends AbstractAction {

	private static final String bulletChars = "*-+";

	public void actionPerformed(ActionEvent ae) {
		JTextPane comp = (JTextPane) ae.getSource();
		Document doc = comp.getDocument();

		if (!comp.isEditable()) {
			return;
		}

		try {
			int caretPosition = comp.getCaretPosition();

			// we need the text form the beginning until the first bullet plus space plus first character
			// it is easier to just read the complete line
			int start = Utilities.getRowStart(comp, caretPosition);
			int end = Utilities.getRowEnd(comp, caretPosition);
			String line = doc.getText(start, end - start);

			if (line.length() <= 1) {
				doc.insertString(caretPosition, "\n", null);
				return;
			}

			// determine whiteSpace and additionalBullet
			String whiteSpace = getLeadingWhiteSpace(line);
			String strAtCaret = doc.getText(caretPosition, 1);
			String additionalBullet;
			if (bulletChars.contains(strAtCaret)) {
				// special case: cursor right before bullet sign --> do not insert additional bullet sign
				additionalBullet = "";
			} else if (bulletChars.contains(line.trim())) {
				// special case: pressing enter after an empty bullet item
				// remove bullet instead of adding something
				// This is in contrast to MS office behavior: remove indent by one level; if no minor level avalible:
				// remove bullet
				doc.remove(start, end - start);
				return;
			} else {
				additionalBullet = determineAdditionalBullet(line);
				if (!additionalBullet.isEmpty()) {
					boolean caretDirectlyAfterBulletSign = (caretPosition == start + whiteSpace.length() + 1);
					if (caretDirectlyAfterBulletSign) {
						// strip trailing space
						additionalBullet = additionalBullet.substring(0, 1);
					}
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

}
