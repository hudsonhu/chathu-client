import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.SQLException;

public class PeerHandler implements Runnable {
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private Client client;

    private String userId;
    private String ip;
    private String port;
    boolean isConnectionAlive = false;

    /**
     * Handles the communication between two clients
     */
    public PeerHandler(Socket socket, Client client) {
        this.socket = socket;
        this.ip = socket.getInetAddress().getHostAddress();
        this.port = String.valueOf(socket.getPort());
        this.client = client;
        try {
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
            isConnectionAlive = true;
        } catch (Exception e) {
            throw new RuntimeException("Error connecting to server", e);
        }
    }

    public String receive() {
        try {
            byte[] buffer = new byte[1024];
            int read = in.read(buffer);
            if (read == -1) {
                isConnectionAlive = false;
                System.err.println("Error receiving message");
            }
            return new String(buffer, 0, read);
        } catch (Exception e) {
            isConnectionAlive = false;
            System.err.println("Error receiving message");
        }
        return null;
    }

    public void resolveMessage(String message) {
        if (message.startsWith("CHAT")) {
            String[] list = message.split("_");
            String sender = list[1];
            String text = list[2];
//            client.ui.updateChat(" " + sender + ": " + text);
            javax.swing.SwingUtilities.invokeLater(() ->
                    client.ui.appendMessage(sender,                     // peer
                            false,                      // incoming
                            System.currentTimeMillis(),
                            text)
            );

            try {
                Message m = new Message(
                        client.name,        // ownerUser
                        sender,             // sender
                        client.name,        // receiver: self
                        text,
                        System.currentTimeMillis(),
                        false               // incoming
                );
                client.getDb().insertMessage(m);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            if (!client.clients.containsKey(sender)) {
                client.getUserAddress(sender);
            }
        }
    }


    public void run() {
        while (isConnectionAlive) {
            String message = receive();
            System.out.println("Message received: " + message);
            if (message != null) {
                resolveMessage(message);
            }
        }
    }
}
