package com.matthiasko.scrollforreddit;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.dean.jraw.RedditClient;
import net.dean.jraw.android.AndroidRedditClient;
import net.dean.jraw.android.AndroidTokenStore;
import net.dean.jraw.auth.NoSuchTokenException;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.http.oauth.OAuthHelper;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;

import java.util.ArrayList;
import java.util.UUID;

/**
 * A fragment representing a single Post detail screen.
 * This fragment is either contained in a {@link PostListActivity}
 * in two-pane mode (on tablets) or a {@link PostDetailActivity}
 * on handsets.
 */
public class PostDetailFragment extends Fragment {

    private boolean mUserlessMode;
    private String mPostTitle;
    private String mCommentAuthor;
    private String mPostId;
    private String mPostFullName;
    private static final String LOG_TAG = PostDetailFragment.class.getSimpleName();
    private static final String CLIENT_ID = "cAizcZuXu-Mn9w";
    private static final String REDIRECT_URL = "http://scroll-for-reddit.com/oauthresponse";
    private TextView mCommentTextView;
    private CommentsAdapter mCommentsAdapter;
    private ArrayList<ScrollComment> mArrayOfComments;
    private RedditClient mRedditClient;
    private UUID mDeviceId;
    private String mSelectedSubreddit;

    private CommentsDBHandler mCommentsDBHandler;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PostDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCommentsDBHandler = new CommentsDBHandler(getContext());

        mUserlessMode = PostListActivity.mUserlessMode;

        if (getArguments().containsKey("POST_TITLE")) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            //mItem = DummyContent.ITEM_MAP.get(getArguments().getString(ARG_ITEM_ID));
            //mItem.mPostTitle = getArguments().getString("POST_TITLE");

            mPostTitle = getArguments().getString("POST_TITLE");
            mPostId = getArguments().getString("POST_ID");
            mPostFullName = getArguments().getString("FULLNAME");
            //mUserlessMode = getArguments().getBoolean("USERLESS_MODE");

