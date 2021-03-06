package br.ufpe.cin.if678.threads;

import java.awt.Cursor;
import java.io.IOException;

import javax.swing.JTextField;

import br.ufpe.cin.if678.UserController;
import br.ufpe.cin.if678.gui.frame.TakeABREAK;
import br.ufpe.cin.if678.gui.panel.StartupPanel;

public class ServerConnectionThread extends Thread {

	private TakeABREAK frame;
	private UserController controller;

	private StartupPanel panel;

	private JTextField addressField;

	public ServerConnectionThread(TakeABREAK frame, StartupPanel panel, JTextField addressField) {
		this.frame = frame;
		this.controller = UserController.getInstance();

		this.panel = panel;

		this.addressField = addressField;
	}

	@Override
	public void run() {
		frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		try {
			controller.initialize(addressField.getText());
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Não foi possível conectar com o servidor");
			panel.resetField();
			frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			return;
		}
		frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

		frame.clearFrame();
		frame.addPanel(frame.getAuthenticationPanel());
		frame.getAuthenticationPanel().grabFocus();
	}

}
