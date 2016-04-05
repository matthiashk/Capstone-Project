package com.matthiasko.scrollforreddit;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.SubredditPaginator;

/**
 * A fragment representing a single Post detail screen.
 * This fragment is either contained in a {@link PostListActivity}
 * in two-pane mode (on tablets) or a {@link PostDetailActivity}
 * on handsets.
 */
public class PostDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    /**
     * The dummy content this fragment is presenting.
     */
    private Post mItem;

    private String postTitle;
    private String commentAuthor;
    private String postId;

    private static final String LOG_TAG = "MainActivity";

    private static final String CLIENT_ID = "cAizcZuXu-Mn9w";

    private static final String REDIRECT_URL = "http://scroll-for-reddit.com/oauthresponse";

    private TextView commentTextView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PostDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidTokenStore store = new AndroidTokenStore(getContext());

        try {

            String refreshToken = store.readToken("EXAMPLE_KEY");
            new RetrieveComments().execute(refreshToken);

        } catch (NoSuchTokenException e) {

            Log.e(LOG_TAG, e.getMessage());
        }



        String title = getArguments().getString("POST_TITLE");

        if (getArguments().containsKey("POST_TITLE")) {


            // TODO: maybe we need the post id to get the specific commentnode




            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            //mItem = DummyContent.ITEM_MAP.get(getArguments().getString(ARG_ITEM_ID));

            //mItem.postTitle = getArguments().getString("POST_TITLE");

            postTitle = getArguments().getString("POST_TITLE");

            postId = getArguments().getString("POST_ID");

            //System.out.println("postId = " + postId);

            //commentAuthor = getArguments().getString("COMMENT_AUTHOR");


            //System.out.println("postTitle = " + postTitle);


        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {





        
        View rootView = inflater.inflate(R.layout.post_detail, container, false);


        ((TextView) rootView.findViewById(R.id.post_detail)).setText(postTitle);

        // setup our textview so we can set it in onpostexecute after getting comments

        commentTextView = (TextView) rootView.findViewById(R.id.comment_author);

        //((TextView) rootView.findViewById(R.id.comment_author)).setText(commentAuthor);

        return rootView;
    }

    private class RetrieveComments extends AsyncTask<String, Void, String> {

        final RedditClient redditClient = new AndroidRedditClient(getContext());

        final OAuthHelper oAuthHelper = redditClient.getOAuthHelper();

        final Credentials credentials = Credentials.installedApp(CLIENT_ID, REDIRECT_URL);

        String commentText;

        @Override
        protected String doInBackground(String... params) {

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

            SubredditPaginator paginator = new SubredditPaginator(redditClient);
            Listing<Submission> submissions = paginator.next();

            for (Submission submission : submissions) {


                //System.out.println("commentNode.getTotalSize() = " + commentNode.getTotalSize());

                //System.out.println("postId = " + postId);

                if (submission.getId().equals(postId)) {

                    Submission fullSubmissionData = redditClient.getSubmission(submission.getId());
                    //System.out.println(fullSubmissionData.getTitle());
                    //System.out.println(fullSubmissionData.getComments());

                    CommentNode commentNode = fullSubmissionData.getComments();

                    commentText = commentNode.get(0).getComment().getBody();

                    // TODO: load 10 comments and show their child comments?

                    // TODO: we need a loading graphic here...





                }


                /*
                String commentAuthor = commentNode.getComment().getAuthor();
                int commentPoints = commentNode.getComment().getScore();
                Date commentTime = commentNode.getComment().getCreated();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(commentTime.toString(), Locale.US);
                String commentText = commentNode.getComment().getBody();
                */

            }


            return commentText;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            System.out.println("string = " + s);

            commentTextView.setText(s);

        }
    }

}
