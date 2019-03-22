package com.pinktwins.elephant.data;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.io.FileUtils;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.Elephant;
import com.pinktwins.elephant.data.WatchDir.WatchDirListener;
import com.pinktwins.elephant.eventbus.NoteChangedEvent;
import com.pinktwins.elephant.eventbus.NotebookEvent;
import com.pinktwins.elephant.eventbus.TagsChangedEvent;
import com.pinktwins.elephant.eventbus.VaultEvent;
import com.pinktwins.elephant.util.Factory;

// 'root' data provider
public class Vault implements WatchDirListener {

	private static final Logger LOG = Logger.getLogger(Vault.class.getName());

	private static Vault instance = null;

	private String HOME = "";
	private String defaultNotebook = "Inbox";

	private File home;
	private File trash;

	private List<Notebook> notebooks = Factory.newArrayList();
	private Tags tags = new Tags();

	WatchDir watchDir;

	public static Vault getInstance() {
		if (instance == null) {
			instance = new Vault();
		}
		return instance;
	}

	private Vault() {
		Elephant.eventBus.register(this);

		String def = Elephant.settings.getString(Settings.Keys.DEFAULT_NOTEBOOK);
		if (!def.isEmpty()) {
			defaultNotebook = def;
		}
	}

	public File getHome() {
		return home;
	}

	public boolean hasLocation() {
		return !HOME.isEmpty();
	}

	public void setLocation(String vaultPath) {
		if (new File(vaultPath).exists()) {
			Elephant.settings.set(Settings.Keys.VAULT_FOLDER, vaultPath);
			HOME = vaultPath;
			populate();

			// When to use Lucene indexing?
			// For now, use memory-based indexing up to 2000 notes. It's fast, doesn't create extra files.
			// After 2k it starts consuming some memory so Lucene preferred.
			// Can overwrite this by setting 'useLucene' to 0 or 1.
			SearchIndexer.useLucene = getNoteCount() >= 2000;

			// Changed 2017-05-27: Attachments are indexed only with Lucene,
			// so defaulting this to true. 'useLucene' setting can still turn this off.
			SearchIndexer.useLucene = true;

			if (Elephant.settings.has(Settings.Keys.USE_LUCENE)) {
				SearchIndexer.useLucene = Elephant.settings.getInt(Settings.Keys.USE_LUCENE) == 1;
			}
		}
	}

	public Notebook getDefaultNotebook() {
		if (!hasLocation()) {
			return null;
		}
		File f = new File(home.getAbsolutePath() + File.separator + defaultNotebook);
		Notebook nb = findNotebook(f);
		if (nb == null && notebooks.size() > 0) {
			nb = notebooks.get(0);
		}
		return nb;
	}

	public File getTrash() {
		return trash;
	}

	/**
	 * Prepare the vault directory, see that everything is present.
	 * <ul>
	 *     <li>Create the Trash notebook.</li>
	 *     <li>Create a Notebook instance for all visible directories in the vault base directory.</li>
	 * </ul>
	 */
	public void populate() {
		home = new File(HOME);

		// The trash directory should always be there.
		trash = new File(home.getAbsolutePath() + File.separator + "Trash");
		trash.mkdirs();

		// Scan the vault directory for notebooks.
		synchronized (Search.lockObject) {
			for (File f : home.listFiles()) {
				if (f.isDirectory() && f.getName().charAt(0) != '.') {
					if (findNotebook(f) == null) {
						notebooks.add(new Notebook(f));
					}
				}
			}
		}

		Collections.sort(notebooks);

		tags.reload(home.getAbsolutePath() + File.separator + ".tags");
		new TagsChangedEvent().post();

		if (watchDir == null) {
			new Thread() {
				@Override
				public void run() {
					try {
						// Recursively watching dirs is needed to be notified of single
						// file notifications inside notebook folders (possibly done with
						// external programs). Just watching the vault folder would only
						// notify on directory level changes (file added/removed/renamed).
						boolean watchRecursive = true;

						watchDir = new WatchDir(HOME, watchRecursive, Vault.this);
						watchDir.processEvents();
					} catch (IOException e) {
						LOG.severe("Fail: " + e);
					}
				}
			}.start();
		}
	}

	public List<Notebook> getNotebooks() {
		return notebooks;
	}

	public void sortNotesInNotebooks() {
		for (Notebook nb : notebooks) {
			nb.sortNotes();
		}
	}

	public int getNoteCount() {
		int count = 0;
		for (Notebook nb : notebooks) {
			count += nb.count();
		}
		return count;
	}

	public Collection<Notebook> getNotebooksWithFilter(final String text) {
		if (text == null || text.isEmpty()) {
			return notebooks;
		}

		final String lo = text.toLowerCase();
		return CollectionUtils.select(notebooks, new Predicate<Notebook>() {
			@Override
			public boolean evaluate(Notebook nb) {
				return nb.name().toLowerCase().indexOf(lo) >= 0;
			}
		});
	}

	public Collection<Tag> getTagsWithFilter(String text) {
		if (text == null || text.isEmpty()) {
			return tags.asList();
		}

		final String lo = text.toLowerCase();
		return CollectionUtils.select(tags.asList(), new Predicate<Tag>() {
			@Override
			public boolean evaluate(Tag t) {
				return t.name.toLowerCase().indexOf(lo) >= 0;
			}
		});
	}

	@SuppressWarnings("unlikely-arg-type")
	public Notebook findNotebook(File f) {
		for (Notebook n : notebooks) {
			if (n.equals(f)) {
				return n;
			}
		}
		return null;
	}

