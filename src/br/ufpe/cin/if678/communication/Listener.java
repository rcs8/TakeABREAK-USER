package br.ufpe.cin.if678.communication;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import br.ufpe.cin.if678.Encryption;
import br.ufpe.cin.if678.UserController;
import br.ufpe.cin.if678.business.Group;
import br.ufpe.cin.if678.gui.DisplayMessage;
import br.ufpe.cin.if678.gui.frame.TakeABREAK;
import br.ufpe.cin.if678.threads.GroupCreationThread;
import br.ufpe.cin.if678.threads.InitialRequestThread;
import br.ufpe.cin.if678.threads.ReconnectionThread;
import br.ufpe.cin.if678.util.Pair;
import br.ufpe.cin.if678.util.Tuple;

public class Listener {

	private UserController controller;

	private Thread initialRequestThread;
	private Thread groupCreationThread;
	private Thread reconnectionThread;

	public Listener(UserController controller) {
		this.controller = controller;
	}

	public void waitUsername(InitialRequestThread requestUsernameThread) {
		this.initialRequestThread = requestUsernameThread;
	}

	public void waitGroupCreation(GroupCreationThread groupCreationThread) {
		this.groupCreationThread = groupCreationThread;
	}

	public void waitReconnection(ReconnectionThread reconnectionThread) {
		this.reconnectionThread = reconnectionThread;
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

		TakeABREAK.getInstance().getUserListPanel().updateUsers();
	}

	public void onUserListUpdate(HashMap<Integer, Pair<String, InetSocketAddress>> data) {
		controller.getIDToNameAddress().clear();
		controller.getNameToID().clear();
		controller.getAddressToID().clear();

		System.out.println(data);
		for (Map.Entry<Integer, Pair<String, InetSocketAddress>> entry : data.entrySet()) {
			controller.getIDToNameAddress().put(entry.getKey(), entry.getValue());
			controller.getNameToID().put(entry.getValue().getFirst(), entry.getKey());
			controller.getAddressToID().put(entry.getValue().getSecond(), entry.getKey());
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
		Integer senderID = data.getSecond();
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

		TakeABREAK.getInstance().getChatPanel().updateScreen();
		TakeABREAK.getInstance().getChatListPanel().updateChatList();
	}

}
