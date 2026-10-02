package com.example.smartgrow.community;

public class CommentModel {
    private String commentId;
    private String username;
    private String userId; 
    private String profileImageUri;
    private String content;
    private String commentImageUri;
    private Long timestamp;

    public CommentModel() {}

    public CommentModel(String commentId, String username, String userId, String profileImageUri, String content, Long timestamp) {
        this.commentId = commentId;
        this.username = username;
        this.userId = userId;
        this.profileImageUri = profileImageUri;
        this.content = content;
        this.timestamp = timestamp;
    }

    public CommentModel(String commentId, String username, String userId, String profileImageUri, String content, String commentImageUri, Long timestamp) {
        this.commentId = commentId;
        this.username = username;
        this.userId = userId;
        this.profileImageUri = profileImageUri;
        this.content = content;
        this.commentImageUri = commentImageUri;
        this.timestamp = timestamp;
    }

    public String getCommentId() { return commentId; }
    public String getUsername() { return username; }
    public String getUserId() { return userId; }
    public String getProfileImageUri() { return profileImageUri; }
    public String getContent() { return content; }
    public String getCommentImageUri() { return commentImageUri; }
    public Long getTimestamp() { return timestamp; }

    public void setCommentId(String commentId) { this.commentId = commentId; }
    public void setUsername(String username) { this.username = username; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setProfileImageUri(String profileImageUri) { this.profileImageUri = profileImageUri; }
    public void setContent(String content) { this.content = content; }
    public void setCommentImageUri(String commentImageUri) { this.commentImageUri = commentImageUri; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}