package com.pinktwins.elephant;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.data.Vault;
import com.pinktwins.elephant.util.IOUtil;

public class Start extends BackgroundPanel {

	static Image tile;

	static {
		try {
			tile = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/notebooks.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public Start(final Runnable runWhenLocationSet) {
		super(tile);

		setLayout(new FlowLayout());

		JPanel main = new JPanel(new GridLayout(3, 1));
		main.setBorder(BorderFactory.createEmptyBorder(200, 0, 0, 0));

		JLabel welcome = new JLabel("Please choose your note location.", JLabel.CENTER);
		welcome.setForeground(Color.DARK_GRAY);
		welcome.setFont(ElephantWindow.fontStart);
		welcome.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

		JButton bLocation = new JButton("Choose folder");

		JLabel hint = new JLabel("Folder 'Elephant' will be created under this folder.", JLabel.CENTER);
		hint.setForeground(Color.DARK_GRAY);
		hint.setFont(ElephantWindow.fontStart);
		hint.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

		main.add(welcome);
		main.add(bLocation);
		main.add(hint);

		add(main);

		bLocation.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser ch = new JFileChooser();
				ch.setCurrentDirectory(new File(System.getProperty("user.home")));
				ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				ch.setMultiSelectionEnabled(false);

				int res = ch.showOpenDialog(Start.this);
				if (res == JFileChooser.APPROVE_OPTION) {
					File f = ch.getSelectedFile();
					if (f.exists()) {
						File folder = new File(f + File.separator + "Elephant");
						if (folder.exists() || folder.mkdirs()) {

							Vault.getInstance().setLocation(folder.getAbsolutePath());

							File inbox = new File(folder + File.separator + "Inbox");
							if (inbox.mkdirs()) {

								File note = new File(inbox + File.separator + "Shortcuts.txt");
								Note n = new Note(note);
								n.getMeta().title("To add shortcuts..");
								n.save("Edit the file \".shortcuts\" in your note folder.\nUI coming eventually!");

								note = new File(inbox + File.separator + "Welcome.txt");
								n = new Note(note);
								n.getMeta().title("Hello there");
								n.save("Welcome to Elephant!\n\nHit CMD-N to create a new note.\n\nDrag an image or file here to attach it.\n\nThis is Elephant release #4. You're an early bird!");

								File shortcuts = new File(folder.getAbsolutePath() + File.separator + ".shortcuts");
								try {
									IOUtil.writeFile(shortcuts, "{\"list\": [\"Inbox\", \"Inbox/Welcome.txt\"]}");
								} catch (IOException e1) {
									e1.printStackTrace();
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
}
