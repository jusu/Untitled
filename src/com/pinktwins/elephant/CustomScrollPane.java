package com.pinktwins.elephant;

import java.awt.Component;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

public class CustomScrollPane extends JScrollPane {
	private boolean isLocked;
	private int lockedValue;

	public CustomScrollPane(Component view) {
		super(view);

		final JScrollBar bar = getVerticalScrollBar();
		
		bar.addAdjustmentListener(new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				if (isLocked) {
					bar.setValue(lockedValue);
				}
			}
		});
	}

	public void setLocked(boolean b) {
		isLocked = b;
		lockedValue = getVerticalScrollBar().getValue();
		this.setVerticalScrollBarPolicy(b ? JScrollPane.VERTICAL_SCROLLBAR_NEVER : JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	}
}
