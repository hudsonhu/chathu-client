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


    public LocalDBManager getDb() {
        return dbManager;
    }

    public java.util.List<Message> getHistoryWith(String peer) {
        try {                                  // ownerUser always current user
            return dbManager.getChatHistory(name, peer);
        } catch (SQLException e) {
            e.printStackTrace();
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
            System.err.println("Error loading database");
            e.printStackTrace();
        }
        scheduler.scheduleAtFixedRate(() -> {
            if (isConnectionEstablished) refreshUser();
        }, 0, 3, java.util.concurrent.TimeUnit.SECONDS);
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
            if (dbManager != null) {
                dbManager.close();
            }
            ui.isConnected(false);
            scheduler.shutdownNow();
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
                ex.printStackTrace();
            }


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

        String finalServer = server;
        int finalPort = port;
        java.awt.EventQueue.invokeLater(() ->
                c.ui.autoConnect(finalServer, finalPort, c.name));
    }

}
