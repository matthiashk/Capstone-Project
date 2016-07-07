package com.matthiasko.scrollforreddit.activities;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;

import com.matthiasko.scrollforreddit.R;
import com.squareup.picasso.Picasso;

/**
 * Created by matthiasko on 5/17/16.
 *
 * Unused class for viewing images, may be used in the future
 *
 */
public class ImageViewActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_imageview);
        ImageView image = (ImageView) findViewById(R.id.imageView1);
        String url = getIntent().getStringExtra("SOURCE");
        Picasso.with(getApplicationContext()).load(url).fit().centerCrop().into(image);
    }
}
