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
 
import javax.swing.*; 
import javax.swing.text.Document;
import javax.swing.text.Utilities;
import javax.swing.text.BadLocationException; 
import java.awt.event.ActionEvent; 
 
/** 
 * @author Santhosh Kumar T 
 * @email santhosh@fiorano.com
 *
 * Updated from JTextArea to JTextPane
 */ 
public class AutoIndentAction extends AbstractAction { 
    public void actionPerformed(ActionEvent ae) { 
        JTextPane comp = (JTextPane)ae.getSource();
        Document doc = comp.getDocument(); 
 
        if(!comp.isEditable()) 
            return; 
        try {
            String str = ActionUtils.getTextUntilCursor(comp);
            String whiteSpace = getLeadingWhiteSpace(str); 
            String additionalBullet = determineAdditionalBullets(str);
            doc.insertString(comp.getCaretPosition(), '\n' + whiteSpace + additionalBullet, null);
        } catch(BadLocationException ex) { 
            try { 
                doc.insertString(comp.getCaretPosition(), "\n", null); 
            } catch(BadLocationException ignore) { 
                // ignore 
            } 
        } 
    } 

    /**
     * Checks if the given string begins with "* ", "- ", or "+ " (after removing the white spaces before)
     * If yes, that string is returned
     * If not, "" is returned
     * @param str
     * @return
     */
    private String determineAdditionalBullets(String str) {
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
     *  Returns leading white space characters in the specified string. 
     */ 
    private String getLeadingWhiteSpace(String str) { 
        return str.substring(0, getLeadingWhiteSpaceWidth(str)); 
    } 
 
    /**
     *  Returns the number of leading white space characters in the specified string. 
     */ 
    private int getLeadingWhiteSpaceWidth(String str) { 
        int whitespace = 0; 
        while(whitespace<str.length()) {
            char ch = str.charAt(whitespace); 
            if(ch==' ' || ch=='\t') 
                whitespace++; 
            else 
                break; 
        } 
        return whitespace; 
    } 
} 