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
import com.pinktwins.elephant.data.Vault;
import com.pinktwins.elephant.util.IOUtil;

public class Start extends BackgroundPanel {

	private static final Logger LOG = Logger.getLogger(Start.class.getName());

	static Image tile;

	static {
		try {
			tile = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/notebooks.png"));
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}
	}

	public Start(final Runnable runWhenLocationSet) {
		super(tile);

		setLayout(new FlowLayout());

		JPanel main = new JPanel(new GridLayout(4, 1));
		main.setBorder(BorderFactory.createEmptyBorder(140, 0, 0, 0));

		JLabel welcome = new JLabel("Please choose your note location.", JLabel.CENTER);
		welcome.setForeground(Color.DARK_GRAY);
		welcome.setFont(ElephantWindow.fontStart);
		welcome.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

		JButton bLocation = new JButton("Choose folder");

		final JCheckBox createElephantFolder = new JCheckBox("Create folder 'Elephant' under this location.");
		createElephantFolder.setForeground(Color.DARK_GRAY);
		createElephantFolder.setFont(ElephantWindow.fontStart);
		createElephantFolder.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
		createElephantFolder.setSelected(true);
		
		final JLabel hint = new JLabel("", JLabel.CENTER);
		hint.setForeground(Color.DARK_GRAY);
		hint.setFont(ElephantWindow.fontStart);
		hint.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

		main.add(welcome);
		main.add(bLocation);
		main.add(createElephantFolder);
		main.add(hint);

		add(main);

		createElephantFolder.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (createElephantFolder.isSelected()) {
					hint.setText("");
				} else {
					hint.setText("Elephant will use the selected folder directly.");
				}
			}
		});

		bLocation.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				JFileChooser ch = new JFileChooser();
				ch.setCurrentDirectory(new File(System.getProperty("user.home")));
				ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				ch.setMultiSelectionEnabled(false);

				int res = ch.showOpenDialog(Start.this);
				if (res == JFileChooser.APPROVE_OPTION) {
					File f = ch.getSelectedFile();
					if (f.exists()) {
						File folder = null;
						if (createElephantFolder.isSelected()) {
							folder = new File(f.getAbsolutePath() + File.separator + "Elephant");
						} else {
							folder = new File(f.getAbsolutePath());
						}

						if (folder.exists() || folder.mkdirs()) {

							Vault.getInstance().setLocation(folder.getAbsolutePath());

							File inbox = new File(folder + File.separator + "Inbox");
							if (inbox.mkdirs()) {

								addBuiltInNote(inbox + File.separator + "Advanced_settings.txt", "Tip #3 - Advanced settings",
										Note.getResourceNote("Advanced_settings.txt"));
								addBuiltInNote(inbox + File.separator + "html_example.html", "HTML Example", Note.getResourceNote("html_example.html"));
								addBuiltInNote(inbox + File.separator + "Markdown.md", "Tip #2 - Markdown", Note.getResourceNote("markdown.md"));
								addBuiltInNote(inbox + File.separator + "Shortcuts.txt", "Tip #1 - Shortcuts", Note.getResourceNote("shortcuts.txt"));
								addBuiltInNote(inbox + File.separator + "Welcome.txt", "Welcome!", Note.getResourceNote("welcome.txt"));

								File shortcuts = new File(folder.getAbsolutePath() + File.separator + ".shortcuts");
								try {
									IOUtil.writeFile(shortcuts, "{\"list\": [\"Inbox\", \"Inbox/Welcome.txt\", \"search:Tip\", \"search:tag:Today\"]}");
								} catch (IOException e) {
									LOG.severe("Fail: " + e);
								}
							}

							Vault.getInstance().populate();
							runWhenLocationSet.run();
						}
					}
				}
			}
		});
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
