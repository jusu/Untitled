package com.pinktwins.elephant;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Iterator;

import org.apache.commons.lang3.SystemUtils;

import com.pinktwins.elephant.SideBarList.SideBarItemModifier;
import com.pinktwins.elephant.SideBarList.SideBarListItem;
import com.pinktwins.elephant.eventbus.ShortcutsChangedEvent;
import com.pinktwins.elephant.util.CustomMouseListener;
import com.pinktwins.elephant.util.Images;

class SidebarModifyingMouseListener extends CustomMouseListener implements MouseMotionListener {

	private static final Image rearrangeArrow, rearrangeArrow_win, redMinus;

	static {
		Iterator<Image> i = Images.iterator(new String[] { "rearrangeArrow", "rearrangeArrow_win", "redMinus" });
		rearrangeArrow = i.next();
		rearrangeArrow_win = i.next();
		redMinus = i.next();
	}

	private final Cursor rearrangeCursor, redMinusCursor;
	private final Component parent;
	private final Cursor defaultCursor;

	private boolean isDragging, isRemoving;
	private SideBarListItem dragObj = null;

	private SideBarItemModifier modifier;

	public SidebarModifyingMouseListener(Component parent, Cursor defaultCursor) {
		this.parent = parent;
		this.defaultCursor = defaultCursor;

		Toolkit toolkit = Toolkit.getDefaultToolkit();
		rearrangeCursor = toolkit.createCustomCursor(SystemUtils.IS_OS_WINDOWS ? rearrangeArrow_win : rearrangeArrow, new Point(0, 10), "rearrangeCursor");
		redMinusCursor = toolkit.createCustomCursor(redMinus, new Point(7, 7), "redMinusCursor");
	}

	public void setItemModifier(SideBarItemModifier m) {
		modifier = m;
	}

	@Override
	public void mousePressed(MouseEvent e) {
		parent.setCursor(rearrangeCursor);
		isDragging = true;
		isRemoving = false;
		dragObj = (SideBarListItem) e.getSource();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		parent.setCursor(defaultCursor);
		isDragging = false;

		dragObj.drawMinus = false;
		dragObj.repaint();

		if (isRemoving) {
			modifier.remove(((SideBarListItem) e.getSource()).rawInitString);
			new ShortcutsChangedEvent().post();
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		SideBarListItem source = (SideBarListItem) e.getSource();
		if (isDragging && dragObj != source && modifier != null) {
			parent.setCursor(defaultCursor);
			isDragging = false;

			modifier.swap(dragObj.rawInitString, source.rawInitString);
			new ShortcutsChangedEvent().post();
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		float f = e.getX() / (float) ((Component) e.getSource()).getWidth();
		float removeLimit = 0.8f;

		if (!dragObj.drawMinus) {
			dragObj.drawMinus = true;
			dragObj.repaint();
		}

		if (f >= removeLimit && !isRemoving) {
			parent.setCursor(redMinusCursor);
			isRemoving = true;
		}

		if (f < removeLimit && isRemoving) {
			parent.setCursor(rearrangeCursor);
			isRemoving = false;
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}
}
