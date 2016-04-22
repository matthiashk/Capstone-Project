package com.matthiasko.scrollforreddit;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

/**
 * Created by matthiasko on 4/1/16.
 * Credit to skyfishjy gist:
 *    https://gist.github.com/skyfishjy/443b7448f59be978bc59
 */
public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.ViewHolder> {

    private boolean mTwoPane; // TODO: change this

    private Context context;

    private Cursor cursor;

    private boolean dataValid;

    private int rowIdColumn;

    private DataSetObserver dataSetObserver;

    public PostsAdapter(Context ctxt, Cursor crsr){
        context = ctxt;
        cursor = crsr;
        dataValid = crsr != null;
        rowIdColumn = dataValid ? cursor.getColumnIndex("_id") : -1;
        dataSetObserver = new NotifyingDataSetObserver();
        if (dataValid){
            cursor.registerDataSetObserver(dataSetObserver);
        }
    }

    public Cursor getCursor() {
        return cursor;
    }

    public Cursor swapCursor(Cursor newCursor) {
        if (newCursor == cursor) {
            return null;
        }
        final Cursor oldCursor = cursor;
        if (oldCursor != null && dataSetObserver != null) {
            oldCursor.unregisterDataSetObserver(dataSetObserver);
        }
        cursor = newCursor;
        if (cursor != null) {
            if (dataSetObserver != null) {
                cursor.registerDataSetObserver(dataSetObserver);
            }
            rowIdColumn = newCursor.getColumnIndexOrThrow("_id");
            dataValid = true;
            notifyDataSetChanged();
        } else {
            rowIdColumn = -1;
            dataValid = false;
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

        cursor.moveToPosition(position);

        final long id = cursor.getLong(0);
        final String title = cursor.getString(1);
        final String subreddit = cursor.getString(2);
        final String author = cursor.getString(3);
        final String source = cursor.getString(4);
        final String thumbnail = cursor.getString(5);
        final String postId = cursor.getString(6);
        final String domain = cursor.getString(7);
        final String fullName = cursor.getString(8);
        final int points = cursor.getInt(9);
        final int numberOfComments = cursor.getInt(10);

        holder.postTitle.setText(title);
        holder.subreddit.setText(subreddit);
        holder.author.setText(author);
        holder.source.setText(source);
        holder.points.setText(String.valueOf(points));
        holder.numberOfComments.setText(String.valueOf(numberOfComments));

        // thumbnail setup
        holder.thumbnail.setVisibility(View.VISIBLE);

        if (thumbnail == null) {

            holder.thumbnail.setVisibility(View.GONE);

        } else {

            Picasso.with(context)
                    .load(thumbnail)
                    .into(holder.thumbnail);

            holder.thumbnail.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    //load 'source' into the webview, send source as string to webview
                    Intent intent = new Intent(context, WebViewActivity.class);
                    intent.putExtra("SOURCE", source);

                    context.startActivity(intent);
                }
            });
        }

        holder.upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //System.out.println("OVERRIDE UP");

                String voteDirection = "up";

                ((PostListActivity) context).onVote(postId, id, voteDirection);
            }
        });

        holder.downButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String voteDirection = "down";

                ((PostListActivity) context).onVote(postId, id, voteDirection);

            }
        });

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //String postId = values.get(position).postId;

                //System.out.println("postId = " + postId);

                //CommentNode commentNode = values.get(position).postCommentNode;

                //System.out.println("commentNode.getTotalSize() = " + commentNode.getTotalSize());

                //String testTitle = values.get(position).postTitle;

                //System.out.println("testTitle = " + testTitle);

                if (mTwoPane) { // this is for tablet mode
                    Bundle arguments = new Bundle();

                    arguments.putString("POST_TITLE", title);
                    arguments.putString("SUBREDDIT", subreddit);
                    arguments.putString("POST_ID", postId);
                    arguments.putString("AUTHOR", author);
                    arguments.putString("SOURCE", source);
                    arguments.putString("THUMBNAIL", thumbnail);
                    arguments.putInt("POINTS", points);
                    arguments.putInt("NUMBER_OF_COMMENTS", numberOfComments);
                    arguments.putString("DOMAIN", domain);
                    arguments.putString("FULLNAME", fullName);

                    //arguments.putString("COMMENT_AUTHOR", commentAuthor);

                    PostDetailFragment fragment = new PostDetailFragment();
                    fragment.setArguments(arguments);

                    //getSupportFragmentManager().beginTransaction().replace(R.id.post_detail_container, fragment).commit();

                } else { // this is for phone mode

                    //System.out.println("postId = " + postId);

                    Context context = v.getContext();
                    Intent intent = new Intent(context, PostDetailActivity.class);
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

                    //intent.putExtra("COMMENT_AUTHOR", commentAuthor);

                    context.startActivity(intent);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        if (dataValid && cursor != null){
            return cursor.getCount();
        }
        return 0;
    }

    @Override public long getItemId(int position) {
        if (dataValid && cursor != null && cursor.moveToPosition(position)){
            return cursor.getLong(rowIdColumn);
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
            dataValid = true;
            notifyDataSetChanged();
        }

        @Override public void onInvalidated() {
            super.onInvalidated();
            dataValid = false;
            notifyDataSetChanged();
        }
    }
}
