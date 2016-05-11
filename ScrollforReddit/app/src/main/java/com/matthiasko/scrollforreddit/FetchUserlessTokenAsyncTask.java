package com.matthiasko.scrollforreddit;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import net.dean.jraw.RedditClient;
import net.dean.jraw.android.AndroidRedditClient;
import net.dean.jraw.android.AndroidTokenStore;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.http.oauth.OAuthHelper;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.SubredditPaginator;

import java.util.UUID;

/**
 * Created by matthiasko on 4/23/16.
 */
public class FetchUserlessTokenAsyncTask extends AsyncTask<String, Void, Void> {

    private Context mContext;
    private final String LOG_TAG = FetchUserlessTokenAsyncTask.class.getSimpleName();

    private FetchUserlessTokenListener mListener;

    private static final String CLIENT_ID = "cAizcZuXu-Mn9w";

    private DBHandler mHandler;

    private static RedditClient redditClient;

    private UUID mDeviceId;

    public FetchUserlessTokenAsyncTask(Context context, FetchUserlessTokenListener listener) {
        this.mContext = context;
        this.mListener = listener;
    }

    @Override
    protected Void doInBackground(String... params) {

        // check if uuid is in sharedprefs before creating a new uuid
        SharedPreferences appSharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        if (appSharedPrefs.contains("com.matthiasko.scrollforreddit.UUID")) {
            // load from sharedprefs
            String uuidString = appSharedPrefs.getString("com.matthiasko.scrollforreddit.UUID", null);
            mDeviceId = UUID.fromString(uuidString);
        } else {
            // no UUID found in sharedPrefs, create new uuid
            mDeviceId = UUID.randomUUID();
        }

        // we need to store the uuid after we create it and reuse it
        // generate random uuid -> needed to request api access
        // store uuid in shared prefs as a string, we need to convert back to uuid to use
        SharedPreferences.Editor sharedPrefsEditor = appSharedPrefs.edit();
        sharedPrefsEditor.putString("com.matthiasko.scrollforreddit.UUID", mDeviceId.toString());;
        sharedPrefsEditor.commit();

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

        mHandler = new DBHandler(mContext);

        SubredditPaginator paginator;

        // check here if database exists, if yes we need to add to existing database
        if (mHandler.getPostCount() == 0) {

            Log.e(LOG_TAG, "POST COUNT IS 0");

            /*

            // TODO: re-enable...

            if (subredditMenuName.equals("Frontpage")) {

                paginator = new SubredditPaginator(redditClient);

            } else {

                paginator = new SubredditPaginator(redditClient, subredditMenuName);
            }
            */

            paginator = new SubredditPaginator(redditClient);

            Listing<Submission> submissions = paginator.next();

            for (Submission submission : submissions) {

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
        } else {
            System.out.println("PostListActivity - loading from database");
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        // hide the spinner in PostListActivity
        mListener.onUserlessTokenFetched();

        // store access token
        String accessToken = redditClient.getOAuthData().getAccessToken();

        AndroidTokenStore store = new AndroidTokenStore(mContext);

        store.writeToken("USERLESS_TOKEN", accessToken);
    }
}
