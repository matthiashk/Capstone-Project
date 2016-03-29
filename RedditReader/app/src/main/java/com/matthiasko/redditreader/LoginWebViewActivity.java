package com.matthiasko.redditreader;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by matthiasko on 3/28/16.
 */
public class LoginWebViewActivity extends Activity {

    private static final String LOG_TAG = "LoginWebViewActivity";
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login_webview);

        Intent intent = getIntent();
        String authorizationUrl = intent.getStringExtra("AUTH_URL");


        System.out.println("authorizationUrl = " + authorizationUrl);




        webView = (WebView) findViewById(R.id.login_webview);

        webView.loadUrl(authorizationUrl);

        //setContentView(webView);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (url.contains("code=")) {
                    Log.v(LOG_TAG, "WebView URL: " + url);
                    // We've detected the redirect URL
                    //new UserChallengeTask(oAuthHelper, credentials).execute(url);
                }
            }
        });



    }
}
