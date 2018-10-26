package com.pinktwins.elephant;

import static javafx.concurrent.Worker.State.FAILED;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

public class BrowserPane extends JPanel {

	private static final Logger LOG = Logger.getLogger(BrowserPane.class.getName());

	private static String HEIGHT_SCRIPT, NUMFRAMESETS_SCRIPT;

	static {
		try {
			HEIGHT_SCRIPT = IOUtils.toString(BrowserPane.class.getResourceAsStream("/style/documentHeight.js"));
			NUMFRAMESETS_SCRIPT = IOUtils.toString(BrowserPane.class.getResourceAsStream("/style/numFramesets.js"));
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}
	}

	interface BrowserEventListener {
		public void mouseWheelEvent(MouseWheelEvent e);
	}

	class CustomJFXPanel extends JFXPanel {
		@Override
		protected void processMouseWheelEvent(MouseWheelEvent e) {
			if (beListener != null) {
				beListener.mouseWheelEvent(e);
			}
		}
	}

	private final CustomJFXPanel jfxPanel = new CustomJFXPanel();
	private WebEngine engine;
	private WebView view;
	private Scene scene;

	private BrowserEventListener beListener;

	EventListener clickListener = new EventListener() {
		public void handleEvent(Event ev) {
			ev.preventDefault();

			final String href = ((Element) ev.getTarget()).getAttribute("href");
			if (href != null) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						try {
							Desktop.getDesktop().browse(new URI(href));
						} catch (IOException e) {
							LOG.severe("Fail: " + e);
						} catch (URISyntaxException e) {
							LOG.severe("Fail: " + e);
						}
					}
				});
			}
		}
	};

	public BrowserPane() {
		super(new GridLayout(1, 1));
		Platform.setImplicitExit(false);
		initComponents();
	}

	public void setBrowserEventListener(BrowserEventListener l) {
		beListener = l;
	}

	private void initComponents() {
		createScene();
		add(jfxPanel);
	}

	private void createScene() {

		Platform.runLater(new Runnable() {
			@Override
			public void run() {

				view = new WebView();
				engine = view.getEngine();

				view.setContextMenuEnabled(false);
				engine.setUserStyleSheetLocation(BrowserPane.class.getResource("/style/webview_style.css").toExternalForm());

				engine.getLoadWorker().exceptionProperty().addListener(new ChangeListener<Throwable>() {
					public void changed(ObservableValue<? extends Throwable> o, Throwable old, final Throwable value) {
						if (engine.getLoadWorker().getState() == FAILED) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									JOptionPane.showMessageDialog(BrowserPane.this,
											(value != null) ? engine.getLocation() + "\n" + value.getMessage() : engine.getLocation() + "\nUnexpected error.",
											"Loading error...", JOptionPane.ERROR_MESSAGE);
								}
							});
						}
					}
				});

				engine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
					public void changed(ObservableValue<? extends State> ov, State oldState, State newState) {
						if (newState == State.SUCCEEDED) {

							Document doc = engine.getDocument();
							NodeList list = doc.getElementsByTagName("a");
							for (int i = 0; i < list.getLength(); i++) {
								((EventTarget) list.item(i)).addEventListener("click", clickListener, false);
							}
						}
					}
				});

				engine.documentProperty().addListener(new ChangeListener<Document>() {
					@Override
					public void changed(ObservableValue<? extends Document> prop, Document oldDoc, Document newDoc) {
						final Runnable resizer = new Runnable() {
							@Override
							public void run() {
								// Calculate document height, set note height to document height.
								// Nice scrolling where webpage is 'embedded' in the note.

								String heightText = engine.executeScript(HEIGHT_SCRIPT).toString();
								double height = Double.valueOf(heightText.replace("px", ""));

								view.resize(view.getWidth(), height);

								final int h = (int) (height);
								EventQueue.invokeLater(new Runnable() {
									@Override
									public void run() {
										setHeightTo(h);
									}
								});

								// If document has a frameset for frame redirection, we have no access to the in-frame
								// document.
								// In this case, apply style with overflow-y: scroll to be able to scroll the page.
								// Not as nice as embedded style, but a good fallback.

								String numFramesetsText = engine.executeScript(NUMFRAMESETS_SCRIPT).toString();
								int numFramesets = Integer.valueOf(numFramesetsText);
								if (numFramesets > 0) {
									engine.setUserStyleSheetLocation(BrowserPane.class.getResource("/style/webview_style_frameset.css").toExternalForm());
								} else {
									String currentCSS = engine.getUserStyleSheetLocation();
									if (currentCSS.indexOf("_frameset") > 0) {
										engine.setUserStyleSheetLocation(BrowserPane.class.getResource("/style/webview_style.css").toExternalForm());
									}
								}
							}
						};

						Platform.runLater(resizer);
					}
				});

				scene = new Scene(view);
				jfxPanel.setScene(scene);
			}
		});
	}

	public void clear() {
		loadURL("about:blank");
	}

	public void loadURL(final String url) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				engine.load(url);
			}
		});
	}

	private void setHeightTo(int h) {
		Rectangle b = jfxPanel.getBounds();
		b.height = h;
		jfxPanel.setBounds(b.x, b.y, b.width, b.height);

		Dimension d = BrowserPane.this.getPreferredSize();
		d.height = h;
		BrowserPane.this.setPreferredSize(d);

		BrowserPane.this.revalidate();
	}
}
