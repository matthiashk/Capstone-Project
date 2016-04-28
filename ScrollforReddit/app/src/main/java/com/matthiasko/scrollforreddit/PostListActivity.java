package com.matthiasko.scrollforreddit;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

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
import net.dean.jraw.models.VoteDirection;
import net.dean.jraw.paginators.SubredditPaginator;

import java.net.URI;
import java.net.URISyntaxException;

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

    private boolean mUserlessMode;

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
        toolbar.setTitle(getTitle());

        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setupNavigationView();
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
                System.out.println("NONE");
                // TODO: check if there are posts in database, if yes load posts
                // check for token
                // how often should we check?
                // why do we need to refresh token?
                //System.out.println("mHandler.getPostCount() = " + mHandler.getPostCount());

                mUserlessMode = true;

                // skip userless authentication if there are posts in database
                // cursor will load existing posts
                if (mHandler.getPostCount() == 0)

                new FetchUserlessTokenAsyncTask(this, new FetchUserlessTokenListener() {
                    @Override
                    public void onUserlessTokenFetched() {
                        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                    }
                }).execute();
                /* allow no user mode
                   make asynctask to fetch token
                   request token from https://www.reddit.com/api/v1/access_token
                   include grant_type=https://oauth.reddit.com/grants/installed_client&\device_id=DEVICE_ID in the POST request
                   user is client_id password is client_secret

                   should we not store results/posts when not logged in?
                */

                /* TODO: we no longer need this logic here, move it when the user requests to login to their account...
                // load webview activity to login and authenticate user
                Intent intent = new Intent(this, LoginWebViewActivity.class);
                //startActivity(intent);
                startActivityForResult(intent, LOGIN_REQUEST);
                */
                break;

            case NEED_REFRESH:
                System.out.println("NEED_REFRESH");
                // get the token from shared prefs using store
                AndroidTokenStore store = new AndroidTokenStore(this);

                try {
                    String refreshToken = store.readToken("EXAMPLE_KEY");
                    new RefreshTokenAsync().execute(refreshToken, "Frontpage"); // TODO: is this correct???
                } catch (NoSuchTokenException e) {
                    Log.e(LOG_TAG, e.getMessage());
                }
                break;

            case READY:
                System.out.println("READY");
                break;
        }

        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        adapter = new PostsAdapter(this, null, mUserlessMode);

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
        /* TEMP DISABLED
        // check first if the list is in shared prefs
        SharedPreferences appSharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        if (appSharedPrefs.contains("com.matthiasko.scrollforreddit.USERSUBREDDITS")) {
            // load from sharedprefs
            Set<String> set = appSharedPrefs.getStringSet("com.matthiasko.scrollforreddit.USERSUBREDDITS", null);
            List<String> userSubredditsList = new ArrayList<>(set);
            // sort the list alphabetically
            Collections.sort(userSubredditsList, String.CASE_INSENSITIVE_ORDER);

            Menu menu = navigationView.getMenu(); // get the default menu from xml
            if (userSubredditsList != null) {
                for (String item : userSubredditsList) { // create the menu items based on arraylist
                    menu.add(R.id.group1, Menu.NONE, 1, item);
                }
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
                    if (arrayList != null) {
                        for (String item : arrayList) { // create the menu items based on arraylist
                            menu.add(R.id.group1, Menu.NONE, 1, item);
                        }
                    }
                }
            });
            task.execute();
        }
        */
    }

    public void selectedNavMenuItem(CharSequence menuTitle) {
        if (menuTitle.equals("subreddits")) {
            // TODO: add edit subreddit button/action here
        } else if (menuTitle.equals("Settings")) {
            //Intent intent = new Intent(this, PreferencesActivity.class);
            //startActivityForResult(intent, UPDATE_THEME);
        } else {
            // show loader
            findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);

            mSelectedSubredditName = menuTitle.toString();

            DBHandler dbHandler = new DBHandler(this);
            SQLiteDatabase db = dbHandler.getWritableDatabase();
            db.execSQL("delete from "+ PostEntry.TABLE_NAME);

            mRecyclerView.setVisibility(View.GONE);

            // read token
            AndroidTokenStore store = new AndroidTokenStore(this);

            try {
                String refreshToken = store.readToken("EXAMPLE_KEY");
                // get updated list
                new RefreshTokenAsync().execute(refreshToken, menuTitle.toString());
            } catch (NoSuchTokenException e) {
                Log.e(LOG_TAG, e.getMessage());
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
        new VoteAsyncTask().execute(postId, String.valueOf(id), voteDirection);
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
        if (requestCode == LOGIN_REQUEST) {
            if (resultCode == RESULT_OK) {
                // coming from loginwebviewactivity after logging in
                // fetch posts
                AndroidTokenStore store = new AndroidTokenStore(this);

                try {
                    String refreshToken = store.readToken("EXAMPLE_KEY");
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

        // get updated list
        AndroidTokenStore store = new AndroidTokenStore(this);

        try {
            String refreshToken = store.readToken("EXAMPLE_KEY");
            new RefreshTokenAsync().execute(refreshToken, mSelectedSubredditName);
        } catch (NoSuchTokenException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(adapter);
    }

    /*
        here we use RefreshTokenAsync to authenticate with our token in the background
        and update our UI in onPostExecute
    */
    private class RefreshTokenAsync extends AsyncTask<String, Void, Void> {

        final RedditClient redditClient = new AndroidRedditClient(PostListActivity.this);

        final OAuthHelper oAuthHelper = redditClient.getOAuthHelper();

        final Credentials credentials = Credentials.installedApp(CLIENT_ID, REDIRECT_URL);

        @Override
        protected Void doInBackground(String... params) {

            String refreshToken = params[0];

            String subredditMenuName = params[1];

            SubredditPaginator paginator;

            oAuthHelper.setRefreshToken(refreshToken);

            try {
                OAuthData finalData = oAuthHelper.refreshToken(credentials);
                redditClient.authenticate(finalData);
                if (redditClient.isAuthenticated()) {
                    Log.v(LOG_TAG, "Authenticated");
                }
            } catch (OAuthException e) {
                e.printStackTrace();
            }

            // check here if database exists, if yes we need to add to existing database
            if (mHandler.getPostCount() == 0) {

                System.out.println("POST COUNT IS 0");

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
            } else {
                System.out.println("PostListActivity - loading from database");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // hide the loading animation
            findViewById(R.id.loadingPanel).setVisibility(View.GONE);
            // show the view after fetching new data
            mRecyclerView.setVisibility(View.VISIBLE);
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
                String refreshToken = store.readToken("EXAMPLE_KEY");

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

                    //TODO: disable future upvotes for this post

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