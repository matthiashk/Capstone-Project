package com.matthiasko.scrollforreddit;

/**
 * Created by matthiasko on 4/5/16.
 */
public class ScrollComment {

    public String body;
    public String author;
    public int score;
    public int depth;

    // TODO: add date/time

    public ScrollComment(String body, String author, int score, int depth) {

        this.body = body;
        this.author = author;
        this.score = score;
        this.depth = depth;
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
}
