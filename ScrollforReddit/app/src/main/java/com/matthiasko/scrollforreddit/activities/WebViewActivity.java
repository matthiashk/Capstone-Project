package com.matthiasko.scrollforreddit.activities;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import com.matthiasko.scrollforreddit.R;

/**
 * Created by matthiasko on 4/11/16.
 * Activity that displays the source url from a post
 *
 */
public class WebViewActivity extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        String url = getIntent().getStringExtra("SOURCE");

        WebView webView = (WebView) findViewById(R.id.webView1);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.loadUrl(url);

        // exit the activity,
        // otherwise a blank view will be displayed when the user returns from the url
        if (url.contains("imgur")) {
            finish();
        }
    }
}
