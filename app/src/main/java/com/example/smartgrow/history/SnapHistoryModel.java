package com.example.smartgrow.history;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;
import java.util.List;
import java.util.Map;

@IgnoreExtraProperties
public class SnapHistoryModel {

    private String id;
    private String userId;
    private String plantName;
    private String scientificName;
    private String healthStatus;
    private int healthPercentage;
    private String healthColor;
    private int matchConfidencePercentage;
    private String careDifficultyText;
    private int careDifficultyPercentage;
    private List<String> leafColors;
    private String aliases;
    private String petToxicity;
    private String weedPotential;
    private String distribution;
    private String habitat;
    private String plantType;
    private String lifespan;
    private boolean isArtificial;
    
    @PropertyName("distribution_coordinates")
    private List<Map<String, Object>> distributionCoordinates;

    private String ultimateHeight;
    private String ultimateSpread;
    private String leafType;
    private String plantingTime;
    private String temperatureRange;
    private String hardinessZones;
    private String sunlight;
    private String soil;
    private String pruning;
    private String propagation;
    private String repotting;

    private String usesText;
    private String adaptationText;
    private String ecologicalText;
    private String historyText;
    private String nameStoryText;
    private String symbolismText;

    private String imageBase64;
    private String rawAnalysisJson;
    private long timestamp;

    public SnapHistoryModel() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @PropertyName("userId")
    public String getUserId() { return userId; }
    @PropertyName("userId")
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("plantName")
    public String getPlantName() { return plantName; }
    @PropertyName("plantName")
    public void setPlantName(String plantName) { this.plantName = plantName; }

    public String getScientificName() { return scientificName; }
    public void setScientificName(String scientificName) { this.scientificName = scientificName; }

    public String getHealthStatus() { return healthStatus; }
    public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }

    public int getHealthPercentage() { return healthPercentage; }
    public void setHealthPercentage(int healthPercentage) { this.healthPercentage = healthPercentage; }

    public String getHealthColor() { return healthColor; }
    public void setHealthColor(String healthColor) { this.healthColor = healthColor; }

    public int getMatchConfidencePercentage() { return matchConfidencePercentage; }
    public void setMatchConfidencePercentage(int matchConfidencePercentage) { this.matchConfidencePercentage = matchConfidencePercentage; }

    public String getCareDifficultyText() { return careDifficultyText; }
    public void setCareDifficultyText(String careDifficultyText) { this.careDifficultyText = careDifficultyText; }

    public int getCareDifficultyPercentage() { return careDifficultyPercentage; }
    public void setCareDifficultyPercentage(int careDifficultyPercentage) { this.careDifficultyPercentage = careDifficultyPercentage; }

    public List<String> getLeafColors() { return leafColors; }
    public void setLeafColors(List<String> leafColors) { this.leafColors = leafColors; }

    public String getAliases() { return aliases; }
    public void setAliases(String aliases) { this.aliases = aliases; }

    public String getPetToxicity() { return petToxicity; }
    public void setPetToxicity(String petToxicity) { this.petToxicity = petToxicity; }

    public String getWeedPotential() { return weedPotential; }
    public void setWeedPotential(String weedPotential) { this.weedPotential = weedPotential; }

    public String getDistribution() { return distribution; }
    public void setDistribution(String distribution) { this.distribution = distribution; }

    public String getHabitat() { return habitat; }
    public void setHabitat(String habitat) { this.habitat = habitat; }

    public String getPlantType() { return plantType; }
    public void setPlantType(String plantType) { this.plantType = plantType; }

    public String getLifespan() { return lifespan; }
    public void setLifespan(String lifespan) { this.lifespan = lifespan; }

    @PropertyName("isArtificial")
    public boolean isArtificial() { return isArtificial; }
    @PropertyName("isArtificial")
    public void setArtificial(boolean artificial) { isArtificial = artificial; }

    @PropertyName("distribution_coordinates")
    public List<Map<String, Object>> getDistributionCoordinates() { return distributionCoordinates; }
    @PropertyName("distribution_coordinates")
    public void setDistributionCoordinates(List<Map<String, Object>> distributionCoordinates) { this.distributionCoordinates = distributionCoordinates; }

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

    @PropertyName("imageBase64")
    public String getImageBase64() { return imageBase64; }
    @PropertyName("imageBase64")
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    @PropertyName("rawAnalysisJson")
    public String getRawAnalysisJson() { return rawAnalysisJson; }
    @PropertyName("rawAnalysisJson")
    public void setRawAnalysisJson(String rawAnalysisJson) { this.rawAnalysisJson = rawAnalysisJson; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
