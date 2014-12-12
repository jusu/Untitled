package com.pinktwins.elephant;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JSplitPane;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;

public class CustomSplitPane extends JSplitPane {
	private static final long serialVersionUID = -8922501394423552180L;

	private String saveKey;
	private int locationLimit;

	int fixedLocation;
	boolean isDragging;

	public CustomSplitPane(int i) {
		super(i);

		addComponentListener(new ComponentListener() {

			@Override
			public void componentResized(ComponentEvent e) {
				setDividerLocation(fixedLocation);
			}

			@Override
			public void componentMoved(ComponentEvent e) {
			}

			@Override
			public void componentShown(ComponentEvent e) {
			}

			@Override
			public void componentHidden(ComponentEvent e) {
			}
		});

		addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent pce) {
				if (!isDragging) {
					setDividerLocation(fixedLocation);
				}

				if (locationLimit > 0 && getDividerLocation() > locationLimit) {
					setDividerLocation(locationLimit);
				}

				if (isDragging) {
					fixedLocation = getDividerLocation();
				}
			}
		});

	}

	public void setFixedLocation(int loc) {
		fixedLocation = loc;
		setDividerLocation(loc);
	}

	public void initLocationWithKey(String key) {
		if (saveKey != null) {
			// Should call only once
			throw new AssertionError();
		}

		int loc = Elephant.settings.getInt(key);
		if (loc > 0) {
			setFixedLocation(loc);
		}

		saveKey = key;

		SplitPaneUI ui = getUI();
		if (ui instanceof BasicSplitPaneUI) {
			BasicSplitPaneUI bui = (BasicSplitPaneUI) ui;

			bui.getDivider().addMouseListener(new MouseListener() {

				@Override
				public void mouseClicked(MouseEvent e) {
				}

				@Override
				public void mousePressed(MouseEvent e) {
					isDragging = true;
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					isDragging = false;
					Elephant.settings.set(saveKey, getDividerLocation());
				}

				@Override
				public void mouseEntered(MouseEvent e) {
				}

				@Override
				public void mouseExited(MouseEvent e) {
				}
			});
		}
	}

	public void limitLocation(int limit) {
		locationLimit = limit;
	}

	@Override
	public void setLeftComponent(Component c) {
		int i = getDividerLocation();
		super.setLeftComponent(c);
		setFixedLocation(i);
	}

	@Override
	public void setRightComponent(Component c) {
		int i = getDividerLocation();
		super.setRightComponent(c);
		setFixedLocation(i);
	}
}
