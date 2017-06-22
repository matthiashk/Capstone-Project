package com.matthiasko.scrollforreddit.asynctasks;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;

import com.matthiasko.scrollforreddit.data.DBHandler;
import com.matthiasko.scrollforreddit.interfaces.FetchUserlessTokenListener;
import com.matthiasko.scrollforreddit.data.PostContract;

import net.dean.jraw.RedditClient;
import net.dean.jraw.android.AndroidRedditClient;
import net.dean.jraw.android.AndroidTokenStore;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.http.oauth.OAuthHelper;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Thumbnails;
import net.dean.jraw.paginators.SubredditPaginator;

import java.util.UUID;

/**
 * Created by matthiasko on 4/23/16.
 * AsyncTask to get access token for userless mode
 *
 */
public class FetchUserlessTokenAsyncTask extends AsyncTask<String, Void, Void> {

    private Context mContext;
    private final String LOG_TAG = FetchUserlessTokenAsyncTask.class.getSimpleName();
    private FetchUserlessTokenListener mListener;
    private static final String CLIENT_ID = "cAizcZuXu-Mn9w";
    private static RedditClient redditClient;

    public FetchUserlessTokenAsyncTask(Context context, FetchUserlessTokenListener listener) {
        this.mContext = context;
        this.mListener = listener;
    }

    @Override
    protected Void doInBackground(String... params) {

        UUID deviceId;
        // check if uuid is in sharedprefs before creating a new uuid
        SharedPreferences appSharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        if (appSharedPrefs.contains("com.matthiasko.scrollforreddit.UUID")) {
            // load from sharedprefs
            String uuidString = appSharedPrefs.getString("com.matthiasko.scrollforreddit.UUID", null);
            deviceId = UUID.fromString(uuidString);
        } else {
            // no UUID found in sharedPrefs, create new uuid
            deviceId = UUID.randomUUID();
        }

        // we need to store the uuid after we create it and reuse it
        // generate random uuid -> needed to request api access
        // store uuid in shared prefs as a string, we need to convert back to uuid to use
        SharedPreferences.Editor sharedPrefsEditor = appSharedPrefs.edit();
        sharedPrefsEditor.putString("com.matthiasko.scrollforreddit.UUID", deviceId.toString());;
        sharedPrefsEditor.commit();

        redditClient = new AndroidRedditClient(mContext);

        final OAuthHelper oAuthHelper = redditClient.getOAuthHelper();

        // note 'userlessApp' used here instead of 'installedApp'
        final Credentials credentials = Credentials.userlessApp(CLIENT_ID, deviceId);

        try {
            OAuthData finalData = oAuthHelper.easyAuth(credentials);
            redditClient.authenticate(finalData);
            if (redditClient.isAuthenticated()) {
                Log.v(LOG_TAG, "Authenticated");
            }
        } catch (OAuthException e) {
            e.printStackTrace();
        }

        DBHandler dbHandler = new DBHandler(mContext);

        // check here if database exists, if yes we need to add to existing database
        if (dbHandler.getPostCount() == 0) {

            SubredditPaginator paginator = new SubredditPaginator(redditClient);
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
                String thumbnail = "";
                String decodedUrl = "";

                // get the thumbnails object that contains an image array of urls, it may also be null
                Thumbnails thumbnails = submission.getThumbnails();

                if (thumbnails != null) {

                    Thumbnails.Image[] images = thumbnails.getVariations();

                    if (images.length >= 3) {
                        // we need to decode the url that is given
                        decodedUrl = Html.fromHtml(images[2].getUrl()).toString(); // get the third variation of the thumbnail
                    }
                }

                // use the default thumbnail if there is no higher quality version
                if (decodedUrl.isEmpty()) {
                    thumbnail = submission.getThumbnail();
                } else {
                    thumbnail = decodedUrl;
                }

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