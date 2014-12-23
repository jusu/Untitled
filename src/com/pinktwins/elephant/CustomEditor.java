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
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataHandler;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
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
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.rtf.RTFEditorKit;
import javax.swing.undo.UndoManager;

import org.apache.commons.io.IOUtils;

import com.google.common.eventbus.Subscribe;

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

		Elephant.eventBus.register(this);

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

		private String prevRtfCopy = "";

		public void setClipboardContents(String aString) {
			StringSelection stringSelection = new StringSelection(aString);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(stringSelection, this);
			prevRtfCopy = "";
		}

		public void setClipboardContentsRtf(String rtf) {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			try {
				DataHandler hand = new DataHandler(new ByteArrayInputStream(rtf.getBytes("UTF-8")), "text/rtf");
				clipboard.setContents(hand, this);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			prevRtfCopy = rtf;
		}

		public String getClipboardContents() {
			String result = "";

			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			// odd: the Object param of getContents is not currently used
			Transferable contents = clipboard.getContents(null);

			DataFlavor[] fl = contents.getTransferDataFlavors();
			ArrayList<DataFlavor> textFlavors = new ArrayList<DataFlavor>();
			for (DataFlavor df : fl) {
				String mime = df.getMimeType();
				if (mime.indexOf("text/rtf") >= 0 || mime.indexOf("text/plain") >= 0) {
					textFlavors.add(df);
				}
			}

			DataFlavor[] te = new DataFlavor[textFlavors.size()];
			te = textFlavors.toArray(te);

			DataFlavor best = DataFlavor.selectBestTextFlavor(te);
			boolean hasTransferableText = (contents != null) && contents.isDataFlavorSupported(best);
			if (hasTransferableText) {
				try {
					Reader r = best.getReaderForText(contents);
					BufferedReader br = new BufferedReader(r);
					result = IOUtils.toString(br);
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

			if (isRichText) {
				int start = note.getSelectionStart();
				int end = note.getSelectionEnd();

				if (end > start) {
					// Put rtf to clipboard: clone document, remove
					// everything but selection.
					Document d = new CustomDocument();
					try {
						RtfUtil.putRtf(d, RtfUtil.getRtf(getDocument()), 0);
						d.remove(end, d.getLength() - end);
						d.remove(0, start);
						String rtf = RtfUtil.getRtf(d);

						setClipboardContentsRtf(rtf);
					} catch (IOException e) {
						e.printStackTrace();
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				}
			}
		}

		@Override
		public void paste() {
			String s = getClipboardContents();

			// For some reason I don't get any rtf text from clipboard copied
			// there by me. Workround.
			if (s.isEmpty() && !prevRtfCopy.isEmpty()) {
				s = prevRtfCopy;
			}

			if (!s.isEmpty()) {
				try {
					CPPInfo i = new CPPInfo();

					if (i.hasSelection) {
						i.doc.remove(i.start, i.selLen);
					}

					i.pos -= i.adjust;

					if (!s.substring(0, 5).equals("{\\rtf")) {
						i.doc.insertString(i.pos, s, null);
					} else {
						// RTFEditorKit doesn't support 'position' argument on
						// read() method, so create a new document and copy
						// text + styles over.
						CustomDocument d = new CustomDocument();

						try {
							RtfUtil.putRtf(d, s, 0);

							Element[] elems = d.getRootElements();
							for (Element e : elems) {
								for (int idx = 0, count = e.getElementCount(); idx < count - 1; idx++) {
									Element sub = e.getElement(idx);
									if ("paragraph".equals(sub.getName())) {
										int start = sub.getStartOffset();
										int end = sub.getEndOffset();
										AttributeSet as = d.getCharacterElement(start).getAttributes();

										if (end > start) {
											String text = d.getText(start, end - start);
											i.doc.insertString(i.pos, text, as);
											i.pos += end - start;
											isRichText = true;
										}
									}
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
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

		InputMap inputMap = note.getInputMap();

		KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_B, ElephantWindow.menuMask);
		inputMap.put(ks, boldAction);
		ks = KeyStroke.getKeyStroke(KeyEvent.VK_I, ElephantWindow.menuMask);
		inputMap.put(ks, italicAction);
		ks = KeyStroke.getKeyStroke(KeyEvent.VK_U, ElephantWindow.menuMask);
		inputMap.put(ks, underlineAction);

		ks = KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, ElephantWindow.menuMask);
		inputMap.put(ks, increaseFontSizeAction);
		ks = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, ElephantWindow.menuMask);
		inputMap.put(ks, decreaseFontSizeAction);

		add(note, BorderLayout.CENTER);

		createPadding();
	}

	private AbstractAction boldAction = new AbstractAction() {
		StyledEditorKit.BoldAction a = new StyledEditorKit.BoldAction();

		@Override
		public void actionPerformed(ActionEvent e) {
			isRichText = true;
			a.actionPerformed(e);
		}
	};

	private AbstractAction italicAction = new AbstractAction() {
		StyledEditorKit.ItalicAction a = new StyledEditorKit.ItalicAction();

		@Override
		public void actionPerformed(ActionEvent e) {
			isRichText = true;
			a.actionPerformed(e);
		}
	};

	private AbstractAction underlineAction = new AbstractAction() {
		StyledEditorKit.UnderlineAction a = new StyledEditorKit.UnderlineAction();

		@Override
		public void actionPerformed(ActionEvent e) {
			isRichText = true;
			a.actionPerformed(e);
		}
	};

	private void shiftFontSize(final int delta) {
		int start = note.getSelectionStart();
		int end = note.getSelectionEnd();

		StyledDocument doc = note.getStyledDocument();

		if (start == end) {
			return;
		}

		AttributeSet attrs = doc.getCharacterElement(start).getAttributes();
		SimpleAttributeSet as = new SimpleAttributeSet();
		as.addAttributes(attrs);

		int size = StyleConstants.getFontSize(attrs);
		StyleConstants.setFontSize(as, size + delta);

		doc.setCharacterAttributes(start, end - start, as, false);

		isRichText = true;
	}

	private AbstractAction increaseFontSizeAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			shiftFontSize(1);
		}
	};

	private AbstractAction decreaseFontSizeAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			shiftFontSize(-1);
		}
	};

	@Subscribe
	public void handleStyleCommandEvent(StyleCommandEvent e) {
		String cmd = e.event.getActionCommand();
		if ("Bold".equals(cmd)) {
			boldAction.actionPerformed(e.event);
			return;
		}
		if ("Italic".equals(cmd)) {
			italicAction.actionPerformed(e.event);
			return;
		}
		if ("Underline".equals(cmd)) {
			underlineAction.actionPerformed(e.event);
			return;
		}
		if ("Bigger".equals(cmd)) {
			increaseFontSizeAction.actionPerformed(e.event);
			return;
		}
		if ("Smaller".equals(cmd)) {
			decreaseFontSizeAction.actionPerformed(e.event);
			return;
		}
		if ("Make Plain Text".equals(cmd)) {
			note.getStyledDocument().setCharacterAttributes(0, note.getDocument().getLength(), new SimpleAttributeSet(), true);
			note.requestFocusInWindow();
			isRichText = false;
		}
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

	public static boolean setTextRtfOrPlain(JTextPane textPane, String s) {
		boolean rich = false;

		if (s != null && s.length() > 0) {
			if (s.indexOf("{\\rtf") == 0) {
				try {
					RtfUtil.putRtf(textPane.getDocument(), s, 0);
					if (textPane.getDocument().getLength() == 0) {
						textPane.setText(s);
					} else {
						rich = true;
					}
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("no rtf");
					textPane.setText(s);
				} catch (BadLocationException e) {
					e.printStackTrace();
					System.out.println("no rtf");
					textPane.setText(s);
				}
			} else {
				textPane.setText(s);
			}
		}

		return rich;
	}

	public void setText(String s) {
		note.setText("");

		isRichText = setTextRtfOrPlain(note, s);

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
