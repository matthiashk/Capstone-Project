package com.matthiasko.scrollforreddit.models;

/**
 * Created by matthiasko on 8/4/16.
 */
public class ImgurImage {

    String id;
    String title;
    String description;
    String type;
    boolean animated;
    int width;
    int height;
    String gifv;

    public boolean isAnimated() {
        return animated;
    }

    public String getDescription() {
        return description;
    }

    public String getGifv() {
        return gifv;
    }

    public int getHeight() {
        return height;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public int getWidth() {
        return width;
    }
}
