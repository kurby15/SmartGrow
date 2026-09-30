package com.example.smartgrow.plants;

public class TaskModel {
    private String taskTitle;
    private String taskTime;
    private boolean isCompleted;

    public TaskModel(String taskTitle, String taskTime, boolean isCompleted) {
        this.taskTitle = taskTitle;
        this.taskTime = taskTime;
        this.isCompleted = isCompleted;
    }

    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }

    public String getTaskTime() { return taskTime; }
    public void setTaskTime(String taskTime) { this.taskTime = taskTime; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
}
