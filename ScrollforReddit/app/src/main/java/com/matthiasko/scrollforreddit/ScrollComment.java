package com.matthiasko.scrollforreddit;

/**
 * Created by matthiasko on 4/5/16.
 */
public class ScrollComment {

    public String body;
    public String author;
    public int score;
    public int depth;
    public String postId;

    // TODO: add date/time

    public ScrollComment(){}

    public ScrollComment(String body, String author, int score, int depth, String postId) {

        this.body = body;
        this.author = author;
        this.score = score;
        this.depth = depth;
        this.postId = postId;
    }

    public String getAuthor() {
        return author;
    }

    public String getBody() {
        return body;
    }

    public int getDepth() {
        return depth;
    }

    public int getScore() {
        return score;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }
}
