package com.example.chatnote.Model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Post implements Serializable {
    private String postId;
    private String userId;
    private String fullName;
    private String profilePicUrl;
    private String title;
    private String content;
    private String imageUrl;
    private long timestamp;
    private List<String> likeUserIds;
    private List<Map<String, Object>> comments; // mỗi comment gồm userId và nội dung

    public Post() {}

    // Getter và Setter
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getProfilePicUrl() { return profilePicUrl; }
    public void setProfilePicUrl(String profilePicUrl) { this.profilePicUrl = profilePicUrl; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public List<String> getLikeUserIds() { return likeUserIds; }
    public void setLikeUserIds(List<String> likeUserIds) { this.likeUserIds = likeUserIds; }

    public List<Map<String, Object>> getComments() { return comments; }
    public void setComments(List<Map<String, Object>> comments) { this.comments = comments; }
}
