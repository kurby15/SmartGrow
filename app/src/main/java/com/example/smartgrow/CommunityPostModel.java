package com.example.smartgrow; // Double-check if your package matches

public class CommunityPostModel {
    private String postId;
    private String username;
    private String profileImageUri;
    private String timeAgo;
    private String content;
    private String postImageUri;
    private int likesCount;
    private int commentsCount;

    public CommunityPostModel(String postId, String username, String profileImageUri, String timeAgo,
                         String content, String postImageUri, int likesCount, int commentsCount) {
        this.postId = postId;
        this.username = username;
        this.profileImageUri = profileImageUri;
        this.timeAgo = timeAgo;
        this.content = content;
        this.postImageUri = postImageUri;
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
    }

    public String getPostId() { return postId; }
    public String getUsername() { return username; }
    public String getProfileImageUri() { return profileImageUri; }
    public String getTimeAgo() { return timeAgo; }
    public String getContent() { return content; }
    public String getPostImageUri() { return postImageUri; }
    public int getLikesCount() { return likesCount; }
    public int getCommentsCount() { return commentsCount; }
}