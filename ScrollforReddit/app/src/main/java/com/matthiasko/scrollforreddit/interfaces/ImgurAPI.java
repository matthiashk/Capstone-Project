package com.matthiasko.scrollforreddit.interfaces;

import com.matthiasko.scrollforreddit.models.ImgurResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by matthiasko on 8/4/16.
 */
public interface ImgurAPI {

    @GET("image/{id}")
    Call<ImgurResponse> getId(@Path("id") String id);
}
