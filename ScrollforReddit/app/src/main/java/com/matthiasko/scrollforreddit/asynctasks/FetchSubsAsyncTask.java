package com.matthiasko.scrollforreddit.asynctasks;

import android.content.Context;
import android.os.AsyncTask;

import com.matthiasko.scrollforreddit.interfaces.AsyncListener;

import net.dean.jraw.RedditClient;
import net.dean.jraw.android.AndroidRedditClient;
import net.dean.jraw.android.AndroidTokenStore;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.NoSuchTokenException;
import net.dean.jraw.auth.RefreshTokenHandler;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.http.oauth.OAuthHelper;
import net.dean.jraw.models.Subreddit;
import net.dean.jraw.paginators.UserSubredditsPaginator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matthiasko on 4/22/16.
 * AsyncTask to get subreddits that user is subscribed to
 *
 */
public class FetchSubsAsyncTask extends AsyncTask<String, Void, ArrayList<String>> {

    private static final String CLIENT_ID = "cAizcZuXu-Mn9w";
    private static final String REDIRECT_URL = "http://scroll-for-reddit.com/oauthresponse";
    private Context mContext;
    private AsyncListener mAsyncListener;

    public FetchSubsAsyncTask(Context context) {
        mContext = context;
    }

    public void setAsyncListener(AsyncListener asyncListener) {
        this.mAsyncListener = asyncListener;
    }

    @Override
    protected ArrayList<String> doInBackground(String... params) {

        RedditClient redditClient = new AndroidRedditClient(mContext);
        final OAuthHelper oAuthHelper = redditClient.getOAuthHelper();
        final Credentials credentials = Credentials.installedApp(CLIENT_ID, REDIRECT_URL);
        RefreshTokenHandler handler = new RefreshTokenHandler(new AndroidTokenStore(mContext), redditClient);
        AuthenticationManager.get().init(redditClient, handler);
        AndroidTokenStore store = new AndroidTokenStore(mContext);

        try {
            String refreshToken = store.readToken("USER_TOKEN");
            oAuthHelper.setRefreshToken(refreshToken);
            try {
                OAuthData finalData = oAuthHelper.refreshToken(credentials);
                redditClient.authenticate(finalData);
                UserSubredditsPaginator userSubredditsPaginator = new UserSubredditsPaginator(redditClient, "subscriber");
                List<Subreddit> subreddits = userSubredditsPaginator.accumulateMergedAllSorted();
                ArrayList<String> subredditNames = new ArrayList<>();

                for (Subreddit subreddit : subreddits) {
                    subredditNames.add(subreddit.getDisplayName());
                }
                return subredditNames;

            } catch (OAuthException e) {
                e.printStackTrace();
            }
        } catch (NoSuchTokenException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(ArrayList<String> arrayList) {
        super.onPostExecute(arrayList);
        if (mAsyncListener != null ) {
            mAsyncListener.createNavMenuItems(arrayList);
        }
    }
}
