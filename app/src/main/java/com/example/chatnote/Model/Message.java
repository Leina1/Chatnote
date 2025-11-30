package com.example.chatnote.Model;

public class Message {
    private String id;         // Nếu muốn lưu id của message
    private String senderId;   // Người gửi
    private String content;    // Nội dung
    private long timestamp;    // Thời gian gửi
    private String imageUrl;   // Link ảnh gửi kèm (có thể null)

    public Message() {} // Firebase Firestore bắt buộc phải có constructor rỗng

    // Constructor cho text message
    public Message(String senderId, String content, long timestamp) {
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
        this.imageUrl = null;
    }

    // Constructor cho image message (có thể có caption hoặc không)
    public Message(String senderId, String content, String imageUrl, long timestamp) {
        this.senderId = senderId;
        this.content = content;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
    }

    // Getter và Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
