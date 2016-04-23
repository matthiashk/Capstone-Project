package com.matthiasko.scrollforreddit;

import android.content.Context;
import android.os.AsyncTask;

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
import net.dean.jraw.models.Subreddit;
import net.dean.jraw.paginators.UserSubredditsPaginator;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by matthiasko on 4/22/16.
 */
public class FetchSubsAsyncTask extends AsyncTask<String, Void, ArrayList> {

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
    protected ArrayList doInBackground(String... params) {

        RedditClient redditClient = new AndroidRedditClient(mContext);

        final OAuthHelper oAuthHelper = redditClient.getOAuthHelper();

        final Credentials credentials = Credentials.installedApp(CLIENT_ID, REDIRECT_URL);

        RefreshTokenHandler handler = new RefreshTokenHandler(new AndroidTokenStore(mContext), redditClient);

        AuthenticationManager.get().init(redditClient, handler);

        // check the authentication state of user
        AuthenticationState authState =  AuthenticationManager.get().checkAuthState();

        AndroidTokenStore store = new AndroidTokenStore(mContext);

        try {
            String refreshToken = store.readToken("EXAMPLE_KEY");
            oAuthHelper.setRefreshToken(refreshToken);
            try {
                OAuthData finalData = oAuthHelper.refreshToken(credentials);

                redditClient.authenticate(finalData);

                UserSubredditsPaginator userSubredditsPaginator = new UserSubredditsPaginator(redditClient, "subscriber");

                Listing<Subreddit> subreddits = userSubredditsPaginator.next();

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
        } catch (NoSuchTokenException e) {
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
