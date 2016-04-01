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

    public Post(String postTitle, String postSubreddit, String postAuthor, String postSource,
                String postThumbnail, int postPoints, int postNumberOfComments) {

        this.postTitle = postTitle;
        this.postSubreddit = "r/" + postSubreddit;
        this.postAuthor = postAuthor;
        this.postSource = postSource;
        this.postThumbnail = postThumbnail;
        this.postPoints = postPoints;
        this.postNumberOfComments = postNumberOfComments;

    }
}
