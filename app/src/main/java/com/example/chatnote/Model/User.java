package com.example.chatnote.Model;

public class User {
    private String userId;
    private String fullName;
    private String email;
    private String phone;
    private String profilePicUrl;
    private String bio;
    private String status;
    private long lastActive;

    public User() { }

    public User(String userId, String fullName, String email, String phone) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
    }

    // Getters
    public String getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getProfilePicUrl() { return profilePicUrl; }
    public String getBio() { return bio; }
    public String getStatus() { return status; }
    public long getLastActive() { return lastActive; }

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setProfilePicUrl(String profilePicUrl) { this.profilePicUrl = profilePicUrl; }
    public void setBio(String bio) { this.bio = bio; }
    public void setStatus(String status) { this.status = status; }
    public void setLastActive(long lastActive) { this.lastActive = lastActive; }
}