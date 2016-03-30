package com.matthiasko.redditreader;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import net.dean.jraw.RedditClient;
import net.dean.jraw.android.AndroidRedditClient;
import net.dean.jraw.android.AndroidTokenStore;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.RefreshTokenHandler;
import net.dean.jraw.http.LoggingMode;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.http.oauth.OAuthHelper;

/**
 * Created by matthiasko on 3/28/16.
 */
public class LoginWebViewActivity extends Activity {

    private static final String LOG_TAG = "LoginWebViewActivity";
    private WebView webView;
    private static RedditClient redditClient;

    private static final String CLIENT_ID = "cAizcZuXu-Mn9w";

    private static final String REDIRECT_URL = "http://scroll-for-reddit.com/oauthresponse";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login_webview);

        /* used code from https://gist.github.com/fbis251/5d54e95d96fbfda22a3f */

        redditClient = new AndroidRedditClient(this);

        redditClient.setLoggingMode(LoggingMode.ALWAYS);

        RefreshTokenHandler handler = new RefreshTokenHandler(new AndroidTokenStore(this), redditClient);

        AuthenticationManager.get().init(redditClient, handler);

        final Credentials credentials = Credentials.installedApp(CLIENT_ID, REDIRECT_URL);

        String[] scopes = {"identity", "read", "vote", "save", "mysubreddits"};

        boolean permanent = true;

        final OAuthHelper oAuthHelper = redditClient.getOAuthHelper();

        String authorizationUrl = oAuthHelper.getAuthorizationUrl(credentials, permanent, scopes)
                .toExternalForm();

        authorizationUrl = authorizationUrl.replace("www.", "i.");

        System.out.println("authorizationUrl = " + authorizationUrl);

        // clear cookies to remove login errors
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

        webView = (WebView) findViewById(R.id.login_webview);

        webView.loadUrl(authorizationUrl);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {

                Log.v(LOG_TAG, "onPageStarted - WebView URL: " + url);

                if (url.contains("code=")) {
                    Log.v(LOG_TAG, "onPageStarted - REDIRECT URL: " + url);
                    // We've detected the redirect URL
                    new UserChallengeTask(oAuthHelper, credentials).execute(url);

                    // exit back to mainactivity once authenticated
                    finish();
                }
            }
        });
    }

    private final class UserChallengeTask extends AsyncTask<String, Void, OAuthData> {

        private OAuthHelper mOAuthHelper;

        private Credentials mCredentials;

        private OAuthData mOAuthData;

        public UserChallengeTask(OAuthHelper oAuthHelper, Credentials credentials) {

            Log.v(LOG_TAG, "UserChallengeTask()");
            mOAuthHelper = oAuthHelper;
            mCredentials = credentials;
        }

        @Override
        protected OAuthData doInBackground(String... params) {

            Log.v(LOG_TAG, "doInBackground()");
            Log.v(LOG_TAG, "params[0]: " + params[0]);

            try {

                mOAuthData = mOAuthHelper.onUserChallenge(params[0], mCredentials);

                redditClient.authenticate(mOAuthData);

                Log.v(LOG_TAG, "Reddit client authentication: " + redditClient.isAuthenticated());

                //return mOAuthHelper.onUserChallenge(params[0], mCredentials);
            } catch (IllegalStateException | NetworkException | OAuthException e) {
                // Handle me gracefully
                Log.e(LOG_TAG, "OAuth failed");
                Log.e(LOG_TAG, e.getMessage());

                //OAuthHelper.AuthStatus authStatus = mOAuthHelper.getAuthStatus();

                //System.out.println("authStatus = " + authStatus.toString());
            }

            return null;
        }

        @Override
        protected void onPostExecute(OAuthData oAuthData) {

            Log.v(LOG_TAG, "onPostExecute()");

            String refreshToken = redditClient.getOAuthData().getRefreshToken();

            AndroidTokenStore store = new AndroidTokenStore(LoginWebViewActivity.this);

            store.writeToken("EXAMPLE_KEY", refreshToken);

            Log.v(LOG_TAG, "Refresh Token: " + refreshToken);

            /*
            if (oAuthData != null) {
                redditClient.authenticate(oAuthData);
                Log.v(LOG_TAG, "Reddit client authentication: " + redditClient.isAuthenticated());
                //mTextView.setText("Logged in");
                //TODO: Save refresh token:
                //String refreshToken = mRedditClient.getOAuthData().getRefreshToken();
                //Log.v(LOG_TAG, "Refresh Token: " + refreshToken);
            } else {
                Log.e(LOG_TAG, "Passed in OAuthData was null");
            }*/
        }
    }
}