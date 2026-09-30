package com.example.smartgrow.plants;

public class ScheduleModel {
    private String taskTitle;
    private String taskTime;
    private String taskNote;

    public ScheduleModel(String taskTitle, String taskTime, String taskNote) {
        this.taskTitle = taskTitle;
        this.taskTime = taskTime;
        this.taskNote = taskNote;
    }

    // Getters
    public String getTaskTitle() {
        return taskTitle;
    }

    public String getTaskTime() {
        return taskTime;
    }

    public String getTaskNote() {
        return taskNote;
    }
}