package br.ufpe.cin.if678.communication;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import br.ufpe.cin.if678.Encryption;
import br.ufpe.cin.if678.UserController;
import br.ufpe.cin.if678.business.Group;
import br.ufpe.cin.if678.gui.DisplayMessage;
import br.ufpe.cin.if678.gui.DisplayReceivingFile;
import br.ufpe.cin.if678.gui.frame.TakeABREAK;
import br.ufpe.cin.if678.threads.InitialRequestThread;
import br.ufpe.cin.if678.util.Pair;
import br.ufpe.cin.if678.util.Tuple;

public class Listener {

	private UserController controller;

	private Thread initialRequestThread;
	private Thread groupCreationThread;
	private Thread reconnectionThread;
	private Thread fileUploadThread;

	public Listener(UserController controller) {
		this.controller = controller;
	}

	public void waitUsername(Thread thread) {
		this.initialRequestThread = thread;
	}

	public void waitGroupCreation(Thread thread) {
		this.groupCreationThread = thread;
	}

	public void waitReconnection(Thread thread) {
		this.reconnectionThread = thread;
	}

	public void waitFileUpload(Thread thread) {
		this.fileUploadThread = thread;
	}

	public void onVerifyUsername(int ID) {
		((InitialRequestThread) initialRequestThread).setID(ID);

		synchronized (initialRequestThread) {
			initialRequestThread.notify();
		}
	}

	public void onUserConnect(Tuple<Integer, String, InetSocketAddress> data) {
		controller.getIDToNameAddress().put(data.getFirst(), new Pair<String, InetSocketAddress>(data.getSecond(), data.getThird()));
		controller.getNameToID().put(data.getSecond(), data.getFirst());
		controller.getAddressToID().put(data.getThird(), data.getFirst());

		if (TakeABREAK.getInstance().getUserListPanel() != null) {
			TakeABREAK.getInstance().getUserListPanel().updateUsers();
		}
	}

	public void onUserListUpdate(HashMap<Integer, Pair<String, InetSocketAddress>> data) {
		controller.getIDToNameAddress().clear();
		controller.getNameToID().clear();
		controller.getAddressToID().clear();

		controller.getIDToNameAddress().putAll(data);
		for (Map.Entry<Integer, Pair<String, InetSocketAddress>> entry : data.entrySet()) {
			controller.getNameToID().put(entry.getValue().getFirst(), entry.getKey());
			controller.getAddressToID().put(entry.getValue().getSecond(), entry.getKey());
		}

		if (TakeABREAK.getInstance().getUserListPanel() != null) {
			TakeABREAK.getInstance().getUserListPanel().updateUsers();
		}

		synchronized (initialRequestThread) {
			initialRequestThread.notify();
		}

		if (reconnectionThread == null) {
			return;
		}

		synchronized (reconnectionThread) {
			reconnectionThread.notify();
		}
	}

	public void onGroupReceive(Group group) {
		controller.getGroups().put(group.getName(), group);

		if (groupCreationThread == null) {
			return;
		}

		synchronized (groupCreationThread) {
			groupCreationThread.notify();
		}
	}

	public void onGroupAddMember(Group group) {
		controller.getGroups().put(group.getName(), group);

		if (groupCreationThread == null) {
			return;
		}

		synchronized (groupCreationThread) {
			groupCreationThread.notify();
		}
	}

	public void onGroupMessage(Tuple<String, Integer, byte[]> data) {
		String groupName = data.getFirst();
		int senderID = data.getSecond();
		byte[] encryted = data.getThird();

		String message = "";
		try {
			message = Encryption.decryptMessage(senderID, encryted);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (controller.getMessages(groupName) == null) {
			controller.getGroupMessages().put(groupName, new ArrayList<DisplayMessage>());
		}

		controller.getMessages(groupName).add(new DisplayMessage(senderID, message));

		if (TakeABREAK.getInstance().getChatPanel() == null) {
			return;
		}

		TakeABREAK.getInstance().getChatPanel().updateScreen();
		TakeABREAK.getInstance().getChatListPanel().updateChatList();
	}

	public void onGroupFile(Tuple<String, Integer, Tuple<Integer, byte[], Long>> data) {
		String groupName = data.getFirst();
		int senderID = data.getSecond();
		Tuple<Integer, byte[], Long> fileInfo = data.getThird();

		int tempFileName = fileInfo.getFirst();
		byte[] encryptedFileName = fileInfo.getSecond();
		long fileLength = fileInfo.getThird();

		String fileName = String.valueOf(tempFileName);
		try {
			fileName = Encryption.decryptMessage(senderID, encryptedFileName);
		} catch (Exception e) {
			e.printStackTrace();
		}

		DisplayReceivingFile displayFile = new DisplayReceivingFile(senderID, tempFileName, fileName, fileLength);
		controller.receiveFile(groupName, senderID, displayFile);
	}

	public void onStartUpload() {
		synchronized (fileUploadThread) {
			fileUploadThread.notify();
		}
	}

}
