package com.matthiasko.scrollforreddit.asynctasks;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;

import com.matthiasko.scrollforreddit.data.DBHandler;
import com.matthiasko.scrollforreddit.data.PostContract;
import com.matthiasko.scrollforreddit.interfaces.FetchUserlessTokenListener;

import net.dean.jraw.RedditClient;
import net.dean.jraw.android.AndroidRedditClient;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.http.oauth.OAuthHelper;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Subreddit;
import net.dean.jraw.models.Thumbnails;
import net.dean.jraw.paginators.SubredditPaginator;
import net.dean.jraw.paginators.SubredditSearchPaginator;

import java.util.UUID;

/**
 * Created by matthiasko on 4/28/16.
 * AsyncTask to get posts for a user in userless mode
 *
 */
public class FetchUserlessPostsAsyncTask extends AsyncTask<String, Void, Boolean> {

    private Context mContext;
    private final String LOG_TAG = FetchUserlessTokenAsyncTask.class.getSimpleName();
    private FetchUserlessTokenListener mListener;
    private static final String CLIENT_ID = "cAizcZuXu-Mn9w";
    private UUID mDeviceId;

    public  FetchUserlessPostsAsyncTask(Context context, FetchUserlessTokenListener listener) {
        this.mContext = context;
        this.mListener = listener;
    }

    @Override
    protected Boolean doInBackground(String... params) {

        String subredditMenuName = params[0];

        // load deviceId here from sharedPrefs
        // get uuid from shared prefs
        SharedPreferences appSharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(mContext);

        if (appSharedPrefs.contains("com.matthiasko.scrollforreddit.UUID")) {
            String uuidString = appSharedPrefs.getString("com.matthiasko.scrollforreddit.UUID", null);
            mDeviceId = UUID.fromString(uuidString);
        }

        RedditClient redditClient = new AndroidRedditClient(mContext);

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

        // check here if the subreddit exists
        SubredditSearchPaginator subredditSearchPaginator =
                new SubredditSearchPaginator(redditClient, subredditMenuName);

        Listing<Subreddit> subreddits = subredditSearchPaginator.next();

        if (subreddits.size() == 0) { // subreddit not found
            // notify user, no subreddit found matching 'name'
            return false;
        } else {
            // remove all items from database, so new results will be shown
            DBHandler dbHandler = new DBHandler(mContext);
            SQLiteDatabase db = dbHandler.getWritableDatabase();
            db.execSQL("delete from "+ PostContract.PostEntry.TABLE_NAME);
            db.close();
        }

        SubredditPaginator paginator;

        if (subredditMenuName.equals("Frontpage")) {
            paginator = new SubredditPaginator(redditClient);
        } else {
            paginator = new SubredditPaginator(redditClient, subredditMenuName);
        }

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

                System.out.println("FETCHUSERLESSPOSTSASYNCTASK - images.length = " + images.length);

                for (Thumbnails.Image thumbnailUrl : images) {

                    System.out.println("thumbnailUrl = " + thumbnailUrl.getUrl().toString());
                }

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

            //System.out.println("FETCHUSERLESSPOSTSASYNCTASK - postId = " + postId);

            mContext.getContentResolver().insert(PostContract.PostEntry.CONTENT_URI, postValues);
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean isSubreddit) {
        super.onPostExecute(isSubreddit);
        if (!isSubreddit) {
            mListener.onSubredditNotFound();
        } else {
            // hide the spinner in PostListActivity
            mListener.onUserlessTokenFetched();
        }
    }
}
