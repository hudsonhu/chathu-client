import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.KeyStore;

public class Client implements Runnable {
    InputStream in;
    OutputStream out;
    String port = "2009";
    String name;
    Socket socketToServer;
    ServerSocket socketToClient;
//    ClientUI ui;
    NewUi_edit ui;
    Map<String, String> clients;  // <userId, ip:port>
    Map<String, Socket> clientSockets;  // <userId, socket>
    Map<String, ArrayList<String>> stuckMessages;  // <userId, messages>
    boolean isConnectionEstablished = false, isRunning = false;

    private String autoSrvIp;
    private int    autoSrvPort;
    private String autoUser;

    private LocalDBManager dbManager;
    private String dbFileName = "chat_hu.db";  // db file

    private final java.util.concurrent.ScheduledExecutorService scheduler =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
    private static final java.util.logging.Logger LOG =
            java.util.logging.Logger.getLogger(Client.class.getName());



    public LocalDBManager getDb() {
        return dbManager;
    }

    public java.util.List<Message> getHistoryWith(String peer) {
        try {                                  // ownerUser always current user
            return dbManager.getChatHistory(name, peer);
        } catch (SQLException e) {
            LOG.severe("Error getting chat history");
            return java.util.Collections.emptyList();
        }
    }

    public void connectServer(String ip, int port, String name) {
        try {  // verification is skipped
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {return null;}
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType){}
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType){}
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            socketToServer = sslSocketFactory.createSocket(ip, port);

            in = socketToServer.getInputStream();
            out = socketToServer.getOutputStream();

            this.name = name;
            isConnectionEstablished = socketToServer.isConnected();
            isRunning = true;

            ui.updateChat("Connected to server via TLS as " + name);
            new Thread(new ConsoleListener(this, true)).start();
            new Thread(new ServerHandler(this)).start();
        } catch (Exception e) {
            throw new RuntimeException("Error connecting to SSL server", e);
            // retry
//            connectServer(ip, port, name);
        }
    }

    public void acceptClient(){
        isRunning = true;
        while (isRunning) {
            try {
                socketToClient = new ServerSocket(Integer.parseInt(port));
                while (isRunning) {
                    LOG.fine("Waiting for incoming P2P on port " + port);
                    Socket clientSocket = socketToClient.accept();
                    LOG.info("P2P peer connected from " + clientSocket.getInetAddress().getHostAddress());
                    new Thread(new PeerHandler(clientSocket, this)).start();
                }
            } catch (Exception e) {
                LOG.warning("Local port " + port + " busy, trying next available");
                port = String.valueOf(Integer.parseInt(port) + 1);
            }
        }
    }
    @Override
    public void run() {

        try {
            ui = new NewUi_edit(this);
            javax.swing.SwingUtilities.invokeAndWait(ui);   // block EDT until UI is ready
        } catch (Exception ex) {                            // (InvocationTargetException|InterruptedException)
            throw new RuntimeException("Failed to init UI", ex);
        }

        if (autoSrvIp != null) {
            String ip   = autoSrvIp;
            int    port = autoSrvPort;
            String user = autoUser;
            javax.swing.SwingUtilities.invokeLater(() ->
                    ui.autoConnect(ip, port, user));
        }

        clients        = new java.util.Hashtable<>();
        clientSockets  = new java.util.Hashtable<>();
        stuckMessages  = new java.util.Hashtable<>();

        try {
            dbManager = new LocalDBManager(dbFileName);
        } catch (Exception e) {
            LOG.severe("Error initializing database: " + e.getMessage());
        }
        scheduler.scheduleAtFixedRate(() -> {
            if (isConnectionEstablished) refreshUser();
        }, 0, 10, java.util.concurrent.TimeUnit.SECONDS);
        acceptClient();
    }

    public void connectPeer(String name) {
        String ipAndPort = clients.get(name);
        String[] ipAndPortArray = ipAndPort.split(":");
        String ip = ipAndPortArray[0];
        String port = ipAndPortArray[1];

        if (ip == null) {
            LOG.fine("Getting ip of peer " + name + " from server");
            getUserAddress(name);
            return;
        }
        try {
            LOG.fine("Dialing peer " + ip + ":" + port);
            String realIp = ip.replace("/", "");
            Socket socket = new Socket(realIp, Integer.parseInt(port));
            clientSockets.put(name, socket);
            PeerHandler peerHandler = new PeerHandler(socket, this);
            new Thread(peerHandler).start();
            sendPendingMessage(name);
        } catch (IOException e) {
            LOG.warning("IO error: " + e.getMessage());
        }
    }

    public void send(String message) {
        try {
            out.write(message.getBytes());
            LOG.fine("Sent to server: " + message);
        } catch (Exception e) {
            if (!message.equals("STOP")) {
                LOG.warning("Error sending message to server, message: " + message);
            }
        }
    }

    public void disconnect() {
        send("STOP");
        isRunning = false;
        try {
            socketToServer.close();
            if (dbManager != null) {
                dbManager.close();
            }
            ui.isConnected(false);
            LOG.info("Disconnected from server");
            scheduler.shutdownNow();
        } catch (IOException e) {
            LOG.warning("Error closing socket");
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
            LOG.warning("Recipient '" + recipient + "' unknown, asking server");
            getUserAddress(recipient);
            pendingMessage(recipient, message);
            return;
        }

        if (message.equals("")) {
            LOG.warning("Message is empty — ignored");
            return;
        }

        // check if recipient is connected
        Socket socketToPeer = clientSockets.get(recipient);
        while (socketToPeer == null) {
            LOG.fine("Peer not yet connected, retrying socket");
            connectPeer(recipient);
            socketToPeer = clientSockets.get(recipient);
        }

        // send message
        try {
            OutputStream out = socketToPeer.getOutputStream();
            out.write(("CHAT_" + name + "_" + message).getBytes());
//            ui.updateChat(" You: " + message);  // deprecated
            ui.appendMessage(recipient,                      // peer
                    true,                           // outgoing
                    System.currentTimeMillis(),
                    message);

            try {                                      // <–– 新增
                Message m = new Message(
                        name,              // ownerUser
                        name,              // sender
                        recipient,         // receiver
                        message,
                        System.currentTimeMillis(),
                        true               // outgoing
                );
                dbManager.insertMessage(m);
            } catch (SQLException ex) {
                LOG.severe("Error inserting message into database");
            }


        } catch (IOException e) {
            LOG.warning("Error sending message to peer " + recipient);
            LOG.warning("IO error: " + e.getMessage());
            ui.updateChat(" Error sending message to peer " + recipient);
            if (clientSockets.get(recipient) == null) {
                pendingMessage(recipient, message);
            } else {
                ui.updateChat(" Peer " + recipient + " not connected, message pending");
            }
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
        LogUtil.init();
        String user = null, server = "127.0.0.1";
        int    port = 2006;
        for (String a : args) {
            if (a.startsWith("--user="))   user   = a.substring(7);
            else if (a.startsWith("--srv="))  server = a.substring(6);
            else if (a.startsWith("--port=")) port   = Integer.parseInt(a.substring(7));
        }

        Client c = new Client();
        c.autoSrvIp   = server;     // --srv
        c.autoSrvPort = port;       // --port
        c.autoUser    = (user != null ? user :
                "User" + (System.nanoTime() % 100000));
        new Thread(c).start();
    }

}
