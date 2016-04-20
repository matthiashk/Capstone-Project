package com.matthiasko.scrollforreddit;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by matthiasko on 4/19/16.
 */
public class PostContract {

    public static final String CONTENT_AUTHORITY = "com.matthiasko.scrollforreddit.PostProvider";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_POST = "post";

    public static final class PostEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_POST).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_POST;

        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_POST;

        public static final String TABLE_NAME = "posts";

        public static final String COLUMN_TITLE = "title";

        public static final String COLUMN_SUBREDDIT = "subreddit";
        public static final String COLUMN_AUTHOR = "author";
        public static final String COLUMN_SOURCE = "source";
        public static final String COLUMN_THUMBNAIL = "thumbnail";
        public static final String COLUMN_POST_ID = "post_id";
        public static final String COLUMN_SOURCE_DOMAIN = "source_domain";
        public static final String COLUMN_FULLNAME = "fullname";
        public static final String COLUMN_SCORE = "score";
        public static final String COLUMN_NUMBER_OF_COMMENTS = "number_of_comments";

        public static Uri buildPostUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }
}
