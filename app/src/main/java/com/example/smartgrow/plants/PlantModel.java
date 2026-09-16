package com.example.smartgrow.plants;

public class PlantModel {
    private String id;
    private String name;
    private String scientificName;
    private String dateAdded;
    private String healthStatus;
    private String medicinal;
    private int healthPercentage;
    private String imageUrl;
    private ReminderModel reminders;

    // Required empty constructor para sa Firebase
    public PlantModel() {
    }

    public PlantModel(String name, String scientificName, String dateAdded, String healthStatus, String medicinal, int healthPercentage) {
        this.name = name;
        this.scientificName = scientificName;
        this.dateAdded = dateAdded;
        this.healthStatus = healthStatus;
        this.medicinal = medicinal;
        this.healthPercentage = healthPercentage;
    }

    // Getters at Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScientificName() {
        return scientificName;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    public String getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(String dateAdded) {
        this.dateAdded = dateAdded;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public String getMedicinal() {
        return medicinal;
    }

    public void setMedicinal(String medicinal) {
        this.medicinal = medicinal;
    }

    public int getHealthPercentage() {
        return healthPercentage;
    }

    public void setHealthPercentage(int healthPercentage) {
        this.healthPercentage = healthPercentage;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public ReminderModel getReminders() {
        return reminders;
    }

    public void setReminders(ReminderModel reminders) {
        this.reminders = reminders;
    }
    public String getPlantName() {
        return name;
    }

    public android.graphics.Bitmap getPlantBitmap() {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                byte[] decodedString = android.util.Base64.decode(imageUrl, android.util.Base64.DEFAULT);
                return android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}