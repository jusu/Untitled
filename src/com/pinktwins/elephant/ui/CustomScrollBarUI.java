package com.pinktwins.elephant.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;

public class CustomScrollBarUI extends BasicScrollBarUI {

	private static final int SCROLL_BAR_ALPHA_ROLLOVER = 255;
	private static final int SCROLL_BAR_ALPHA = 115;
	private static final int THUMB_BORDER_SIZE = 0;
	private static final int THUMB_OFFSET_X = 4;
	private static final int THUMB_OFFSET_Y = 4;
	private static final int THUMB_SIZE = 8;
	private static final Color THUMB_COLOR = Color.decode("#7d7d7d");// #c1c1c1");
	private static final Color TRACK_COLOR_A = Color.decode("#eaeaea");
	private static final Color TRACK_COLOR_B = Color.decode("#fafafa");
	private static final Color TRACK_LEFT = Color.decode("#e8e8e8"); // "#eaeaea");
	private static final Color TRACK_RIGHT = Color.decode("#ededed"); // #eeeeee");

	private Color trackColor = TRACK_COLOR_A;
	private boolean usingTrackColorB = false;

	public void useTrackColorB() {
		trackColor = TRACK_COLOR_B;
		usingTrackColorB = true;
	}

	private class VoidButton extends JButton {
		private final Dimension none = new Dimension(0, 1);

		@Override
		public Dimension getPreferredSize() {
			return none;
		}
	}

	@Override
	protected void installDefaults() {
		scrollBarWidth = 12;
		super.installDefaults();
	}

	@Override
	protected void installComponents() {
		incrButton = new VoidButton(); // createIncreaseButton(SOUTH);
		decrButton = new VoidButton(); // createDecreaseButton(NORTH);
		scrollbar.setEnabled(scrollbar.isEnabled());
	}

	public static ComponentUI createUI(JComponent c) {
		return new CustomScrollBarUI();
	}

	@Override
	protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
		g.setColor(trackColor);
		g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
		g.fillRect(0, 0, trackBounds.width, 1);
		g.fillRect(0, c.getHeight() - 1, trackBounds.width, 1);

		if (!usingTrackColorB) {
			g.setColor(TRACK_LEFT);
			g.fillRect(0, 0, 1, trackBounds.height);
			g.setColor(TRACK_RIGHT);
			g.fillRect(trackBounds.width - 1, trackBounds.y, 1, trackBounds.height);
		}
	}

	@Override
	protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
		// Awesome: http://ui-ideas.blogspot.fi/2012/06/mac-os-x-mountain-lion-scrollbars-in.html

		int alpha = isThumbRollover() ? SCROLL_BAR_ALPHA_ROLLOVER : SCROLL_BAR_ALPHA;
		int orientation = scrollbar.getOrientation();
		int arc = THUMB_SIZE;

		int x, y;
		if (orientation == JScrollBar.VERTICAL) {
			x = thumbBounds.x + THUMB_BORDER_SIZE + THUMB_OFFSET_X;
			y = thumbBounds.y + THUMB_BORDER_SIZE;
		} else {
			x = thumbBounds.x + THUMB_BORDER_SIZE;
			y = thumbBounds.y + THUMB_BORDER_SIZE + THUMB_OFFSET_Y;
		}

		int width = orientation == JScrollBar.VERTICAL ? THUMB_SIZE : thumbBounds.width - (THUMB_BORDER_SIZE * 2);
		width = Math.max(width, THUMB_SIZE);

		int height = orientation == JScrollBar.VERTICAL ? thumbBounds.height - (THUMB_BORDER_SIZE * 2) : THUMB_SIZE;
		height = Math.max(height, THUMB_SIZE);

		Graphics2D graphics2D = (Graphics2D) g.create();
		graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics2D.setColor(new Color(THUMB_COLOR.getRed(), THUMB_COLOR.getGreen(), THUMB_COLOR.getBlue(), alpha));
		graphics2D.fillRoundRect(x, y, width, height, arc, arc);
		graphics2D.dispose();
	}
}