            //System.out.println("PostDetailFragment - mUserlessMode = " + mUserlessMode);
            //System.out.println("mPostFullName = " + mPostFullName);
            //System.out.println("mPostId = " + mPostId);
            //mCommentAuthor = getArguments().getString("COMMENT_AUTHOR");
            //System.out.println("mPostTitle = " + mPostTitle);
        }
        // fetch userless token if in userless mode
        if (mUserlessMode) {

            Log.e(LOG_TAG, "***** USERLESS MODE *****");

            mRedditClient = new AndroidRedditClient(getContext());
            // get access token
            AndroidTokenStore store = new AndroidTokenStore(getContext());

            try {
                String refreshToken = store.readToken("USERLESS_TOKEN");
                //System.out.println("refreshToken = " + refreshToken);
                new RetrieveUserlessComments().execute(refreshToken);
            } catch (NoSuchTokenException e) {
                Log.e(LOG_TAG, e.getMessage());
            }

        } else {
            Log.e(LOG_TAG, "***** NOT USERLESS MODE *****");
            // get access token
            AndroidTokenStore store = new AndroidTokenStore(getContext());

            try {
                String refreshToken = store.readToken("USER_TOKEN");
                new RetrieveComments().execute(refreshToken);
            } catch (NoSuchTokenException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.post_detail, container, false);

        RecyclerView commentsRecyclerView = (RecyclerView) rootView.findViewById(R.id.commentsList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        commentsRecyclerView.setLayoutManager(layoutManager);

        //LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        //llm.setOrientation(LinearLayoutManager.VERTICAL);
        //commentsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        mArrayOfComments = new ArrayList<>();
        // Create the mCommentsAdapter to convert the array to views
        mCommentsAdapter = new CommentsAdapter(getContext(), mArrayOfComments);

        //View commentsRecyclerView = rootView.findViewById(R.id.commentsList);

        //commentsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        //assert commentsRecyclerView != null;
        //setupRecyclerView((RecyclerView) commentsRecyclerView);

        commentsRecyclerView.setAdapter(mCommentsAdapter);

        //((TextView) rootView.findViewById(R.id.post_detail)).setText(mPostTitle);

        // setup our textview so we can set it in onpostexecute after getting comments

        //mCommentTextView = (TextView) rootView.findViewById(R.id.comment_author);

        //((TextView) rootView.findViewById(R.id.comment_author)).setText(mCommentAuthor);

        return rootView;
    }

    private class RetrieveComments extends AsyncTask<String, Void, Void> {

        final RedditClient redditClient = new AndroidRedditClient(getContext());

        final OAuthHelper oAuthHelper = redditClient.getOAuthHelper();

        final Credentials credentials = Credentials.installedApp(CLIENT_ID, REDIRECT_URL);

        String commentText;

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

            if (mCommentsDBHandler.getCommentsCount(mPostId) == 0) { // fetch comments

                Log.e(LOG_TAG, "comments count == 0");

                // use getSubmission instead of paginator to get the specific post + comments,
                // otherwise the post will not be found in the paginator after some time has passed
                Submission specificSubmission = redditClient.getSubmission(mPostId);

                CommentNode commentNode = specificSubmission.getComments();
                Iterable<CommentNode> iterable = commentNode.walkTree();

                // if depth is more than 5
                // create new cell with 'load more' label...
                // OR just limit the amount of comments fetched...

                for (CommentNode node : iterable) {
                    Comment comment = node.getComment();
                    ScrollComment scrollComment = new ScrollComment(comment.getBody(),
                            comment.getAuthor(), comment.getScore(), node.getDepth(), mPostId);
                    //System.out.println("comment.getBody() = " + comment.getBody());

                    mCommentsDBHandler.addComment(scrollComment); // adding to comments database

                    mArrayOfComments.add(scrollComment);
                }

            } else { // else load comments from database...
                // note: have to use addAll, not just set the arraylist
                mArrayOfComments.addAll(mCommentsDBHandler.getAllComments(mPostId));
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //System.out.println("mArrayOfComments.size() = " + mArrayOfComments.size());
            //System.out.println("string = " + s);
            //mCommentTextView.setText(s);
            mCommentsAdapter.notifyDataSetChanged();
            try {
                ((PostDetailActivity) getActivity()).postDetailSpinner();
            } catch (ClassCastException cce) {
                cce.printStackTrace();
            }
        }
    }

    public interface OnCommentsLoadedListener{
        public void postDetailSpinner();
    }

    private class RetrieveUserlessComments extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {

            // get uuid from shared prefs
            SharedPreferences appSharedPrefs = PreferenceManager
                    .getDefaultSharedPreferences(getContext());

            if (appSharedPrefs.contains("com.matthiasko.scrollforreddit.UUID")) {
                Log.e(LOG_TAG, "UUID FOUND IN PREFS");
                String uuidString = appSharedPrefs.getString("com.matthiasko.scrollforreddit.UUID", null);
                mDeviceId = UUID.fromString(uuidString);
            } else {
                Log.e(LOG_TAG, "UUID NOT FOUND IN PREFS, CREATING");
                mDeviceId = UUID.randomUUID();
            }

            if (appSharedPrefs.contains("com.matthiasko.scrollforreddit.SELECTED_SUBREDDIT")) {
                mSelectedSubreddit =
                        appSharedPrefs.getString("com.matthiasko.scrollforreddit.SELECTED_SUBREDDIT", null);
            } else {

                mSelectedSubreddit = "Frontpage";
            }

            // check authentication
            mRedditClient = new AndroidRedditClient(getContext());
            final OAuthHelper oAuthHelper = mRedditClient.getOAuthHelper();

            // note 'userlessApp' used here instead of 'installedApp'
            final Credentials credentials = Credentials.userlessApp(CLIENT_ID, mDeviceId);

            try {
                OAuthData finalData = oAuthHelper.easyAuth(credentials);
                mRedditClient.authenticate(finalData);
                if (mRedditClient.isAuthenticated()) {
                    Log.e(LOG_TAG, "PostDetailFragment - Authenticated");
                }
            } catch (OAuthException e) {
                e.printStackTrace();
            }

            // we need to check if comments are in the database here
            // check if the post id exists in the database
            // if yes, filter comments by post id

            if (mCommentsDBHandler.getCommentsCount(mPostId) == 0) { // fetch comments

                Log.e(LOG_TAG, "comments count == 0");

                // use getSubmission instead of paginator to get the specific post + comments,
                // otherwise the post will not be found in the paginator after some time has passed
                Submission specificSubmission = mRedditClient.getSubmission(mPostId);

                CommentNode commentNode = specificSubmission.getComments();
                Iterable<CommentNode> iterable = commentNode.walkTree();

                // if depth is more than 5
                // create new cell with 'load more' label...
                // OR just limit the amount of comments fetched...

                for (CommentNode node : iterable) {
                    Comment comment = node.getComment();
                    ScrollComment scrollComment = new ScrollComment(comment.getBody(),
                            comment.getAuthor(), comment.getScore(), node.getDepth(), mPostId);
                    //System.out.println("comment.getBody() = " + comment.getBody());

                    mCommentsDBHandler.addComment(scrollComment); // adding to comments database

                    mArrayOfComments.add(scrollComment);
                }

            } else { // else load comments from database...
                // note: have to use addAll, not just set the arraylist
                mArrayOfComments.addAll(mCommentsDBHandler.getAllComments(mPostId));
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mCommentsAdapter.notifyDataSetChanged();
            // stop the spinner
            try {
                ((PostDetailActivity) getActivity()).postDetailSpinner();
            } catch (ClassCastException cce) {
                cce.printStackTrace();
            }
        }
    }
}
