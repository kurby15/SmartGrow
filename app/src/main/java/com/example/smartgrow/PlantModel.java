package com.example.smartgrow;

public class PlantModel {
    private String name;
    private String species;
    private String datePlanted;
    private String healthStatus;

    // Constructor para sa madaling pag-add ng data
    public PlantModel(String name, String species, String datePlanted, String healthStatus) {
        this.name = name;
        this.species = species;
        this.datePlanted = datePlanted;
        this.healthStatus = healthStatus;
    }

    // Getters (Kailangan ng Adapter para mahila ang text)
    public String getName() { return name; }
    public String getSpecies() { return species; }
    public String getDatePlanted() { return datePlanted; }
    public String getHealthStatus() { return healthStatus; }
}