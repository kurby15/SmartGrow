package com.example.smartgrow.plants;

public class ReminderModel {
    private String wateringSchedule;
    private String fertilizerSchedule;
    private String sunlightSchedule;
    private String preferredTime;

    // Required empty constructor para sa Firebase
    public ReminderModel() {
    }

    // Parameterized constructor para sa pag-save galing sa UI
    public ReminderModel(String wateringSchedule, String fertilizerSchedule, String sunlightSchedule, String preferredTime) {
        this.wateringSchedule = wateringSchedule;
        this.fertilizerSchedule = fertilizerSchedule;
        this.sunlightSchedule = sunlightSchedule;
        this.preferredTime = preferredTime;
    }

    public String getWateringSchedule() {
        return wateringSchedule;
    }

    public void setWateringSchedule(String wateringSchedule) {
        this.wateringSchedule = wateringSchedule;
    }

    public String getFertilizerSchedule() {
        return fertilizerSchedule;
    }

    public void setFertilizerSchedule(String fertilizerSchedule) {
        this.fertilizerSchedule = fertilizerSchedule;
    }

    public String getSunlightSchedule() {
        return sunlightSchedule;
    }

    public void setSunlightSchedule(String sunlightSchedule) {
        this.sunlightSchedule = sunlightSchedule;
    }

    public String getPreferredTime() {
        return preferredTime;
    }

    public void setPreferredTime(String preferredTime) {
        this.preferredTime = preferredTime;
    }
}