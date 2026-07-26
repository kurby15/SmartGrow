package com.example.smartgrow;

import java.util.HashMap;
import java.util.Map;

public class CommunityPostModel {
    private String postId;
    private String username;
    private String userId;
    private String profileImageUri;
    private Long timestamp;
    private String content;
    private String postImageUri;
    private String location; 
    private boolean archived = false;
    private int likesCount = 0;
    private int commentsCount = 0;
    private Map<String, Boolean> likes = new HashMap<>();

    public CommunityPostModel() {}

    public CommunityPostModel(String postId, String username, String userId, String profileImageUri, 
                         Long timestamp, String content, String postImageUri, String location) {
        this.postId = postId;
        this.username = username;
        this.userId = userId;
        this.profileImageUri = profileImageUri;
        this.timestamp = timestamp;
        this.content = content;
        this.postImageUri = postImageUri;
        this.location = location;
    }

    // Getters
    public String getPostId() { return postId; }
    public String getUsername() { return username; }
    public String getUserId() { return userId; }
    public String getProfileImageUri() { return profileImageUri; }
    public Long getTimestamp() { return timestamp; }
    public String getContent() { return content; }
    public String getPostImageUri() { return postImageUri; }
    public String getLocation() { return location; }
    public boolean isArchived() { return archived; }
    public int getLikesCount() { return likesCount; }
    public int getCommentsCount() { return commentsCount; }
    public Map<String, Boolean> getLikes() { return likes; }

    // Setters
    public void setPostId(String postId) { this.postId = postId; }
    public void setUsername(String username) { this.username = username; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setProfileImageUri(String profileImageUri) { this.profileImageUri = profileImageUri; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    public void setContent(String content) { this.content = content; }
    public void setPostImageUri(String postImageUri) { this.postImageUri = postImageUri; }
    public void setLocation(String location) { this.location = location; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }
    public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }
    public void setLikes(Map<String, Boolean> likes) { this.likes = likes; }
}