package com.pinktwins.elephant.ui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Utilities;

/**
 * When tab or shift tab is pressed something should happen if a bullet list is edited
 * If no bullet list is there, default behavior is called
 * The concrete behavior is implemented by the inheriting classes
 */
public abstract class GenericTabAction extends AbstractAction {

	public void actionPerformed(ActionEvent e) {
		JTextPane comp = (JTextPane)e.getSource();
		Document doc = comp.getDocument();

		if (!comp.isEditable()) {
			return;
		}
		int caretPosition = comp.getCaretPosition();
		try {
			int start = Utilities.getRowStart(comp, caretPosition);
			int end = Utilities.getRowEnd(comp, caretPosition);
			String line = doc.getText(start, end - start);
			if (line.isEmpty()) {
				// line is empty --> default behavior
				defaultBehavior(doc, caretPosition);
				return;
			}
			char seperator = line.charAt(0);
			int curPos = 0;
			int minWhiteSpaceCount = getMinSpaceCount();
			if ((seperator == ' ') || (seperator == '\t')) {
				// first character is really a separator
				// go to first char not being a separator
				while ((curPos < line.length()) && (line.charAt(curPos) == seperator)) {
					curPos++;
				}
				if (curPos == line.length()) {
					// only tabs/spaces --> default behavior
					defaultBehavior(doc, caretPosition);
					return;
				}
				if (seperator == '\t') {
					minWhiteSpaceCount = minWhiteSpaceCount / 2;
				}
			}
			if (curPos < minWhiteSpaceCount) {
				// too less white spaces --> default behavior
				defaultBehavior(doc, caretPosition);
				return;
			}
			char curChar = line.charAt(curPos);
			if ((curChar == '*') || (curChar == '-') || (curChar == '+')) {
				// we found a bullet list marker
				if (curPos+1 < line.length()) {
					// not EOL -> check for next char
					if (line.charAt(curPos+1) != ' ') {
						// no space following the list char --> default behavior
						defaultBehavior(doc, caretPosition);
						return;
					}
				}
				// everything allright --> bulletListBehavior
				bulletListBehavior(comp, caretPosition);
			} else {
				// no list char --> default behavior
				defaultBehavior(doc, caretPosition);
				return;
			}
		} catch (BadLocationException ex) {
			try {
				defaultBehavior(doc, caretPosition);
			} catch(BadLocationException ignore) {
				// ignore
			}
		}
	}

	/**
	 * Behavior when caret is on a line containing a bullet list item
	 */
	abstract void bulletListBehavior(JTextPane comp, int caretPosition) throws BadLocationException;

	/**
	 * Behavior when caret is not on a line containing a bullet list item
	 */
	abstract void defaultBehavior(Document doc, int caretPosition) throws BadLocationException;

	/**
	 *
	 * @return the minimum amount of space characters such that the bulletListBehavior may be triggered
	 */
	abstract int getMinSpaceCount();

}
