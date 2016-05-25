package com.matthiasko.scrollforreddit;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * Created by matthiasko on 5/17/16.
 */
public class ScrollWidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private static final String LOG_TAG = ScrollWidgetViewsFactory.class.getSimpleName();


    private Context context;
    private int appWidgetId;
    private ArrayList<Post> listItemList = new ArrayList<>();

    private DBHandler dbHandler;

    public ScrollWidgetViewsFactory(Context context, Intent intent) {
        this.context = context;
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }



    @Override
    public void onCreate() {

        dbHandler = new DBHandler(context);

        // populate an arraylist of post items to display
        listItemList = dbHandler.getAllPosts();

        // TODO: fix list_row layout, thumbnail should be on left, text to the right
        // change layout_width of textviews?

    }

    @Override
    public int getCount() {
        return listItemList.size();
    }

    @Override
    public void onDataSetChanged() {}

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public RemoteViews getViewAt(int position) {

        final RemoteViews remoteView = new RemoteViews(context.getPackageName(), R.layout.list_row);
        Post listItem = listItemList.get(position);

        remoteView.setTextViewText(R.id.title, listItem.postTitle);
        remoteView.setTextViewText(R.id.source, listItem.postSource);

        remoteView.setTextViewText(R.id.subreddit, listItem.postSubreddit);

        remoteView.setTextViewText(R.id.author, listItem.postAuthor);
        remoteView.setTextViewText(R.id.score, String.valueOf(listItem.postPoints));
        remoteView.setTextViewText(R.id.numberOfComments, String.valueOf(listItem.postNumberOfComments));


        // set bitmap using https://groups.google.com/forum/?fromgroups=#!topic/android-developers/jupslaeAEuo
        // tried using picasso to load image, but doesn't work in a widget listview yet

        if (listItem.postThumbnail != null) {

            remoteView.setImageViewBitmap(R.id.thumbnail, getImageBitmap(listItem.postThumbnail));
        } else {

            remoteView.setImageViewBitmap(R.id.thumbnail, BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.transparent_pixel));
        }

        // TODO: we need to set an empty thumbnail, otherwise wrong thumbnail displayed

        return remoteView;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onDestroy() {}

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private Bitmap getImageBitmap(String url) {
        Bitmap bm = null;
        try {
            URL aURL = new URL(url);
            URLConnection conn = aURL.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            bm = BitmapFactory.decodeStream(bis);
            bis.close();
            is.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error getting bitmap", e);
        }
        return bm;
    }
}
