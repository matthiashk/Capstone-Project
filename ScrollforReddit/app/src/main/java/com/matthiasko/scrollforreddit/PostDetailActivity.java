package com.matthiasko.scrollforreddit;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

/**
 * An activity representing a single Post detail screen. This
 * activity is only used narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link PostListActivity}.
 */
public class PostDetailActivity extends AppCompatActivity implements PostDetailFragment.OnCommentsLoadedListener {

    private ImageView mHeaderImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        //toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);

        mHeaderImageView = (ImageView) findViewById(R.id.header_imageview);

        /*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own detail action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {

            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getIntent().getStringExtra("SUBREDDIT"));
        }

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString("POST_TITLE", getIntent().getStringExtra("POST_TITLE"));
            arguments.putString("POST_ID", getIntent().getStringExtra("POST_ID"));
            arguments.putString("FULLNAME", getIntent().getStringExtra("FULLNAME"));
            //arguments.putBoolean("USERLESS_MODE", getIntent().getBooleanExtra("USERLESS_MODE", false));

            PostDetailFragment postDetailFragment = new PostDetailFragment();
            postDetailFragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.post_detail_container, postDetailFragment)
                    .commit();
        }

        // populate the applayoutbar header for the detail view here
        ((TextView) findViewById(R.id.header_textview)).setText(getIntent().getStringExtra("POST_TITLE"));
        (findViewById(R.id.header_textview)).setContentDescription(getIntent().getStringExtra("POST_TITLE"));

        ((TextView) findViewById(R.id.subreddit_textview)).setText(getIntent().getStringExtra("SUBREDDIT"));
        (findViewById(R.id.subreddit_textview)).setContentDescription(getIntent().getStringExtra("SUBREDDIT"));

        ((TextView) findViewById(R.id.author_textview)).setText(getIntent().getStringExtra("AUTHOR"));
        (findViewById(R.id.author_textview)).setContentDescription(getIntent().getStringExtra("AUTHOR"));

        ((TextView) findViewById(R.id.source_textview)).setText(getIntent().getStringExtra("DOMAIN"));
        (findViewById(R.id.source_textview)).setContentDescription(getIntent().getStringExtra("DOMAIN"));

        // image loading logic here for detail view
        // if the source is an image load it with picasso
        // otherwise load a placeholder

        // extension code will detect .com
        // imgur files may not have an extension -> detect hostname
        // animated gifs on imgur have extension .gifv?
        // youtube
        // twitter

        String sourceUrl = getIntent().getStringExtra("SOURCE");

        String extension = "";

        int i = sourceUrl.lastIndexOf('.');

        if (i > 0) {

            extension = sourceUrl.substring(i + 1);
        }

        if (extension.equals("jpg")) {

            System.out.println("JPG MATCH");


            android.view.ViewGroup.LayoutParams layoutParams = mHeaderImageView.getLayoutParams();
            layoutParams.height = 800;
            mHeaderImageView.setLayoutParams(layoutParams);

            Picasso.with(getBaseContext())
                    .load(sourceUrl)
                    .resize(layoutParams.width, layoutParams.height)
                    .centerCrop()
                    .into(mHeaderImageView);

        } else if (sourceUrl.contains("imgur.com")) {

            System.out.println("IMGUR MATCH");

            android.view.ViewGroup.LayoutParams layoutParams = mHeaderImageView.getLayoutParams();
            layoutParams.height = 800;
            mHeaderImageView.setLayoutParams(layoutParams);

            // we need to add .jpg to the url to load it properly
            String modifiedUrl = sourceUrl.concat(".jpg");

            Picasso.with(getBaseContext())
                    .load(modifiedUrl)
                    .resize(layoutParams.width, layoutParams.height)
                    .centerCrop()
                    .into(mHeaderImageView);

        }  else if (getIntent().getStringExtra("THUMBNAIL") == null) {

            System.out.println("NULL THUMBNAIL MATCH");


            // change the height of the imageview to fit just the subreddit title
            android.view.ViewGroup.LayoutParams layoutParams = mHeaderImageView.getLayoutParams();
            layoutParams.height = 200;
            mHeaderImageView.setLayoutParams(layoutParams);

        } else if (!getIntent().getStringExtra("THUMBNAIL").isEmpty()) {

            System.out.println("EMPTY THUMBNAIL MATCH");

            android.view.ViewGroup.LayoutParams layoutParams = mHeaderImageView.getLayoutParams();
            layoutParams.height = 800;
            mHeaderImageView.setLayoutParams(layoutParams);

            Picasso.with(getBaseContext())
                    .load(getIntent().getStringExtra("THUMBNAIL"))
                    .resize(layoutParams.width, layoutParams.height)
                    .centerCrop()
                    .into(mHeaderImageView);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_post_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        PostDetailFragment postDetailFragment = (PostDetailFragment) getSupportFragmentManager()
                .findFragmentById(R.id.post_detail_container);

        switch (item.getItemId()) {
            case android.R.id.home:
                navigateUpTo(new Intent(this, PostListActivity.class));
                return false;
            case R.id.action_refresh:
                // get fragment
                postDetailFragment.refreshComments();
                return true;
            case R.id.action_get_more_comments:
                //getMoreComments()
                //System.out.println("GET MORE COMMENTS");
                return true;
            case R.id.action_comment:
                postDetailFragment.postComment();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // we need to load the spinner here
    // then in postdetailfragment in the onpostexecute, stop the spinner by calling a method in the activity
    public void postDetailSpinner() {
        // hide spinner, called from postdetailfragment / onpostexecute
        if (findViewById(R.id.loadingPanel) != null) {
            findViewById(R.id.loadingPanel).setVisibility(View.GONE);
        }
    }

    public void showPostDetailSpinner() {
        // hide spinner, called from postdetailfragment / onpostexecute
        if (findViewById(R.id.loadingPanel) != null) {
            findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
        }
    }
}
