package com.matthiasko.scrollforreddit;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.SubredditPaginator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

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

    private boolean mUserlessMode;

    private Post mItem;

    private String postTitle;
    private String commentAuthor;
    private String postId;
    private String postFullName;

    private static final String LOG_TAG = "MainActivity";

    private static final String CLIENT_ID = "cAizcZuXu-Mn9w";

    private static final String REDIRECT_URL = "http://scroll-for-reddit.com/oauthresponse";

    private TextView commentTextView;

    private CommentsAdapter adapter;

    private ArrayList<ScrollComment> arrayOfComments;



    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PostDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (getArguments().containsKey("POST_TITLE")) {

            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            //mItem = DummyContent.ITEM_MAP.get(getArguments().getString(ARG_ITEM_ID));

            //mItem.postTitle = getArguments().getString("POST_TITLE");

            postTitle = getArguments().getString("POST_TITLE");

            postId = getArguments().getString("POST_ID");

            postFullName = getArguments().getString("FULLNAME");

            mUserlessMode = getArguments().getBoolean("USERLESS_MODE");

            //System.out.println("PostDetailFragment - mUserlessMode = " + mUserlessMode);

            //System.out.println("postFullName = " + postFullName);

            //System.out.println("postId = " + postId);

            //commentAuthor = getArguments().getString("COMMENT_AUTHOR");

            //System.out.println("postTitle = " + postTitle);
        }



        // TODO: fetch userless token if in userless mode

        if (mUserlessMode) {

            System.out.println("***** USERLESS MODE *****");

            // get access token
            AndroidTokenStore store = new AndroidTokenStore(getContext());

            try {

                String refreshToken = store.readToken("USERLESS_TOKEN");

                System.out.println("refreshToken = " + refreshToken);

                new RetrieveUserlessComments().execute(refreshToken);

            } catch (NoSuchTokenException e) {

                Log.e(LOG_TAG, e.getMessage());
            }


        } else {

            System.out.println("***** NOT USERLESS MODE *****");

            // get access token
            AndroidTokenStore store = new AndroidTokenStore(getContext());

            try {

                String refreshToken = store.readToken("EXAMPLE_KEY");
                new RetrieveComments().execute(refreshToken);

            } catch (NoSuchTokenException e) {

                Log.e(LOG_TAG, e.getMessage());
            }

        }



        //String title = getArguments().getString("POST_TITLE");



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

        arrayOfComments = new ArrayList<>();
        // Create the adapter to convert the array to views
        adapter = new CommentsAdapter(getContext(), arrayOfComments);

        //View commentsRecyclerView = rootView.findViewById(R.id.commentsList);

        //commentsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));


        //assert commentsRecyclerView != null;
        //setupRecyclerView((RecyclerView) commentsRecyclerView);

        commentsRecyclerView.setAdapter(adapter);


        //((TextView) rootView.findViewById(R.id.post_detail)).setText(postTitle);

        // setup our textview so we can set it in onpostexecute after getting comments

        //commentTextView = (TextView) rootView.findViewById(R.id.comment_author);

        //((TextView) rootView.findViewById(R.id.comment_author)).setText(commentAuthor);

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

            // TODO: we need to get a specific post using its ID, but the paginator only
            // TODO: use the fullname and a specificpaginator to access the post

            // seems to get frontpage posts
            // therefore we have to query the post more directly...

            //SpecificPaginator specificPaginator = new SpecificPaginator(redditClient);

            //specificPaginator.getSubmissions();




            SubredditPaginator paginator = new SubredditPaginator(redditClient);

            Listing<Submission> submissions = paginator.next();

            System.out.println("submissions.size() = " + submissions.size());

            for (Submission submission : submissions) {


                //System.out.println("commentNode.getTotalSize() = " + commentNode.getTotalSize());

                //System.out.println("postId = " + postId);

                if (submission.getId().equals(postId)) {

                    Submission fullSubmissionData = redditClient.getSubmission(submission.getId());
                    //System.out.println(fullSubmissionData.getTitle());
                    //System.out.println(fullSubmissionData.getComments());

                    CommentNode commentNode = fullSubmissionData.getComments();


                    Iterable<CommentNode> iterable = commentNode.walkTree();

                    for (CommentNode node : iterable) {

                        Comment comment = node.getComment();

                        ScrollComment scrollComment = new ScrollComment(comment.getBody(),
                                comment.getAuthor(), comment.getScore(), node.getDepth());




                        //System.out.println("comment.getBody() = " + comment.getBody());

                        arrayOfComments.add(scrollComment);


                    }



                    //commentText = commentNode.get(0).getComment().getBody();

                    // load 10 comments and show their child comments?

                    // load 10 comments into the array

                    /*
                    for (int i = 0; i < 10; i++) {

                        Comment comment = commentNode.get(i).getComment();

                        ScrollComment scrollComment = new ScrollComment(comment.getBody(),
                                comment.getAuthor(), comment.getScore(), commentNode.getDepth());




                        //System.out.println("comment.getBody() = " + comment.getBody());

                        arrayOfComments.add(scrollComment);

                    }
                    */
                }


                /*
                String commentAuthor = commentNode.getComment().getAuthor();
                int commentPoints = commentNode.getComment().getScore();
                Date commentTime = commentNode.getComment().getCreated();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(commentTime.toString(), Locale.US);
                String commentText = commentNode.getComment().getBody();
                */

            }


            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            System.out.println("arrayOfComments.size() = " + arrayOfComments.size());

            //System.out.println("string = " + s);

            //commentTextView.setText(s);

            adapter.notifyDataSetChanged();

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

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;


        @Override
        protected Void doInBackground(String... params) {

            String userlessToken = params[0];

            try {
                // get access_token from response data

                // call api using the token we just recieved and ask for hot posts
                final String REDDIT_OAUTH_API_BASE_URL = "https://oauth.reddit.com/comments/";
                final String bearer = "Bearer " + userlessToken;

                //System.out.println("userlessToken = " + userlessToken);

                Uri builtUri = Uri.parse(REDDIT_OAUTH_API_BASE_URL)
                        .buildUpon()
                        .appendPath(postId)
                        .build();

                URL oauthUrl = new URL(builtUri.toString());

                //System.out.println("oauthUrl = " + oauthUrl);

                urlConnection = (HttpURLConnection) oauthUrl.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Authorization", bearer);
                urlConnection.connect();

                //int responseCode = urlConnection.getResponseCode();

                //System.out.println("responseCode = " + responseCode);

                // Read the input stream into a String
                InputStream is = urlConnection.getInputStream();
                StringBuffer b = new StringBuffer();
                if (is == null) {

                    System.out.println("INPUTSTREAM IS NULL");
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
                    System.out.println("STRINGBUFFER IS EMPTY");
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                String r = b.toString();

                System.out.println("PostDetailFragment - r = " + r);

                //JSONObject aResponse = new JSONObject(r);
                //JSONObject data = aResponse.getJSONObject("data");
                JSONArray jsonArray = new JSONArray(r);

                System.out.println("jsonArray.length() = " + jsonArray.length());

                String commentText;

                int depth = 0;


                // TODO: parse json here
                for (int i = 0; i < jsonArray.length(); i++) {

                    JSONObject jsonObject = jsonArray.getJSONObject(i).getJSONObject("data");

                    //System.out.println("jsonObject.toString() = " + jsonObject.toString());

                    JSONArray commentsArray = jsonObject.getJSONArray("children");

                    System.out.println("commentsArray.length() = " + commentsArray.length());

                    //System.out.println("commentsArray.toString() = " + commentsArray.toString());

                    for (int j = 0; j < commentsArray.length(); j++) {

                        if (commentsArray.getJSONObject(i).optString("kind").equals("t1") == false)
                            continue;

                        JSONObject innerElem = commentsArray.getJSONObject(j);

                        JSONObject commentElem = innerElem.getJSONObject("data");








                        //System.out.println("commentElem.toString() = " + commentElem.toString());

                        //System.out.println("commentElem.getString(\"body\") = " + commentElem.optString("body"));

                        // TODO: one body value will be null, this contains a list of comment ids in a 'children' array

                        // TODO: how do we get/handle the depth?

                        //System.out.println("commentsIds.toString() = " + commentsIds.toString());

                        if (commentElem.optString("body").isEmpty())
                            continue;

                        ScrollComment scrollComment = new ScrollComment(commentElem.optString("body"),
                                commentElem.optString("author"), commentElem.optInt("score"), 0);

                        arrayOfComments.add(scrollComment);



                        if (!commentElem.get("replies").equals("")) {

                            JSONArray replies = commentElem.getJSONObject("replies")
                                    .getJSONObject("data")
                                    .getJSONArray("children");

                            //System.out.println("replies = " + replies);


                            // TODO: how do we put the replies under the proper comment?
                            // TODO: how do we set the level of replies?




                            for (int k = 0; k < replies.length(); k++){

                                if (replies.getJSONObject(k).optString("kind") == null)
                                    continue;
                                if (replies.getJSONObject(k).optString("kind").equals("t1") == false)
                                    continue;
                                JSONObject data = replies.getJSONObject(k).getJSONObject("data");


                                System.out.println("data.getString(\"body\") = " + data.getString("body"));

                                //depth++;

                                System.out.println("data.getString(\"parent_id\") = " + data.getString("parent_id"));


                                //System.out.println("depth = " + depth);

                                //System.out.println("***** data = " + data);


                                /*
                                Comment comment = loadComment(data,level);
                                if (comment.author != null) {
                                    comments.add(comment);
                                    addReplies(comments, data, level+1);
                                }
                                */
                            }


                        }











                        //System.out.println("innerElem.toString() = " + innerElem.toString());

                        //commentText = innerElem.getString("body_html");

                        //System.out.println("commentText = " + commentText);

                    }
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
            } finally {
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
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            adapter.notifyDataSetChanged();

            // stop the spinner
            try {
                ((PostDetailActivity) getActivity()).postDetailSpinner();
            } catch (ClassCastException cce) {
                cce.printStackTrace();
            }
        }
    }
}
