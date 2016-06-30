package com.matthiasko.scrollforreddit;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.matthiasko.scrollforreddit.PostContract.PostEntry;

import net.dean.jraw.ApiException;
import net.dean.jraw.RedditClient;
import net.dean.jraw.android.AndroidRedditClient;
import net.dean.jraw.android.AndroidTokenStore;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.AuthenticationState;
import net.dean.jraw.auth.NoSuchTokenException;
import net.dean.jraw.auth.RefreshTokenHandler;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.http.oauth.OAuthHelper;
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.managers.CaptchaHelper;
import net.dean.jraw.models.Captcha;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Subreddit;
import net.dean.jraw.models.VoteDirection;
import net.dean.jraw.paginators.SubredditPaginator;
import net.dean.jraw.paginators.SubredditSearchPaginator;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PostListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private boolean mTwoPane;

    public static boolean userlessMode;

    private static final String LOG_TAG = PostListActivity.class.getSimpleName();

    private static final String CLIENT_ID = "cAizcZuXu-Mn9w";

    private static final String REDIRECT_URL = "http://scroll-for-reddit.com/oauthresponse";

    private PostsAdapter mPostsAdapter;

    private DBHandler mHandler;

    static final int LOGIN_REQUEST = 1;

    private static final String DATABASE_NAME = "posts.db";

    private static final String COMMENTS_DB_NAME = "comments.db";

    private static final int CURSOR_LOADER_ID = 0;

    private Cursor mCursor;

    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;

    private String mSelectedSubredditName;

    private View mRecyclerView;

    private ActionBar mActionBar;

    private Tracker mTracker;

    private CheckBox mSubscribeCheckBox;

    private String mScreenLayoutSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationView = (NavigationView) findViewById(R.id.navigation);

        mHandler = new DBHandler(this);

        RedditClient redditClient = new AndroidRedditClient(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //toolbar.setTitle(getTitle());

        mActionBar = getSupportActionBar();

        if (mActionBar != null) {
            mActionBar.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }

        // removed comments older than 12 hours from db
        String sql = "DELETE FROM comments WHERE date_added <= date('now','12 hours')";

        CommentsDBHandler dbHandler = new CommentsDBHandler(this);
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        db.execSQL(sql);
        db.close();

        // Obtain the shared Tracker instance.
        AnalyticsApplication analyticsApplication = (AnalyticsApplication) getApplication();
        mTracker = analyticsApplication.getDefaultTracker();

        // setup google mobile ads
        MobileAds.initialize(getApplicationContext(), "ca-app-pub-3940256099942544~3347511713");

        AdView mAdView = (AdView) findViewById(R.id.adView);
        //AdRequest adRequest = new AdRequest.Builder().build();

        AdRequest request = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)        // All emulators
                .addTestDevice("939A21019B69A9BB56EB6EF8C371161A")  // An example device ID
                .build();

        mAdView.loadAd(request);

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

        // how should we handle start up/credential checking?
        // should we save the mode in preferences?

        // check preferences before checking authentication state
        // if there is no saved usermode in preferences, create default of userlessmode
        // when the user logs in, change the preference to usermode
                SharedPreferences appSharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        if (appSharedPrefs.contains("com.matthiasko.scrollforreddit.USERLESS_MODE")) {

            userlessMode = appSharedPrefs.getBoolean("com.matthiasko.scrollforreddit.USERLESS_MODE", true);

        } else {

            userlessMode = true; // set default mode as userless mode

        }

        Log.e(LOG_TAG, "userlessMode = " + userlessMode);

        if (!userlessMode) { // if mUserless mode is false, we are in logged in mode, so check our credentials

            RefreshTokenHandler handler = new RefreshTokenHandler(new AndroidTokenStore(this), redditClient);
            AuthenticationManager.get().init(redditClient, handler);

            // check the authentication state of user
            AuthenticationState authState =  AuthenticationManager.get().checkAuthState();

            switch (authState) {
                case NONE:
                    System.out.println("NO CREDENTIALS, GETTING USERLESS AUTHENTICATION");

                    userlessMode = true;

                    // skip userless authentication if there are posts in database
                    // mCursor will load existing posts
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

                    userlessMode = false;

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

        } else {

            if (mHandler.getPostCount() == 0)

                new FetchUserlessTokenAsyncTask(this, new FetchUserlessTokenListener() {
                    @Override
                    public void onUserlessTokenFetched() {
                        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                    }
                    @Override
                    public void onSubredditNotFound() {}
                }).execute();
        }

        // detect screen size from
        // http://stackoverflow.com/questions/11252067/how-do-i-get-the-screensize-programmatically-in-android
        int screenSize = getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;

        switch(screenSize) {
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
                mScreenLayoutSize = "Large screen";
                break;
            case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                mScreenLayoutSize = "Normal screen";
                break;
            case Configuration.SCREENLAYOUT_SIZE_SMALL:
                mScreenLayoutSize = "Small screen";
                break;
            default:
                mScreenLayoutSize = "Screen size is neither large, normal or small";
        }

        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        mPostsAdapter = new PostsAdapter(this, null);

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

        /*
        // get and set selected subreddit menu item if it exists

        if (appSharedPrefs.contains("com.matthiasko.scrollforreddit.SELECTED_SUBREDDIT")) {
            // load from sharedprefs
            mSelectedSubredditName = appSharedPrefs.getString("com.matthiasko.scrollforreddit.SELECTED_SUBREDDIT", null);
            mActionBar.setTitle("r/" + mSelectedSubredditName);
        }
        */
    }

    private void setupNavigationView() {
        mNavigationView.setNavigationItemSelectedListener(
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

                        System.out.println("menuItem.getTitle() = " + menuItem.getTitle());
                        /*

                        if (mPreviousMenuItem != null) {
                            mPreviousMenuItem.setChecked(false);
                        }
                        mPreviousMenuItem = menuItem;
                        */
                        mDrawerLayout.closeDrawers();
                        selectedNavMenuItem(menuItem.getTitle());
                        return true;
                    }
                });

        SharedPreferences appSharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        View header = mNavigationView.getHeaderView(0);
        TextView navHeaderTextView = (TextView) header.findViewById(R.id.nav_header_textview);

        // check if we are in userless mode
        if (userlessMode) {

            navHeaderTextView.setText(R.string.nav_menu_title_userless);

            // navigation menu logic setup when in 'userless' mode
            if (appSharedPrefs.contains("com.matthiasko.scrollforreddit.USERLESS_SUBREDDITS")) {
                // load from sharedprefs
                Set<String> set = appSharedPrefs.getStringSet("com.matthiasko.scrollforreddit.USERLESS_SUBREDDITS", null);
                List<String> userSubredditsList = new ArrayList<>(set);
                // sort the list alphabetically
                Collections.sort(userSubredditsList, String.CASE_INSENSITIVE_ORDER);
                Menu menu = mNavigationView.getMenu(); // get the default menu from xml
                // clear previous menu
                menu.removeGroup(R.id.group1);
                menu.add(R.id.group1, R.id.inbox, 1, "Frontpage");
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
                        Menu menu = mNavigationView.getMenu(); // get the default menu from xml
                        menu.removeGroup(R.id.group1);
                        menu.add(R.id.group1, R.id.inbox, 1, "Frontpage");
                        for (String item : arrayList) { // create the menu items based on arraylist
                            menu.add(R.id.group1, Menu.NONE, 1, item);
                        }
                    }
                });
                task.execute();
            }
        // navigation menu logic setup when logged in
        } else if (appSharedPrefs.contains("com.matthiasko.scrollforreddit.USERSUBREDDITS")) {
            navHeaderTextView.setText(R.string.nav_menu_title_logged_in);
            // load from sharedprefs
            Set<String> set = appSharedPrefs.getStringSet("com.matthiasko.scrollforreddit.USERSUBREDDITS", null);
            List<String> userSubredditsList = new ArrayList<>(set);
            // sort the list alphabetically
            Collections.sort(userSubredditsList, String.CASE_INSENSITIVE_ORDER);
            Menu menu = mNavigationView.getMenu(); // get the default menu from xml
            menu.removeGroup(R.id.group1);
            menu.add(R.id.group1, R.id.inbox, 1, "Frontpage");
            for (String item : userSubredditsList) { // create the menu items based on arraylist
                menu.add(R.id.group1, Menu.NONE, 1, item);

                //System.out.println("item = " + item);
            }
        } else {
            navHeaderTextView.setText(R.string.nav_menu_title_logged_in);
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

                    Menu menu = mNavigationView.getMenu(); // get the default menu from xml
                    menu.removeGroup(R.id.group1);
                    menu.add(R.id.group1, R.id.inbox, 1, "Frontpage");
                    for (String item : arrayList) { // create the menu items based on arraylist
                        menu.add(R.id.group1, Menu.NONE, 1, item);

                        //System.out.println("item = " + item);
                    }
                }
            });
            task.execute();
        }
    }

    public void forceRefreshNavigationView() {
        mNavigationView.setNavigationItemSelectedListener(
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

                        System.out.println("menuItem.getTitle() = " + menuItem.getTitle());
                        /*

                        if (mPreviousMenuItem != null) {
                            mPreviousMenuItem.setChecked(false);
                        }
                        mPreviousMenuItem = menuItem;
                        */
                        mDrawerLayout.closeDrawers();
                        selectedNavMenuItem(menuItem.getTitle());
                        return true;
                    }
                });


        View header = mNavigationView.getHeaderView(0);
        TextView navHeaderTextView = (TextView) header.findViewById(R.id.nav_header_textview);
        navHeaderTextView.setText(R.string.nav_menu_title_logged_in);

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

                Menu menu = mNavigationView.getMenu(); // get the default menu from xml
                menu.removeGroup(R.id.group1);
                menu.add(R.id.group1, R.id.inbox, 1, "Frontpage");
                for (String item : arrayList) { // create the menu items based on arraylist
                    menu.add(R.id.group1, Menu.NONE, 1, item);
                }
            }
        });
        task.execute();
    }


    public void selectedNavMenuItem(CharSequence menuTitle) {

        if (menuTitle.equals("Change Subreddit")) {

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

            // setup checkbox to subscribe to subreddit, only if user is logged in
            if (!userlessMode) {

                mSubscribeCheckBox = (CheckBox) dialogLayout.findViewById(R.id.subreddit_checkbox);
                mSubscribeCheckBox.setVisibility(View.VISIBLE);

            }

            AlertDialog.Builder alertDialogBuilder =
                    new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);

            alertDialogBuilder
                    .setView(dialogLayout)
                    .setMessage("subreddit")
                    .setCancelable(false)
                    .setPositiveButton("OK",new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int id) {
                            // show loader
                            findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);

                            mRecyclerView.setVisibility(View.GONE);

                            if (userlessMode) {

                                new FetchUserlessPostsAsyncTask(getApplicationContext(), new FetchUserlessTokenListener() {
                                    @Override
                                    public void onUserlessTokenFetched() {
                                        // this block only runs if the subreddit exists
                                        // and this block will run after new posts are fetched

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

                                mSelectedSubredditName = editText.getText().toString();
                                mActionBar.setTitle("r/" + mSelectedSubredditName);

                                // TODO: implement
                                if (mSubscribeCheckBox.isChecked()) {

                                    new SubscribeAsyncTask().execute(mSelectedSubredditName);

                                }


                                // read token
                                AndroidTokenStore store = new AndroidTokenStore(getApplicationContext());
                                try {
                                    String refreshToken = store.readToken("USER_TOKEN");
                                    new RefreshTokenAsync().execute(refreshToken, mSelectedSubredditName);
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
        } else if (menuTitle.equals("Login")) {

            // load webview activity to login and authenticate user
            Intent intent = new Intent(this, LoginWebViewActivity.class);
            //startActivity(intent);
            startActivityForResult(intent, LOGIN_REQUEST);

        } else if (menuTitle.equals("Logout")) {

            // set userless mode to true
            userlessMode = true;

            // set userless_mode to true, since we are logged out now
            SharedPreferences appSharedPrefs = PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor edit = appSharedPrefs.edit();
            edit.putBoolean("com.matthiasko.scrollforreddit.USERLESS_MODE", true);
            edit.commit();

            // remove posts from database
            DBHandler dbHandler = new DBHandler(this);
            SQLiteDatabase db = dbHandler.getWritableDatabase();
            db.execSQL("delete from "+ PostEntry.TABLE_NAME);
            db.close();

            // fetch posts from frontpage using userlessmode
            new FetchUserlessTokenAsyncTask(this, new FetchUserlessTokenListener() {
                @Override
                public void onUserlessTokenFetched() {
                    findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                }
                @Override
                public void onSubredditNotFound() {}
            }).execute();

            // change subreddits in the navigation menu
            setupNavigationView();

            // notify user?

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

            if (userlessMode) {
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

        if (userlessMode) {
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
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.action_refresh:
                refreshPosts();
                return true;
            case R.id.action_get_more_posts:
                getMorePosts();
                return true;
            case R.id.action_submit_post:
                submitPost();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // result from loginwebviewactivity here
        if (requestCode == LOGIN_REQUEST) {
            if (resultCode == RESULT_OK) {

                userlessMode = false;

                //mNavigationView.getMenu().clear();
                setupNavigationView();

                // show spinner and hide list
                findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.GONE);

                // coming from loginwebviewactivity after logging in
                // fetch posts
                AndroidTokenStore store = new AndroidTokenStore(this);
                try {
                    String refreshToken = store.readToken("USER_TOKEN");
                    new RefreshTokenAsync().execute(refreshToken, "Frontpage");
                } catch (NoSuchTokenException e) {
                    Log.e(LOG_TAG, e.getMessage());
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);


        // google analytics code
        String name = "PostListActivity";
        mTracker.setScreenName(name);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());


        //System.out.println("mSelectedSubredditName = " + mSelectedSubredditName);
    }

    private void refreshPosts() {

        SharedPreferences appSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        int refreshCounter = 2; // set to 2 so we can get the next page of posts using fetchmoreposts

        SharedPreferences.Editor edit = appSharedPrefs.edit();
        edit.putInt("com.matthiasko.scrollforreddit.REFRESH_COUNTER", refreshCounter);
        edit.commit();

        // for google analytics
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction("Refresh Posts")
                .build());

        if (mSelectedSubredditName == null) {
            mSelectedSubredditName = "Frontpage";
        }
        findViewById(R.id.loadingPanel).bringToFront();
        // show loader
        findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);

        if (userlessMode) {
            // remove existing posts from db, and fetch new posts
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

    private void getMorePosts() {

        if (mSelectedSubredditName == null) {
            mSelectedSubredditName = "Frontpage";
        }
        findViewById(R.id.loadingPanel).bringToFront();
        // show loader
        findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);

        new FetchMorePostsAsyncTask(this, new FetchUserlessTokenListener() {
            @Override
            public void onUserlessTokenFetched() {
                findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
            }
            @Override
            public void onSubredditNotFound() {}
        }).execute(mSelectedSubredditName);
    }

    private void submitPost() {

        if (userlessMode) {

            new AlertDialog.Builder(this)
                    .setTitle("Error posting submission")
                    .setMessage("Please login to post a submission.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else {

            if (mSelectedSubredditName == null) {
                mSelectedSubredditName = "Frontpage";
            }

            // first try and submit post without captcha
            // if that fails, we need to get a captcha, show to user, and get user input
            // then resubmit post with captcha data

            // show text entry dialog
            // when user presses 'submit' then we call the asynctask
            LayoutInflater layoutInflater = LayoutInflater.from(this);
            View dialogView = layoutInflater.inflate(R.layout.edit_text_submit_post, null);

            AlertDialog.Builder alertDialogBuilder =
                    new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);

            alertDialogBuilder.setView(dialogView);

            final EditText title = (EditText) dialogView.findViewById(R.id.add_post_title_edit_text);
            final EditText input = (EditText) dialogView.findViewById(R.id.add_post_edit_text);

            alertDialogBuilder
                    .setTitle("Post Submission")
                    .setCancelable(false)
                    .setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {

                                    // TODO: if fields are blank, abort

                                    new PostSubmissionAsyncTask(
                                            title.getText().toString(),
                                            input.getText().toString(),
                                            null,
                                            null
                                    ).execute();
                                }
                            })
                    .setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    dialog.cancel();
                                }
                            });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(mPostsAdapter);

        // TODO: detect tablet mode and only use staggerd layout in tablet mode...

        if (mScreenLayoutSize.contains("Large")) {

            StaggeredGridLayoutManager gridLayoutManager = new StaggeredGridLayoutManager(2,1);

            recyclerView.setLayoutManager(gridLayoutManager);
        }


    }

    private class CaptchaAsyncTask extends AsyncTask<String, Void, Wrapper> {

        final RedditClient redditClient = new AndroidRedditClient(PostListActivity.this);

        final OAuthHelper oAuthHelper = redditClient.getOAuthHelper();

        final Credentials credentials = Credentials.installedApp(CLIENT_ID, REDIRECT_URL);

        AndroidTokenStore store = new AndroidTokenStore(PostListActivity.this);

        protected Wrapper doInBackground(String... params) {

            String title = params[0];

            String userInput = params[1];

            String selectedSubredditName = params[2];

            Wrapper wrapper = new Wrapper();

            wrapper.setTitle(title);
            wrapper.setUserInput(userInput);
            wrapper.setSelectedSubredditName(selectedSubredditName);

            try {
                String refreshToken = store.readToken("USER_TOKEN");
                oAuthHelper.setRefreshToken(refreshToken);

                try {
                    OAuthData finalData = oAuthHelper.refreshToken(credentials);
                    redditClient.authenticate(finalData);

                } catch (OAuthException e) {
                    e.printStackTrace();
                }
                CaptchaHelper captchaHelper = new CaptchaHelper(redditClient);

                if (captchaHelper.isNecessary()) {
                    Captcha captcha = captchaHelper.getNew();
                    //URL captchaUrl = mCaptcha.getImageUrl();
                    //wrapper.setCaptchaUrl(captchaUrl);
                    wrapper.setCaptcha(captcha);
                }
            } catch (NoSuchTokenException e) {
                e.printStackTrace();
            } catch (ApiException e) {
                e.printStackTrace();
            }
            return wrapper;
        }

        protected void onPostExecute(Wrapper wrapper) {

            final Captcha captcha = wrapper.getCaptcha();

            URL captchaUrl = captcha.getImageUrl();

            final String title = wrapper.getTitle();
            final String userInput = wrapper.getUserInput();
            String selectedSubredditName = wrapper.getSelectedSubredditName();

            if (captchaUrl != null) {

                LayoutInflater layoutInflater = LayoutInflater.from(PostListActivity.this);
                View dialogView = layoutInflater.inflate(R.layout.dialog_catpcha, null);

                AlertDialog.Builder alertDialogBuilder =
                        new AlertDialog.Builder(PostListActivity.this, R.style.AppCompatAlertDialogStyle);

                alertDialogBuilder.setView(dialogView);

                final EditText captchaText = (EditText) dialogView.findViewById(R.id.captcha_edit_text);

                ImageView captchaImageView = (ImageView) dialogView.findViewById(R.id.captcha_imageview);

                try {
                    new DownloadImageTask(captchaImageView).execute(captchaUrl.toString());
                } catch (NetworkException e) {
                    e.printStackTrace();
                }

                alertDialogBuilder
                        .setTitle("Captcha")
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {

                                        // TODO: if fields are blank, abort or show error

                                        new PostSubmissionAsyncTask(
                                                title,
                                                userInput,
                                                captchaText.getText().toString(),
                                                captcha
                                        ).execute();
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        dialog.cancel();
                                    }
                                });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        }
    }

    // DownloadImageTask is used to download the captcha image and create a bitmap to display
    // code is based on http://stackoverflow.com/questions/2471935/how-to-load-an-imageview-by-url-in-android
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

    public class PostSubmissionAsyncTask extends AsyncTask<String, Void, Boolean> {

        String title;
        String userInput;
        String captchaText;
        Captcha captcha;

        public PostSubmissionAsyncTask(String title, String userInput, String captchaText, Captcha captcha) {

            this.title = title;
            this.userInput = userInput;
            this.captchaText = captchaText;
            this.captcha = captcha;
        }

        @Override
        protected Boolean doInBackground(String... params) { // send postId and user comment text as var

            final RedditClient redditClient = new AndroidRedditClient(PostListActivity.this);

            final OAuthHelper oAuthHelper = redditClient.getOAuthHelper();

            final Credentials credentials = Credentials.installedApp(CLIENT_ID, REDIRECT_URL);

            AndroidTokenStore store = new AndroidTokenStore(PostListActivity.this);

            Boolean NEEDS_CAPTCHA = false;

            try {
                String refreshToken = store.readToken("USER_TOKEN");

                oAuthHelper.setRefreshToken(refreshToken);

                try {

                    OAuthData finalData = oAuthHelper.refreshToken(credentials);

                    redditClient.authenticate(finalData);

                    AccountManager accountManager = new AccountManager(redditClient);

                    if (captcha == null) {

                        try {
                            AccountManager.SubmissionBuilder submissionBuilder =
                                    new AccountManager.SubmissionBuilder(userInput, mSelectedSubredditName, title);
                            accountManager.submit(submissionBuilder);

                        } catch (ApiException e) {
                            if (e.getMessage().contains("BAD_CAPTCHA")) {
                                NEEDS_CAPTCHA = true;
                            }
                            Log.e(LOG_TAG, e.getMessage());
                        }

                    } else {

                        try {
                            AccountManager.SubmissionBuilder submissionBuilder =
                                    new AccountManager.SubmissionBuilder(userInput, mSelectedSubredditName, title);
                            accountManager.submit(submissionBuilder, captcha, captchaText);

                        } catch (ApiException e) {
                            if (e.getMessage().contains("BAD_CAPTCHA")) {
                                NEEDS_CAPTCHA = true;
                            }
                            Log.e(LOG_TAG, e.getMessage());
                        }
                    }

                } catch (OAuthException e) {
                    e.printStackTrace();
                }
            } catch (NoSuchTokenException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
            return NEEDS_CAPTCHA;
        }

        @Override
        protected void onPostExecute(Boolean NEEDS_CAPTCHA) {

            if (NEEDS_CAPTCHA) {
                new CaptchaAsyncTask().execute(title, userInput, mSelectedSubredditName);
            } else {
                // TODO: refresh posts here

                // swap mCursor???
            }
        }
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
                    userlessMode = false;
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

            } else {

                // the user could have populated the database with posts using the 'userless' mode
                // so let's just clear the database here?
                // no need to check the count?
                //delete database entries
                DBHandler dbHandler = new DBHandler(PostListActivity.this);
                SQLiteDatabase db = dbHandler.getWritableDatabase();
                db.execSQL("delete from "+ PostEntry.TABLE_NAME);
                db.close();
            }

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
                Log.e(LOG_TAG, "SUBREDDIT FOUND");
                // hide the loading animation
                findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                // show the view after fetching new data
                mRecyclerView.setVisibility(View.VISIBLE);

                // set userless_mode to false, since we are now logged in
                SharedPreferences appSharedPrefs = PreferenceManager
                        .getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor edit = appSharedPrefs.edit();
                edit.putBoolean("com.matthiasko.scrollforreddit.USERLESS_MODE", false);
                edit.commit();
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
            mPostsAdapter.notifyDataSetChanged();
        }
    }

    private class SubscribeAsyncTask extends AsyncTask<String, Void, Void> {

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

                    String subredditMenuName = params[0];

                    AccountManager accountManager = new AccountManager(redditClient);

                    Subreddit subreddit = redditClient.getSubreddit(subredditMenuName);

                    try {
                        accountManager.subscribe(subreddit);
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

            // refresh navigation menu
            forceRefreshNavigationView();
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
        mPostsAdapter.swapCursor(data);
        mCursor = data;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader){
        mPostsAdapter.swapCursor(null);
    }
}