package com.pinktwins.elephant;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JCheckBox;

import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.data.Search;
import com.pinktwins.elephant.data.Vault;
import com.pinktwins.elephant.util.IOUtil;

public class Start extends BackgroundPanel {

	private static final Logger LOG = Logger.getLogger(Start.class.getName());

	static Image tile;

	static {
		try {
			tile = ImageIO.read(Sidebar.class.getResourceAsStream("/images/notebooks.png"));
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}
	}

	private JPanel startupPanel;

	private JLabel welcomeLabel;

	private JButton chooseFolderButton;

	private JCheckBox createElephantFolderCheckBox;

	private JLabel hintLabel;

	public Start(final Runnable runWhenLocationSet) {
		super(tile);

		setLayout(new FlowLayout());
		createComponents();

		createElephantFolderCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String hintLabelText = createElephantFolderCheckBox.isSelected() ? "" : "Elephant will use the selected folder directly.";
				hintLabel.setText(hintLabelText);
			}
		});

		chooseFolderButton.addActionListener((event) -> {

			// Show the panel to select a new vault directory.
			// Set the new vault directory in the settings.
			// Restart the application.

			JFileChooser baseDirectoryChooser = buildBaseDirectoryChooser();
			int res = baseDirectoryChooser.showOpenDialog(Start.this);
			if (res != JFileChooser.APPROVE_OPTION) {
				// User cancelled the location change.
				return;
			}

			// The user selected  a new vault directory.
			File selectedFile = baseDirectoryChooser.getSelectedFile();

			String baseDirectoryPath = selectedFile.getAbsolutePath();
			if (createElephantFolderCheckBox.isSelected()) {
				baseDirectoryPath += File.separator + "Elephant";
			}

			boolean baseDirReady = createBaseDirIfNotExists(baseDirectoryPath);
			if (baseDirReady) {
				// Stop the indexer before changing the vault location.
				// The indexer must not write to the new location.
				Search.ssi.stop();

				// Change the vault location.
				Vault.getInstance().setLocation(baseDirectoryPath);

				// Restart ...
				runWhenLocationSet.run();
			}

		});
	}

	private void createComponents() {
		startupPanel = new JPanel(new GridLayout(4, 1));
		startupPanel.setBorder(BorderFactory.createEmptyBorder(140, 0, 0, 0));

		welcomeLabel = new JLabel("Please choose your note location.", JLabel.CENTER);
		welcomeLabel.setForeground(Color.DARK_GRAY);
		welcomeLabel.setFont(ElephantWindow.fontStart);
		welcomeLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

		chooseFolderButton = new JButton("Choose folder");

		createElephantFolderCheckBox = new JCheckBox("Create folder 'Elephant' under this location.");
		createElephantFolderCheckBox.setForeground(Color.DARK_GRAY);
		createElephantFolderCheckBox.setFont(ElephantWindow.fontStart);
		createElephantFolderCheckBox.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
		createElephantFolderCheckBox.setSelected(true);

		hintLabel = new JLabel("", JLabel.CENTER);
		hintLabel.setForeground(Color.DARK_GRAY);
		hintLabel.setFont(ElephantWindow.fontStart);
		hintLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

		startupPanel.add(welcomeLabel);
		startupPanel.add(chooseFolderButton);
		startupPanel.add(createElephantFolderCheckBox);
		startupPanel.add(hintLabel);

		add(startupPanel);
	}

	private JFileChooser buildBaseDirectoryChooser() {
		JFileChooser baseDirectoryChooser = new JFileChooser();
		baseDirectoryChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
		baseDirectoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		baseDirectoryChooser.setMultiSelectionEnabled(false);
		return baseDirectoryChooser;
	}

	private boolean createBaseDirIfNotExists(String baseDirectoryPath) {
		File baseDirectory = new File(baseDirectoryPath);
		if (!baseDirectory.exists() && !baseDirectory.mkdirs()) {
			// Base dir does not exist and we cannot create it ...
			// That means trouble.
			return false;
		}

		createInboxIfNotExists(baseDirectoryPath + File.separator + "Inbox", baseDirectory);

		return true;
	}

	private void createInboxIfNotExists(String inboxPath, File baseDirectory) {
		File inbox = new File(inboxPath);
		if (!inbox.mkdirs()) {
			return;
		}
		addBuiltInNotes(inbox);
		createShortcuts(baseDirectory);
	}

	private void createShortcuts(File baseDirectory) {
		File shortcuts = new File(baseDirectory.getAbsolutePath() + File.separator + ".shortcuts");
		try {
			IOUtil.writeFile(shortcuts, "{\"list\": [\"Inbox\", \"Inbox/Welcome.txt\", \"search:Tip\", \"search:tag:Today\"]}");
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}
	}

	private void addBuiltInNotes(File inbox) {
		addBuiltInNote(inbox + File.separator + "Advanced_settings.txt", "Tip #3 - Advanced settings", Note.getResourceNote("Advanced_settings.txt"));
		addBuiltInNote(inbox + File.separator + "html_example.html", "HTML Example", Note.getResourceNote("html_example.html"));
		addBuiltInNote(inbox + File.separator + "Markdown.md", "Tip #2 - Markdown", Note.getResourceNote("markdown.md"));
		addBuiltInNote(inbox + File.separator + "Shortcuts.txt", "Tip #1 - Shortcuts", Note.getResourceNote("shortcuts.txt"));
		addBuiltInNote(inbox + File.separator + "Welcome.txt", "Welcome!", Note.getResourceNote("welcome.txt"));
	}

	private void addBuiltInNote(String filePath, String title, String contents) {
		File note = new File(filePath);
		Note n = new Note(note);
		n.getMeta().title(title);
		try {
			IOUtil.writeFile(note, contents);
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}
	}
}
