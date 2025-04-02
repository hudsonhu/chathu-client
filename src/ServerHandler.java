import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;

public class ServerHandler implements Runnable {
    Client client;
    InputStream in;
    OutputStream out;


    @Override
    public void run() {
        client.send("INIT_" + client.name + "_" + client.port);
        startListening();
    }

    ServerHandler(Client client) {
        this.client = client;
        this.in = client.in;
        this.out = client.out;
    }


    public String receive() {
        try {
            byte[] buffer = new byte[1024];
            int read = in.read(buffer);
            if (read == -1) {
                client.disconnect();
                throw new RuntimeException("Error receiving message");
            }
            return new String(buffer, 0, read);
        } catch (IOException e) {
            client.disconnect();
            throw new RuntimeException("Error receiving message", e);
        }
    }

    private void startListening() {
        System.out.println("Connected to " + client.socketToServer.getInetAddress().getHostAddress() +
                ":" + client.socketToServer.getPort());
        client.ui.isConnected(client.isConnectionEstablished);
        while (client.isConnectionEstablished) {
            String message = receive();
            System.out.println("Message received: " + message);
            // TODO: Handle message
//            client.ui.updateChat(" DEBUG " + message);
            try {
                resolveMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Disconnected from " + client.socketToServer.getInetAddress().getHostAddress() +
                ":" + client.socketToServer.getPort());
        client.ui.isConnected(false);
        client.disconnect();
    }


    /**
     * Resolve the message received from the server
     * @param message
     */
    private void resolveMessage(String message) throws IOException {
        if (message.startsWith("LIST_CLIENTS")) {
            // Format: LIST_CLIENTS userId_ip:port userId_ip:port ...
            String[] list = message.split(" ");
            Hashtable<String, String> list_clients = new Hashtable<>();
            for (int i = 1; i < list.length; i++) {
                String[] clientInfo = list[i].split("_");
                String userId = clientInfo[0];
                String[] ipAndPort = clientInfo[1].split(":");
                String ip = ipAndPort[0];
                String port = ipAndPort[1];
                // save client info
                list_clients.put(userId, ip + ":" + port);
            }
            client.clients = list_clients;
            client.ui.updateUsers();
        } else if (message.startsWith("KICKED by")) {
            String[] split = message.split(" ");
            String kicker = split[2];
            client.ui.updateChat(" [Server] You have been KICKED by " + kicker);
            client.isConnectionEstablished = false;
        } else if (message.startsWith("STAT")) {
            String[] split = message.split("\n");
            String user = split[0].split("_")[1];
            client.ui.updateChat(" [STAT] " + user + " has executed following commands: ");
            for (int i = 1; i < split.length; i++) {
                client.ui.updateChat(" [STAT] " + split[i]);
            }

        } else if (message.startsWith("GET")) {
            String[] split = message.split("_");
            if (split[2].equals("NOTFOUND")) {
                client.destroyPendingMessage(split[1]);
                return;
            }
            client.clients.put(split[1], split[2]);
            client.connectPeer(split[1]);
            client.ui.updateChat(" [Server] " + split[1] + " address is " + split[2]);
        } else {
            client.ui.updateChat(" [Server] " + message);
        }
    }
}
