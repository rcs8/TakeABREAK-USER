package br.ufpe.cin.if678.threads;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.JTextPane;

import br.ufpe.cin.if678.Encryption;
import br.ufpe.cin.if678.UserController;
import br.ufpe.cin.if678.communication.UserAction;
import br.ufpe.cin.if678.gui.DisplayReceivingFile;

public class FileDownloadThread extends Thread {

	private UserController controller;

	private ServerSocket serverSocket;

	private int senderID;
	private DisplayReceivingFile displayFile;

	private JTextPane progress;
	private JTextPane time;
	private JProgressBar bar;
	private JButton start;
	private JButton pause;
	private JButton stop;
	private JButton restart;

	public FileDownloadThread(String groupName, int senderID, DisplayReceivingFile displayFile, JTextPane progress, JTextPane time, JProgressBar bar, JButton start, JButton pause, JButton stop, JButton restart) {
		this.controller = UserController.getInstance();

		try {
			this.serverSocket = new ServerSocket(1901);
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.senderID = senderID;
		this.displayFile = displayFile;

		this.progress = progress;
		this.time = time;
		this.bar = bar;
		this.start = start;
		this.pause = pause;
		this.stop = stop;
		this.restart = restart;
	}

	@Override
	public void run() {
		try {
			start.setEnabled(false);
			restart.setEnabled(false);

			pause.setEnabled(true);
			stop.setEnabled(true);

			Thread thread = this;
			pause.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {
					synchronized (thread) {
						thread.interrupt();

						start.setEnabled(true);
						pause.setEnabled(false);
					}
				}
			});
			stop.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {
					synchronized (thread) {
						thread.interrupt();

						start.setEnabled(false);
						pause.setEnabled(false);
						stop.setEnabled(false);
						restart.setEnabled(true);
					}
				}
			});

			controller.getWriter().queueAction(UserAction.RECEIVE_READY, displayFile.getTempFileName());

			Socket socket = serverSocket.accept();

			String fileName = displayFile.getFileName();
			long length = displayFile.getLength();

			Cipher cipher = Encryption.getDecryptionCipher(senderID);

			CipherInputStream CIS = new CipherInputStream(socket.getInputStream(), cipher);
			FileOutputStream FOS = new FileOutputStream(fileName, displayFile.getBytesReceived() != 0 ? true : false);

			byte[] buffer = new byte[4 * 1024];

			int count;
			long downloaded = 0;
			long time = 0;
			long start = System.currentTimeMillis();
			int counting = 0;
			ArrayList<Double> avg = new ArrayList<Double>();
			while ((count = CIS.read(buffer)) > 0 && !isInterrupted()) {
				if (downloaded == displayFile.getBytesReceived()) {
					displayFile.setBytesReceived(displayFile.getBytesReceived() + count);
					bar.setValue((int) ((displayFile.getBytesReceived() * 100L) / length));
					progress.setText(String.format("%02d%%", bar.getValue()));
					FOS.write(buffer, 0, count);
					time += System.currentTimeMillis() - start;
					counting += count;
					start = System.currentTimeMillis();
					if (time >= 1000) {
						avg.add((double) counting);
						counting = 0;
						time = 0;
						this.time.setText(String.format("%.02f segundos", ((length - downloaded) / getAvg(avg, 5))));
					}
				}

				downloaded += count;
			}

			this.time.setText("");

			if (!isInterrupted()) {
				pause.setEnabled(false);
				stop.setEnabled(false);
			}

			FOS.close();
			CIS.close();

			socket.close();

			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
	}

	private double getAvg(ArrayList<Double> avg, int amount) {
		double value = avg.get(avg.size() - 1);
		for (int c = Math.max(avg.size() - amount, 0); c < avg.size(); c++) {
			value += avg.get(c);
			value /= 2;
		}
		return value;
	}

}
