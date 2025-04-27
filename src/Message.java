public class Message {
    private long id;
    private String ownerUser;  // owner of the message
    private String sender;
    private String receiver;
    private String content;
    private long timestamp;
    private boolean outgoing;  // true=out false=in

    public Message() {}

    public Message(String ownerUser, String sender, String receiver,
                   String content, long timestamp, boolean outgoing) {
        this.ownerUser = ownerUser;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.timestamp = timestamp;
        this.outgoing = outgoing;
    }

    // Getters & Setters ...
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }

    public String getOwnerUser() {
        return ownerUser;
    }
    public void setOwnerUser(String ownerUser) {
        this.ownerUser = ownerUser;
    }

    public String getSender() {
        return sender;
    }
    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }
    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isOutgoing() {
        return outgoing;
    }
    public void setOutgoing(boolean outgoing) {
        this.outgoing = outgoing;
    }
}
