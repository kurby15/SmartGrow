package com.example.smartgrow;

public class ChatMessageModel {
    // Tatlong possible values para sa identifier uri
    public static final int TYPE_AI = 0;
    public static final int TYPE_USER = 1;
    public static final int TYPE_LOADING = 2;

    private String messageText;
    private String messageTime;
    private int messageType; // Humahawak kung AI, USER, o LOADING status

    public ChatMessageModel(String messageText, String messageTime, int messageType) {
        this.messageText = messageText;
        this.messageTime = messageTime;
        this.messageType = messageType;
    }

    public String getMessageText() { return messageText; }
    public String getMessageTime() { return messageTime; }
    public int getMessageType() { return messageType; }
}