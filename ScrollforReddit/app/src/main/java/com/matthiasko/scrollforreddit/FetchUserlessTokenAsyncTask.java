package com.matthiasko.scrollforreddit;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import net.dean.jraw.android.AndroidTokenStore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

/**
 * Created by matthiasko on 4/23/16.
 */
public class FetchUserlessTokenAsyncTask extends AsyncTask<String, Void, String> {

    private Context mContext;
    private final String LOG_TAG = FetchUserlessTokenAsyncTask.class.getSimpleName();
    private String mToken;

    private FetchUserlessTokenListener mListener;

    public FetchUserlessTokenAsyncTask(Context context, FetchUserlessTokenListener listener) {
        this.mContext = context;
        this.mListener = listener;
    }

    @Override
    protected String doInBackground(String... params) {
        /*
        request token from https://www.reddit.com/api/v1/access_token
        include grant_type=https://oauth.reddit.com/grants/installed_client&\device_id=DEVICE_ID in the POST request
        user is client_id password is blank
        */
        // generate random device_id
        String uuid = UUID.randomUUID().toString();

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        final String ACCESS_TOKEN = "access_token";

        try {
            final String REDDIT_API_BASE_URL = "https://www.reddit.com/api/v1/access_token";
            final String GRANT_TYPE = "grant_type";
            final String DEVICE_ID = "device_id";
            final String basicAuth = "Basic " + Base64.encodeToString("cAizcZuXu-Mn9w:".getBytes(), Base64.NO_WRAP);

            Uri builtUri = Uri.parse(REDDIT_API_BASE_URL)
                    .buildUpon()
                    .appendQueryParameter(GRANT_TYPE, "https://oauth.reddit.com/grants/installed_client")
                    .appendQueryParameter(DEVICE_ID, uuid)
                    .build();
            URL url = new URL(builtUri.toString());

            //System.out.println("builtUri = " + builtUri.toString());
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Authorization", basicAuth);
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            String response = buffer.toString();

            // get access_token from response data
            JSONTokener tokener = new JSONTokener(response);

            JSONObject oneObject = new JSONObject(tokener);

            // TODO: store token?
            mToken = oneObject.getString(ACCESS_TOKEN);

            // call api using the token we just recieved and ask for hot posts
            final String REDDIT_OAUTH_API_BASE_URL = "https://oauth.reddit.com/hot";
            final String bearer = "Bearer " + mToken;

            URL oauthUrl = new URL(REDDIT_OAUTH_API_BASE_URL);

            urlConnection = (HttpURLConnection) oauthUrl.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Authorization", bearer);
            urlConnection.connect();

            // Read the input stream into a String
            InputStream is = urlConnection.getInputStream();
            StringBuffer b = new StringBuffer();
            if (is == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(is));

            String l;
            while ((l = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                b.append(l + "\n");
            }

            if (b.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            String r = b.toString();

            System.out.println("r = " + r);

            JSONObject aResponse = new JSONObject(r);
            JSONObject data = aResponse.getJSONObject("data");
            JSONArray hotTopics = data.getJSONArray("children");

            for (int i = 0; i < hotTopics.length(); i++) {
                JSONObject topic = hotTopics.getJSONObject(i).getJSONObject("data");

                String title = topic.getString("title");
                String subreddit = topic.getString("subreddit");
                String author = topic.getString("author");
                String domain = topic.getString("domain");
                String source = topic.getString("url");// might be null...
                String score = topic.getString("score");
                String numberOfComments = topic.getString("num_comments");
                String thumbnail = topic.getString("thumbnail");
                String postId = topic.getString("id");
                String fullName = topic.getString("name");
                //String postTime = topic.getString("created_utc");

                // TODO: put results into database...

                ContentValues postValues = new ContentValues();

                postValues.put(PostContract.PostEntry.COLUMN_TITLE, title);
                postValues.put(PostContract.PostEntry.COLUMN_SUBREDDIT, subreddit);
                postValues.put(PostContract.PostEntry.COLUMN_AUTHOR, author);
                postValues.put(PostContract.PostEntry.COLUMN_SOURCE, source);
                postValues.put(PostContract.PostEntry.COLUMN_THUMBNAIL, thumbnail);
                postValues.put(PostContract.PostEntry.COLUMN_SCORE, score);
                postValues.put(PostContract.PostEntry.COLUMN_NUMBER_OF_COMMENTS, numberOfComments);
                postValues.put(PostContract.PostEntry.COLUMN_POST_ID, postId);
                postValues.put(PostContract.PostEntry.COLUMN_SOURCE_DOMAIN, domain);
                postValues.put(PostContract.PostEntry.COLUMN_FULLNAME, fullName);

                mContext.getContentResolver().insert(PostContract.PostEntry.CONTENT_URI, postValues);

                //System.out.println("title = " + title);
                //System.out.println("thumbnail = " + thumbnail);

                //topicdata.add(new ListData(title, author, imageUrl, postTime, rScore));
                //Log.v(DEBUG_TAG, topicdata.toString());
            }

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        return mToken;
    }

    @Override
    protected void onPostExecute(String token) {
        super.onPostExecute(token);
        mListener.onUserlessTokenFetched();

        // store access token
        AndroidTokenStore store = new AndroidTokenStore(mContext);
        store.writeToken("USERLESS_TOKEN", token);
    }
}
