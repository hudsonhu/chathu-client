import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
public class Client implements Runnable {
    InputStream in;
    OutputStream out;
    String port = "2009";
    String name;
    Socket socketToServer;
    ServerSocket socketToClient;
    ClientUI ui;
    Map<String, String> clients;  // <userId, ip:port>
    Map<String, Socket> clientSockets;  // <userId, socket>
    Map<String, ArrayList<String>> stuckMessages;  // <userId, messages>
    boolean isConnectionEstablished = false, isRunning = false;

    public void connectServer(String ip, int port, String name) {
        try {
            socketToServer = new Socket(ip, port);
            in = socketToServer.getInputStream();
            out = socketToServer.getOutputStream();
            this.name = name;
            isConnectionEstablished = socketToServer.isConnected();
            isRunning = true;
            ui.updateChat(" Connected to server as " + name);
            new Thread(new ConsoleListener(this, true)).start();
            new Thread(new ServerHandler(this)).start();
        } catch (Exception e) {
            throw new RuntimeException("Error connecting to server", e);
        }
    }

    public void acceptClient(){
        isRunning = true;
        while (isRunning) {
            try {
                socketToClient = new ServerSocket(Integer.parseInt(port));
                while (isRunning) {
                    System.out.println("Waiting for client...");
                    Socket clientSocket = socketToClient.accept();
                    System.out.println("Client connected");
                    new Thread(new PeerHandler(clientSocket, this)).start();
                }
            } catch (Exception e) {
                System.err.println("Error accepting client, changing port");
                port = String.valueOf(Integer.parseInt(port) + 1);
            }
        }
    }

    public void run() {
        ui = new ClientUI(this);
        new Thread(ui).start();
        clients = new Hashtable<>();
        clientSockets = new Hashtable<>();
        stuckMessages = new Hashtable<>();
        acceptClient();
    }

    public void connectPeer(String name) {
        String ipAndPort = clients.get(name);
        String[] ipAndPortArray = ipAndPort.split(":");
        String ip = ipAndPortArray[0];
        String port = ipAndPortArray[1];

        if (ip == null) {
            System.out.println("Client not found locally, getting from server...");
            getUserAddress(name);
            return;
        }
        try {
            System.out.println("Trying to connect to " + ip + ":" + port);
            String realIp = ip.replace("/", "");
            Socket socket = new Socket(realIp, Integer.parseInt(port));
            clientSockets.put(name, socket);
            PeerHandler peerHandler = new PeerHandler(socket, this);
            new Thread(peerHandler).start();
            sendPendingMessage(name);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(String message) {
        try {
            out.write(message.getBytes());
            System.out.println("Message sent: " + message);
        } catch (Exception e) {
            throw new RuntimeException("Error sending message", e);
        }
    }

    public void disconnect() {
        send("STOP");
        isRunning = false;
        try {
            socketToServer.close();
            ui.isConnected(false);
        } catch (IOException e) {
            System.out.println();
        }

        isConnectionEstablished = false;
    }

    public void refreshUser() {
        send("LIST");
    }

    public void getUserAddress(String name) {
        send("GET_" + name);
    }

    public void sendChatMessage(String recipient, String message) {
        // check if recipient exist
        if (clients.get(recipient) == null) {
            System.out.println("Recipient not found");
            getUserAddress(recipient);
            pendingMessage(recipient, message);
            System.out.println("Message to " + recipient + " is pending");
            return;
        }

        if (message.equals("")) {
            System.out.println("Message is empty");
            return;
        }

        // check if recipient is connected
        Socket socketToPeer = clientSockets.get(recipient);
        while (socketToPeer == null) {
            System.out.println("Recipient not connected, trying to connect...");
            connectPeer(recipient);
            socketToPeer = clientSockets.get(recipient);
        }

        // send message
        try {
            OutputStream out = socketToPeer.getOutputStream();
            out.write(("CHAT_" + name + "_" + message).getBytes());
            ui.updateChat(" You: " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendBroadcast(String message) {
        send("BROADCAST_" + message);
    }

    // kick user
    public void kickUser(String name) {
        send("KICK_" + name);
    }

    // view stat
    public void viewStat(String name) {
        send("STAT_" + name);
    }

    public void sendPort() {
        send("PORT_" + port);
    }

    public void pendingMessage(String recipient, String message) {
        ArrayList<String> messages = stuckMessages.get(recipient);
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
        stuckMessages.put(recipient, messages);
    }

    public void destroyPendingMessage(String recipient) {
        stuckMessages.remove(recipient);
        ui.updateChat(" User " + recipient + " not found, message discarded");
    }

    public void sendPendingMessage(String recipient) {
        ArrayList<String> messages = stuckMessages.get(recipient);
        if (messages == null) {
            return;
        }
        for (String message : messages) {
            sendChatMessage(recipient, message);
        }
        stuckMessages.remove(recipient);
    }


    public static void main(String[] args) {
        Client client = new Client();
        new Thread(client).start();
    }
}
