package com.pinktwins.elephant.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

public class PasswordDialog {

	private JPasswordField pField;
	private JCheckBox showInput;
	private JCheckBox checkbox;
	private String remember;
	private char defaultEchoChar = '*';
	
	public String getPassword() {
		if (remember != null) {
			return remember;
		} else {
			return show();
		}
	}

	public String show() {
		int selection = JOptionPane.showConfirmDialog(null, getPanel(), "Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (selection == JOptionPane.OK_OPTION) {
			if (checkbox.isSelected()) {
				remember = new String(pField.getPassword());
			} else {
				remember = null;
			}

			return new String(pField.getPassword());
		}

		return null;
	}

	private JPanel getPanel() {
		JPanel panel = new JPanel();

		panel.setLayout(new BorderLayout(5, 5));
		panel.setOpaque(true);

		pField = new JPasswordField(10);
		pField.addAncestorListener(new RequestFocusListener());
		defaultEchoChar = pField.getEchoChar();

		showInput = new JCheckBox("Show password");
		checkbox = new JCheckBox("Remember until restart");

		JPanel inputs = new JPanel();
		inputs.setLayout(new BoxLayout(inputs, BoxLayout.PAGE_AXIS));
		inputs.add(pField);
		inputs.add(showInput);
		inputs.add(checkbox);

		panel.add(inputs);

		showInput.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				if (showInput.isSelected()) {
					pField.setEchoChar((char)0);
				} else {
					pField.setEchoChar(defaultEchoChar);
				}
			}});
		
		return panel;
	}

	class RequestFocusListener implements AncestorListener {
		private boolean removeListener;

		public RequestFocusListener() {
			this(true);
		}

		public RequestFocusListener(boolean removeListener) {
			this.removeListener = removeListener;
		}

		@Override
		public void ancestorAdded(AncestorEvent e) {
			JComponent component = e.getComponent();
			component.requestFocusInWindow();

			if (removeListener)
				component.removeAncestorListener(this);
		}

		@Override
		public void ancestorRemoved(AncestorEvent event) {
		}

		@Override
		public void ancestorMoved(AncestorEvent event) {
		}
	}

}
