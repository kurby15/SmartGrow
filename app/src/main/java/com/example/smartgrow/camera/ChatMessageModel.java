package com.example.smartgrow.camera;

import android.graphics.Bitmap;
import java.util.ArrayList;

public class ChatMessageModel {
    public static final int TYPE_AI = 1;
    public static final int TYPE_USER = 2;
    public static final int TYPE_LOADING = 3;

    private final String messageText;
    private final String messageTime;
    private final int messageType;
    private final boolean isUser;
    private Bitmap imageBitmap;

    private ArrayList<String> followUpSuggestions = new ArrayList<>();

    public ChatMessageModel(String messageText, String messageTime, int messageType) {
        this.messageText = messageText != null ? messageText : "";
        this.messageTime = messageTime;
        this.messageType = messageType;
        this.isUser = (messageType == TYPE_USER);
        this.imageBitmap = null;
    }

    public ChatMessageModel(Bitmap imageBitmap, String messageTime, int messageType) {
        this.messageText = "";
        this.imageBitmap = imageBitmap;
        this.messageTime = messageTime;
        this.messageType = messageType;
        this.isUser = (messageType == TYPE_USER);
    }

    public ChatMessageModel(String messageText, Bitmap imageBitmap, String messageTime, int messageType) {
        this.messageText = messageText != null ? messageText : "";
        this.imageBitmap = imageBitmap;
        this.messageTime = messageTime;
        this.messageType = messageType;
        this.isUser = (messageType == TYPE_USER);
    }

    @Deprecated
    public ChatMessageModel(String messageText, String messageTime, boolean isUser) {
        this(messageText, messageTime, isUser ? TYPE_USER : TYPE_AI);
    }

    public String getMessageText() { return messageText; }
    public String getMessageTime() { return messageTime; }
    public int getMessageType() { return messageType; }
    public boolean isUser() { return isUser; }
    public Bitmap getImageBitmap() { return imageBitmap; }
    public void setImageBitmap(Bitmap imageBitmap) { this.imageBitmap = imageBitmap; }

    public ArrayList<String> getFollowUpSuggestions() {
        return followUpSuggestions != null ? followUpSuggestions : new ArrayList<>();
    }

    public void setFollowUpSuggestions(ArrayList<String> followUpSuggestions) {
        this.followUpSuggestions = followUpSuggestions;
    }

    public boolean hasImage() { return imageBitmap != null; }
}
