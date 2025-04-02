import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class ClientUI extends JFrame implements Runnable {
    private final Client client;


    JPanel panelBottom; // the panel is not visible in output
    JLabel labelConnectStatus, labelUserName, labelServerAddress, labelServerPort,
            labelChatRecipient, labelChatMessage, labelBroadcastMessage;
    JTextField inputUserName, inputServerIp, inputServerPort, inputChatMessage, inputChatRecipient,
            inputBroadcastMessage, inputUserOperation;
    JButton buttonConnect, buttonSendBroadcast, buttonSendChatMessage, buttonUserRefresh,
            buttonUserKick, buttonUserStat;
    JPanel userPanel, chatPanel, broadcastPanel;
    JTextArea messageBoard;
    JTable table;

    ClientUI(Client client) {
        this.client = client;
    }

    public void run() {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(400, 500);
        this.setTitle("ChatHu Client - Not connected");
        setDefaultLookAndFeelDecorated(true);

        //Creating the MenuBar and adding components
        JTabbedPane tabbedPane = new JTabbedPane();

        // Init 3 panels
        chatPanel = initChatPanel();
        userPanel = initUserPanel();
        broadcastPanel = initBroadcastPanel();

        // set background color for tabbed pane
        tabbedPane.setBackground(Color.DARK_GRAY);
        tabbedPane.addTab("Chat", chatPanel);
        tabbedPane.addTab("Users", userPanel);
        tabbedPane.addTab("Broadcast", broadcastPanel);
        this.add(tabbedPane);

        //Creating the panel at bottom and adding components
        panelBottom = initBottomPanel();


        // Text Area at the Center
        messageBoard = new JTextArea(30,30);
        messageBoard.setBackground(Color.WHITE);
        messageBoard.setLineWrap(true);
        messageBoard.setEditable(false);

        // make the text area scrollable
        JScrollPane scroll = new JScrollPane (messageBoard);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        initButtons();

        // Finish setting up the frame
        this.getContentPane().add(BorderLayout.EAST, tabbedPane);
        this.getContentPane().add(BorderLayout.SOUTH, panelBottom);
        this.getContentPane().add(BorderLayout.WEST, messageBoard);
        this.pack();
        this.setVisible(true);
    }

    /**
     * Init the User page panel
     * @return the generated panel
     */
    private JPanel initUserPanel() {
        JPanel userPanel = new JPanel();
        userPanel.setLayout(new BorderLayout());
        String[] columnNames = {"Name", "IP"};
        Object[][] data = new Object[client.clients.size()][2];
        int i = 0;
        for (Map.Entry<String, String> entry : client.clients.entrySet()) {
            data[i][0] = entry.getKey();
            data[i][1] = entry.getValue();
            i++;
        }
        table = new JTable(data, columnNames);

        // input user to be operated
        JPanel userControlPanel = new JPanel();
        inputUserOperation = new JTextField(10);
        buttonUserKick = new JButton("Kick");
        buttonUserStat = new JButton("View Stat");
        buttonUserRefresh = new JButton("Refresh");
        userControlPanel.add(inputUserOperation);
        userControlPanel.add(buttonUserKick);
        userControlPanel.add(buttonUserStat);
        userControlPanel.add(buttonUserRefresh);
        userPanel.add(table.getTableHeader(), BorderLayout.PAGE_START);
        userPanel.add(table, BorderLayout.CENTER);
        userPanel.add(userControlPanel, BorderLayout.PAGE_END);

        return userPanel;
    }

    /**
     * Init the Chat page panel
     * @return the generated panel
     */
    private JPanel initChatPanel() {
        JPanel chatPanel = new JPanel();
        chatPanel.setLayout(new BorderLayout());
        labelChatRecipient = new JLabel("To: ");
        inputChatRecipient = new JTextField(10);
        inputChatMessage = new JTextField(20);
        labelChatMessage = new JLabel("Message: ");
        JPanel panelChatInput = new JPanel();
        panelChatInput.add(labelChatRecipient);
        panelChatInput.add(inputChatRecipient);
        chatPanel.add(BorderLayout.NORTH, panelChatInput);
        // rest of the components are on the next line
        chatPanel.add(BorderLayout.CENTER, labelChatMessage);
        chatPanel.add(BorderLayout.CENTER, inputChatMessage);
        // add send button in chatPanel
        buttonSendChatMessage = new JButton("Send");
        chatPanel.add(BorderLayout.SOUTH, buttonSendChatMessage);

        return chatPanel;
    }

    /**
     * Init the Broadcast page panel
     * @return the generated panel
     */
    private JPanel initBroadcastPanel() {
        JPanel broadcastPanel = new JPanel();
        broadcastPanel.setLayout(new BorderLayout());
        labelBroadcastMessage = new JLabel("Announce");
        inputBroadcastMessage = new JTextField(20);
        broadcastPanel.add(BorderLayout.NORTH, labelBroadcastMessage);
        broadcastPanel.add(BorderLayout.CENTER, inputBroadcastMessage);
        // add send button in broadcastPanel
        buttonSendBroadcast = new JButton("Send");
        broadcastPanel.add(BorderLayout.SOUTH, buttonSendBroadcast);
        return broadcastPanel;
    }

    /**
     * Init the bottom panel
     * @return the bottom panel
     */
    private JPanel initBottomPanel() {
        JPanel bottomPanel = new JPanel();
        labelConnectStatus = new JLabel("      Not connected      ");
        labelUserName = new JLabel("Username");
        inputUserName = new JTextField(8);
        // get random username by last 5 digit of nanoseconds
        inputUserName.setText("User" + (System.nanoTime() % 100000));

        labelServerAddress = new JLabel("Server");
        inputServerIp = new JTextField(8);
        labelServerPort = new JLabel(":");
        inputServerPort = new JTextField(5);
        buttonConnect = new JButton("Connect");

        labelConnectStatus.setHorizontalAlignment(JLabel.LEFT);
        labelConnectStatus.setForeground(Color.RED);

        inputServerIp.setText("127.0.0.1");
        inputServerPort.setText("2006");

        bottomPanel.add(labelUserName);
        bottomPanel.add(inputUserName);
        bottomPanel.add(labelConnectStatus);
        bottomPanel.add(labelServerAddress);
        bottomPanel.add(inputServerIp);
        bottomPanel.add(labelServerPort);
        bottomPanel.add(inputServerPort);
        bottomPanel.add(buttonConnect);

        return bottomPanel;
    }

    /**
     * Init all the buttons
     * Set the action listener for each button
     */
    private void initButtons() {
        buttonConnect.addActionListener(e -> {
            if (!client.isConnectionEstablished) {
                System.out.println("Connecting to ip: " + inputServerIp.getText() + " port: " + inputServerPort.getText());
                client.connectServer(inputServerIp.getText(), Integer.parseInt(inputServerPort.getText()), inputUserName.getText());
                buttonConnect.setText("Disconnect");
            } else {
                client.disconnect();
                buttonConnect.setText("Connect");
            }
        });

        buttonSendBroadcast.addActionListener(e -> {
            String message = inputBroadcastMessage.getText();
            if (client.isConnectionEstablished) client.sendBroadcast(message);
        });

        buttonSendChatMessage.addActionListener(e -> {
            String recipient = inputChatRecipient.getText();
            String message = inputChatMessage.getText();
            if (client.isConnectionEstablished) client.sendChatMessage(recipient, message);
        });

        // kick button
        buttonUserKick.addActionListener(e -> {
            String username = inputUserOperation.getText();
            if (client.isConnectionEstablished) client.kickUser(username);
        });

        // view stat button
        buttonUserStat.addActionListener(e -> {
            String username = inputUserOperation.getText();
            if (client.isConnectionEstablished) client.viewStat(username);
        });

        buttonUserRefresh.addActionListener(e -> {
            if (client.isConnectionEstablished) client.refreshUser();
        });
    }


    /**
     * Update the user list
     * Repaint the table
     */
    public void updateUsers() {
        // create a table with some data
        String[] columnNames = {"Name", "IP"};
        Object[][] data = new Object[client.clients.size()][2];
        int i = 0;
        for (Map.Entry<String, String> entry : client.clients.entrySet()) {
            data[i][0] = entry.getKey();
            data[i][1] = entry.getValue();
            i++;
        }
        JTable new_table = new JTable(data, columnNames);
        new_table.setEnabled(false);

        System.out.println("Updating user list");

        // add the table to the second panel
        userPanel.remove(table);
        table = new_table;
        userPanel.add(new_table.getTableHeader(), BorderLayout.PAGE_START);
        userPanel.add(new_table, BorderLayout.CENTER);
        userPanel.revalidate();
        userPanel.repaint();

    }

    /**
     * Update the chat log
     * @param message the message to be added
     */
    public void updateChat(String message) {
        messageBoard.append(message + "\n");
        // scroll to end
        messageBoard.setCaretPosition(messageBoard.getDocument().getLength());

    }

    /**
     * Update the status label
     * @param connected the status to be updated
     */
    public void isConnected(boolean connected) {
        System.out.println("Connected: " + connected);
        if (connected) {
            this.setTitle("ChatHu Client - Connected");
            labelConnectStatus.setText("      Connected       ");
            labelConnectStatus.setForeground(Color.GREEN);
            buttonConnect.setText("Disconnect");
        } else {
            this.setTitle("ChatHu Client - Not connected");
            labelConnectStatus.setText("      Not connected      ");
            labelConnectStatus.setForeground(Color.RED);
            buttonConnect.setText("Connect");
        }
    }
}