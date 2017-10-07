package com.pinktwins.elephant;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.text.BadLocationException;

import org.apache.commons.io.FilenameUtils;

import com.pinktwins.elephant.CustomEditor.AttachmentInfo;
import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.data.Notebook;
import com.pinktwins.elephant.data.Vault;
import com.pinktwins.elephant.eventbus.NoteChangedEvent;

public class SaveChanges {

	private static final Logger LOG = Logger.getLogger(SaveChanges.class.getName());

	private SaveChanges() {
	}

	private static void renameAccordingToFormat(Note currentNote, CustomEditor editor, String title) {
		if (!currentNote.isHtml()) {
			try {
				String newName = title;

				String defaultFiletype = Elephant.settings.getDefaultFiletype();
				if (!defaultFiletype.contains(".")) {
					defaultFiletype = "." + defaultFiletype;
				}

				if (!newName.endsWith(".md") && !newName.endsWith(".txt")) {
					newName = title + (currentNote.isMarkdown() ? ".md" : editor.isRichText ? ".rtf" : defaultFiletype);
				}
				currentNote.attemptSafeRename(newName);
			} catch (IOException e) {
				LOG.severe("Fail: " + e);
			}
		}
	}

	public static void saveChanges(Note currentNote, NoteAttachments attachments, NoteEditor noteEditor, TagEditorPane tagPane) {
		if (currentNote != null) {
			CustomEditor editor = noteEditor.editor;

			editor.saveSelection();

			boolean changed = false;
			boolean contentChanged = false;

			try {
				// Title
				String fileTitle = currentNote.getMeta().title();
				String editedTitle = editor.getTitle();
				if (!fileTitle.equals(editedTitle)) {
					currentNote.getMeta().title(editedTitle);
					renameAccordingToFormat(currentNote, editor, editedTitle);
					changed = true;
				}

				// Format
				String ext = FilenameUtils.getExtension(currentNote.file().getAbsolutePath()).toLowerCase();
				if (!changed) {
					// Did format change during edit?
					if ((editor.isRichText && "txt".equals(ext)) || (!editor.isRichText && "rtf".equals(ext))) {
						renameAccordingToFormat(currentNote, editor, editedTitle);
						changed = true;
					}
				}

				// If we saved as markdown note, set editor to markdown mode.
				if (changed) {
					if ("md".equals(ext) && !editor.isMarkdown) {
						editor.setMarkdown(true);
					}
				}

				// Tags
				if (tagPane.isDirty()) {
					List<String> tagNames = tagPane.getTagNames();
					List<String> tagIds = Vault.getInstance().resolveTagNames(tagNames);
					currentNote.getMeta().setTags(tagIds, tagNames);
					changed = true;
				}

				// Fetch attachment info
				List<AttachmentInfo> info = editor.getAttachmentInfo();

				// Check which attachments were removed
				Set<Object> remainingAttachments = attachments.keySet();
				for (AttachmentInfo i : info) {
					if (i.object instanceof ImageIcon || i.object instanceof FileAttachment) {
						File f = attachments.get(i.object);
						if (f != null) {
							remainingAttachments.remove(i.object);
						}
					}
				}

				// Remove all icon & component elements
				// that were inserted to hold attachments

				List<AttachmentInfo> info_reverse = editor.removeAttachmentElements(info);

				String fileText = currentNote.contents();
				String editedText = editor.getText();

				if (!fileText.equals(editedText)) {
					currentNote.save(editedText);
					changed = true;
					contentChanged = true;
				}

				// remainingAttachments were not found in document anymore.
				// Move to 'deleted'
				for (Object o : remainingAttachments) {
					File f = attachments.get(o);
					System.out.println("No longer in document: " + f);
					currentNote.removeAttachment(f);
					attachments.remove(o);

					changed = true;
					contentChanged = true;
				}

				if (changed || attachments.didChange()) {
					// update attachment positions in metadata
					for (AttachmentInfo i : info) {
						if (i.object instanceof ImageIcon || i.object instanceof FileAttachment) {
							File f = attachments.get(i.object);
							if (f != null) {
								currentNote.getMeta().setAttachmentPosition(f, i.startPosition);
							}
						}
					}

					changed = true;
					contentChanged = true;
				}

				// Finally, insert attachments back
				noteEditor.importAttachments(info_reverse);

				// update last note save time to help sync efforts
				if (changed) {
					Notebook nb = currentNote.findContainingNotebook();
					if (nb != null) {
						nb.markNoteSavedTimestamp();
					}
				}

			} catch (BadLocationException e) {
				LOG.severe("Fail: " + e);
			}

			editor.restoreSelection();

			if (changed) {
				new NoteChangedEvent(currentNote, contentChanged).post();
			}
		}

	}
}
