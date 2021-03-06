package com.matthiasko.scrollforreddit.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.matthiasko.scrollforreddit.R;

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
 * Created by matthiasko on 4/1/16.
 *
 * Activity that allows user to login and allow app access to their reddit account in order to
 * get the users subreddits, allow voting, submitting posts, create comments, reply to comments,
 * subscribe to subreddits, unsubscribe to subreddits
 *
 */
public class LoginWebViewActivity extends Activity {

    private static final String LOG_TAG = "LoginWebViewActivity";
    private static final String CLIENT_ID = "cAizcZuXu-Mn9w";
    private static final String REDIRECT_URL = "http://scroll-for-reddit.com/oauthresponse";
    private static RedditClient redditClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_webview);

        /* based on parts of code from https://gist.github.com/fbis251/5d54e95d96fbfda22a3f */
        redditClient = new AndroidRedditClient(this);
        redditClient.setLoggingMode(LoggingMode.ALWAYS);

        RefreshTokenHandler handler = new RefreshTokenHandler(new AndroidTokenStore(this), redditClient);

        AuthenticationManager.get().init(redditClient, handler);

        final Credentials credentials = Credentials.installedApp(CLIENT_ID, REDIRECT_URL);

        String[] scopes = {"identity", "read", "vote", "save", "mysubreddits", "submit", "subscribe"};

        boolean permanent = true;

        final OAuthHelper oAuthHelper = redditClient.getOAuthHelper();

        String authorizationUrl = oAuthHelper.getAuthorizationUrl(credentials, permanent, scopes)
                .toExternalForm();

        authorizationUrl = authorizationUrl.replace("www.", "i.");

        // clear cookies to remove login errors
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

        WebView webView = (WebView) findViewById(R.id.login_webview);
        webView.loadUrl(authorizationUrl);
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {

                if (url.contains("code=")) {
                    //Log.v(LOG_TAG, "onPageStarted - REDIRECT URL: " + url);
                    // We've detected the redirect URL
                    new UserChallengeTask(oAuthHelper, credentials).execute(url);
                    // exit back to mainactivity once authenticated
                }
            }
        });
    }

    private final class UserChallengeTask extends AsyncTask<String, Void, OAuthData> {

        private OAuthHelper mOAuthHelper;
        private Credentials mCredentials;
        private OAuthData mOAuthData;

        public UserChallengeTask(OAuthHelper oAuthHelper, Credentials credentials) {
            mOAuthHelper = oAuthHelper;
            mCredentials = credentials;
        }

        @Override
        protected OAuthData doInBackground(String... params) {

            try {
                mOAuthData = mOAuthHelper.onUserChallenge(params[0], mCredentials);
                redditClient.authenticate(mOAuthData);
            } catch (IllegalStateException | NetworkException | OAuthException e) {
                Log.e(LOG_TAG, "OAuth failed");
                Log.e(LOG_TAG, e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(OAuthData oAuthData) {
            // loader will be hidden in PostListActivity / onActivityResult
            // store access token
            String refreshToken = redditClient.getOAuthData().getRefreshToken();
            AndroidTokenStore store = new AndroidTokenStore(LoginWebViewActivity.this);
            store.writeToken("USER_TOKEN", refreshToken);
            // send the onactivityresult intent, since we are done with this activity here
            Intent i = getIntent();
            setResult(RESULT_OK, i);
            finish();
        }
    }
}