	public List<String> resolveTagNames(List<String> tagNames) {
		return tags.resolveNames(tagNames);
	}

	public List<String> resolveTagIds(List<String> tagIds) {
		return tags.resolveIds(tagIds);
	}

	public String getLuceneIndexPath() {
		return Paths.get(getHome().getAbsolutePath(), ".searchIndex").toString();
	}

	public void saveNewTag(final Tag tag) {
		tags.saveTag(tag);
	}

	public void deleteTag(final String tagId, final String tagName) {
		Notebook nb = Notebook.getNotebookWithTag(tagId, tagName);

		// Confirm deletion IF tag assigned to notes
		int count = nb.count();
		if (count > 0) {
			String message = String.format("The tag will be removed from %d note%s.", count, count == 1 ? "" : "s");
			if (JOptionPane.showConfirmDialog(null, message, String.format("Delete tag %s?", tagName), JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
				return;
			}
		}

		// Remove tag from notes
		for (Note n : nb.notes) {
			Note.Meta m = n.getMeta();
			List<String> ids = m.tags();
			List<String> names = resolveTagIds(ids);

			int idIndex = ids.indexOf(tagId);
			int nameIndex = names.indexOf(tagName);

			if (idIndex >= 0 && nameIndex >= 0) {
				String name = names.get(nameIndex);
				if (tagName.equals(name)) {
					ids.remove(idIndex);
					names.remove(nameIndex);
					m.setTags(ids, names);
				} else {
					LOG.info("Fail: name mismatch while deleting tag: wanted to delete " + tagName + " but found " + name
							+ " + instead. Not deleting tag from note " + m.title());
				}
			} else {
				LOG.info("Fail: id lookup while deleting tag: " + idIndex + ", " + nameIndex + ". Not deleting tag from note " + m.title());
			}

			new NoteChangedEvent(n, true).post();
		}

		tags.deleteTag(tagId, tagName);
	}

	public void deleteNotebook(Notebook nb) {
		if (nb.isTrash()) {
			return;
		}

		if (nb.isDynamicallyCreatedNotebook()) {
			LOG.info("Fail: tried to delete dynamically created notebook: " + nb.name() + ". Doing nothing.");
			return;
		}

		if (nb.folder().exists()) {
			// Confirm deletion IF any notes
			int count = nb.count();
			if (count > 0) {
				String message = String.format("The notebook directory (with %d note%s) will be moved to Trash. It will no longer be visible in Elephant.",
						count, count == 1 ? "" : "s");
				if (JOptionPane.showConfirmDialog(null, message, String.format("Delete notebook %s?", nb.name()),
						JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
					return;
				}
			}

			// Check if dest directory already exists, rename to unique if needed
			File source = nb.folder();
			File dest = new File(getTrash().getAbsoluteFile() + File.separator + source.getName());

			if (dest.exists()) {
				File renamed = new File(source.getAbsoluteFile() + "_" + Long.toString(System.currentTimeMillis(), 36));
				try {
					FileUtils.moveDirectory(source, renamed);
				} catch (IOException e) {
					LOG.severe("Fail: cannot rename " + source + " -> " + renamed + " e: " + e);
					return;
				}
				source = renamed;
			}

			// Purge notes from search index
			for (Note n : nb.notes) {
				Search.ssi.purgeNote(n);
			}

			// Move
			try {
				FileUtils.moveDirectoryToDirectory(source, getTrash(), false);
			} catch (IOException e) {
				LOG.severe("Fail: " + e);
			}
		}

		notebooks.remove(nb);

		new VaultEvent(VaultEvent.Kind.notebookListChanged, nb).post();
	}

	@Subscribe
	public void handleNotebookEvent(NotebookEvent event) {
		populate();
	}

	@Subscribe
	public void handleVaultEvent(VaultEvent event) {
		switch (event.kind) {
		case notebookCreated:
			populate();
			break;
		case notebookListChanged:
			populate();
			break;
		case notebookRefreshed:
			break;
		default:
			break;
		}
	}

	@Override
	public void watchEvent(final String kind, final String file) {
		if ("ENTRY_MODIFY".equals(kind)) {
			File f = new File(file);
			if (f.isFile()) {
				f = f.getParentFile();
			}
			Notebook nb = findNotebook(f);
			if (nb != null) {
				// Refresh notebook, but use one second throttle to keep number of
				// refreshes to minimum when multiple files have changed.
				RefreshNotebook pending = pendingRefreshes.get(nb);
				if (pending != null) {
					pending.cancel();
				}
				pending = new RefreshNotebook(nb);
				pendingRefreshes.put(nb, pending);
				timer.schedule(pending, 1000);
			}
		}
	}

	private Timer timer = new Timer();
	private Map<Notebook, RefreshNotebook> pendingRefreshes = Factory.newConcurrentHashMap();

	class RefreshNotebook extends TimerTask {
		private final Notebook nb;

		public RefreshNotebook(Notebook nb) {
			this.nb = nb;
		}

		@Override
		public void run() {
			pendingRefreshes.remove(nb);

			EventQueue.invokeLater(new Runnable() {
				@Override
				public void run() {
					// need latest tags loaded when refreshing notebook.
					tags.refresh();

					// refresh notebook
					nb.refresh();

					new VaultEvent(VaultEvent.Kind.notebookRefreshed, nb).post();
					new VaultEvent(VaultEvent.Kind.notebookListChanged, nb).post();
				}
			});
		}
	}

}
