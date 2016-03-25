package com.matthiasko.redditreader;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ArrayList<Post> arrayOfUsers = new ArrayList<Post>();
        // Create the adapter to convert the array to views
        PostsAdapter adapter = new PostsAdapter(this, arrayOfUsers);

        ListView listView = (ListView) findViewById(R.id.listview_posts);
        listView.setAdapter(adapter);

        String title1 = "Post title 1";
        String title2 = "Post title 2";

        Post post = new Post(title1);
        Post post2 = new Post(title2);


        adapter.add(post);
        adapter.add(post2);

    }

    // create list fragment
    // load fragment in mainactivity

    /*  change basic list to custom list - add title, name of subreddit, username, time etc

        title = title of post
        subreddit = name of subreddit
        username = name of poster
        source = name of source
        points = number of points
        comments = number of comments

        non text items
        thumbnail preview
        up/down vote button

        TODO: make a model?

     */

}
