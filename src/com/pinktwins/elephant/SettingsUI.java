package com.pinktwins.elephant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;

import org.apache.commons.lang.SystemUtils;

import com.pinktwins.elephant.data.Settings;
import com.pinktwins.elephant.data.Settings.Keys;
import com.pinktwins.elephant.eventbus.FontChangedEvent;
import com.pinktwins.elephant.util.Factory;

import io.github.dheid.fontchooser.FontDialog;

public class SettingsUI extends BackgroundPanel {

	interface FontSetter {
		public void setFont(Font f);
	};

	private JScrollPane scroll;
	private JPanel main;

	private Map<String, JLabel> labels = Factory.newHashMap();

	public SettingsUI() {
		createComponents();
	}

	private String styleName(int style) {
		switch (style) {
		case Font.PLAIN:
			return "PLAIN";
		case Font.BOLD:
			return "BOLD";
		case Font.ITALIC:
			return "ITALIC";
		case Font.BOLD + Font.ITALIC:
			return "BOLD+ITALIC";
		default:
			return "PLAIN";
		}
	}

	private String fullFontName(Font f) {
		return String.format("%s-%s-%d", f.getName(), styleName(f.getStyle()), f.getSize());
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
				final JToggleButton yes = new JToggleButton("Yes");
				final JToggleButton no = new JToggleButton("No");

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
			case Fonts:
				add(addFontButton(Keys.FONT_EDITOR, fullFontName(ElephantWindow.fontEditor), "Editor", new FontSetter() {
					@Override
					public void setFont(Font f) {
						ElephantWindow.fontEditor = f;
					}
				}));
				add(addFontButton(Keys.FONT_EDITORTITLE, fullFontName(ElephantWindow.fontEditorTitle), "Editor Title", new FontSetter() {
					@Override
					public void setFont(Font f) {
						ElephantWindow.fontEditorTitle = f;
					}
				}));
				add(addFontButton(Keys.FONT_CARDNAME, fullFontName(ElephantWindow.fontNoteListCardName), "Note List: Card Title", new FontSetter() {
					@Override
					public void setFont(Font f) {
						ElephantWindow.fontNoteListCardName = f;
					}
				}));
				add(addFontButton(Keys.FONT_CARDPREVIEW, fullFontName(ElephantWindow.fontCardPreview), "Note List: Card Preview", new FontSetter() {
					@Override
					public void setFont(Font f) {
						ElephantWindow.fontCardPreview = f;
					}
				}));
				add(addFontButton(Keys.FONT_SNIPPETNAME, fullFontName(ElephantWindow.fontNoteListSnippetName), "Note List: Snippet Title", new FontSetter() {
					@Override
					public void setFont(Font f) {
						ElephantWindow.fontNoteListSnippetName = f;
					}
				}));
				add(addFontButton(Keys.FONT_SNIPPETPREVIEW, fullFontName(ElephantWindow.fontSnippetPreview), "Note List: Snippet Preview", new FontSetter() {
					@Override
					public void setFont(Font f) {
						ElephantWindow.fontSnippetPreview = f;
					}
				}));

				if (SystemUtils.IS_OS_MAC_OSX) {
					final JButton resetAvenir = new JButton("Use theme 'Avenir'");
					resetAvenir.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							ElephantWindow.fontEditorTitle = Font.decode("AvenirNext-Bold-PLAIN-16");
							ElephantWindow.fontEditor = Font.decode("Avenir-Roman-PLAIN-14");
							ElephantWindow.fontNoteListSnippetName = Font.decode("AvenirNext-Medium-BOLD-13");
							ElephantWindow.fontSnippetPreview = Font.decode("AvenirNext-Medium-PLAIN-11");
							ElephantWindow.fontNoteListCardName = Font.decode("AvenirNext-Bold-BOLD-12");
							ElephantWindow.fontCardPreview = Font.decode("AvenirNext-Medium-PLAIN-10");

							setFontLabel(ElephantWindow.fontEditorTitle);
							setFontLabel(ElephantWindow.fontEditor);
							setFontLabel(ElephantWindow.fontNoteListSnippetName);
							setFontLabel(ElephantWindow.fontSnippetPreview);
							setFontLabel(ElephantWindow.fontNoteListCardName);
							setFontLabel(ElephantWindow.fontCardPreview);

							Elephant.settings.set("fontEditorTitle", "AvenirNext-Bold-PLAIN-16");
							Elephant.settings.set("fontEditor", "Avenir-Roman-PLAIN-14");
							Elephant.settings.set("fontSnippetName", "AvenirNext-Medium-BOLD-13");
							Elephant.settings.set("fontSnippetPreview", "AvenirNext-Medium-PLAIN-11");
							Elephant.settings.set("fontCardName", "AvenirNext-Bold-BOLD-12");
							Elephant.settings.set("fontCardPreview", "AvenirNext-Medium-PLAIN-10");
							Elephant.settings.set("fontScale", "1.0");

							new FontChangedEvent().post();
						}

						private void setFontLabel(Font f) {
							String fontName = fullFontName(f);
							JLabel l = labels.get(fontName);
							if (l != null) {
								l.setText(fontName);
								l.setFont(f);
							}
						}
					});
					JPanel p = new JPanel(new BorderLayout());
					p.setAlignmentX(0);
					p.add(resetAvenir, BorderLayout.EAST);
					p.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
					add(p);
				}

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

		private JPanel addFontButton(final Keys fontKey, final String defaultFontName, final String buttonName, final FontSetter fontSetter) {
			String fEditor = Elephant.settings.getString(fontKey);
			if (fEditor == null || fEditor.isEmpty()) {
				fEditor = defaultFontName;
			}
			final JButton butt = new JButton(buttonName);
			final JLabel label = new JLabel(fEditor);
			final JButton reset = new JButton("Reset");

			butt.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					FontDialog dialog = new FontDialog((Frame) null, "Select Font: " + buttonName, true);
					dialog.setSelectedFont(Font.decode(defaultFontName));
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
					int w = (int) d.getWidth();
					int h = (int) d.getHeight();
					int ww = 640;
					int hh = 720;
					dialog.setBounds((w - ww) / 2, (h - hh) / 2, ww, hh);
					dialog.setVisible(true);
					if (!dialog.isCancelSelected()) {
						Font f = dialog.getSelectedFont();
						String s = fullFontName(f);
						Elephant.settings.set(fontKey, s);
						label.setText(s);
						label.setFont(f);
						fontSetter.setFont(f);

						Elephant.settings.set(Keys.FONT_SCALE, "1.0");
						new FontChangedEvent().post();
					}
				}
			});

			labels.put(defaultFontName, label);

			JPanel p = new JPanel(new BorderLayout());
			p.setAlignmentX(0);
			reset.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String fontName = fontKey.fontDefaults();
					Font f = Font.decode(fontName);

					Elephant.settings.set(fontKey, fontName);
					fontSetter.setFont(f);

					label.setText(fontName);
					label.setFont(f);
					new FontChangedEvent().post();
				}
			});

			label.setFont(Font.decode(defaultFontName));
			JPanel pLeft = new JPanel();
			JPanel pRight = new JPanel();

			butt.setAlignmentX(0);
			label.setAlignmentX(0);
			reset.setAlignmentX(0);

			pLeft.add(butt);
			pLeft.add(label);
			pRight.add(reset);

			p.add(pLeft, BorderLayout.WEST);
			p.add(pRight, BorderLayout.EAST);

			return p;
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
