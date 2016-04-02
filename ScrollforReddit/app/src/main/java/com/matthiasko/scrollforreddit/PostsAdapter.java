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
    public void onBindViewHolder(final ViewHolder holder, int position) {


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
                if (mTwoPane) {
                    Bundle arguments = new Bundle();

                    // change ARG_ITEM_ID, they need to be unique

                    arguments.putString(PostDetailFragment.ARG_ITEM_ID, holder.mItem.postTitle);
                    arguments.putString(PostDetailFragment.ARG_ITEM_ID, holder.mItem.postSubreddit);
                    PostDetailFragment fragment = new PostDetailFragment();
                    fragment.setArguments(arguments);
                    /*
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.post_detail_container, fragment)
                            .commit();
                            */
                } else {
                    Context context = v.getContext();
                    Intent intent = new Intent(context, PostDetailActivity.class);
                    intent.putExtra(PostDetailFragment.ARG_ITEM_ID, holder.mItem.postTitle);
                    intent.putExtra(PostDetailFragment.ARG_ITEM_ID, holder.mItem.postSubreddit);
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
