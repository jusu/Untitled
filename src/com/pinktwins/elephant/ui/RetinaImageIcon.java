package com.pinktwins.elephant.ui;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.ImageObserver;

import javax.swing.ImageIcon;

import com.pinktwins.elephant.util.ScreenUtil;

public class RetinaImageIcon extends ImageIcon {

	public RetinaImageIcon(Image i) {
		super(i);
	}

	@Override
	public int getIconWidth() {
		if (!ScreenUtil.isRetina()) {
			return super.getIconWidth();
		}

		// Retina
		return super.getIconWidth() / 2;
	}

	@Override
	public int getIconHeight() {
		if (!ScreenUtil.isRetina()) {
			return super.getIconHeight();
		}

		// Retina
		return super.getIconHeight() / 2;
	}

	@Override
	public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
		if (!ScreenUtil.isRetina()) {
			super.paintIcon(c, g, x, y);
			return;
		}

		// Retina
		ImageObserver observer = getImageObserver();

		if (observer == null) {
			observer = c;
		}

		Image image = getImage();
		int width = image.getWidth(observer);
		int height = image.getHeight(observer);
		final Graphics2D g2d = (Graphics2D) g.create(x, y, width, height);

		g2d.scale(0.5, 0.5);
		g2d.drawImage(image, 0, 0, observer);
		g2d.scale(1, 1);
		g2d.dispose();
	}
}
