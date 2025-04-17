import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Map;

public class NewUi_edit extends JFrame implements Runnable {
    private final Client client;

    private JTextArea messageBoard;
    private JTextField inputMessage,inputRecipient;
    private JButton buttonSend,buttonBroadcast,buttonRefreshUsers,buttonLogout,buttonConnect;
    private DefaultListModel<String> userListModel;
    private JList<String> userList;
    private JLabel labelConnectStatus, labelUserName, labelServerAddress, labelServerPort,userListTitle;
    private JTextField inputUserName, inputServerIp, inputServerPort;


    NewUi_edit(Client client) {
        this.client = client;
    }

    public void run(){
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("ChatHu Client");
        setSize(630, 450);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        this.add(initTopPanel(), BorderLayout.NORTH);
        this.add(initCenterPanel(), BorderLayout.CENTER);
        this.add(initBottomPanel(), BorderLayout.SOUTH);

        initButtons();
        setVisible(true);

    }
    private JPanel initTopPanel() {

        userListTitle = new JLabel(" Online Users (0)");
        userListTitle.setForeground(new Color(0, 128, 0));

        buttonBroadcast = new JButton("Broadcast");
        buttonRefreshUsers = new JButton("Refresh");

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
//        leftPanel.setOpaque(false);
        leftPanel.add(userListTitle);
        leftPanel.add(buttonRefreshUsers);

        topPanel.add(leftPanel, BorderLayout.WEST);
//        topPanel.add(userListTitle, BorderLayout.WEST);
        topPanel.add(buttonBroadcast,BorderLayout.EAST);
        return topPanel;
    }

    /**
     * 初始化中间面板：用户列表 + 聊天区域
     */
    private JPanel initCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout());
        JPanel spacer = buildSpacerPanel();
        JScrollPane userListScroll = buildUserListPanel();
        JPanel messageArea = buildMessageArea(); // 包含 messageBoard + inputPanel

        // 横向组合：用户列表 + 聊天区域+ spacer
        JPanel chatBodyPanel = new JPanel(new BorderLayout());
        chatBodyPanel.add(userListScroll, BorderLayout.WEST);
        chatBodyPanel.add(spacer, BorderLayout.CENTER);
        chatBodyPanel.add(messageArea, BorderLayout.EAST);

