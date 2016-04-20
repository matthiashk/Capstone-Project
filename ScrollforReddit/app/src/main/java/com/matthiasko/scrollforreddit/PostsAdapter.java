package com.matthiasko.scrollforreddit;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by matthiasko on 4/1/16.
 */
public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.ViewHolder> {

    private boolean mTwoPane; // TODO: change this

    private final ArrayList<Post> mValues;

    private Context mContext;

    public PostsAdapter(Context context, ArrayList<Post> items) {

        mContext = context;
        mValues = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        // using domain as 'source' text, save source for later

        holder.mItem = mValues.get(position);
        holder.postTitle.setText(mValues.get(position).postTitle);
        holder.subreddit.setText(mValues.get(position).postSubreddit);
        holder.author.setText(mValues.get(position).postAuthor);
        holder.source.setText(mValues.get(position).postDomain);
        holder.points.setText(String.valueOf(mValues.get(position).postPoints));
        holder.numberOfComments.setText(String.valueOf(mValues.get(position).postNumberOfComments));

        final String postFullName = mValues.get(position).postFullName;

        //System.out.println("onBindViewHolder - postFullName = " + postFullName);

        //holder.mIdView.setText(mValues.get(position).id);
        //holder.mContentView.setText(mValues.get(position).content);


        //System.out.println("mValues.get(position).postThumbnail = " + mValues.get(position).postThumbnail);


        //System.out.println("mValues.get(position).postTitle = " + mValues.get(position).postTitle);

        holder.upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //System.out.println("OVERRIDE UP");

                ((PostListActivity) mContext).onVote(mValues.get(position).getPostId(),
                        mValues.get(position).getId());



                // test
                //notifyItemChanged(position);





            }
        });

        holder.downButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        // check if there is a thumbnail
        holder.thumbnail.setVisibility(View.VISIBLE);

        if (mValues.get(position).postThumbnail == null) {

            holder.thumbnail.setVisibility(View.GONE);

        } else {

            Picasso.with(mContext)
                    .load(mValues.get(position).postThumbnail)
                    .into(holder.thumbnail);

            holder.thumbnail.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    // TODO: load 'source' into the webview, send source as string to webview
                    Intent intent = new Intent(mContext, WebViewActivity.class);
                    intent.putExtra("SOURCE", holder.mItem.postSource);

                    mContext.startActivity(intent);
                }
            });
        }

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String postId = mValues.get(position).postId;

                //System.out.println("postId = " + postId);

                //CommentNode commentNode = mValues.get(position).postCommentNode;

                //System.out.println("commentNode.getTotalSize() = " + commentNode.getTotalSize());

                //String testTitle = mValues.get(position).postTitle;

                //System.out.println("testTitle = " + testTitle);

                /*
                String commentAuthor = commentNode.getComment().getAuthor();
                int commentPoints = commentNode.getComment().getScore();
                Date commentTime = commentNode.getComment().getCreated();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(commentTime.toString(), Locale.US);
                String commentText = commentNode.getComment().getBody();
                */

                if (mTwoPane) { // this is for tablet mode
                    Bundle arguments = new Bundle();

                    arguments.putString("POST_TITLE", holder.mItem.postTitle);
                    arguments.putString("SUBREDDIT", holder.mItem.postSubreddit);
                    arguments.putString("POST_ID", postId);
                    arguments.putString("AUTHOR", holder.mItem.postAuthor);
                    arguments.putString("SOURCE", holder.mItem.postSource);
                    arguments.putString("THUMBNAIL", holder.mItem.postThumbnail);
                    arguments.putInt("POINTS", holder.mItem.postPoints);
                    arguments.putInt("NUMBER_OF_COMMENTS", holder.mItem.postNumberOfComments);
                    arguments.putString("DOMAIN", holder.mItem.postDomain);
                    arguments.putString("FULLNAME", postFullName);

                    //arguments.putString("COMMENT_AUTHOR", commentAuthor);

                    PostDetailFragment fragment = new PostDetailFragment();
                    fragment.setArguments(arguments);
                    /*
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.post_detail_container, fragment)
                            .commit();
                            */
                } else { // this is for phone mode

                    //System.out.println("postId = " + postId);

                    Context context = v.getContext();
                    Intent intent = new Intent(context, PostDetailActivity.class);
                    intent.putExtra("POST_TITLE", holder.mItem.postTitle);
                    intent.putExtra("SUBREDDIT", holder.mItem.postSubreddit);
                    intent.putExtra("POST_ID", postId);
                    intent.putExtra("AUTHOR", holder.mItem.postAuthor);
                    intent.putExtra("SOURCE", holder.mItem.postSource);
                    intent.putExtra("THUMBNAIL", holder.mItem.postThumbnail);
                    intent.putExtra("POINTS", holder.mItem.postPoints);
                    intent.putExtra("NUMBER_OF_COMMENTS", holder.mItem.postNumberOfComments);
                    intent.putExtra("DOMAIN", holder.mItem.postDomain);
                    intent.putExtra("FULLNAME", postFullName);

                    //intent.putExtra("COMMENT_AUTHOR", commentAuthor);

                    context.startActivity(intent);
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public final View mView;
        public Post mItem;

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
            //mIdView = (TextView) view.findViewById(R.id.id);
            //mContentView = (TextView) view.findViewById(R.id.content);

            postTitle = (TextView) view.findViewById(R.id.postTitle);
            subreddit = (TextView) view.findViewById(R.id.postSubreddit);
            author = (TextView) view.findViewById(R.id.postAuthor);
            source = (TextView) view.findViewById(R.id.postSource);
            points = (TextView) view.findViewById(R.id.postPoints);
            numberOfComments = (TextView) view.findViewById(R.id.postNumberOfComments);
            thumbnail = (ImageView) view.findViewById(R.id.thumbnail);

            upButton = (Button) view.findViewById(R.id.upButton);
            downButton = (Button) view.findViewById(R.id.downButton);
        }
    }
}
