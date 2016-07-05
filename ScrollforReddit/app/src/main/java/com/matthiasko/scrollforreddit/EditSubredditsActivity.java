package com.matthiasko.scrollforreddit;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import net.dean.jraw.RedditClient;
import net.dean.jraw.android.AndroidRedditClient;
import net.dean.jraw.android.AndroidTokenStore;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.NoSuchTokenException;
import net.dean.jraw.auth.RefreshTokenHandler;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.http.oauth.OAuthHelper;
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.models.Subreddit;
import net.dean.jraw.paginators.UserSubredditsPaginator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by matthiasko on 7/3/16.
 */
public class EditSubredditsActivity extends AppCompatActivity {

    private ArrayAdapter<String> mItemsAdapter;

    private List<String> mItems = new ArrayList<>();
    private static final String LOG_TAG = EditSubredditsActivity.class.getSimpleName();

    private SharedPreferences mAppSharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_subreddits_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        ListView lv = (ListView) findViewById(R.id.lv);
        setSupportActionBar(toolbar);

        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Edit Subreddits");
        }


        /*
        // load subreddits from sharedprefs
        mAppSharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        if (mAppSharedPrefs.contains("com.matthiasko.scrollforreddit.USERSUBREDDITS")) {
            // load from sharedprefs
            Set<String> set = mAppSharedPrefs.getStringSet("com.matthiasko.scrollforreddit.USERSUBREDDITS", null);
            List<String> userSubredditsList = new ArrayList<>(set);
            // sort the list alphabetically
            Collections.sort(userSubredditsList, String.CASE_INSENSITIVE_ORDER);
            for (String item : userSubredditsList) { // create the menu mItems based on arraylist
                mItems.add(item);
            }
        }
        */

        mItemsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mItems);
        lv.setAdapter(mItemsAdapter);
        lv.setDividerHeight(1);
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int pos, long id) {

                final String selectedItem = mItems.get(pos);

                new AlertDialog.Builder(EditSubredditsActivity.this)
                        .setTitle("Unsubscribe from Subreddit")
                        .setMessage("Are you sure you want to unsubscribe from this Subreddit?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                new UnsubscribeAsyncTask().execute(selectedItem);
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
                return true;
            }
        });

        // TODO: fetch subreddit names
        new RefetchSubsAsyncTask(EditSubredditsActivity.this).execute();


    }

    private class UnsubscribeAsyncTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {

            final String CLIENT_ID = "cAizcZuXu-Mn9w";

            final String REDIRECT_URL = "http://scroll-for-reddit.com/oauthresponse";

            // we need to check authentication to get submission info and vote
            final RedditClient redditClient = new AndroidRedditClient(EditSubredditsActivity.this);

            final OAuthHelper oAuthHelper = redditClient.getOAuthHelper();

            final Credentials credentials = Credentials.installedApp(CLIENT_ID, REDIRECT_URL);

            AndroidTokenStore store = new AndroidTokenStore(EditSubredditsActivity.this);

            try {
                String refreshToken = store.readToken("USER_TOKEN");

                oAuthHelper.setRefreshToken(refreshToken);

                try {

                    OAuthData finalData = oAuthHelper.refreshToken(credentials);

                    redditClient.authenticate(finalData);

                    String subredditName = params[0];

                    System.out.println("subredditName = " + subredditName);

                    AccountManager accountManager = new AccountManager(redditClient);

                    Subreddit subreddit = redditClient.getSubreddit(subredditName);

                    try {
                        accountManager.unsubscribe(subreddit);
                    } catch (NetworkException e) {
                        e.printStackTrace();
                    }

                } catch (OAuthException e) {
                    e.printStackTrace();
                }
            } catch (NoSuchTokenException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            // fetch new list and save to shared prefs
            new RefetchSubsAsyncTask(EditSubredditsActivity.this).execute();
        }
    }

    public class RefetchSubsAsyncTask extends AsyncTask<String, Void, ArrayList<String>> {

        private static final String CLIENT_ID = "cAizcZuXu-Mn9w";

        private static final String REDIRECT_URL = "http://scroll-for-reddit.com/oauthresponse";

        private Context mContext;

        public RefetchSubsAsyncTask(Context context) {
            mContext = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            findViewById(R.id.loadingPanel).bringToFront();
            // show loader
            findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
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

            // refresh listview
            mItems.clear();
            mItems.addAll(arrayList);
            mItemsAdapter.notifyDataSetChanged();

            findViewById(R.id.loadingPanel).setVisibility(View.GONE);

            // save to shared prefs
            mAppSharedPrefs = PreferenceManager
                    .getDefaultSharedPreferences(mContext);

            if (mAppSharedPrefs.contains("com.matthiasko.scrollforreddit.USERSUBREDDITS")) {
                SharedPreferences.Editor edit = mAppSharedPrefs.edit();
                Set<String> set = new HashSet<>();
                set.addAll(arrayList);
                edit.putStringSet("com.matthiasko.scrollforreddit.USERSUBREDDITS", set);
                edit.commit();
            }
        }
    }
}
