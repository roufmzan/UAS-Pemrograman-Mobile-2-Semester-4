package com.example.tugasku;

public class Note {
    private String id;
    private String title;
    private String content;
    private long timestamp;
    private int color;

    public Note(String id, String title, String content, long timestamp, int color) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
        this.color = color;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
}
