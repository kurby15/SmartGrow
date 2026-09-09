package com.example.smartgrow.plants;

public class CareTaskModel {
    private String title;
    private String dueText;
    private String actionText;
    private int imageResId;

    public CareTaskModel(String title, String dueText, String actionText, int imageResId) {
        this.title = title;
        this.dueText = dueText;
        this.actionText = actionText;
        this.imageResId = imageResId;
    }

    public String getTitle() {
        return title;
    }

    public String getDueText() {
        return dueText;
    }

    public String getActionText() {
        return actionText;
    }

    public int getImageResId() {
        return imageResId;
    }
}