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
import net.dean.jraw.paginators.SubredditStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

/**
 * Created by matthiasko on 4/28/16.
 */
public class FetchUserlessSubsAsyncTask extends AsyncTask<String, Void, ArrayList> {

    private static final String LOG_TAG = FetchUserlessSubsAsyncTask.class.getSimpleName();

    private static final String CLIENT_ID = "cAizcZuXu-Mn9w";

    private static final String REDIRECT_URL = "http://scroll-for-reddit.com/oauthresponse";

    private Context mContext;

    private AsyncListener mAsyncListener;

    private UUID mDeviceId;

    private RedditClient mRedditClient;

    public FetchUserlessSubsAsyncTask(Context context) {
        mContext = context;
    }

    public void setAsyncListener(AsyncListener asyncListener) {
        this.mAsyncListener = asyncListener;
    }

    @Override
    protected ArrayList doInBackground(String... params) {

        mRedditClient = new AndroidRedditClient(mContext);

        // get uuid from shared prefs
        SharedPreferences appSharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(mContext);

        if (appSharedPrefs.contains("com.matthiasko.scrollforreddit.UUID")) {
            String uuidString = appSharedPrefs.getString("com.matthiasko.scrollforreddit.UUID", null);
            mDeviceId = UUID.fromString(uuidString);
        }

        // check authentication
        mRedditClient = new AndroidRedditClient(mContext);
        final OAuthHelper oAuthHelper = mRedditClient.getOAuthHelper();

        // note 'userlessApp' used here instead of 'installedApp'
        final Credentials credentials = Credentials.userlessApp(CLIENT_ID, mDeviceId);

        try {
            OAuthData finalData = oAuthHelper.easyAuth(credentials);
            mRedditClient.authenticate(finalData);
            if (mRedditClient.isAuthenticated()) {
                Log.v(LOG_TAG, "Authenticated");
            }

            SubredditStream subredditStream = new SubredditStream(mRedditClient, "popular");

            Listing<Subreddit> subreddits = subredditStream.next();

            ArrayList<String> subredditNames = new ArrayList<>();

            // put into array, sort array alphabetically, send array back to postlistactivity, populate menu
            for (Subreddit subreddit : subreddits) {
                subredditNames.add(subreddit.getDisplayName());
            }
            // sort list alphabetically
            Collections.sort(subredditNames, String.CASE_INSENSITIVE_ORDER);
            return subredditNames;

        } catch (OAuthException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(ArrayList arrayList) {
        super.onPostExecute(arrayList);
        if (mAsyncListener != null ) {
            mAsyncListener.createNavMenuItems(arrayList);
        }
    }
}
