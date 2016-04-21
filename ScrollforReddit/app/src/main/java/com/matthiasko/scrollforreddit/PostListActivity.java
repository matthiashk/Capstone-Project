package com.matthiasko.scrollforreddit;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

    private static final String LOG_TAG = "MainActivity";

    private static final String CLIENT_ID = "cAizcZuXu-Mn9w";

    private static final String REDIRECT_URL = "http://scroll-for-reddit.com/oauthresponse";

    private PostsAdapter adapter;

    //private ArrayList<Post> arrayOfPosts;

    DBHandler handler;

    static final int LOGIN_REQUEST = 1;

    private static final String DATABASE_NAME = "posts.db";


    private static final int CURSOR_LOADER_ID = 0;

    private Cursor cursor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);

        handler = new DBHandler(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

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


        final RedditClient redditClient = new AndroidRedditClient(this);

        RefreshTokenHandler handler = new RefreshTokenHandler(new AndroidTokenStore(this), redditClient);

        AuthenticationManager.get().init(redditClient, handler);

        // check the authentication state of user
        AuthenticationState authState =  AuthenticationManager.get().checkAuthState();

        switch (authState) {

            case NONE:
                System.out.println("NONE");

                // load webview activity to login and authenticate user
                Intent intent = new Intent(this, LoginWebViewActivity.class);
                //startActivity(intent);
                startActivityForResult(intent, LOGIN_REQUEST);

                break;

            case NEED_REFRESH:
                System.out.println("NEED_REFRESH");

                // get the token from shared prefs using store
                AndroidTokenStore store = new AndroidTokenStore(this);

                try {

                    String refreshToken = store.readToken("EXAMPLE_KEY");
                    new RefreshTokenAsync().execute(refreshToken);

                } catch (NoSuchTokenException e) {

                    Log.e(LOG_TAG, e.getMessage());
                }

                break;

            case READY:
                System.out.println("READY");

                break;
        }

        //arrayOfPosts = new ArrayList<>();


        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        adapter = new PostsAdapter(this, null);


        View recyclerView = findViewById(R.id.post_list);
        assert recyclerView != null;
        setupRecyclerView((RecyclerView) recyclerView);

        //RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST);
        //recyclerView.addItemDecoration(itemDecoration);

        if (findViewById(R.id.post_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

    }


    public void onVote (String postId, long id) {

        //System.out.println("VOTE UP");

        new VoteAsyncTask().execute(postId, String.valueOf(id));


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_post_list, menu);
        return true;


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_refresh) {

            refreshPosts();

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == LOGIN_REQUEST) {

            if (resultCode == RESULT_OK) {

                //String value = data.getStringExtra("MY_KEY");

                //System.out.println("value = " + value);

                // coming from loginwebviewactivity after logging in
                // fetch posts

                AndroidTokenStore store = new AndroidTokenStore(this);

                try {

                    String refreshToken = store.readToken("EXAMPLE_KEY");
                    new RefreshTokenAsync().execute(refreshToken);

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
        // first, clear the array for the recyclerview
        //arrayOfPosts.clear();

        adapter.notifyDataSetChanged();

        // show loader
        findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);

        //delete database
        getApplicationContext().deleteDatabase(DATABASE_NAME);

        // get updated list
        AndroidTokenStore store = new AndroidTokenStore(this);

        try {

            String refreshToken = store.readToken("EXAMPLE_KEY");
            new RefreshTokenAsync().execute(refreshToken);

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

            // TODO: check here if database exists, if yes we need to add to existing database
            if (handler.getPostCount() == 0) {

                System.out.println("POST COUNT IS 0");

                SubredditPaginator paginator = new SubredditPaginator(redditClient);
                Listing<Submission> submissions = paginator.next();

                for (Submission submission : submissions) {

                    String title = submission.getTitle();
                    //System.out.println("title = " + title);

                    // store fullname so we can get the specific post later...

                    //System.out.println("submission.getFullName() = " + submission.getFullName());

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

                    //System.out.println("postId = " + postId);

                    // we should process comments in the adapter?
                    // loading them here takes too long, hangs the UI

                    //CommentNode commentNode = submission.getComments();

                    //System.out.println("commentNode.getTotalSize() = " + commentNode.getTotalSize());

                    //Submission fullSubmissionData = redditClient.getSubmission(submission.getId());
                    //System.out.println(fullSubmissionData.getTitle());
                    //System.out.println(fullSubmissionData.getComments());

                    //CommentNode commentNode = fullSubmissionData.getComments();

                    /*
                    String commentAuthor = commentNode.getComment().getAuthor();
                    int commentPoints = commentNode.getComment().getScore();
                    Date commentTime = commentNode.getComment().getCreated();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(commentTime.toString(), Locale.US);
                    String commentText = commentNode.getComment().getBody();
                    */




                    //Post post = new Post(title, subreddit, username, source, thumbnail, points, numberOfComments, postId, domain, fullName);

                    // add the post to the database
                    //handler.addPost(post);

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

                System.out.println("POST COUNT IS NOT 0");

                //arrayOfPosts.addAll(handler.getAllPosts());

                //System.out.println("arrayOfPosts.size() = " + arrayOfPosts.size());

            }




            //System.out.println("arrayOfPosts.size() = " + arrayOfPosts.size());

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            // we need this to update the adapter on the main thread
            adapter.notifyDataSetChanged();

            // hide the loading animation
            findViewById(R.id.loadingPanel).setVisibility(View.GONE);
        }
    }



    private class VoteAsyncTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {

            // we need to check authentication to get submission info and vote

            final RedditClient redditClient = new AndroidRedditClient(PostListActivity.this);

            final OAuthHelper oAuthHelper = redditClient.getOAuthHelper();

            final Credentials credentials = Credentials.installedApp(CLIENT_ID, REDIRECT_URL);

            RefreshTokenHandler handler = new RefreshTokenHandler(new AndroidTokenStore(PostListActivity.this), redditClient);

            AuthenticationManager.get().init(redditClient, handler);

            // check the authentication state of user
            AuthenticationState authState =  AuthenticationManager.get().checkAuthState();

            System.out.println("authState = " + authState.toString());

            AndroidTokenStore store = new AndroidTokenStore(PostListActivity.this);

            try {

                String refreshToken = store.readToken("EXAMPLE_KEY");
                //new RefreshTokenAsync().execute(refreshToken);

                oAuthHelper.setRefreshToken(refreshToken);

                try {

                    OAuthData finalData = oAuthHelper.refreshToken(credentials);

                    redditClient.authenticate(finalData);

                    String postId = params[0];

                    //System.out.println("fullName = " + fullName);
                    // we crop the prefix from fullName, since we only need id
                    //StringBuilder cropped = new StringBuilder(fullName);
                    //cropped.delete(0, 3);
                    //System.out.println("cropped.toString() = " + cropped.toString());

                    AccountManager accountManager = new AccountManager(redditClient);

                    Submission submission = redditClient.getSubmission(postId);

                    int score = submission.getScore();

                    System.out.println("score = " + score);

                    try {
                        accountManager.vote(submission, VoteDirection.UPVOTE);
                    }
                    catch (ApiException e) {
                        e.printStackTrace();
                    }

                    // update post in database

                    long id = Long.valueOf(params[1]);

                    ContentValues values = new ContentValues();
                    values.put(PostContract.PostEntry.COLUMN_SCORE, score + 1);

                    //System.out.println("PostContract.PostEntry.CONTENT_URI.toString() = " + PostContract.PostEntry.CONTENT_URI.toString());

                    getContentResolver().update(PostContract.PostEntry.CONTENT_URI, values,
                            PostContract.PostEntry._ID + "=?", new String[]{String.valueOf(id)});

                    //TODO: update UI to show new post score


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
