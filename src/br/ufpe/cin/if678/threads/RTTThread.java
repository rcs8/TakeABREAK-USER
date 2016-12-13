package br.ufpe.cin.if678.threads;

import java.util.List;

import javax.swing.JTextPane;

import br.ufpe.cin.if678.UserController;
import br.ufpe.cin.if678.communication.UserAction;
import br.ufpe.cin.if678.gui.frame.TakeABREAK;

public class RTTThread extends Thread {

	public static long RTT = 0;

	@Override
	public void run() {
		while (true) {
			UserController.getInstance().getListener().waitRTT(this);
			synchronized (this) {
				try {
					UserController.getInstance().getWriter().queueAction(UserAction.PING, null);
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			List<JTextPane> RTTPanes = TakeABREAK.getInstance().getChatPanel().getRTTPanes();
			for (JTextPane pane : RTTPanes) {
				pane.setText(String.format("RTT: %.02fms", (RTT / 1000000.0)));
			}

			try {
				sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}