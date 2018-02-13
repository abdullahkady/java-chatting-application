package server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class ServerThread extends Thread {
	protected Socket clientSocket;
	protected String currentID;
	protected boolean terminateThread;

	public Socket getClientSocket() {
		return clientSocket;
	}

	public ServerThread(Socket connectionSocket) {
		this.clientSocket = connectionSocket;
		this.terminateThread = false;
	}

	public void run() {
		this.welcomeClient();

		if (terminateThread) // In case user quits during the welcoming (before joining)
			return;
		//Servicing a client normally.
		String messageReceived;
		try {
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			DataOutputStream outToClient = new DataOutputStream(clientSocket.getOutputStream());

			while (true) {
				messageReceived = inFromClient.readLine();
				if (messageReceived.equals("Quit")) {
					Server.clientsList.remove(this.currentID);
					System.out.println("--Internal-- Removed " + this.currentID + " from the member's list");
					clientSocket.close();
					return;
				}

				if (messageReceived.equals("GetMemberList()")) {
					Object[] tempClients = Server.clientsList.keySet().toArray();
					outToClient.writeBytes("The current online members are : \n");
					for (int i = 0; i < tempClients.length; i++) {
						outToClient.writeBytes("	 - " + tempClients[i] + "\n");
					}
				} else if (messageReceived.startsWith("Chat(") && messageReceived.endsWith(")")) {
					String pureArguments = messageReceived.substring(4, messageReceived.length() - 1);
					String[] params = pureArguments.split(","); // will just ignore the source?
					String dest = params[1];
					String msg = params[2];
					// int TTL = Integer.parseInt(params[3]); for later use, with more than one server as specified.
					if (Server.clientsList.containsKey(dest)) {
						Socket tempSocket = Server.clientsList.get(dest).getClientSocket();
						DataOutputStream outToTemp = new DataOutputStream(tempSocket.getOutputStream());
						outToTemp.writeBytes(currentID + " : " + msg + "\n");
					} else {
						outToClient.writeBytes(
								"Wrong destination specified, you can type 'GetMemberList()' to get a list of the current online members \n");
					}
				} else {
					outToClient.writeBytes("Since you didn't use any command, I will just capitalize your input and send it back : \n");
					outToClient.writeBytes(messageReceived.toUpperCase() + "\n");
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void welcomeClient() {
		String messageReceived;
		try {
			DataOutputStream outToClient = new DataOutputStream(this.clientSocket.getOutputStream());
			outToClient.writeBytes("Connected to server! \nPlease use the Join(YourUserName) to join the server \n");

			// Waiting for the join message
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			while (true) {
				messageReceived = inFromClient.readLine();
				if (messageReceived.equals("Quit")) {
					clientSocket.close();
					terminateThread = true;
					break;
				}
				if (messageReceived.length() < 7 || !(messageReceived.substring(0, 5).equals("Join(")
						&& messageReceived.charAt(messageReceived.length() - 1) == ')')) {
					outToClient.writeBytes("Your message need to be of the format 'Join(Username)' \n");
				} else if (Server.clientsList.containsKey(messageReceived.substring(5, messageReceived.length() - 1))) {
					outToClient.writeBytes("Username already in use, please choose another one \n");
				} else {
					this.currentID = messageReceived.substring(5, messageReceived.length() - 1);
					Server.clientsList.put(this.currentID, this);
					System.out.println("--Internal-- Added " + this.currentID + " to the member's list");
					outToClient.writeBytes("You have successfully joined. Welcome " + this.currentID + "\n");
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}