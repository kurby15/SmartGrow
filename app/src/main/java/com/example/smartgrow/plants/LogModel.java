package com.example.smartgrow.plants;

public class LogModel {
    private String logId;
    private String date;
    private boolean watered;
    private boolean fertilized;
    private String sunlightExposure;
    private String healthStatus;
    private String notes;

    public LogModel() {
        // Required for Firebase
    }

    public LogModel(String logId, String date, boolean watered, boolean fertilized, String sunlightExposure, String healthStatus, String notes) {
        this.logId = logId;
        this.date = date;
        this.watered = watered;
        this.fertilized = fertilized;
        this.sunlightExposure = sunlightExposure;
        this.healthStatus = healthStatus;
        this.notes = notes;
    }

    // Getters and Setters
    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public boolean isWatered() { return watered; }
    public void setWatered(boolean watered) { this.watered = watered; }

    public boolean isFertilized() { return fertilized; }
    public void setFertilized(boolean fertilized) { this.fertilized = fertilized; }

    public String getSunlightExposure() { return sunlightExposure; }
    public void setSunlightExposure(String sunlightExposure) { this.sunlightExposure = sunlightExposure; }

    public String getHealthStatus() { return healthStatus; }
    public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}