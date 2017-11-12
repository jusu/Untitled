package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Image;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.data.Vault;
import com.pinktwins.elephant.eventbus.IndexProgressEvent;
import com.pinktwins.elephant.util.Images;

public class Toolbar extends BackgroundPanel {

	private static final long serialVersionUID = -8186087241529191436L;

	ElephantWindow window;

	private static Image toolbarBg, toolbarBgInactive;

	SearchTextField search;
	private static final String searchNotes = "Search notes";

	private static boolean skipNextFocusLost = false;
	private boolean isIndexing = false;

	static {
		Iterator<Image> i = Images.iterator(new String[] { "toolbarBg", "toolbarBgInactive" });
		toolbarBg = i.next();
		toolbarBgInactive = i.next();
	}

	public Toolbar(ElephantWindow w) {
		super(toolbarBg);
		keepScaleOnRetina(false, true);
		window = w;

		Elephant.eventBus.register(this);

		createComponents();
	}

	public void cleanup() {
		Elephant.eventBus.unregister(this);
		window = null;
	}

	private void createComponents() {
		final int searchWidth = 360;

		search = new SearchTextField(searchNotes, ElephantWindow.fontMediumPlus);
		search.setPreferredSize(new Dimension(searchWidth, 26));
		search.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 20));
		search.setFocusable(false);

		JPanel p = new JPanel(new FlowLayout());
		p.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 10));
		p.add(search);

		add(p, BorderLayout.EAST);

		search.getDocument().addDocumentListener(new DocumentListener() {

			Timer t = new Timer();

			class PendingSearch extends TimerTask {
				private final String text;

				public PendingSearch(String text) {
					this.text = text;
				}

				@Override
				public void run() {
					EventQueue.invokeLater(new Runnable() {
						@Override
						public void run() {
							if (search.isFocusable()) {
								window.search(text);
							}
						}
					});
				}
			}

			PendingSearch pending = null;

			private void doSearch(String text) {
				if (pending != null) {
					pending.cancel();
					pending = null;
				}

				pending = new PendingSearch(text);
				t.schedule(pending, 250);
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				doSearch(search.getText());
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				doSearch(search.getText());
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				doSearch(search.getText());
			}
		});
	}

	public void focusGained() {
		setImage(toolbarBg);
		search.windowFocusGained();

		search.setVisible(Vault.getInstance().hasLocation());
	}

	public void focusLost() {
		if (skipNextFocusLost) {
			skipNextFocusLost = false;
			return;
		}

		setImage(toolbarBgInactive);
		search.windowFocusLost();
	}

	public boolean isEditing() {
		return search.isFocusOwner();
	}

	public void focusSearch() {
		if (!isIndexing) {
			search.setFocusable(true);
			search.requestFocusInWindow();
		}
	}

	public void clearSearch() {
		search.setFocusable(false);
		search.setText("");
	}

	public void indexingInProgress(boolean b) {
		isIndexing = b;
		search.setEnabled(!b);
		clearSearch();
		search.setHintText(searchNotes);
	}

	public static void skipNextFocusLost() {
		skipNextFocusLost = true;
	}

	@Subscribe
	public void handleIndexProgress(IndexProgressEvent event) {
		if (isIndexing) {
			search.setHintText(ProgressBars.getCharacterBar((int) event.progress));
		} else {
			search.setHintText(searchNotes);
		}
	}
}
