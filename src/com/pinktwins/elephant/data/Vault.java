package com.pinktwins.elephant.data;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.SystemUtils;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.Elephant;
import com.pinktwins.elephant.data.WatchDir.WatchDirListener;
import com.pinktwins.elephant.eventbus.TagsChangedEvent;
import com.pinktwins.elephant.eventbus.VaultEvent;
import com.pinktwins.elephant.util.Factory;

// 'root' data provider
public class Vault implements WatchDirListener {

	private static final Logger log = Logger.getLogger(Vault.class.getName());

	private static Vault instance = null;

	public static Vault getInstance() {
		if (instance == null) {
			instance = new Vault();
		}
		return instance;
	}

	private String HOME = "";
	private String defaultNotebook = "Inbox";

	private File home;
	private File trash;

	private List<Notebook> notebooks = Factory.newArrayList();
	private Tags tags = new Tags();

	WatchDir watchDir;

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

	public void populate() {
		home = new File(HOME);

		trash = new File(home.getAbsolutePath() + File.separator + "Trash");
		trash.mkdirs();

		for (File f : home.listFiles()) {
			if (f.isDirectory() && f.getName().charAt(0) != '.') {
				if (findNotebook(f) == null) {
					notebooks.add(new Notebook(f));
				}
			}
		}

		Collections.sort(notebooks);

		tags.reload(home.getAbsolutePath() + File.separator + ".tags");
		Elephant.eventBus.post(new TagsChangedEvent());

		if (watchDir == null) {
			new Thread() {
				@Override
				public void run() {
					try {
						// On linux the watcher needs to recursively add directories.
						// On mac and win we get a suitable modified event without.
						boolean watchRecursive = SystemUtils.IS_OS_LINUX;

						watchDir = new WatchDir(HOME, watchRecursive, Vault.this);
						watchDir.processEvents();
					} catch (IOException e) {
						log.severe("Fail: " + e);
					}
				}
			}.start();
		}
	}

	public List<Notebook> getNotebooks() {
		return notebooks;
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
			EventQueue.invokeLater(new Runnable() {
				@Override
				public void run() {
					File f = new File(file);
					if (f.isFile()) {
						f = f.getParentFile();
					}
					Notebook nb = findNotebook(f);
					if (nb != null) {
						// need latest tags loaded when refreshing notebook.
						tags.refresh();

						nb.refresh();
						Elephant.eventBus.post(new VaultEvent(VaultEvent.Kind.notebookRefreshed, nb));
						Elephant.eventBus.post(new VaultEvent(VaultEvent.Kind.notebookListChanged, nb));
					}
				}
			});
		}
	}

	public String getLuceneIndexPath() {
		return Elephant.settings.userHomePath() + File.separator + ".com.pinktwins.elephant.searchIndex";
	}

	public void saveNewTag(final Tag tag) {
		tags.saveTag(tag);
	}
}
