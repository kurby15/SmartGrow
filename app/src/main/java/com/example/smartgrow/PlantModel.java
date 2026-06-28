package com.example.smartgrow;

import java.io.Serializable;

public class PlantModel implements Serializable {
    private String id;
    private String name;
    private String species;
    private String medicinalUse;
    private String datePlanted;
    private String healthStatus;
    private int healthPercentage;
    private String imageUrl;

    public PlantModel() {
        // Required for Firebase
    }

    // Constructor for old entries
    public PlantModel(String name, String species, String datePlanted, String healthStatus) {
        this.name = name;
        this.species = species;
        this.datePlanted = datePlanted;
        this.healthStatus = healthStatus;
        this.medicinalUse = "General Herb";
        this.healthPercentage = 100;
    }

    // Constructor for new entries
    public PlantModel(String name, String species, String datePlanted, String healthStatus, String medicinalUse, int healthPercentage) {
        this.name = name;
        this.species = species;
        this.datePlanted = datePlanted;
        this.healthStatus = healthStatus;
        this.medicinalUse = medicinalUse;
        this.healthPercentage = healthPercentage;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }

    public String getMedicinalUse() { return medicinalUse; }
    public void setMedicinalUse(String medicinalUse) { this.medicinalUse = medicinalUse; }

    public String getDatePlanted() { return datePlanted; }
    public void setDatePlanted(String datePlanted) { this.datePlanted = datePlanted; }

    public String getHealthStatus() { return healthStatus; }
    public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }

    public int getHealthPercentage() { return healthPercentage; }
    public void setHealthPercentage(int healthPercentage) { this.healthPercentage = healthPercentage; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}