package com.matthiasko.scrollforreddit;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by matthiasko on 4/1/16.
 */
public class PostsAdapter extends ArrayAdapter<Post> {

    private static class ViewHolder {
        TextView postTitle;
        TextView subreddit;
        TextView author;
        TextView source;
        TextView points;
        TextView numberOfComments;
        ImageView thumbnail;

    }

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
}
