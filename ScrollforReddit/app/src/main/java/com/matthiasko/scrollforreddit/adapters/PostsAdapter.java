package com.matthiasko.scrollforreddit.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.matthiasko.scrollforreddit.R;
import com.matthiasko.scrollforreddit.activities.PostDetailActivity;
import com.matthiasko.scrollforreddit.activities.PostListActivity;
import com.matthiasko.scrollforreddit.activities.WebViewActivity;
import com.squareup.picasso.Picasso;

/**
 * Created by matthiasko on 4/1/16.
 * Credit to skyfishjy gist:
 *    https://gist.github.com/skyfishjy/443b7448f59be978bc59
 */
public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.ViewHolder> {

    private static final String LOG_TAG = PostsAdapter.class.getSimpleName();
    private Context mContext;
    private Cursor mCursor;
    private boolean mDataValid;
    private int mRowIdColumn;
    private DataSetObserver mDataSetObserver;

    public PostsAdapter(Context ctxt, Cursor crsr){
        mContext = ctxt;
        mCursor = crsr;
        mDataValid = crsr != null;
        mRowIdColumn = mDataValid ? mCursor.getColumnIndex("_id") : -1;
        mDataSetObserver = new NotifyingDataSetObserver();
        if (mDataValid){
            mCursor.registerDataSetObserver(mDataSetObserver);
        }
    }

    public Cursor swapCursor(Cursor newCursor) {
        if (newCursor == mCursor) {
            return null;
        }
        final Cursor oldCursor = mCursor;
        if (oldCursor != null && mDataSetObserver != null) {
            oldCursor.unregisterDataSetObserver(mDataSetObserver);
        }
        mCursor = newCursor;
        if (mCursor != null) {
            if (mDataSetObserver != null) {
                mCursor.registerDataSetObserver(mDataSetObserver);
            }
            mRowIdColumn = newCursor.getColumnIndexOrThrow("_id");
            mDataValid = true;
            notifyDataSetChanged();
        } else {
            mRowIdColumn = -1;
            mDataValid = false;
            notifyDataSetChanged();
            //There is no notifyDataSetInvalidated() method in RecyclerView.Adapter
        }
        return oldCursor;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        mCursor.moveToPosition(position);

        final long id = mCursor.getLong(0);
        final String title = mCursor.getString(1);
        final String subreddit = mCursor.getString(2);
        final String author = mCursor.getString(3);
        final String source = mCursor.getString(4);
        final String thumbnail = mCursor.getString(5);
        final String postId = mCursor.getString(6);
        final String domain = mCursor.getString(7);
        final String fullName = mCursor.getString(8);
        final int points = mCursor.getInt(9);
        final int numberOfComments = mCursor.getInt(10);

        Resources res = mContext.getResources();
        String points_text = String.format(res.getString(R.string.posts_adapter_points), points);
        String comments_text = String.format(res.getString(R.string.posts_adapter_comments), numberOfComments);

        holder.postTitle.setText(title);
        holder.subreddit.setText(subreddit);
        holder.author.setText(author);
        holder.source.setText(domain);
        holder.points.setText(points_text);
        holder.numberOfComments.setText(comments_text);

        holder.postTitle.setContentDescription(title);
        holder.subreddit.setContentDescription(subreddit);
        holder.author.setContentDescription(author);
        holder.source.setContentDescription(domain);
        holder.points.setContentDescription(String.valueOf(points));
        holder.numberOfComments.setContentDescription(String.valueOf(numberOfComments));

        // thumbnail setup
        holder.thumbnail.setVisibility(View.VISIBLE);

        if (thumbnail == null || thumbnail.isEmpty()) {
            holder.thumbnail.setVisibility(View.GONE);
        } else {
            Picasso.with(mContext)
                    .load(thumbnail)
                    .resize(400, 200)
                    .centerCrop()
                    .into(holder.thumbnail);

            holder.thumbnail.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext, WebViewActivity.class);

                    String trimmedString;

                    if (source.contains(".jpg")) {

                        trimmedString = source.substring(0, source.lastIndexOf('.'));

                        intent.putExtra("SOURCE", trimmedString);
                    } else {

                        intent.putExtra("SOURCE", source);
                    }






                    System.out.println("source = " + source);

                    // remove extention? .jpg




                    mContext.startActivity(intent);
                }
            });
        }

        holder.upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String voteDirection = "up";
                ((PostListActivity) mContext).onVote(postId, id, voteDirection);
            }
        });

        holder.downButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String voteDirection = "down";
                ((PostListActivity) mContext).onVote(postId, id, voteDirection);
            }
        });

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(mContext, PostDetailActivity.class);
                intent.putExtra("POST_TITLE", title);
                intent.putExtra("SUBREDDIT", subreddit);
                intent.putExtra("POST_ID", postId);
                intent.putExtra("AUTHOR", author);
                intent.putExtra("SOURCE", source);
                intent.putExtra("THUMBNAIL", thumbnail);
                intent.putExtra("POINTS", points);
                intent.putExtra("NUMBER_OF_COMMENTS", numberOfComments);
                intent.putExtra("DOMAIN", domain);
                intent.putExtra("FULLNAME", fullName);
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (mDataValid && mCursor != null){
            return mCursor.getCount();
        }
        return 0;
    }

    @Override public long getItemId(int position) {
        if (mDataValid && mCursor != null && mCursor.moveToPosition(position)){
            return mCursor.getLong(mRowIdColumn);
        }
        return 0;
    }

    @Override public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(true);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public final View mView;
        TextView postTitle;
        TextView subreddit;
        TextView author;
        TextView source;
        TextView points;
        TextView numberOfComments;
        ImageView thumbnail;
        Button upButton;
        Button downButton;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            postTitle = (TextView) view.findViewById(R.id.postTitle);
            subreddit = (TextView) view.findViewById(R.id.postSubreddit);
            author = (TextView) view.findViewById(R.id.postAuthor);
            source = (TextView) view.findViewById(R.id.postSource);
            points = (TextView) view.findViewById(R.id.postPoints);
            numberOfComments = (TextView) view.findViewById(R.id.postNumberOfComments);
            thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
            upButton = (Button) view.findViewById(R.id.upButton);
            downButton = (Button) view.findViewById(R.id.downButton);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
        }
    }

    private class NotifyingDataSetObserver extends DataSetObserver{
        @Override public void onChanged() {
            super.onChanged();
            mDataValid = true;
            notifyDataSetChanged();
        }

        @Override public void onInvalidated() {
            super.onInvalidated();
            mDataValid = false;
            notifyDataSetChanged();
        }
    }
}
