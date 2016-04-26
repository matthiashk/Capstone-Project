package com.matthiasko.scrollforreddit;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);

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
            arguments.putBoolean("USERLESS_MODE", getIntent().getBooleanExtra("USERLESS_MODE", false));

            PostDetailFragment fragment = new PostDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.post_detail_container, fragment)
                    .commit();
        }

        // populate the applayoutbar header for the detail view here
        ((TextView) findViewById(R.id.header_textview)).setText(getIntent().getStringExtra("POST_TITLE"));

        ((TextView) findViewById(R.id.subreddit_textview)).setText(getIntent().getStringExtra("SUBREDDIT"));

        ((TextView) findViewById(R.id.author_textview)).setText(getIntent().getStringExtra("AUTHOR"));

        ((TextView) findViewById(R.id.source_textview)).setText(getIntent().getStringExtra("DOMAIN"));

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

        //System.out.println("PostDetailActivity - extension = " + extension);

        if (extension.equals("jpg")) {

            Picasso.with(getBaseContext())
                    .load(sourceUrl)
                    .resize(400, 200)
                    .centerCrop()
                    .into((ImageView) findViewById(R.id.header_imageview));

        } else if (sourceUrl.contains("imgur.com")) {

            // we need to add .jpg to the url to load it properly
            String modifiedUrl = sourceUrl.concat(".jpg");

            Picasso.with(getBaseContext())
                    .load(modifiedUrl)
                    .resize(400, 200)
                    .centerCrop()
                    .into((ImageView) findViewById(R.id.header_imageview));

        } else if (!getIntent().getStringExtra("THUMBNAIL").isEmpty()) {

            Picasso.with(getBaseContext())
                    .load(getIntent().getStringExtra("THUMBNAIL"))
                    .resize(400, 200)
                    .centerCrop()
                    .into((ImageView) findViewById(R.id.header_imageview));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //System.out.println("id = " + id);

        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            //onBackPressed();
            //System.out.println("BACK BUTTON PRESSED");
            navigateUpTo(new Intent(this, PostListActivity.class));
            return false;
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
}
