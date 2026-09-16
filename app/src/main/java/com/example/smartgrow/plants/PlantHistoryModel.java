package com.example.smartgrow.plants;

public class PlantHistoryModel {
    private String date;
    private String action;

    public PlantHistoryModel(String date, String action) {
        this.date = date;
        this.action = action;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}