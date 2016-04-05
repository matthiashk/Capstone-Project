package com.matthiasko.scrollforreddit;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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


        holder.mItem = mValues.get(position);
        holder.postTitle.setText(mValues.get(position).postTitle);
        holder.subreddit.setText(mValues.get(position).postSubreddit);
        holder.author.setText(mValues.get(position).postAuthor);
        holder.source.setText(mValues.get(position).postSource);
        holder.points.setText(String.valueOf(mValues.get(position).postPoints));
        holder.numberOfComments.setText(String.valueOf(mValues.get(position).postNumberOfComments));
        //holder.mIdView.setText(mValues.get(position).id);
        //holder.mContentView.setText(mValues.get(position).content);
        Picasso.with(mContext).load(mValues.get(position).postThumbnail).into(holder.thumbnail);

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

                if (mTwoPane) {
                    Bundle arguments = new Bundle();

                    // TODO: change ARG_ITEM_ID, they need to be unique

                    arguments.putString("POST_TITLE", holder.mItem.postTitle);
                    arguments.putString("SUBREDDIT", holder.mItem.postSubreddit);
                    arguments.putString("POST_ID", postId);

                    //arguments.putString("COMMENT_AUTHOR", commentAuthor);

                    // send as parcelable???



                    PostDetailFragment fragment = new PostDetailFragment();
                    fragment.setArguments(arguments);
                    /*
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.post_detail_container, fragment)
                            .commit();
                            */
                } else {

                    //System.out.println("postId = " + postId);

                    Context context = v.getContext();
                    Intent intent = new Intent(context, PostDetailActivity.class);
                    intent.putExtra("POST_TITLE", holder.mItem.postTitle);
                    intent.putExtra("SUBREDDIT", holder.mItem.postSubreddit);
                    intent.putExtra("POST_ID", postId);
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
        //public final TextView mIdView;
        //public final TextView mContentView;
        public Post mItem;

        TextView postTitle;
        TextView subreddit;
        TextView author;
        TextView source;
        TextView points;
        TextView numberOfComments;
        ImageView thumbnail;
        //CommentNode commentNode;

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

        }

    }


    /*
    public PostsAdapter(Context context, ArrayList<Post> post) {

        super(context, R.layout.item_post, post);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Post post = getItem(position);

        ViewHolder viewHolder;

        if (convertView == null) {

            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.item_post, parent, false);
            viewHolder.postTitle = (TextView) convertView.findViewById(R.id.postTitle);
            viewHolder.subreddit = (TextView) convertView.findViewById(R.id.postSubreddit);
            viewHolder.author = (TextView) convertView.findViewById(R.id.postAuthor);
            viewHolder.source = (TextView) convertView.findViewById(R.id.postSource);
            viewHolder.points = (TextView) convertView.findViewById(R.id.postPoints);
            viewHolder.numberOfComments = (TextView) convertView.findViewById(R.id.postNumberOfComments);
            viewHolder.thumbnail = (ImageView) convertView.findViewById(R.id.thumbnail);


            convertView.setTag(viewHolder);

        } else {

            viewHolder = (ViewHolder) convertView.getTag();
        }



        viewHolder.postTitle.setText(post.postTitle);
        viewHolder.subreddit.setText(post.postSubreddit);
        viewHolder.author.setText(post.postAuthor);
        viewHolder.source.setText(post.postSource);
        viewHolder.points.setText(String.valueOf(post.postPoints));
        viewHolder.numberOfComments.setText(String.valueOf(post.postNumberOfComments));

        Picasso.with(getContext()).load(post.postThumbnail).into(viewHolder.thumbnail);



        return convertView;

    }
    */
}
