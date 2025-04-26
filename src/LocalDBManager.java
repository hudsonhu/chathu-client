import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LocalDBManager {
    private Connection conn;   // JDBC 连接对象

    public LocalDBManager(String dbPath) throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC not found", e);
        }

        String url = "jdbc:sqlite:" + dbPath;  // e.g., "jdbc:sqlite:chat_hu.db"
        this.conn = DriverManager.getConnection(url);
        initTables();
    }

    // create message table if not exist
    private void initTables() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS messages ("
                + "  id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "  owner_user TEXT NOT NULL,"
                + "  sender TEXT NOT NULL,"
                + "  receiver TEXT NOT NULL,"
                + "  content TEXT,"
                + "  timestamp INTEGER,"
                + "  is_outgoing INTEGER,"
                + "  is_read INTEGER DEFAULT 0"
                + ");";
        try(Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }


    public void insertMessage(Message msg) throws SQLException {
        String sql = "INSERT INTO messages(owner_user, sender, receiver, content, timestamp, is_outgoing) "
                + "VALUES(?,?,?,?,?,?)";
        try(PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, msg.getOwnerUser());
            ps.setString(2, msg.getSender());
            ps.setString(3, msg.getReceiver());
            ps.setString(4, msg.getContent());
            ps.setLong(5, msg.getTimestamp());
            ps.setInt(6, msg.isOutgoing() ? 1 : 0);
            ps.executeUpdate();
        }
    }

    // 查询与某个对话的历史记录(按时间顺序)
    public List<Message> getChatHistory(String ownerUser, String peer) throws SQLException {
        String sql = "SELECT id, sender, receiver, content, timestamp, is_outgoing "
                + " FROM messages "
                + " WHERE owner_user=? "
                + "   AND (sender=? OR receiver=?) "
                + " ORDER BY timestamp ASC";
        List<Message> list = new ArrayList<>();
        try(PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ownerUser);
            ps.setString(2, peer);
            ps.setString(3, peer);
            try(ResultSet rs = ps.executeQuery()) {
                while(rs.next()) {
                    Message m = new Message();
                    m.setId(rs.getLong("id"));
                    m.setOwnerUser(ownerUser);
                    m.setSender(rs.getString("sender"));
                    m.setReceiver(rs.getString("receiver"));
                    m.setContent(rs.getString("content"));
                    m.setTimestamp(rs.getLong("timestamp"));
                    m.setOutgoing(rs.getInt("is_outgoing")==1);
                    list.add(m);
                }
            }
        }
        return list;
    }

    public void close() {
        if(conn != null){
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
