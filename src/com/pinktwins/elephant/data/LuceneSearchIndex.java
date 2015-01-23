package com.pinktwins.elephant.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.pinktwins.elephant.util.Factory;

public class LuceneSearchIndex implements SearchIndexInterface {

	private static final Logger log = Logger.getLogger(LuceneSearchIndex.class.getName());

	Directory dir;
	Analyzer analyzer = new StandardAnalyzer();
	IndexWriter writer;

	IndexReader reader;
	IndexSearcher searcher;

	Object readerSync = new Object();
	Object writerSync = new Object();

	QueryParser parser;

	public static int lastSearchTotalHits = 0;

	private final String indexPath;

	// http://lucene.apache.org/core/4_10_3/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#Escaping_Special_Characters
	// not escaped: *
	private String escapeChars = "+-&|!(){}[]^\"~?:\\/+";

	public LuceneSearchIndex() {
		indexPath = Vault.getInstance().getLuceneIndexPath();

		if (SearchIndexer.useLucene) {
			try {
				File f = new File(indexPath);
				f.mkdirs();
				dir = FSDirectory.open(f);
				parser = new QueryParser("contents", analyzer);
				parser.setAllowLeadingWildcard(true);
			} catch (IOException e) {
				log.log(Level.SEVERE, e.toString());

				// Fail. Turn us off.
				SearchIndexer.useLucene = false;
			}
		}
	}

	public void openWriter() throws IOException {
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LATEST, analyzer);
		iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
		iwc.setRAMBufferSizeMB(256.0);

		writer = new IndexWriter(dir, iwc);
	}

	public void closeWriter() {
		synchronized (writerSync) {
			if (writer != null) {
				try {
					writer.commit();
					writer.close();
				} catch (IOException e) {
					log.log(Level.SEVERE, e.toString());
				}
				writer = null;
			}
		}
	}

	public void openReader() throws IOException {
		reader = DirectoryReader.open(dir);
		searcher = new IndexSearcher(reader);
	}

	public void closeReader() {
		synchronized (readerSync) {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					log.log(Level.SEVERE, e.toString());
				}
				reader = null;
			}
		}
	}

	@Override
	public void digestText(Note n, String text) {
		try {
			indexFile(n.file());
		} catch (IOException e) {
			log.log(Level.SEVERE, e.toString());
		}
	}

	@Override
	public Set<Note> search(String text) {
		closeWriter();

		if (text.isEmpty()) {
			return Collections.emptySet();
		}

		StringBuffer b = new StringBuffer(text);
		for (int n = b.length() - 1; n >= 0; n--) {
			if (escapeChars.indexOf(b.charAt(n)) != -1) {
				b.insert(n, '\\');
			}
		}
		text = b.toString();

		// Substring search always. May have performance considerations.
		text = '*' + text + '*';

		try {
			synchronized (readerSync) {
				if (reader == null) {
					openReader();
				}

				Query query = parser.parse(text);
				return searchNotes(query);
			}
		} catch (ParseException e) {
			log.log(Level.SEVERE, e.toString());
		} catch (IOException e) {
			log.log(Level.SEVERE, e.toString());
		}
		return Collections.emptySet();
	}

	@Override
	public void purgeNote(Note note) {
		try {
			synchronized (writerSync) {
				if (writer == null) {
					openWriter();
				}

				Term term = new Term("path", note.file().getAbsolutePath());
				writer.deleteDocuments(term);
			}
		} catch (IOException e) {
			log.log(Level.SEVERE, e.toString());
		}
	}

	@Override
	public void debug() {
	}

	@Override
	public void commit() {
		closeWriter();
		closeReader();
	}

	private void indexFile(File file) throws IOException {
		if (file.getParentFile().getName().equals("Trash")) {
			return;
		}

		synchronized (readerSync) {
			if (reader == null) {
				try {
					openReader();
				} catch (IndexNotFoundException e) {
					// Index not creatd yet
				}
			}

			// Check if file already indexed and up-to-date
			if (reader != null) {
				Term t = new Term("path", file.getAbsolutePath());
				if (reader.docFreq(t) > 0) {
					Query q = new TermQuery(t);
					ScoreDoc[] sd = searcher.search(q, 1).scoreDocs;
					if (sd.length == 1) {
						Number n = searcher.doc(sd[0].doc).getField("modified").numericValue();
						if (n.equals(file.lastModified())) {
							// File indexed and not changed, no need to reindex.
							return;
						}
					}
				}
			}
		}

		FileInputStream fis;
		try {
			fis = new FileInputStream(file);
		} catch (FileNotFoundException fnfe) {
			// at least on windows, some temporary files raise this exception with an "access denied" message
			// checking if the file can be read doesn't help
			return;
		}
		try {
			Document doc = new Document();

			doc.add(new StringField("path", file.getAbsolutePath(), Field.Store.YES));
			doc.add(new LongField("modified", file.lastModified(), Field.Store.YES));
			doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))));

			synchronized (writerSync) {
				if (writer == null) {
					openWriter();
				}

				writer.updateDocument(new Term("path", file.getAbsolutePath()), doc);
			}
		} finally {
			fis.close();
		}
	}

	private Set<Note> searchNotes(Query query) throws IOException {
		Set<Note> found = Factory.newHashSet();

		int hitsPerPage = 100000;

		TopDocs td = searcher.search(query, hitsPerPage);
		ScoreDoc[] hits = td.scoreDocs;

		int start = 0, end = Math.min(hits.length, start + hitsPerPage);
		lastSearchTotalHits = td.totalHits;

		for (int i = start; i < end; i++) {
			Document doc = searcher.doc(hits[i].doc);
			String path = doc.get("path");
			if (path != null) {
				File f = new File(path);
				if (f.exists()) {
					found.add(new Note(f));
				}
			} else {
				System.out.println((i + 1) + ". " + "No path for this document.");
			}
		}

		return found;
	}

}
