package com.pinktwins.elephant;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.JTextField;

import org.apache.commons.lang3.SystemUtils;

// http://stackoverflow.com/questions/1738966/java-jtextfield-with-input-hint

public class HintTextField extends JTextField {
	private static final long serialVersionUID = 7575368067562925690L;

	private static final int adjustOS = (SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_LINUX) ? -1 : 0;

	private String _hint;
	private Font _font;

	public HintTextField(String hint, Font font) {
		_hint = hint;
		_font = font;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		if (getText().length() == 0) {
			g.setFont(_font);

			int adjust_y = _font == ElephantWindow.fontMediumPlus ? 2 + adjustOS : 0;

			int h = getHeight();
			((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			Insets ins = getInsets();
			FontMetrics fm = g.getFontMetrics();
			int c0 = getBackground().getRGB();
			int c1 = getForeground().getRGB();
			int m = 0xfefefefe;
			int c2 = ((c0 & m) >>> 1) + ((c1 & m) >>> 1);
			g.setColor(new Color(c2, true));
			g.drawString(_hint, ins.left, h / 2 + fm.getAscent() / 2 - 2 + adjust_y);
		}
	}

	public void setHintText(String hint) {
		_hint = hint;
		repaint();
	}
}
