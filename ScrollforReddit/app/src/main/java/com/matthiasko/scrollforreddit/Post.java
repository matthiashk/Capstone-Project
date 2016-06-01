package com.matthiasko.scrollforreddit;

/**
 * Created by matthiasko on 4/1/16.
 */
public class Post {

    public String postTitle;
    public String postSubreddit;
    public String postAuthor;
    public String postSource;
    public String postThumbnail;
    public int postPoints;
    public int postNumberOfComments;
    public String postId;
    public String postDomain;
    public long id;
    public String postFullName;

    public Post() {}

    public Post(String postTitle, String postSubreddit, String postAuthor, String postSource,
                String postThumbnail, int postPoints, int postNumberOfComments, String postId,
                String postDomain, String postFullName) {

        this.postTitle = postTitle;
        this.postSubreddit = "r/" + postSubreddit;
        this.postAuthor = postAuthor;
        this.postSource = postSource;
        this.postThumbnail = postThumbnail;
        this.postPoints = postPoints;
        this.postNumberOfComments = postNumberOfComments;
        this.postId = postId;
        this.postDomain = postDomain;

        this.postFullName = postFullName;

    }

    public String getPostFullName() {
        return postFullName;
    }

    public void setPostFullName(String postFullName) {
        this.postFullName = postFullName;
    }

    public String getPostAuthor() {
        return postAuthor;
    }

    public void setPostAuthor(String postAuthor) {
        this.postAuthor = postAuthor;
    }

    public String getPostDomain() {
        return postDomain;
    }

    public void setPostDomain(String postDomain) {
        this.postDomain = postDomain;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public int getPostNumberOfComments() {
        return postNumberOfComments;
    }

    public void setPostNumberOfComments(int postNumberOfComments) {
        this.postNumberOfComments = postNumberOfComments;
    }

    public int getPostPoints() {
        return postPoints;
    }

    public void setPostPoints(int postPoints) {
        this.postPoints = postPoints;
    }

    public String getPostSource() {
        return postSource;
    }

    public void setPostSource(String postSource) {
        this.postSource = postSource;
    }

    public String getPostSubreddit() {
        return postSubreddit;
    }

    public void setPostSubreddit(String postSubreddit) {
        this.postSubreddit = postSubreddit;
    }

    public String getPostThumbnail() {
        return postThumbnail;
    }

    public void setPostThumbnail(String postThumbnail) {
        this.postThumbnail = postThumbnail;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public void setPostTitle(String postTitle) {
        this.postTitle = postTitle;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
