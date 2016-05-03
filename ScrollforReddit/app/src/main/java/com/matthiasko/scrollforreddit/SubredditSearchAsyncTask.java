package com.matthiasko.scrollforreddit;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import net.dean.jraw.RedditClient;
import net.dean.jraw.android.AndroidRedditClient;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.http.oauth.OAuthHelper;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Subreddit;
import net.dean.jraw.paginators.SubredditSearchPaginator;

import java.util.UUID;

/**
 * Created by matthiasko on 5/1/16.
 */
public class SubredditSearchAsyncTask extends AsyncTask<String, Void, Void> {

    private Context mContext;

    private final String LOG_TAG = SubredditSearchAsyncTask.class.getSimpleName();

    private static RedditClient redditClient;

    private static final String CLIENT_ID = "cAizcZuXu-Mn9w";

    private UUID mDeviceId;

    public SubredditSearchAsyncTask(Context context) {
        this.mContext = context;
    }

    @Override
    protected Void doInBackground(String... params) {

        String searchName = params[0];

        SharedPreferences appSharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(mContext);

        if (appSharedPrefs.contains("com.matthiasko.scrollforreddit.UUID")) {
            String uuidString = appSharedPrefs.getString("com.matthiasko.scrollforreddit.UUID", null);
            mDeviceId = UUID.fromString(uuidString);
        }

        redditClient = new AndroidRedditClient(mContext);

        final OAuthHelper oAuthHelper = redditClient.getOAuthHelper();

        // note 'userlessApp' used here instead of 'installedApp'
        final Credentials credentials = Credentials.userlessApp(CLIENT_ID, mDeviceId);

        try {
            OAuthData finalData = oAuthHelper.easyAuth(credentials);
            redditClient.authenticate(finalData);
            if (redditClient.isAuthenticated()) {
                Log.v(LOG_TAG, "Authenticated");
            }
        } catch (OAuthException e) {
            e.printStackTrace();
        }


        SubredditSearchPaginator subredditSearchPaginator =
                new SubredditSearchPaginator(redditClient, searchName);

        Listing<Subreddit> subreddits = subredditSearchPaginator.next();

        System.out.println("subreddits.size() = " + subreddits.size());


        //ArrayList<String> subredditNames = new ArrayList<>();


        for (Subreddit subreddit : subreddits) {
            System.out.println("subreddit.getDisplayName() = " + subreddit.getDisplayName());
        }



        return null;
    }
}
