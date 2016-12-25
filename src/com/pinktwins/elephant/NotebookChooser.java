package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;

import com.pinktwins.elephant.data.Notebook;

public class NotebookChooser extends JDialog {

	private static final long serialVersionUID = 4290404794842317473L;

	public static final int fixedWidth = 442;
	public static final int fixedWidthJump = 406;
	public static final int fixedHeight = 622;

	private NotebooksModal notebooks;

	private NotebookActionListener naListener;

	public void setNotebookActionListener(NotebookActionListener l) {
		naListener = l;
	}

	public NotebookChooser(Frame owner, String title) {
		super(owner, title, false);

		Toolbar.skipNextFocusLost();

		setUndecorated(true);
		setLayout(new BorderLayout());

		if (title.isEmpty()) {
			float opacity = 0.95f;
			this.setOpacity(opacity);
		}

		notebooks = new NotebooksModal((ElephantWindow) owner, title);
		add(notebooks);

		notebooks.search.requestFocusInWindow();

		notebooks.setNotebookActionListener(new NotebookActionListener() {
			@Override
			public void didCancelSelection() {
				close();
			}

			@Override
			public void didSelect(Notebook nb) {
				naListener.didSelect(nb);
				close();
			}
		});

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowActivated(WindowEvent e) {
				notebooks.search.requestFocusInWindow();
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				close();
			}
		});
	}

	protected void close() {
		setVisible(false);
		dispose();
	}

	public void handleKeyEvent(KeyEvent e) {
		switch (e.getID()) {
		case KeyEvent.KEY_PRESSED:
			switch (e.getKeyCode()) {
			case KeyEvent.VK_ESCAPE:
				close();
				break;
			case KeyEvent.VK_UP:
				// arrows move focus away from search automatically
				if (!notebooks.search.hasFocus()) {
					notebooks.changeSelection(-1, e.getKeyCode());
				}
				break;
			case KeyEvent.VK_DOWN:
				if (!notebooks.search.hasFocus()) {
					notebooks.changeSelection(1, e.getKeyCode());
				}
				break;
			default:
				notebooks.handleKeyEvent(e);
			}
			break;
		}
	}
}
