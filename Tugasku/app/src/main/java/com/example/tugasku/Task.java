package com.example.tugasku;

public class Task {
    private String title;
    private String course;
    private String deadline;
    private String description;
    private String imagePath;
    private int colorIndicator;
    private boolean isCompleted;
    private java.util.List<Attachment> attachments;

    public Task(String title, String course, String deadline, int colorIndicator, boolean isCompleted) {
        this.title = title;
        this.course = course;
        this.deadline = deadline;
        this.description = "";
        this.imagePath = "";
        this.colorIndicator = colorIndicator;
        this.isCompleted = isCompleted;
        this.attachments = new java.util.ArrayList<>();
    }

    public Task(String title, String course, String deadline, String description, int colorIndicator, boolean isCompleted) {
        this.title = title;
        this.course = course;
        this.deadline = deadline;
        this.description = description;
        this.imagePath = "";
        this.colorIndicator = colorIndicator;
        this.isCompleted = isCompleted;
        this.attachments = new java.util.ArrayList<>();
    }

    public Task(String title, String course, String deadline, String description, String imagePath, int colorIndicator, boolean isCompleted) {
        this.title = title;
        this.course = course;
        this.deadline = deadline;
        this.description = description;
        this.imagePath = imagePath;
        this.colorIndicator = colorIndicator;
        this.isCompleted = isCompleted;
        this.attachments = new java.util.ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public int getColorIndicator() {
        return colorIndicator;
    }

    public void setColorIndicator(int colorIndicator) {
        this.colorIndicator = colorIndicator;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public java.util.List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(java.util.List<Attachment> attachments) {
        this.attachments = attachments != null ? attachments : new java.util.ArrayList<>();
    }
}
