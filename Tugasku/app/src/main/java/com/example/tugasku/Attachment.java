package com.example.tugasku;

public class Attachment {
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_FILE = "file";
    public static final String TYPE_LINK = "link";

    private String type;       // image, file, link
    private String name;       // display name
    private String uri;        // content Uri or http(s) link
    private String mimeType;   // e.g., application/pdf, application/msword

    public Attachment(String type, String name, String uri, String mimeType) {
        this.type = type;
        this.name = name;
        this.uri = uri;
        this.mimeType = mimeType;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
}
