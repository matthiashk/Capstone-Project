package com.matthiasko.redditreader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by matthiasko on 3/25/16.
 */
public class PostsAdapter extends ArrayAdapter<Post> {

    private static class ViewHolder {
        TextView postTitle;
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

            convertView.setTag(viewHolder);

        } else {

            viewHolder = (ViewHolder) convertView.getTag();
        }



        viewHolder.postTitle.setText(post.postTitle);

        return convertView;

    }

}
