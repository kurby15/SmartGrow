package com.example.smartgrow.plants;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;

@IgnoreExtraProperties
public class MyGardenPlantModel {

    private String id;
    private String plantId;
    private String userId; // 🔴 Added userId field
    private String plantName;
    private String scientificName;
    private String healthStatus;
    private int healthPercentage;
    private String imageBase64;
    private String rawAnalysisJson;

    public MyGardenPlantModel() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @PropertyName("plantId")
    public String getPlantId() {
        return plantId;
    }

    @PropertyName("plantId")
    public void setPlantId(String plantId) {
        this.plantId = plantId;
    }

    // --- User ID ---
    @PropertyName("userId")
    public String getUserId() {
        return userId;
    }

    @PropertyName("userId")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @PropertyName("plantName")
    public String getPlantName() {
        return plantName;
    }

    @PropertyName("plantName")
    public void setPlantName(String plantName) {
        this.plantName = plantName;
    }

    @PropertyName("scientificName")
    public String getScientificName() {
        return scientificName;
    }

    @PropertyName("scientificName")
    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    @PropertyName("healthStatus")
    public String getHealthStatus() {
        return healthStatus;
    }

    @PropertyName("healthStatus")
    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    @PropertyName("healthPercentage")
    public int getHealthPercentage() {
        return healthPercentage;
    }

    @PropertyName("healthPercentage")
    public void setHealthPercentage(int healthPercentage) {
        this.healthPercentage = healthPercentage;
    }

    @PropertyName("imageBase64")
    public String getImageBase64() {
        return imageBase64;
    }

    @PropertyName("imageBase64")
    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    @PropertyName("rawAnalysisJson")
    public String getRawAnalysisJson() {
        return rawAnalysisJson;
    }

    @PropertyName("rawAnalysisJson")
    public void setRawAnalysisJson(String rawAnalysisJson) {
        this.rawAnalysisJson = rawAnalysisJson;
    }
}