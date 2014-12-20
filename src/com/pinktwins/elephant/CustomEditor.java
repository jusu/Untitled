package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.TransferHandler;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.StyleConstants;
import javax.swing.undo.UndoManager;

public class CustomEditor extends RoundPanel {

	private static final long serialVersionUID = -6604641427725747091L;

	private static final String ELEM = AbstractDocument.ElementNameAttribute;
	private static final String ICON = StyleConstants.IconElementName;
	private static final String COMP = StyleConstants.ComponentElementName;

	private JTextField title;
	private CustomTextPane note;
	private JPanel padding;

	private UndoManager undoManager = new UndoManager();

	public boolean isRichText;

	public boolean isRichText() {
		return isRichText;
	}

	final Color kDividerColor = Color.decode("#dbdbdb");

	public interface EditorEventListener {
		public void editingFocusGained();

		public void editingFocusLost();

		public void caretChanged(JTextPane text);

		public void filesDropped(List<File> files);
	}

	public JTextPane getTextPane() {
		return note;
	}

	EditorEventListener eeListener;

	public void setEditorEventListener(EditorEventListener l) {
		eeListener = l;
	}

	FocusListener editorFocusListener = new FocusListener() {
		@Override
		public void focusGained(FocusEvent e) {
			if (eeListener != null) {
				eeListener.editingFocusGained();
			}
		}

		@Override
		public void focusLost(FocusEvent e) {
			if (eeListener != null) {
				eeListener.editingFocusLost();
			}
		}
	};

	static class CustomDocument extends DefaultStyledDocument {
		private static final long serialVersionUID = 2807153134148093523L;

