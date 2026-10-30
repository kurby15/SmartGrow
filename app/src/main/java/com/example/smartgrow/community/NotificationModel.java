package com.example.smartgrow.community;

public class NotificationModel {
    private String notificationId;
    private String type; // "like" or "comment"
    private String senderId;
    private String senderName;
    private String receiverId;
    private String postId;
    private String content; // Optional: comment text or post preview
    private long timestamp;
    private boolean read;

    public NotificationModel() {}

    public NotificationModel(String notificationId, String type, String senderId, String senderName, String receiverId, String postId, String content, long timestamp) {
        this.notificationId = notificationId;
        this.type = type;
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.postId = postId;
        this.content = content;
        this.timestamp = timestamp;
        this.read = false;
    }

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
