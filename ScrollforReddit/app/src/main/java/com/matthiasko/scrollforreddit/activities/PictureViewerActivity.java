package com.matthiasko.scrollforreddit.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import com.matthiasko.scrollforreddit.R;
import com.matthiasko.scrollforreddit.interfaces.ImgurAPI;
import com.matthiasko.scrollforreddit.models.ImgurResponse;
import com.squareup.picasso.Picasso;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by matthiasko on 8/4/16.
 */
public class PictureViewerActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pictureviewer);

        final String clientId = "b46d70caaf7d345"; // TODO: remove before check in?
        String url = "https://api.imgur.com/3/";



        ImageView imageView = (ImageView) findViewById(R.id.pictureviewer_imageview);

        Intent intent = getIntent();

        String source = intent.getStringExtra("SOURCE");

        String postId = intent.getStringExtra("POST_ID");



        String thumbnail = intent.getStringExtra("THUMBNAIL");

        //System.out.println("PICTUREVIEWERACTIVITY - thumbnail = " + thumbnail);



        // set up authorization header
        Interceptor interceptor = new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request newRequest = chain.request().newBuilder().addHeader("Authorization", "Client-ID " + clientId).build();
                return chain.proceed(newRequest);
            }
        };

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Add the interceptor to OkHttpClient
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.interceptors().add(interceptor);

        // add logging as last interceptor
        builder.addInterceptor(logging);

        //Creating Rest Services
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .client(builder.build())
                .build();

        ImgurAPI service = retrofit.create(ImgurAPI.class);

        Call<ImgurResponse> call = service.getId(postId);

        call.enqueue(new Callback<ImgurResponse>() {

            @Override
            public void onResponse(Call<ImgurResponse> call, Response<ImgurResponse> response) {

                try {

                    ImgurResponse imgurResponse = response.body();

                    //System.out.println("imgurResponse.getResults().getWidth() = " + imgurResponse.getResults().getWidth());

                    // id, title, description, type, animated, width, height, link

                    //System.out.println("imgurResponse.getResults().getId() = " + imgurResponse.getResults().getId());

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ImgurResponse> call, Throwable t) {
                //System.out.println("onFailure");
            }
        });


        // we should use the imgur api to handle gallery images
        // just use source to load single images?




        //Picasso.with(this).setLoggingEnabled(true);

        Picasso.with(this)
                .load(thumbnail)
                .into(imageView);

    }
}
