package com.matthiasko.scrollforreddit;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;

import net.dean.jraw.ApiException;
import net.dean.jraw.RedditClient;
import net.dean.jraw.android.AndroidRedditClient;
import net.dean.jraw.android.AndroidTokenStore;
import net.dean.jraw.auth.NoSuchTokenException;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.http.oauth.OAuthHelper;
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.models.Comment;

import java.util.ArrayList;

/**
 * Created by matthiasko on 4/4/16.
 */
public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.ViewHolder> {

    private boolean mUserlessMode;

    private boolean mTwoPane;

    private final ArrayList<ScrollComment> mValues;

    private Context mContext;

    private Toolbar mToolbar;

    private static final String CLIENT_ID = "cAizcZuXu-Mn9w";
    private static final String REDIRECT_URL = "http://scroll-for-reddit.com/oauthresponse";

    private static final String LOG_TAG = CommentsAdapter.class.getSimpleName();

    public CommentsAdapter(Context context, ArrayList<ScrollComment> items) {
        this.mContext = context;
        mValues = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.comments_card_view, parent, false);

        mUserlessMode = PostListActivity.mUserlessMode;

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        holder.mItem = mValues.get(position);

        int commentDepth = mValues.get(position).getDepth();

        //System.out.println("commentDepth = " + commentDepth);

        if (commentDepth > 1) {

            // set new indentation by multiplying comment depth
            int indentedMarginSize = commentDepth * 10;

            //System.out.println("commentDepth = " + commentDepth);

            //int bodyCharCount = mValues.get(position).getBody().length();

            //int totalIndent = bodyCharCount + commentDepth;

            //String indentedComment = mValues.get(position).getBody().replaceAll("(?m)^", "\t");

            //String indentedComment = String.format("%" + totalIndent + "s", mValues.get(position).getBody());

            holder.commentBody.setText(mValues.get(position).getBody());
            holder.commentBody.setContentDescription(mValues.get(position).getBody());

            // setting margins dynamically here depending on the comment level
            // margins are set in pixels, might have to convert to dp...
            ViewGroup.MarginLayoutParams commentMargins = (ViewGroup.MarginLayoutParams) holder.commentBody
                    .getLayoutParams();

            commentMargins.setMargins(indentedMarginSize, 0, 0, 0);

            ViewGroup.MarginLayoutParams authorMargin = (ViewGroup.MarginLayoutParams) holder.commentAuthor
                    .getLayoutParams();

            authorMargin.setMargins(indentedMarginSize, 0, 0, 0);

        } else {

            holder.commentBody.setText(mValues.get(position).getBody());
            holder.commentBody.setContentDescription(mValues.get(position).getBody());
        }

        holder.commentAuthor.setText(mValues.get(position).getAuthor());
        holder.commentAuthor.setContentDescription(mValues.get(position).getAuthor());
        holder.commentPoints.setText(String.valueOf(mValues.get(position).getScore()));
        holder.commentPoints.setContentDescription(String.valueOf(mValues.get(position).getScore()));

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // on click check if a toolbar is visible
                // if yes -> hide and destroy toolbar
                // if no -> create and show new toolbar

                if (mToolbar != null && mToolbar.getVisibility() == View.VISIBLE) { // toolbar exists and is visible

                    mToolbar.setVisibility(View.GONE); // first hide toolbar
                    mToolbar.getMenu().clear();

                } else { // create toolbar

                    mToolbar = (Toolbar) v.findViewById(R.id.comments_toolbar);
                    mToolbar.inflateMenu(R.menu.menu_comments);
                    mToolbar.setBackgroundColor(mContext.getResources().getColor(R.color.colorPrimary));




                    mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            switch (menuItem.getItemId()) {
                                case R.id.action_reply:
                                    JsonNode dataNode = holder.mItem.getDataNode();
                                    postComment(dataNode);

                                    return true;
                            }
                            return true;
                        }
                    });
                    mToolbar.setVisibility(View.VISIBLE); // set to View.GONE in xml

                }

                // how should we handle urls in comments?

                /*
                // match url in body of comment text, so user can follow link
                String body = mValues.get(position).getBody();

                Pattern pattern = Patterns.WEB_URL;
                Matcher matcher = pattern.matcher(body);

                ArrayList links = new ArrayList();

                while(matcher.find()) {

                    String urlStr = matcher.group();

                    urlStr = urlStr.substring(0, urlStr.length());

                    links.add(urlStr);
                }

                String extractedUrl = links.get(0).toString(); // get first url

                // open extractedUrl in a web browser
                Intent intent = new Intent(mContext, WebViewActivity.class);
                intent.putExtra("SOURCE", extractedUrl);

                mContext.startActivity(intent);
                */
            }
        });
    }

    public void postComment(final JsonNode dataNode) {
        // if not logged in, send a message and dont start asynctask
        if (mUserlessMode) {

            new AlertDialog.Builder(mContext)
                    .setTitle("Error posting comment")
                    .setMessage("Please login to post a comment.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else {
            // show text entry dialog
            // when user presses 'submit' then we call the asynctask
            LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            View dialogView = layoutInflater.inflate(R.layout.edit_text_add_comment, null);

            AlertDialog.Builder alertDialogBuilder =
                    new AlertDialog.Builder(mContext, R.style.AppCompatAlertDialogStyle);

            alertDialogBuilder.setView(dialogView);

            final EditText input = (EditText) dialogView.findViewById(R.id.add_comment_edit_text);

            alertDialogBuilder
                    .setTitle("Post Comment")
                    .setCancelable(false)
                    .setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    new PostCommentAsyncTask(dataNode).execute(input.getText().toString());
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

    @Override
    public int getItemCount() {

        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public final View mView;
        public ScrollComment mItem;
        TextView commentBody;
        TextView commentAuthor;
        TextView commentPoints;

        public ViewHolder(View view) {
            super(view);
            mView = view;

            commentBody = (TextView) view.findViewById(R.id.comment_body);
            commentAuthor = (TextView) view.findViewById(R.id.comment_author);
            commentPoints = (TextView) view.findViewById(R.id.comment_points);
        }
    }

    private class PostCommentAsyncTask extends AsyncTask<String, Void, Void> {

        private JsonNode dataNode;

        public PostCommentAsyncTask (JsonNode dataNode) {
            this.dataNode = dataNode;
        }

        @Override
        protected Void doInBackground(String... params) { // send postId and user comment text as var

            final RedditClient redditClient = new AndroidRedditClient(mContext);

            final OAuthHelper oAuthHelper = redditClient.getOAuthHelper();

            final Credentials credentials = Credentials.installedApp(CLIENT_ID, REDIRECT_URL);

            AndroidTokenStore store = new AndroidTokenStore(mContext);

            try {
                String refreshToken = store.readToken("USER_TOKEN");

                oAuthHelper.setRefreshToken(refreshToken);

                try {

                    OAuthData finalData = oAuthHelper.refreshToken(credentials);

                    redditClient.authenticate(finalData);

                    String userInput = params[0];

                    AccountManager accountManager = new AccountManager(redditClient);

                    Comment comment = new Comment(dataNode);

                    try {
                        accountManager.reply(comment, userInput);
                    } catch (ApiException e) {
                        Log.e(LOG_TAG, e.getMessage());
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
            //adapter.notifyDataSetChanged();

            // TODO: refresh comments here?
            // how do we show the comment just posted?
            //refreshComments();
        }
    }
}
