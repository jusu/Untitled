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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
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
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.pinktwins.elephant.util.Factory;

public class LuceneSearchIndex implements SearchIndexInterface {

	Directory dir;
	Analyzer analyzer = new StandardAnalyzer();
	IndexWriterConfig iwc = new IndexWriterConfig(Version.LATEST, analyzer);
	IndexWriter writer;

	IndexReader reader;
	IndexSearcher searcher;

	QueryParser parser;

	public LuceneSearchIndex() {
		try {
			dir = FSDirectory.open(new File("/Users/jusu/Desktop/test.index"));
			parser = new QueryParser(Version.LATEST, "contents", analyzer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void openWriter() throws IOException {
		System.out.println("OPENWRITER");
		iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
		iwc.setRAMBufferSizeMB(256.0);

		writer = new IndexWriter(dir, iwc);
	}

	public void closeWriter() {
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			writer = null;
		}
	}

	public void openReader() throws IOException {
		reader = DirectoryReader.open(dir);
		searcher = new IndexSearcher(reader);
	}

	public void closeReader() {
		if (reader != null) {
			try {
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			reader = null;
		}
	}

	@Override
	public void digestWord(Note n, String text) {
		try {
			if (writer == null) {
				openWriter();
			}
			indexFile(n.file());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Set<Note> search(String text) {
		try {
			if (reader == null) {
				openReader();
			}
			Query query = parser.parse(text);
			System.out.println("Searching for: " + query.toString("contents"));
			Set<Note> found = searchNotes(query);

			closeReader();

			return found;
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Collections.emptySet();
	}

	@Override
	public void purgeNote(Note note) {
		try {
			if (writer == null) {
				openWriter();
			}
			Term term = new Term("path", note.file().getAbsolutePath());
			writer.deleteDocuments(term);
			// closeWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void debug() {
	}

	private void indexFile(File file) throws IOException {
		if (reader == null) {
			openReader();
		}

		// XXX FIX
		// Note in index? Fine for initial startup, but how about next saved version of the note?
		Term t = new Term("path", file.getPath());
		if (reader.docFreq(t) > 0) {
			return;
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
			// make a new, empty document
			Document doc = new Document();

			// Add the path of the file as a field named "path". Use a
			// field that is indexed (i.e. searchable), but don't tokenize
			// the field into separate words and don't index term frequency
			// or positional information:
			Field pathField = new StringField("path", file.getAbsolutePath(), Field.Store.YES);
			doc.add(pathField);

			doc.add(new LongField("modified", file.lastModified(), Field.Store.NO));

			// Add the contents of the file to a field named "contents". Specify a Reader,
			// so that the text of the file is tokenized and indexed, but not stored.
			// Note that FileReader expects the file to be in UTF-8 encoding.
			// If that's not the case searching for special characters will fail.
			doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))));

			// Existing index (an old copy of this document may have been indexed) so
			// we use updateDocument instead to replace the old one matching the exact
			// path, if present:
			System.out.println("updating " + file);
			writer.updateDocument(new Term("path", file.getPath()), doc);
		} finally {
			fis.close();
		}
	}

	private Set<Note> searchNotes(Query query) throws IOException {
		Set<Note> found = Factory.newHashSet();

		int hitsPerPage = 1000;

		int start = 0;
		ScoreDoc[] hits = searcher.search(query, hitsPerPage).scoreDocs;
		int end = Math.min(hits.length, start + hitsPerPage);

		System.out.println("hits.length: " + hits.length);

		for (int i = start; i < end; i++) {
			Document doc = searcher.doc(hits[i].doc);
			String path = doc.get("path");
			if (path != null) {
				found.add(new Note(new File(path)));
			} else {
				System.out.println((i + 1) + ". " + "No path for this document.");
			}
		}

		return found;
	}

}
