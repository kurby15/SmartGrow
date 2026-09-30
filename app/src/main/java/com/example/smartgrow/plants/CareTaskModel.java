package com.example.smartgrow.plants;

import android.graphics.Bitmap;

public class CareTaskModel {
    private String id; // Plant ID
    private String title;
    private String dueText;
    private String actionText;
    private int imageResId;
    private boolean isDone;
    private String taskType; // "Water", "Check", "Fertilize"
    private Bitmap plantImageBitmap;


    public CareTaskModel(String id, String title, String dueText, String actionText, int imageResId, String taskType) {
        this.id = id;
        this.title = title;
        this.dueText = dueText;
        this.actionText = actionText;
        this.imageResId = imageResId;
        this.taskType = taskType;
        this.isDone = false;
        this.plantImageBitmap = null;
    }


    public CareTaskModel(String id, String title, String dueText, String actionText, Bitmap plantImageBitmap, String taskType) {
        this.id = id;
        this.title = title;
        this.dueText = dueText;
        this.actionText = actionText;
        this.plantImageBitmap = plantImageBitmap;
        this.taskType = taskType;
        this.isDone = false;
        this.imageResId = 0;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDueText() {
        return dueText;
    }

    public String getActionText() {
        return isDone ? "Done" : actionText;
    }

    public int getImageResId() {
        return imageResId;
    }

    public Bitmap getPlantImageBitmap() {
        return plantImageBitmap;
    }

    public void setPlantImageBitmap(Bitmap plantImageBitmap) {
        this.plantImageBitmap = plantImageBitmap;
    }

    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean done) {
        isDone = done;
    }

    public String getTaskType() {
        return taskType;
    }
}