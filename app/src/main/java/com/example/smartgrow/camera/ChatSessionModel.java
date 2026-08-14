package com.example.smartgrow.camera;

public class ChatSessionModel {
    private String sessionId;
    private String title;
    private long timestamp;

    public ChatSessionModel() {}

    public ChatSessionModel(String sessionId, String title, long timestamp) {
        this.sessionId = sessionId;
        this.title = title;
        this.timestamp = timestamp;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}