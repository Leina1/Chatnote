package com.example.chatnote.Model;

public class ModelNote {
    private String id;
    private String title;
    private String content;
    private String imageUrl;
    private String time;

    public ModelNote(String id, String title, String content, String imageUrl, String time) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        this.time = time;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getImageUrl() { return imageUrl; }
    public String getTime() { return time; }
}
