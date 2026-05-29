package com.example.smartgrow;

public class PlantModel {
    private String name;
    private String species;
    private String datePlanted;
    private String healthStatus;

    // 🌟 MGA BAGONG DATA FIELDS NA IDINAGDAG NATIN
    private String medicinalUse;     // Taga-hawak ng Auto-Generated API data (e.g., "Cough/Fever")
    private int healthPercentage;    // Approach B: AI Predicted Health Level Int Score (0-100)

    // 🏗️ 1. LUMANG CONSTRUCTOR (Para iwas error sa mga dati mo nang nagawang fragments/adapters)
    public PlantModel(String name, String species, String datePlanted, String healthStatus) {
        this.name = name;
        this.species = species;
        this.datePlanted = datePlanted;
        this.healthStatus = healthStatus;
        this.medicinalUse = "General Herb"; // Default value para sa mga lumang entry
        this.healthPercentage = 100;        // Default na perpekto ang health score
    }

    // 🏗️ 2. BAGONG CONSTRUCTOR (Overloaded - Para naman sa bagong premium Home Card implementation)
    public PlantModel(String name, String species, String datePlanted, String healthStatus, String medicinalUse, int healthPercentage) {
        this.name = name;
        this.species = species;
        this.datePlanted = datePlanted;
        this.healthStatus = healthStatus;
        this.medicinalUse = medicinalUse;
        this.healthPercentage = healthPercentage;
    }

    // 📝 MGA LUMANG GETTERS
    public String getName() { return name; }
    public String getSpecies() { return species; }
    public String getDatePlanted() { return datePlanted; }
    public String getHealthStatus() { return healthStatus; }

    // 🌟 MGA BAGONG GETTERS AT SETTERS PARA SA AUTOMATION
    public String getMedicinalUse() { return medicinalUse; }
    public void setMedicinalUse(String medicinalUse) { this.medicinalUse = medicinalUse; }

    public int getHealthPercentage() { return healthPercentage; }
    public void setHealthPercentage(int healthPercentage) { this.healthPercentage = healthPercentage; }
}