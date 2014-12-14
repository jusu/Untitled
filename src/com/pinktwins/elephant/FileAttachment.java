package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.io.FileUtils;

public class FileAttachment extends JPanel {
	private static final long serialVersionUID = 5444731416148596756L;

	private static JFileChooser chooser = new JFileChooser();

	private static Image quickLook, openFolder;
	private static boolean qlExists;
	private static String qlPath = "/usr/bin/qlmanage";

	static {
		try {
			quickLook = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/quickLook.png"));
			openFolder = ImageIO.read(Sidebar.class.getClass().getResourceAsStream("/images/openFolder.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		qlExists = new File(qlPath).exists();
	}

	private JPanel left, text, right;
	private JLabel label, size;
	private JButton icon, show, open;

	public FileAttachment(final File f) {
		super();

		String labelStr = f.getName();
		long fileLen = 0;

		if (f.exists()) {
			fileLen = f.length();
		}

		setLayout(new BorderLayout());
		setBackground(Color.WHITE);
		setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));

		left = new JPanel(new FlowLayout(FlowLayout.LEFT));
		left.setOpaque(false);

		text = new JPanel(new BorderLayout());
		text.setOpaque(false);

		right = new JPanel(new FlowLayout());
		right.setOpaque(false);

		label = new JLabel(labelStr);
		label.setFont(ElephantWindow.fontBoldEditor);
		label.setForeground(ElephantWindow.colorGray5);

		size = new JLabel(FileUtils.byteCountToDisplaySize(fileLen));
		size.setFont(ElephantWindow.fontMedium);
		size.setForeground(Color.DARK_GRAY);

		icon = new JButton("");
		Icon i = chooser.getIcon(f);
		BufferedImage image = new BufferedImage(i.getIconWidth(), i.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
		i.paintIcon(new JPanel(), image.getGraphics(), 0, 0);
		Image scaled = image.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
		icon.setIcon(new ImageIcon(scaled));
		icon.setBorder(ElephantWindow.emptyBorder);
		icon.setBorderPainted(false);

		show = new JButton("");
		show.setIcon(new ImageIcon(quickLook));
		show.setBorderPainted(false);
		show.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 4));

		open = new JButton("");
		open.setIcon(new ImageIcon(openFolder));
		open.setBorderPainted(false);
		open.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 14));

		text.add(label, BorderLayout.NORTH);
		text.add(size, BorderLayout.CENTER);
		left.add(icon);
		left.add(text);
		if (qlExists) {
			right.add(show);
		} else {
			open.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		}
		right.add(open);

		add(left, BorderLayout.WEST);
		add(right, BorderLayout.EAST);

		setMaximumSize(new Dimension(294, 38));

		icon.addMouseListener(new CustomMouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					try {
						java.awt.Desktop.getDesktop().open(f);
					} catch (IOException e1) {
						e1.printStackTrace();
					}

				}
			}
		});

		show.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (qlExists) {
					try {
						String[] cmd = new String[3];
						cmd[0] = qlPath;
						cmd[1] = "-p";
						cmd[2] = f.getAbsolutePath();

						Runtime.getRuntime().exec(cmd, new String[0], null);
					} catch (IOException e2) {
						e2.printStackTrace();
					}
				} else {
					try {
						java.awt.Desktop.getDesktop().open(f.getParentFile());
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		});

		open.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					java.awt.Desktop.getDesktop().open(f.getParentFile());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
	}
}
