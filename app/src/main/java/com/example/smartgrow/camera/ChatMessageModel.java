package com.example.smartgrow.camera;

import android.graphics.Bitmap;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;

public class ChatMessageModel {
    public static final int TYPE_AI = 1;
    public static final int TYPE_USER = 2;
    public static final int TYPE_LOADING = 3;

    private String messageText;
    private String messageTime;
    private int messageType;
    private boolean isUser;

    @Exclude
    private Bitmap imageBitmap;

    private String imageBase64; // Added for Firestore persistence
    private ArrayList<String> followUpSuggestions = new ArrayList<>();

    // Required empty constructor for Firebase Firestore deserialization
    public ChatMessageModel() {}

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

    // Standard Getters
    public String getMessageText() { return messageText; }
    public String getMessageTime() { return messageTime; }

    // Convenience Alias Getters
    public String getText() { return messageText; }
    public String getTime() { return messageTime; }

    public int getMessageType() { return messageType; }

    @Exclude
    public boolean isUser() { return isUser; }

    // EXCLUDE Bitmap from Firestore auto-serialization (handled via Base64)
    @Exclude
    public Bitmap getImageBitmap() { return imageBitmap; }

    public String getImageBase64() { return imageBase64; }

    // Setters for Firestore
    public void setMessageText(String messageText) { this.messageText = messageText; }
    public void setMessageTime(String messageTime) { this.messageTime = messageTime; }
    public void setMessageType(int messageType) {
        this.messageType = messageType;
        this.isUser = (messageType == TYPE_USER);
    }

    @Exclude
    public void setImageBitmap(Bitmap imageBitmap) { this.imageBitmap = imageBitmap; }

    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    public ArrayList<String> getFollowUpSuggestions() {
        return followUpSuggestions != null ? followUpSuggestions : new ArrayList<>();
    }

    public void setFollowUpSuggestions(ArrayList<String> followUpSuggestions) {
        this.followUpSuggestions = followUpSuggestions;
    }

    @Exclude
    public boolean hasImage() {
        return imageBitmap != null || (imageBase64 != null && !imageBase64.isEmpty());
    }
}