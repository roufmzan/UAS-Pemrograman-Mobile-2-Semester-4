package com.example.tugasku;

public class AudioNote {
    private String id;
    private String title;
    private String transcript;
    private String summary;
    private long timestamp;
    private String audioPath;

    public AudioNote(String id, String title, String transcript, String summary, long timestamp, String audioPath) {
        this.id = id;
        this.title = title;
        this.transcript = transcript;
        this.summary = summary;
        this.timestamp = timestamp;
        this.audioPath = audioPath;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getTranscript() { return transcript; }
    public String getSummary() { return summary; }
    public long getTimestamp() { return timestamp; }
    public String getAudioPath() { return audioPath; }

    public void setTitle(String title) { this.title = title; }
    public void setTranscript(String transcript) { this.transcript = transcript; }
    public void setSummary(String summary) { this.summary = summary; }
}