//        centerPanel.add(chatBodyPanel, BorderLayout.CENTER);
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12)); // 左右各 20px
        wrapperPanel.add(chatBodyPanel, BorderLayout.CENTER);

        centerPanel.add(wrapperPanel, BorderLayout.CENTER);
        return centerPanel;
    }

    /**
     * 构建左侧用户列表
     */
    private JScrollPane buildUserListPanel() {
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane scrollPane = new JScrollPane(userList);
        scrollPane.setPreferredSize(new Dimension(250, 0));
        return scrollPane;
    }

    /**
     * 构建聊天输入区域（底部输入框 + 发送按钮）
     */
    private JPanel buildInputPanel() {
        inputRecipient = new JTextField(4);
        inputMessage = new JTextField(10);
        buttonSend = new JButton("Send");

//        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
//        inputPanel.add(new JLabel("To:"));
//        inputPanel.add(inputRecipient);
//        inputPanel.add(inputMessage);
//        inputPanel.add(buttonSend);
//        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        JLabel labelTo = new JLabel("To:");
        labelTo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        labelTo.setForeground(new Color(30, 30, 30)); // 微灰

        inputRecipient.setPreferredSize(new Dimension(100, 28));
        inputMessage.setPreferredSize(new Dimension(250, 28));
        buttonSend.setPreferredSize(new Dimension(80, 28));

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
//        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // 上下左右内边距
        inputPanel.add(labelTo);
        inputPanel.add(inputRecipient);
        inputPanel.add(inputMessage);
        inputPanel.add(buttonSend);


        return inputPanel;
    }
    /**
     * spacer
     *
     */
    private JPanel buildSpacerPanel() {
        JPanel spacer = new JPanel();
        spacer.setPreferredSize(new Dimension(7, 0));  // 设置宽度为 5 像素
        spacer.setOpaque(false);
        return spacer;
    }
    /**
     * 构建聊天记录展示区（文本区域）
     */

    private JScrollPane buildMessageBoardPanel() {
        messageBoard = new JTextArea();
        messageBoard.setEditable(false);
        messageBoard.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(messageBoard);
        scrollPane.setPreferredSize(new Dimension(300, 0));
        return scrollPane;
    }

    /**
     * 构建右侧整体聊天区（记录区 + 输入区）
     */
    private JPanel buildMessageArea() {
        JScrollPane messageScroll = buildMessageBoardPanel();
        JPanel inputPanel = buildInputPanel();

        JPanel messageArea = new JPanel(new BorderLayout());
        messageArea.add(messageScroll, BorderLayout.CENTER);
        messageArea.add(inputPanel, BorderLayout.SOUTH);

        return messageArea;
    }

    /**
     * 初始化底部面板
     */

    private JPanel initBottomPanel() {
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        labelConnectStatus = new JLabel("  Not connected");
        labelConnectStatus.setForeground(Color.RED);

        labelUserName = new JLabel("  Username");
        inputUserName = new JTextField(6);
        inputUserName.setText("User" + (System.nanoTime() % 100000));

        labelServerAddress = new JLabel("  Server");
        inputServerIp = new JTextField(6);
        labelServerPort = new JLabel(":");
        inputServerPort = new JTextField(5);
        buttonConnect = new JButton("Connect");

        inputServerIp.setText("127.0.0.1");
        inputServerPort.setText("2006");

//        buttonLogout = new JButton("Logout");

        bottomPanel.add(labelUserName);
        bottomPanel.add(inputUserName);
        bottomPanel.add(labelConnectStatus);
        bottomPanel.add(labelServerAddress);
        bottomPanel.add(inputServerIp);
        bottomPanel.add(labelServerPort);
        bottomPanel.add(inputServerPort);
        bottomPanel.add(buttonConnect);
        bottomPanel.add(Box.createHorizontalStrut(20));
//        bottomPanel.add(buttonLogout);

        return bottomPanel;
    }
    private class BroadcastDialog extends JDialog {
        JTextField inputField;
        JButton sendButton;

        public BroadcastDialog(JFrame parent) {
            super(parent, "Broadcast Message", true);
            setSize(400, 100);
            setLayout(new BorderLayout());

            inputField = new JTextField();
            sendButton = new JButton("Send");

            JPanel panel = new JPanel(new BorderLayout(5, 5));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            panel.add(inputField, BorderLayout.CENTER);
            panel.add(sendButton, BorderLayout.EAST);
            add(panel, BorderLayout.CENTER);

            // 绑定发送事件
            sendButton.addActionListener(e -> {
                String message = inputField.getText();
                if (!message.trim().isEmpty() && client.isConnectionEstablished) {
                    client.sendBroadcast(message);
                    updateChat(" [You Broadcast] " + message);
                    dispose();
                }
            });

            setLocationRelativeTo(parent); // 居中弹出
            setVisible(true);
        }
    }
    private void initButtons() {
        buttonConnect.addActionListener(e -> {
            if (!client.isConnectionEstablished) {
                client.connectServer(inputServerIp.getText(), Integer.parseInt(inputServerPort.getText()), inputUserName.getText());
                buttonConnect.setText("Disconnect");
                client.refreshUser();
            } else {
                client.disconnect();
                isConnected(false);
                client.clients.clear();
                updateUsers();
                buttonConnect.setText("Connect");
            }
        });

        buttonSend.addActionListener(e -> {
            String recipient = inputRecipient.getText();
            String message = inputMessage.getText();
            if (!recipient.trim().isEmpty() && !message.trim().isEmpty() && client.isConnectionEstablished) {
                client.sendChatMessage(recipient, message);
                inputMessage.setText("");  // 清空输入框
            }
//            if (client.isConnectionEstablished) client.sendChatMessage(recipient, message);
        });

        buttonBroadcast.addActionListener(e -> {
            if (client.isConnectionEstablished) {
                new BroadcastDialog(this);  // 弹出窗口
            }
        });

        buttonRefreshUsers.addActionListener(e -> {
            if (client.isConnectionEstablished) {
                client.refreshUser();  // 向服务器请求最新用户列表
            }
        });
        // 如果你之后添加 refresh、logout 按钮，可以在此加监听器
    }

    public void updateChat(String message) {
        messageBoard.append(message + "\n");
        messageBoard.setCaretPosition(messageBoard.getDocument().getLength());
    }

    public void updateUsers() {
        System.out.println("Updating user list");
        userListModel.clear();
        for (Map.Entry<String, String> entry : client.clients.entrySet()) {
            userListModel.addElement(entry.getKey() + " - " + entry.getValue());
        }
        userListTitle.setText(" Online Users (" + client.clients.size() + ")");
    }

    public void isConnected(boolean connected) {
        System.out.println("Connected: " + connected);
        if (connected) {
            setTitle("ChatHu Client - Connected");
            labelConnectStatus.setText("      Connected       ");
            labelConnectStatus.setForeground(Color.GREEN);
            buttonConnect.setText("Disconnect");
        } else {
            setTitle("ChatHu Client - Not connected");
            labelConnectStatus.setText("      Not connected      ");
            labelConnectStatus.setForeground(Color.RED);
            buttonConnect.setText("Connect");
        }
    }

//    public static void main(String[] args) {
//        Client client = new Client();
//        SwingUtilities.invokeLater(new NewUi_edit(client));
//    }
}