		@Override
		public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
			str = str.replaceAll("\t", "    ");
			super.insertString(offs, str, a);
		}
	}

	@SuppressWarnings("serial")
	public CustomEditor() {
		super();

		this.setDoubleBuffered(true);

		setBackground(Color.WHITE);
		setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		setLayout(new BorderLayout());

		JPanel titlePanel = new JPanel();
		titlePanel.setLayout(new BorderLayout());
		titlePanel.setBackground(kDividerColor);
		titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 1, 0));

		title = new JTextField();
		title.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
		title.addFocusListener(editorFocusListener);
		title.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				if (title.getCaretPosition() == 0) {
					String s = title.getText();
					if (s.length() == 9 && s.indexOf("Untitled") == 1) {
						EventQueue.invokeLater(new Runnable() {
							@Override
							public void run() {
								try {
									title.getDocument().remove(1, "Untitled".length());
								} catch (BadLocationException e1) {
									e1.printStackTrace();
								}
							}
						});
					}
				}
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
			}
		});

		final KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();

		title.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					manager.focusNextComponent();
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}
		});

		titlePanel.add(title, BorderLayout.CENTER);

		title.setText("");
		add(titlePanel, BorderLayout.NORTH);

		createNote();

		// resize padding so note is at least kMinNoteSize height
		addComponentListener(new ResizeListener() {
			@Override
			public void componentResized(ComponentEvent e) {
				int h = getHeight();
				int needed = NoteEditor.kMinNoteSize - h;
				int preferred = h + needed - (padding.getLocation().y + 12);

				if (needed > 0) {
					if (preferred < 0) {
						createPadding();
						preferred = 10;
					}
					revalidate();
				} else {
					padding.setVisible(false);

					if (preferred > 0) {
						createPadding();
					}
				}

				if (preferred > 0) {
					padding.setVisible(true);
					padding.setPreferredSize(new Dimension(10, preferred));
				} else {
					padding.setVisible(false);
				}
			}
		});

	}

	CustomMouseListener paddingClick = new CustomMouseListener() {
		@Override
		public void mouseClicked(MouseEvent e) {
			note.requestFocusInWindow();
		}
	};

	class CustomTextPane extends JTextPane implements ClipboardOwner {

		private static final long serialVersionUID = -5388021236896195540L;

		// http://www.javapractices.com/topic/TopicAction.do?Id=82

		public void setClipboardContents(String aString) {
			StringSelection stringSelection = new StringSelection(aString);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(stringSelection, this);
		}

		public String getClipboardContents() {
			String result = "";
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			// odd: the Object param of getContents is not currently used
			Transferable contents = clipboard.getContents(null);
			boolean hasTransferableText = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
			if (hasTransferableText) {
				try {
					result = (String) contents.getTransferData(DataFlavor.stringFlavor);
				} catch (UnsupportedFlavorException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return result;
		}

		private class CPPInfo {
			Document doc;
			int pos, len, start, end, selLen, adjust;
			boolean hasSelection;

			public CPPInfo() {
				doc = getDocument();
				pos = getCaretPosition();
				len = doc.getLength();
				start = getSelectionStart();
				end = getSelectionEnd();
				selLen = end - start;
				adjust = 0;
				if (pos >= end) {
					adjust = end - start;
				}
				hasSelection = (start >= 0 && start < len && end > start && end <= len);
			}
		}

		@Override
		public void cut() {
			copy();
			CPPInfo i = new CPPInfo();
			if (i.hasSelection) {
				try {
					i.doc.remove(i.start, i.selLen);
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void copy() {
			String s = getSelectedText();
			if (s != null && !s.isEmpty()) {
				setClipboardContents(s);
			}
		}

		@Override
		public void paste() {
			String s = getClipboardContents();
			if (!s.isEmpty()) {
				try {
					CPPInfo i = new CPPInfo();

					if (i.hasSelection) {
						i.doc.remove(i.start, i.selLen);
					}

					i.pos -= i.adjust;
					i.doc.insertString(i.pos, s, null);
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void lostOwnership(Clipboard clipboard, Transferable contents) {
		}
	}

	private void createNote() {

		if (note != null) {
			remove(note);
		}

		note = new CustomTextPane();
		note.setDocument(new CustomDocument());
		note.addFocusListener(editorFocusListener);
		note.setFont(ElephantWindow.fontEditor);
		note.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
		note.setDragEnabled(true);

		note.setTransferHandler(new TransferHandler() {
			private static final long serialVersionUID = -4777142447614165019L;

			@Override
			public boolean canImport(TransferHandler.TransferSupport info) {
				return true;
			}

			@SuppressWarnings("unchecked")
			@Override
			public boolean importData(TransferHandler.TransferSupport info) {
				if (!info.isDrop()) {
					return false;
				}

				Transferable t = info.getTransferable();
				List<File> data;
				try {
					data = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
				} catch (Exception e) {
					return false;
				}
				if (eeListener != null) {
					eeListener.filesDropped(data);
				}

				return true;
			}
		});

		note.addCaretListener(new CaretListener() {
			@Override
			public void caretUpdate(CaretEvent e) {
				if (eeListener != null) {
					eeListener.caretChanged(note);
				}
			}
		});

		note.getDocument().addUndoableEditListener(new UndoEditListener());

		add(note, BorderLayout.CENTER);

		createPadding();
	}

	private void createPadding() {
		if (padding != null) {
			padding.setVisible(false);
			remove(padding);
		}

		padding = new JPanel(null);
		padding.setBackground(Color.WHITE);
		padding.addMouseListener(paddingClick);
		padding.setPreferredSize(new Dimension(0, 0));
		add(padding, BorderLayout.SOUTH);
	}

	public void setTitle(String s) {
		title.setText(s);
		title.setCaretPosition(0);
		title.setSelectionEnd(0);
	}

	public void setText(String s) {
		note.setText("");
		isRichText = false;

		if (s != null && s.length() > 0) {
			if (s.indexOf("{\\rtf") == 0) {
				try {
					RtfUtil.putRtf(note.getDocument(), s, 0);
					if (note.getDocument().getLength() == 0) {
						note.setText(s);
					} else {
						isRichText = true;
					}
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("no rtf");
					note.setText(s);
				} catch (BadLocationException e) {
					e.printStackTrace();
					System.out.println("no rtf");
					note.setText(s);
				}
			} else {
				note.setText(s);
			}
		}

		note.setCaretPosition(0);
	}

	public String getTitle() {
		return title.getText();
	}

	public String getText() throws BadLocationException {
		Document doc = note.getDocument();
		String plain = doc.getText(0, doc.getLength());
		String rtf = RtfUtil.getRtf(doc);

		return rtf != null && rtf.length() > 0 && isRichText ? rtf : plain;
	}

	public void clear() {
		setTitle("");

		// replace JTextPane with new instance to get rid of old styles.
		createNote();
		discardUndoBuffer();
	}

	public boolean hasFocus() {
		return note.hasFocus() || title.hasFocus();
	}

	public void initialFocus() {
		note.setCaretPosition(0);
		note.requestFocusInWindow();
	}

	public void focusTitle() {
		title.setCaretPosition(0);
		title.requestFocusInWindow();
	}

	class AttachmentInfo {
		Object object;
		int startPosition;
	}

	public List<AttachmentInfo> getAttachmentInfo() {
		ArrayList<AttachmentInfo> list = new ArrayList<AttachmentInfo>();

		ElementIterator iterator = new ElementIterator(note.getDocument());
		Element element;
		while ((element = iterator.next()) != null) {
			AttributeSet as = element.getAttributes();
			if (as.containsAttribute(ELEM, ICON)) {
				AttachmentInfo info = new AttachmentInfo();
				info.object = StyleConstants.getIcon(as);
				info.startPosition = element.getStartOffset();
				list.add(info);
			}

			if (as.containsAttribute(ELEM, COMP)) {
				AttachmentInfo info = new AttachmentInfo();
				info.object = StyleConstants.getComponent(as);
				info.startPosition = element.getStartOffset();
				list.add(info);
			}
		}

		return list;
	}

	protected class UndoEditListener implements UndoableEditListener {
		public void undoableEditHappened(UndoableEditEvent e) {
			// Remember the edit and update the menus
			undoManager.addEdit(e.getEdit());
			Elephant.eventBus.post(new UndoRedoStateUpdateRequest(undoManager));
		}
	}

	public void undo() {
		if (undoManager.canUndo()) {
			undoManager.undo();
		}
		Elephant.eventBus.post(new UndoRedoStateUpdateRequest(undoManager));
	}

	public void redo() {
		if (undoManager.canRedo()) {
			undoManager.redo();
		}
		Elephant.eventBus.post(new UndoRedoStateUpdateRequest(undoManager));
	}

	public void discardUndoBuffer() {
		undoManager.discardAllEdits();
		Elephant.eventBus.post(new UndoRedoStateUpdateRequest(undoManager));
	}
}
