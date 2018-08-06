package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

import com.pinktwins.elephant.data.Settings;

public class SettingsUI extends BackgroundPanel {

	private JScrollPane scroll;
	private JPanel main;

	public SettingsUI() {
		createComponents();
	}

	class KeyEditor extends JPanel {
		Settings.Keys key;

		public KeyEditor(final Settings.Keys key) {
			this.key = key;

			JLabel name = new JLabel(key.title());
			name.setFont(Font.decode("Helvetica-BOLD-18"));

			JLabel desc = new JLabel("<html>" + key.description() + "</html>");
			desc.setPreferredSize(new Dimension(100, 100));
			desc.setMaximumSize(new Dimension(463, 400));
			desc.setFont(Font.decode("Helvetica-14"));

			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

			name.setAlignmentX(0);
			desc.setAlignmentX(0);

			JPanel f1 = new JPanel();
			f1.setPreferredSize(new Dimension(10, 32));
			f1.setAlignmentX(0);

			add(f1);
			add(name);
			add(desc);

			switch (key.getKind()) {
			case String:
				final JTextField tf = new JTextField();
				tf.setText(Elephant.settings.getString(key));
				tf.setMaximumSize(new Dimension(400, 100));
				tf.setAlignmentX(0);

				tf.addFocusListener(new FocusListener() {
					@Override
					public void focusGained(FocusEvent e) {
					}

					@Override
					public void focusLost(FocusEvent e) {
						Elephant.settings.set(key, tf.getText());
					}
				});

				add(tf);
				break;
			case Boolean:
				final JButton yes = new JButton("Yes");
				final JButton no = new JButton("No");

				if (Elephant.settings.getBoolean(key)) {
					yes.setSelected(true);
					no.setSelected(false);
				} else {
					yes.setSelected(false);
					no.setSelected(true);
				}

				yes.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						Elephant.settings.set(key, true);
						yes.setSelected(true);
						no.setSelected(false);
					}
				});

				no.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						Elephant.settings.set(key, false);
						yes.setSelected(false);
						no.setSelected(true);
					}
				});

				add(yes);
				add(no);
				break;
			case Float:
				final JTextField fe = new JTextField();
				fe.setText(String.valueOf(Elephant.settings.getFloat(key)));
				fe.setMaximumSize(new Dimension(400, 100));
				fe.setAlignmentX(0);

				fe.addFocusListener(new FocusListener() {

					@Override
					public void focusGained(FocusEvent e) {
					}

					@Override
					public void focusLost(FocusEvent e) {
						try {
							float f = Float.valueOf(fe.getText());
							if (f > 0.0) {
								Elephant.settings.set(key, fe.getText());
							}
						} catch (NumberFormatException ex) {
						}
					}
				});

				add(fe);
				break;
			default:
				break;

			}

			JPanel f2 = new JPanel();
			f2.setPreferredSize(new Dimension(10, 32));
			f2.setAlignmentX(0);
			add(f2);

			setBorder(BorderFactory.createMatteBorder(4, 0, 4, 0, Color.decode("#dddddd")));
		}
	}

	private void createComponents() {
		main = new JPanel();
		main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));

		for (Settings.Keys k : Settings.uiKeys) {
			KeyEditor e = new KeyEditor(k);
			main.add(e);
		}

		scroll = new JScrollPane(main);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(ElephantWindow.emptyBorder);
		scroll.getVerticalScrollBar().setUnitIncrement(10);
		scroll.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 0));

		add(scroll, BorderLayout.CENTER);
	}

}
