package com.matthiasko.scrollforreddit;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import net.dean.jraw.RedditClient;
import net.dean.jraw.android.AndroidRedditClient;
import net.dean.jraw.http.LoggingMode;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.http.oauth.OAuthHelper;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.SubredditPaginator;

import java.util.List;
import java.util.UUID;

/**
 * Created by matthiasko on 6/1/16.
 */
public class FetchMorePostsAsyncTask extends AsyncTask<String, Void, Boolean> {

    private Context mContext;

    private final String LOG_TAG = FetchMorePostsAsyncTask.class.getSimpleName();

    private FetchUserlessTokenListener mListener;

    private static final String CLIENT_ID = "cAizcZuXu-Mn9w";

    private static RedditClient redditClient;

    private UUID mDeviceId;

    private SubredditPaginator paginator;

    public FetchMorePostsAsyncTask(Context context, FetchUserlessTokenListener listener) {
        this.mContext = context;
        this.mListener = listener;
    }

    @Override
    protected Boolean doInBackground(String... params) {

        String subredditMenuName = params[0];

        int refreshCounter;

        // load deviceId here from sharedPrefs
        // get uuid from shared prefs
        SharedPreferences appSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        if (appSharedPrefs.contains("com.matthiasko.scrollforreddit.UUID")) {
            String uuidString = appSharedPrefs.getString("com.matthiasko.scrollforreddit.UUID", null);
            mDeviceId = UUID.fromString(uuidString);
        }

        redditClient = new AndroidRedditClient(mContext);
        redditClient.setLoggingMode(LoggingMode.ALWAYS);

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

        if (paginator == null) {

            if (subredditMenuName.equals("Frontpage")) {
                paginator = new SubredditPaginator(redditClient);
            } else {
                paginator = new SubredditPaginator(redditClient, subredditMenuName);
            }
        }

        if (appSharedPrefs.contains("com.matthiasko.scrollforreddit.REFRESH_COUNTER")) {
            refreshCounter = appSharedPrefs.getInt("com.matthiasko.scrollforreddit.REFRESH_COUNTER", 0);
        } else {

            refreshCounter = 2; // if there is no refreshCounter in sharedPrefs, set to '2' so we can load 2nd page

            // save the refreshCounter value to sharedPrefs
            SharedPreferences.Editor edit = appSharedPrefs.edit();
            edit.putInt("com.matthiasko.scrollforreddit.REFRESH_COUNTER", refreshCounter);
            edit.commit();
        }

        //System.out.println("refreshCounter = " + refreshCounter);

        // store counter in prefs and check for counter before loop
        for (int i = 0; i < refreshCounter; i++) {

            List<Submission> next = paginator.iterator().next().getChildren(); // this is calling the api?

            if (refreshCounter - 1 == i) {

                for (Submission submission : next) { // insert posts into database

                    String title = submission.getTitle();
                    // store fullname so we can get the specific post later...
                    String subreddit = submission.getSubredditName();

                    String username = submission.getAuthor();

                    String source = submission.getUrl();

                    String domain = submission.getDomain();

                    int points = submission.getScore();

                    int numberOfComments = submission.getCommentCount();

                    String thumbnail = submission.getThumbnail();

                    // we need to add this to the post item data so we can retrieve the commentnode in details view
                    String postId = submission.getId();

                    String fullName = submission.getFullName();

                    // add post data to database
                    ContentValues postValues = new ContentValues();

                    postValues.put(PostContract.PostEntry.COLUMN_TITLE, title);
                    postValues.put(PostContract.PostEntry.COLUMN_SUBREDDIT, subreddit);
                    postValues.put(PostContract.PostEntry.COLUMN_AUTHOR, username);
                    postValues.put(PostContract.PostEntry.COLUMN_SOURCE, source);
                    postValues.put(PostContract.PostEntry.COLUMN_THUMBNAIL, thumbnail);
                    postValues.put(PostContract.PostEntry.COLUMN_SCORE, points);
                    postValues.put(PostContract.PostEntry.COLUMN_NUMBER_OF_COMMENTS, numberOfComments);
                    postValues.put(PostContract.PostEntry.COLUMN_POST_ID, postId);
                    postValues.put(PostContract.PostEntry.COLUMN_SOURCE_DOMAIN, domain);
                    postValues.put(PostContract.PostEntry.COLUMN_FULLNAME, fullName);

                    mContext.getContentResolver().insert(PostContract.PostEntry.CONTENT_URI, postValues);
                }
            }
        }

        refreshCounter++;
        SharedPreferences.Editor edit = appSharedPrefs.edit();
        edit.putInt("com.matthiasko.scrollforreddit.REFRESH_COUNTER", refreshCounter);
        edit.commit();

        return true;
    }

    @Override
    protected void onPostExecute(Boolean isSubreddit) {
        super.onPostExecute(isSubreddit);
        if (!isSubreddit) {
            System.out.println("SUBREDDIT NOT FOUND");
            mListener.onSubredditNotFound();
        } else {
            System.out.println("SUBREDDIT FOUND");
            // hide the spinner in PostListActivity
            mListener.onUserlessTokenFetched();
        }
    }
}
