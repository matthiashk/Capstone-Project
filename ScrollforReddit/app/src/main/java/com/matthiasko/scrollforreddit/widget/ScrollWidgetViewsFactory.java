package com.matthiasko.scrollforreddit.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.matthiasko.scrollforreddit.R;
import com.matthiasko.scrollforreddit.data.DBHandler;
import com.matthiasko.scrollforreddit.models.Post;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * Created by matthiasko on 5/17/16.
 *
 * widget based on https://laaptu.wordpress.com/2013/07/19/android-app-widget-with-listview/
 * and my own app 'details to do list' available in the google play store
 *
 */
public class ScrollWidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private static final String LOG_TAG = ScrollWidgetViewsFactory.class.getSimpleName();

    private Context mContext;
    private int mAppWidgetId;
    private ArrayList<Post> mListItemList = new ArrayList<>();

    public ScrollWidgetViewsFactory(Context context, Intent intent) {
        this.mContext = context;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {
        DBHandler dbHandler = new DBHandler(mContext);
        // populate an arraylist of post items to display
        mListItemList = dbHandler.getAllPosts();
    }

    @Override
    public int getCount() {
        return mListItemList.size();
    }

    @Override
    public void onDataSetChanged() {}

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public RemoteViews getViewAt(int position) {

        final RemoteViews remoteView = new RemoteViews(mContext.getPackageName(), R.layout.list_row);
        Post listItem = mListItemList.get(position);

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
            remoteView.setImageViewBitmap(R.id.thumbnail, BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.transparent_pixel));
        }
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
