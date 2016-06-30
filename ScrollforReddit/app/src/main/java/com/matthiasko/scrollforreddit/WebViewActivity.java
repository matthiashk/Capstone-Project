package com.matthiasko.scrollforreddit;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

/**
 * Created by matthiasko on 4/11/16.
 */
public class WebViewActivity extends Activity {

    private WebView webView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        String url = getIntent().getStringExtra("SOURCE");

        System.out.println("WebViewActivity - url = " + url);

        webView = (WebView) findViewById(R.id.webView1);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);

        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);

        /*
        // use this to load the webview in the app
        this.webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        */

        webView.loadUrl(url);

        // exit the activity,
        // otherwise a blank view will be displayed when the user returns from the url

        if (url.contains("imgur")) {

            finish();
        }
        //finish();

        //String imgSrcHtml = "<html><img src='" + url + "' /></html>";
        //webView.loadData(imgSrcHtml, "text/html", "UTF-8");
    }
}
