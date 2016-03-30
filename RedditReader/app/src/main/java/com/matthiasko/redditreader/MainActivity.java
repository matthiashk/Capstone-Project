package com.matthiasko.redditreader;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ListView;

import net.dean.jraw.RedditClient;
import net.dean.jraw.android.AndroidRedditClient;
import net.dean.jraw.android.AndroidTokenStore;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.AuthenticationState;
import net.dean.jraw.auth.NoSuchTokenException;
import net.dean.jraw.auth.RefreshTokenHandler;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.http.oauth.OAuthHelper;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.SubredditPaginator;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "MainActivity";

    private static final String CLIENT_ID = "cAizcZuXu-Mn9w";

    private static final String REDIRECT_URL = "http://scroll-for-reddit.com/oauthresponse";

    private PostsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final RedditClient redditClient = new AndroidRedditClient(this);

        RefreshTokenHandler handler = new RefreshTokenHandler(new AndroidTokenStore(this), redditClient);

        AuthenticationManager.get().init(redditClient, handler);

        // check the authentication state of user
        AuthenticationState authState =  AuthenticationManager.get().checkAuthState();

        switch (authState) {

            case NONE:
                System.out.println("NONE");

                // load webview activity to login and authenticate user
                Intent intent = new Intent(this, LoginWebViewActivity.class);
                startActivity(intent);

                break;

            case NEED_REFRESH:
                System.out.println("NEED_REFRESH");

                // get the token from shared prefs using store
                AndroidTokenStore store = new AndroidTokenStore(MainActivity.this);

                try {

                    String refreshToken = store.readToken("EXAMPLE_KEY");
                    new RefreshTokenAsync().execute(refreshToken);

                } catch (NoSuchTokenException e) {

                    Log.e(LOG_TAG, e.getMessage());
                }

                break;

            case READY:
                System.out.println("READY");

                break;
        }

        ArrayList<Post> arrayOfUsers = new ArrayList<Post>();
        // Create the adapter to convert the array to views
        adapter = new PostsAdapter(MainActivity.this, arrayOfUsers);

        ListView listView = (ListView) findViewById(R.id.listview_posts);
        listView.setAdapter(adapter);
    }

    /*
        here we use RefreshTokenAsync to authenticate with our token in the background
        and update our UI in onPostExecute
    */
    private class RefreshTokenAsync extends AsyncTask <String, Void, String> {

        final RedditClient redditClient = new AndroidRedditClient(MainActivity.this);

        final OAuthHelper oAuthHelper = redditClient.getOAuthHelper();

        final Credentials credentials = Credentials.installedApp(CLIENT_ID, REDIRECT_URL);

        @Override
        protected String doInBackground(String... params) {

            String refreshToken = params[0];

            oAuthHelper.setRefreshToken(refreshToken);

            try {
                OAuthData finalData = oAuthHelper.refreshToken(credentials);
                redditClient.authenticate(finalData);
                if (redditClient.isAuthenticated()) {
                    Log.v(LOG_TAG, "Authenticated");
                }
            } catch (OAuthException e) {
                e.printStackTrace();
            }

            SubredditPaginator paginator = new SubredditPaginator(redditClient);
            Listing<Submission> submissions = paginator.next();
            final Submission first = submissions.get(0);

            String title = first.getTitle();
            //System.out.println("title = " + title);
            return title;
        }

        @Override
        protected void onPostExecute(String s) {

            Post post = new Post(s);
            adapter.add(post);
        }
    }

    // create list fragment
    // load fragment in mainactivity

    /*  change basic list to custom list - add title, name of subreddit, username, time etc

        title = title of post
        subreddit = name of subreddit
        username = name of poster
        source = name of source
        points = number of points
        comments = number of comments

        non text items
        thumbnail preview
        up/down vote button

        TODO: make a model?

     */
}
