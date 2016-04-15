package com.pinktwins.elephant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.text.html.HTMLEditorKit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

import com.pinktwins.elephant.data.Settings;

public class HtmlPaneStylesheet {

	// private StyleSheet stylesheet = null;
	private List<String> rules = new ArrayList<String>();

	private static HtmlPaneStylesheet instance = null;

	private static final Logger LOG = Logger.getLogger(HtmlPaneStylesheet.class.getName());

	public static HtmlPaneStylesheet getInstance() {
		String property = System.getenv("elephant.alwaysReadStylesheet");
		boolean alwaysLoad = Boolean.TRUE.toString().equals(property);
		if (instance == null || alwaysLoad) {
			instance = new HtmlPaneStylesheet();
		}
		return instance;
	}

	private HtmlPaneStylesheet() {
		createStylesheet();
	}

	private void createStylesheet() {
		String string = Elephant.settings.getString(Settings.Keys.VAULT_FOLDER);
		string = string + File.separator + "style.css";
		File file = new File(string);
		if (file.exists() && file.canRead()) {
			try {
				String styleString = IOUtils.toString(new FileInputStream(file));
				/*
				 * Add a Stylesheet-Parser, but first all simple, every Style in one line!
				 */
				String lines[] = styleString.split("\\r?\\n");
				for (String line : lines) {
					// stylesheet.addRule(line);
					rules.add(line);
				}
			} catch (FileNotFoundException e) {
				LOG.log(Level.WARNING, string + " not found", e);
			} catch (IOException e) {
				LOG.log(Level.WARNING, "error reading style.css", e);
			}
		}
	}

	public void addStylesheet(HTMLEditorKit kit) {
		Validate.notNull(kit);
		if (!rules.isEmpty()) {
			for (String rule : rules) {
				kit.getStyleSheet().addRule(rule);
			}
			// kit.getStyleSheet().addStyleSheet(stylesheet);
		}
	}

}
