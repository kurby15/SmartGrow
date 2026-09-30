package com.example.smartgrow.plants;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;
import java.util.List;
import java.util.Map;

@IgnoreExtraProperties
public class MyGardenPlantModel {

    private String id;
    private String plantId;
    private String plant_uid;
    private String userId;
    private String plantName;
    private String scientificName;
    private String healthStatus;
    private int healthPercentage;
    private String imageBase64;
    private String rawAnalysisJson;
    private boolean isArtificial;
    
    private Map<String, Object> reminders;
    
    // Tracking last care actions
    private String lastWateredDate;
    private String lastFertilizedDate;
    private String lastCheckedDate;

    // Additional fields for Diary info
    private String commonProblemsJson;
    private List<Map<String, Object>> common_problems_list;
    private List<Map<String, Object>> common_pests;
    private List<String> leafColors;
    private String petToxicity;
    private String weedPotential;
    private String distribution;
    private String habitat;
    private String plantType;
    private String lifespan;
    private String careDifficultyText;
    private String aliases;

    // Characteristics
    private String ultimateHeight;
    private String ultimateSpread;
    private String leafType;
    private String plantingTime;

    // Care Conditions
    private String temperatureRange;
    private String hardinessZones;
    private String sunlight;
    private String soil;

    // How-tos
    private String pruning;
    private String propagation;
    private String repotting;

    // Additional Dynamic Sections
    private String usesText;
    private String adaptationText;
    private String ecologicalText;
    private String historyText;
    private String nameStoryText;
    private String symbolismText;

    private List<Map<String, Object>> distribution_coordinates;

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

    @PropertyName("plant_uid")
    public String getPlantUid() {
        return plant_uid;
    }

    @PropertyName("plant_uid")
    public void setPlantUid(String plant_uid) {
        this.plant_uid = plant_uid;
    }

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

    @PropertyName("isArtificial")
    public boolean isArtificial() {
        return isArtificial;
    }

    @PropertyName("isArtificial")
    public void setArtificial(boolean artificial) {
        isArtificial = artificial;
    }

    public Map<String, Object> getReminders() {
        return reminders;
    }

    public void setReminders(Map<String, Object> reminders) {
        this.reminders = reminders;
    }

    public String getLastWateredDate() {
        return lastWateredDate;
    }

    public void setLastWateredDate(String lastWateredDate) {
        this.lastWateredDate = lastWateredDate;
    }

    public String getLastFertilizedDate() {
        return lastFertilizedDate;
    }

    public void setLastFertilizedDate(String lastFertilizedDate) {
        this.lastFertilizedDate = lastFertilizedDate;
    }

    public String getLastCheckedDate() {
        return lastCheckedDate;
    }

    public void setLastCheckedDate(String lastCheckedDate) {
        this.lastCheckedDate = lastCheckedDate;
    }

    @PropertyName("commonProblemsJson")
    public String getCommonProblemsJson() {
        return commonProblemsJson;
    }

    @PropertyName("commonProblemsJson")
    public void setCommonProblemsJson(String commonProblemsJson) {
        this.commonProblemsJson = commonProblemsJson;
    }

    @PropertyName("common_problems_list")
    public List<Map<String, Object>> getCommon_problems_list() {
        return common_problems_list;
    }

    @PropertyName("common_problems_list")
    public void setCommon_problems_list(List<Map<String, Object>> common_problems_list) {
        this.common_problems_list = common_problems_list;
    }

    @PropertyName("common_pests")
    public List<Map<String, Object>> getCommon_pests() {
        return common_pests;
    }

    @PropertyName("common_pests")
    public void setCommon_pests(List<Map<String, Object>> common_pests) {
        this.common_pests = common_pests;
    }

    @PropertyName("leafColors")
    public List<String> getLeafColors() {
        return leafColors;
    }

    @PropertyName("leafColors")
    public void setLeafColors(List<String> leafColors) {
        this.leafColors = leafColors;
    }

    public String getPetToxicity() {
        return petToxicity;
    }

    public void setPetToxicity(String petToxicity) {
        this.petToxicity = petToxicity;
    }

    public String getWeedPotential() {
        return weedPotential;
    }

    public void setWeedPotential(String weedPotential) {
        this.weedPotential = weedPotential;
    }

    public String getDistribution() {
        return distribution;
    }

    public void setDistribution(String distribution) {
        this.distribution = distribution;
    }

    public String getHabitat() {
        return habitat;
    }

    public void setHabitat(String habitat) {
        this.habitat = habitat;
    }

    public String getPlantType() {
        return plantType;
    }

    public void setPlantType(String plantType) {
        this.plantType = plantType;
    }

    public String getLifespan() {
        return lifespan;
    }

    public void setLifespan(String lifespan) {
        this.lifespan = lifespan;
    }

    public String getCareDifficultyText() {
        return careDifficultyText;
    }

    public void setCareDifficultyText(String careDifficultyText) {
        this.careDifficultyText = careDifficultyText;
    }

    public String getAliases() {
        return aliases;
    }

    public void setAliases(String aliases) {
        this.aliases = aliases;
    }

    public String getUltimateHeight() { return ultimateHeight; }
    public void setUltimateHeight(String ultimateHeight) { this.ultimateHeight = ultimateHeight; }

    public String getUltimateSpread() { return ultimateSpread; }
    public void setUltimateSpread(String ultimateSpread) { this.ultimateSpread = ultimateSpread; }

    public String getLeafType() { return leafType; }
    public void setLeafType(String leafType) { this.leafType = leafType; }

    public String getPlantingTime() { return plantingTime; }
    public void setPlantingTime(String plantingTime) { this.plantingTime = plantingTime; }

    public String getTemperatureRange() { return temperatureRange; }
    public void setTemperatureRange(String temperatureRange) { this.temperatureRange = temperatureRange; }

    public String getHardinessZones() { return hardinessZones; }
    public void setHardinessZones(String hardinessZones) { this.hardinessZones = hardinessZones; }

    public String getSunlight() { return sunlight; }
    public void setSunlight(String sunlight) { this.sunlight = sunlight; }

    public String getSoil() { return soil; }
    public void setSoil(String soil) { this.soil = soil; }

    public String getPruning() { return pruning; }
    public void setPruning(String pruning) { this.pruning = pruning; }

    public String getPropagation() { return propagation; }
    public void setPropagation(String propagation) { this.propagation = propagation; }

    public String getRepotting() { return repotting; }
    public void setRepotting(String repotting) { this.repotting = repotting; }

    public String getUsesText() { return usesText; }
    public void setUsesText(String usesText) { this.usesText = usesText; }

    public String getAdaptationText() { return adaptationText; }
    public void setAdaptationText(String adaptationText) { this.adaptationText = adaptationText; }

    public String getEcologicalText() { return ecologicalText; }
    public void setEcologicalText(String ecologicalText) { this.ecologicalText = ecologicalText; }

    public String getHistoryText() { return historyText; }
    public void setHistoryText(String historyText) { this.historyText = historyText; }

    public String getNameStoryText() { return nameStoryText; }
    public void setNameStoryText(String nameStoryText) { this.nameStoryText = nameStoryText; }

    public String getSymbolismText() { return symbolismText; }
    public void setSymbolismText(String symbolismText) { this.symbolismText = symbolismText; }

    @PropertyName("distribution_coordinates")
    public List<Map<String, Object>> getDistribution_coordinates() { return distribution_coordinates; }

    @PropertyName("distribution_coordinates")
    public void setDistribution_coordinates(List<Map<String, Object>> distribution_coordinates) { this.distribution_coordinates = distribution_coordinates; }
}
