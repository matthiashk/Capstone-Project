package com.matthiasko.scrollforreddit.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by matthiasko on 8/4/16.
 */
public class ImgurResponse {

    @Expose
    @SerializedName("data")
    private ImgurImage results;

    public ImgurImage getResults() {
        return results;
    }

    public void setResults(ImgurImage results) {
        this.results = results;
    }
}
