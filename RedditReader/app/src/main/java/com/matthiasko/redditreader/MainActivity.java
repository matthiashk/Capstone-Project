package com.matthiasko.redditreader;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import net.dean.jraw.RedditClient;
import net.dean.jraw.android.AndroidRedditClient;
import net.dean.jraw.android.AndroidTokenStore;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.AuthenticationState;
import net.dean.jraw.auth.RefreshTokenHandler;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthHelper;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "cAizcZuXu-Mn9w";

    private static final String REDIRECT_URL = "http://scroll-for-reddit.com/oauthresponse"; // TODO: fix

    private static final String LOG_TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ArrayList<Post> arrayOfUsers = new ArrayList<Post>();
        // Create the adapter to convert the array to views
        PostsAdapter adapter = new PostsAdapter(this, arrayOfUsers);

        ListView listView = (ListView) findViewById(R.id.listview_posts);
        listView.setAdapter(adapter);

        String title1 = "Post title 1";
        String title2 = "Post title 2";

        Post post = new Post(title1);
        Post post2 = new Post(title2);


        adapter.add(post);
        adapter.add(post2);

        RedditClient reddit = new AndroidRedditClient(this);

        RefreshTokenHandler handler = new RefreshTokenHandler(new AndroidTokenStore(this), reddit);

        AuthenticationManager.get().init(reddit, handler);

        AuthenticationState authState =  AuthenticationManager.get().checkAuthState();

        switch (authState) {

            case NONE:
                System.out.println("NONE");

                final Credentials credentials = Credentials.installedApp(CLIENT_ID, REDIRECT_URL);
                String[] scopes = {"identity", "read", "vote", "save", "mysubreddits"};

                boolean permanent = true;

                final OAuthHelper oAuthHelper = reddit.getOAuthHelper();

                String authorizationUrl = oAuthHelper.getAuthorizationUrl(credentials, permanent, scopes)
                        .toExternalForm();
                authorizationUrl = authorizationUrl.replace("www.", "i.");
                //Log.v(LOG_TAG, "Auth URL: " + authorizationUrl);


                // load webview activity here...

                Intent intent = new Intent(this, LoginWebViewActivity.class);

                intent.putExtra("AUTH_URL", authorizationUrl);
                startActivity(intent);





                break;

            case NEED_REFRESH:
                System.out.println("NEED_REFRESH");
                break;

            case READY:
                System.out.println("READY");
                break;

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
