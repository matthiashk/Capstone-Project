package com.matthiasko.redditreader;

/**
 * Created by matthiasko on 3/24/16.
 */
public class Post {

    public String postTitle;
    public String postSubreddit;
    public String postUserName;
    public String postSource;
    public int postPoints;
    public int postNumberOfComments;

    public Post(String postTitle) {

        //, String postSubreddit, String postUserName, String postSource, int postPoints, int postNumberOfComments

        this.postTitle = postTitle;
        /*
        this.postSubreddit = postSubreddit;
        this.postUserName = postUserName;
        this.postSource = postSource;
        this.postPoints = postPoints;
        this.postNumberOfComments = postNumberOfComments;
        */
    }
}
