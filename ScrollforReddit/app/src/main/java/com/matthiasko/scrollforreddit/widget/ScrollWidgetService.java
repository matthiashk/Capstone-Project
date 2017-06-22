package com.matthiasko.scrollforreddit.widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.widget.RemoteViewsService;

/**
 * Created by matthiasko on 5/17/16.
 *
 * widget based on https://laaptu.wordpress.com/2013/07/19/android-app-widget-with-listview/
 * and my own app 'details to do list' available in the google play store
 *
 */
public class ScrollWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        int appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        return (new ScrollWidgetViewsFactory(this.getApplicationContext(), intent));
    }
}
