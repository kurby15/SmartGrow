package com.example.smartgrow;

public class ChatMessageModel {
    // Mga constants na hinahanap ng MainActivity mo para hindi siya mag-error
    public static final int TYPE_AI = 1;
    public static final int TYPE_USER = 2;
    public static final int TYPE_LOADING = 3;

    private String messageText;
    private String messageTime;
    private boolean isUser;
    private int messageType;

    // CONSTRUCTOR 1: Para sa HomeFragment setup natin (isUser)
    public ChatMessageModel(String messageText, String messageTime, boolean isUser) {
        this.messageText = messageText;
        this.messageTime = messageTime;
        this.isUser = isUser;
        this.messageType = isUser ? TYPE_USER : TYPE_AI;
    }

    // CONSTRUCTOR 2: Para naman sa MainActivity mo (messageType)
    public ChatMessageModel(String messageText, String messageTime, int messageType) {
        this.messageText = messageText;
        this.messageTime = messageTime;
        this.messageType = messageType;
        this.isUser = (messageType == TYPE_USER);
    }

    public String getMessageText() { return messageText; }
    public String getMessageTime() { return messageTime; }
    public boolean isUser() { return isUser; }
    public int getMessageType() { return messageType; }
}