package com.matthiasko.scrollforreddit;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.matthiasko.scrollforreddit.PostContract.PostEntry;

import net.dean.jraw.ApiException;
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
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Subreddit;
import net.dean.jraw.models.VoteDirection;
import net.dean.jraw.paginators.SubredditPaginator;
import net.dean.jraw.paginators.SubredditSearchPaginator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An activity representing a list of Posts. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link PostDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class PostListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    public static boolean mUserlessMode;

    private static final String LOG_TAG = PostListActivity.class.getSimpleName();

    private static final String CLIENT_ID = "cAizcZuXu-Mn9w";

    private static final String REDIRECT_URL = "http://scroll-for-reddit.com/oauthresponse";

    private PostsAdapter adapter;

    private DBHandler mHandler;

    static final int LOGIN_REQUEST = 1;

    private static final String DATABASE_NAME = "posts.db";

    private static final int CURSOR_LOADER_ID = 0;

    private Cursor cursor;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private RedditClient redditClient;

    private String mSelectedSubredditName;

    private View mRecyclerView;

    private ActionBar mActionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView = (NavigationView) findViewById(R.id.navigation);

        mHandler = new DBHandler(this);

        redditClient = new AndroidRedditClient(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //toolbar.setTitle(getTitle());

        mActionBar = getSupportActionBar();

        if (mActionBar != null) {
            mActionBar.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }

        /*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */
        RefreshTokenHandler handler = new RefreshTokenHandler(new AndroidTokenStore(this), redditClient);
        AuthenticationManager.get().init(redditClient, handler);

        // check the authentication state of user
        AuthenticationState authState =  AuthenticationManager.get().checkAuthState();

        switch (authState) {
            case NONE:
                System.out.println("NO CREDENTIALS, GETTING USERLESS AUTHENTICATION");

                mUserlessMode = true;

                // skip userless authentication if there are posts in database
                // cursor will load existing posts
                if (mHandler.getPostCount() == 0)

                new FetchUserlessTokenAsyncTask(this, new FetchUserlessTokenListener() {
                    @Override
                    public void onUserlessTokenFetched() {
                        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                    }
                    @Override
                    public void onSubredditNotFound() {}
                }).execute();
                break;

            case NEED_REFRESH:
                System.out.println("NEED_REFRESH"); // only for logged in mode, userless mode does not get a refresh token
                // get the token from shared prefs using store

                mUserlessMode = false;

                AndroidTokenStore store = new AndroidTokenStore(this);

                try {
                    String refreshToken = store.readToken("USER_TOKEN");
                    new RefreshTokenAsync().execute(refreshToken, "Frontpage");
                } catch (NoSuchTokenException e) {
                    Log.e(LOG_TAG, e.getMessage());
                }
                break;

            case READY:
                System.out.println("READY");
                break;
        }

        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        adapter = new PostsAdapter(this, null);

        mRecyclerView = findViewById(R.id.post_list);
        assert mRecyclerView != null;
        setupRecyclerView((RecyclerView) mRecyclerView);

        //RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST);
        //mRecyclerView.addItemDecoration(itemDecoration);

        if (findViewById(R.id.post_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }
        // check if the spinner is visible
        if (mHandler.getPostCount() > 0) {
            findViewById(R.id.loadingPanel).setVisibility(View.GONE);
        }
        setupNavigationView();

        // get and set selected subreddit menu item if it exists
        SharedPreferences appSharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        if (appSharedPrefs.contains("com.matthiasko.scrollforreddit.SELECTED_SUBREDDIT")) {
            // load from sharedprefs
            mSelectedSubredditName = appSharedPrefs.getString("com.matthiasko.scrollforreddit.SELECTED_SUBREDDIT", null);
            mActionBar.setTitle("r/" + mSelectedSubredditName);
        }
    }

    private void setupNavigationView() {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // dont highlight 'subreddits', only allow highlight on other menu items
                        if (menuItem.getTitle().equals("subreddits")) {
                            menuItem.setCheckable(false);
                            menuItem.setChecked(false);
                        } else {
                            menuItem.setCheckable(true);
                            menuItem.setChecked(true);
                        }
                        /*

                        if (mPreviousMenuItem != null) {
                            mPreviousMenuItem.setChecked(false);
                        }
                        mPreviousMenuItem = menuItem;
                        */
                        drawerLayout.closeDrawers();
                        selectedNavMenuItem(menuItem.getTitle());
                        return true;
                    }
                });

        SharedPreferences appSharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        // check if we are in userless mode
        if (mUserlessMode) {
            // navigation menu logic setup when in 'userless' mode
            if (appSharedPrefs.contains("com.matthiasko.scrollforreddit.USERLESS_SUBREDDITS")) {
                // load from sharedprefs
                Set<String> set = appSharedPrefs.getStringSet("com.matthiasko.scrollforreddit.USERLESS_SUBREDDITS", null);
                List<String> userSubredditsList = new ArrayList<>(set);
                // sort the list alphabetically
                Collections.sort(userSubredditsList, String.CASE_INSENSITIVE_ORDER);
                Menu menu = navigationView.getMenu(); // get the default menu from xml
                for (String item : userSubredditsList) { // create the menu items based on arraylist
                    menu.add(R.id.group1, Menu.NONE, 1, item);
                }
            } else {
                // populate menu programatically based on user subreddits
                // get arraylist of subreddits
                FetchUserlessSubsAsyncTask task = new FetchUserlessSubsAsyncTask(this);
                task.setAsyncListener(new AsyncListener() {
                    @Override
                    public void createNavMenuItems(ArrayList<String> arrayList) {
                        // put list into sharedprefs
                        SharedPreferences appSharedPrefs = PreferenceManager
                                .getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor edit = appSharedPrefs.edit();
                        Set<String> set = new HashSet<>();
                        set.addAll(arrayList);
                        edit.putStringSet("com.matthiasko.scrollforreddit.USERLESS_SUBREDDITS", set);
                        edit.commit();
                        Menu menu = navigationView.getMenu(); // get the default menu from xml
                        for (String item : arrayList) { // create the menu items based on arraylist
                            menu.add(R.id.group1, Menu.NONE, 1, item);
                        }
                    }
                });
                task.execute();
            }
        // navigation menu logic setup when logged in
        } else if (appSharedPrefs.contains("com.matthiasko.scrollforreddit.USERSUBREDDITS")) {
            // load from sharedprefs
            Set<String> set = appSharedPrefs.getStringSet("com.matthiasko.scrollforreddit.USERSUBREDDITS", null);
            List<String> userSubredditsList = new ArrayList<>(set);
            // sort the list alphabetically
            Collections.sort(userSubredditsList, String.CASE_INSENSITIVE_ORDER);
            Menu menu = navigationView.getMenu(); // get the default menu from xml
            for (String item : userSubredditsList) { // create the menu items based on arraylist
                menu.add(R.id.group1, Menu.NONE, 1, item);
            }
        } else {
            // populate menu programatically based on user subreddits
            // get arraylist of subreddits
            FetchSubsAsyncTask task = new FetchSubsAsyncTask(this);

            task.setAsyncListener(new AsyncListener() {
                @Override
                public void createNavMenuItems(ArrayList<String> arrayList) {
                    // put list into sharedprefs
                    SharedPreferences appSharedPrefs = PreferenceManager
                            .getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor edit = appSharedPrefs.edit();

                    Set<String> set = new HashSet<>();
                    set.addAll(arrayList);
                    edit.putStringSet("com.matthiasko.scrollforreddit.USERSUBREDDITS", set);
                    edit.commit();

                    Menu menu = navigationView.getMenu(); // get the default menu from xml
                    for (String item : arrayList) { // create the menu items based on arraylist
                        menu.add(R.id.group1, Menu.NONE, 1, item);
                    }
                }
            });
            task.execute();
        }
    }

    public void selectedNavMenuItem(CharSequence menuTitle) {
        if (menuTitle.equals("Subreddits")) {

            // show keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

            LayoutInflater inflater = getLayoutInflater();
            final View dialogLayout = inflater.inflate(R.layout.nav_menu_subreddit, null);

            final EditText editText = (EditText) dialogLayout.findViewById(R.id.subreddit_edittext);
            editText.requestFocus();
            editText.setSingleLine();
            editText.setSelection(editText.getText().length());
            editText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                    this);

            alertDialogBuilder
                    .setView(dialogLayout)
                    .setMessage("subreddit")
                    .setCancelable(false)
                    .setPositiveButton("OK",new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int id) {
                            // show loader
                            findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);

                            mRecyclerView.setVisibility(View.GONE);

                            if (mUserlessMode) {
                                new FetchUserlessPostsAsyncTask(getApplicationContext(), new FetchUserlessTokenListener() {
                                    @Override
                                    public void onUserlessTokenFetched() {
                                        // this block only runs if the subreddit exists

                                        // remove all items from database
                                        DBHandler dbHandler = new DBHandler(getApplicationContext());
                                        SQLiteDatabase db = dbHandler.getWritableDatabase();
                                        db.execSQL("delete from "+ PostEntry.TABLE_NAME);

                                        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                                        mRecyclerView.setVisibility(View.VISIBLE);

                                        mSelectedSubredditName = editText.getText().toString();
                                        mActionBar.setTitle("r/" + mSelectedSubredditName);

                                        // save selected menu item to prefs so we can load later, on start, etc.
                                        SharedPreferences appSharedPrefs = PreferenceManager
                                                .getDefaultSharedPreferences(getApplicationContext());
                                        SharedPreferences.Editor edit = appSharedPrefs.edit();
                                        edit.putString("com.matthiasko.scrollforreddit.SELECTED_SUBREDDIT", mSelectedSubredditName);
                                        edit.commit();
                                    }
                                    @Override
                                    public void onSubredditNotFound() {
                                    // this block only runs if the subreddit does not exist
                                        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                                        mRecyclerView.setVisibility(View.VISIBLE);

                                        new AlertDialog.Builder(PostListActivity.this)
                                                .setTitle("Subreddit not found")
                                                .setMessage("Please try again.")
                                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int which) {
                                                    }
                                                })
                                                .setIcon(android.R.drawable.ic_dialog_alert)
                                                .show();
                                    }
                                }).execute(editText.getText().toString());
                            } else {
                                // read token
                                AndroidTokenStore store = new AndroidTokenStore(getApplicationContext());
                                try {
                                    String refreshToken = store.readToken("USER_TOKEN");
                                    new RefreshTokenAsync().execute(refreshToken, editText.getText().toString());
                                } catch (NoSuchTokenException e) {
                                    Log.e(LOG_TAG, e.getMessage());
                                }
                            }
                            // dismiss keyboard
                            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                        }
                    })
                    .setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int id) {
                            // dismiss keyboard
                            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                            // close dialog
                            dialog.cancel();
                        }
                    });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();

        } else if (menuTitle.equals("Settings")) {
            //Intent intent = new Intent(this, PreferencesActivity.class);
            //startActivityForResult(intent, UPDATE_THEME);
        } else if (menuTitle.equals("Accounts")) {

            // load webview activity to login and authenticate user
            Intent intent = new Intent(this, LoginWebViewActivity.class);
            //startActivity(intent);
            startActivityForResult(intent, LOGIN_REQUEST);

            // show spinner and hide list
            findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);

        } else { // this should match the subreddit names
            // show loader
            findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);

            mSelectedSubredditName = menuTitle.toString();

            mActionBar.setTitle("r/" + mSelectedSubredditName);

            // save selected menu item to prefs so we can load later, on start, etc.
            SharedPreferences appSharedPrefs = PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor edit = appSharedPrefs.edit();
            edit.putString("com.matthiasko.scrollforreddit.SELECTED_SUBREDDIT", mSelectedSubredditName);
            edit.commit();

            DBHandler dbHandler = new DBHandler(this);
            SQLiteDatabase db = dbHandler.getWritableDatabase();
            db.execSQL("delete from "+ PostEntry.TABLE_NAME);

            mRecyclerView.setVisibility(View.GONE);

            if (mUserlessMode) {
                new FetchUserlessPostsAsyncTask(this, new FetchUserlessTokenListener() {
                    @Override
                    public void onUserlessTokenFetched() {
                        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                        mRecyclerView.setVisibility(View.VISIBLE);
                    }
                    @Override
                    public void onSubredditNotFound() {}
                }).execute(mSelectedSubredditName);
            } else {
                // read token
                AndroidTokenStore store = new AndroidTokenStore(this);
                try {
                    String refreshToken = store.readToken("USER_TOKEN");
                    // get updated list
                    new RefreshTokenAsync().execute(refreshToken, menuTitle.toString());
                } catch (NoSuchTokenException e) {
                    Log.e(LOG_TAG, e.getMessage());
                }
            }
        }
        /*
        // show selected list
        DSLVFragment dragFragment = (DSLVFragment) getSupportFragmentManager()
                .findFragmentByTag("dslvTag");

        dragFragment.changeListAdapter(newTitle);

        // change title of toolbar to selected list
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {

            actionBar.setTitle(menuTitle);
        }
        */
    }

    public void onVote (String postId, long id, String voteDirection) {

        if (mUserlessMode) {
            new AlertDialog.Builder(PostListActivity.this)
                    .setTitle("Unable to vote")
                    .setMessage("Please login and try again.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else {
            new VoteAsyncTask().execute(postId, String.valueOf(id), voteDirection);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_post_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.action_refresh:
                refreshPosts();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // result from loginwebviewactivity here
        if (requestCode == LOGIN_REQUEST) {
            if (resultCode == RESULT_OK) {
                // coming from loginwebviewactivity after logging in
                // fetch posts
                AndroidTokenStore store = new AndroidTokenStore(this);
                try {
                    String refreshToken = store.readToken("USER_TOKEN");
                    new RefreshTokenAsync().execute(refreshToken, "Frontpage");
                } catch (NoSuchTokenException e) {
                    Log.e(LOG_TAG, e.getMessage());
                }

                // hide spinner
                findViewById(R.id.loadingPanel).setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
        //System.out.println("mSelectedSubredditName = " + mSelectedSubredditName);
    }

    private void refreshPosts() {
        // we need to remove posts from the database
        if (mSelectedSubredditName == null) {
            mSelectedSubredditName = "Frontpage";
        }
        // show loader
        findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);

        //delete database entries
        DBHandler dbHandler = new DBHandler(this);
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        db.execSQL("delete from "+ PostEntry.TABLE_NAME);
        mRecyclerView.setVisibility(View.GONE);

        if (mUserlessMode) {
            new FetchUserlessPostsAsyncTask(this, new FetchUserlessTokenListener() {
                @Override
                public void onUserlessTokenFetched() {
                    findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                    mRecyclerView.setVisibility(View.VISIBLE);
                }
                @Override
                public void onSubredditNotFound() {}
            }).execute(mSelectedSubredditName);
        } else {
            // get updated list
            AndroidTokenStore store = new AndroidTokenStore(this);
            try {
                String refreshToken = store.readToken("USER_TOKEN");
                new RefreshTokenAsync().execute(refreshToken, mSelectedSubredditName);
            } catch (NoSuchTokenException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(adapter);
    }

    /*
        here we use RefreshTokenAsync to authenticate with our token in the background
        and update our UI in onPostExecute
    */
    private class RefreshTokenAsync extends AsyncTask<String, Void, Boolean> {

        final RedditClient redditClient = new AndroidRedditClient(PostListActivity.this);

        final OAuthHelper oAuthHelper = redditClient.getOAuthHelper();

        final Credentials credentials = Credentials.installedApp(CLIENT_ID, REDIRECT_URL);

        @Override
        protected Boolean doInBackground(String... params) {

            String refreshToken = params[0];

            String subredditMenuName = params[1];

            SubredditPaginator paginator;

            oAuthHelper.setRefreshToken(refreshToken);

            try {
                OAuthData finalData = oAuthHelper.refreshToken(credentials);
                redditClient.authenticate(finalData);
                if (redditClient.isAuthenticated()) {
                    Log.v(LOG_TAG, "Authenticated");
                    // set to false, since user will no longer be in this mode
                    mUserlessMode = false;
                }
            } catch (OAuthException e) {
                e.printStackTrace();
            }

            // check here if the subreddit exists
            SubredditSearchPaginator subredditSearchPaginator =
                    new SubredditSearchPaginator(redditClient, subredditMenuName);

            Listing<Subreddit> subreddits = subredditSearchPaginator.next();

            //System.out.println("subreddits.size() = " + subreddits.size());

            if (subreddits.size() == 0) { // subreddit not found
                // notify user, no subreddit found matching 'name'
                return false;
            }

            // the user could have populated the database with posts using the 'userless' mode
            // so let's just clear the database here?
            // no need to check the count?
            //delete database entries
            DBHandler dbHandler = new DBHandler(PostListActivity.this);
            SQLiteDatabase db = dbHandler.getWritableDatabase();
            db.execSQL("delete from "+ PostEntry.TABLE_NAME);

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

                // shorten source url by extracting domain name and send as string
                String domain = "";

                try {
                    URI uri = new URI(source);
                    domain = uri.getHost();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }

                int points = submission.getScore();

                int numberOfComments = submission.getCommentCount();

                String thumbnail = submission.getThumbnail();

                // we need to add this to the post item data so we can retrieve the commentnode in details view
                String postId = submission.getId();

                String fullName = submission.getFullName();

                // add post data to database
                ContentValues postValues = new ContentValues();

                postValues.put(PostEntry.COLUMN_TITLE, title);
                postValues.put(PostEntry.COLUMN_SUBREDDIT, subreddit);
                postValues.put(PostEntry.COLUMN_AUTHOR, username);
                postValues.put(PostEntry.COLUMN_SOURCE, source);
                postValues.put(PostEntry.COLUMN_THUMBNAIL, thumbnail);
                postValues.put(PostEntry.COLUMN_SCORE, points);
                postValues.put(PostEntry.COLUMN_NUMBER_OF_COMMENTS, numberOfComments);
                postValues.put(PostEntry.COLUMN_POST_ID, postId);
                postValues.put(PostEntry.COLUMN_SOURCE_DOMAIN, domain);
                postValues.put(PostEntry.COLUMN_FULLNAME, fullName);

                getContentResolver().insert(PostEntry.CONTENT_URI, postValues);
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean isSubreddit) {
            super.onPostExecute(isSubreddit);
            if (!isSubreddit) {
                System.out.println("SUBREDDIT NOT FOUND");
                // hide the loading animation
                findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);

                new AlertDialog.Builder(PostListActivity.this)
                        .setTitle("Subreddit not found")
                        .setMessage("Please try again.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            } else {
                System.out.println("SUBREDDIT FOUND");
                // hide the loading animation
                findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                // show the view after fetching new data
                mRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private class VoteAsyncTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {

            // we need to check authentication to get submission info and vote
            final RedditClient redditClient = new AndroidRedditClient(PostListActivity.this);

            final OAuthHelper oAuthHelper = redditClient.getOAuthHelper();

            final Credentials credentials = Credentials.installedApp(CLIENT_ID, REDIRECT_URL);

            AndroidTokenStore store = new AndroidTokenStore(PostListActivity.this);

            try {
                String refreshToken = store.readToken("USER_TOKEN");

                oAuthHelper.setRefreshToken(refreshToken);

                try {

                    OAuthData finalData = oAuthHelper.refreshToken(credentials);

                    redditClient.authenticate(finalData);

                    String postId = params[0];

                    String voteDirection = params[2];

                    AccountManager accountManager = new AccountManager(redditClient);

                    Submission submission = redditClient.getSubmission(postId);

                    int score = submission.getScore();

                    try {
                        if (voteDirection.equals("up")) {
                            accountManager.vote(submission, VoteDirection.UPVOTE);
                        } else if (voteDirection.equals("down")) {
                            accountManager.vote(submission, VoteDirection.DOWNVOTE);
                        }
                    } catch (ApiException e) {
                        e.printStackTrace();
                    }

                    // update post in database
                    long id = Long.valueOf(params[1]);

                    ContentValues values = new ContentValues();
                    values.put(PostContract.PostEntry.COLUMN_SCORE, score + 1);

                    getContentResolver().update(PostContract.PostEntry.CONTENT_URI, values,
                            PostContract.PostEntry._ID + "=?", new String[]{String.valueOf(id)});
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
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args){
        return new CursorLoader(this, PostEntry.CONTENT_URI,
                new String[]{ PostEntry._ID, PostEntry.COLUMN_TITLE, PostEntry.COLUMN_SUBREDDIT,
                        PostEntry.COLUMN_AUTHOR, PostEntry.COLUMN_SOURCE, PostEntry.COLUMN_THUMBNAIL,
                        PostEntry.COLUMN_POST_ID, PostEntry.COLUMN_SOURCE_DOMAIN, PostEntry.COLUMN_FULLNAME,
                        PostEntry.COLUMN_SCORE, PostEntry.COLUMN_NUMBER_OF_COMMENTS},
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data){
        adapter.swapCursor(data);
        cursor = data;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader){
        adapter.swapCursor(null);
    }
}