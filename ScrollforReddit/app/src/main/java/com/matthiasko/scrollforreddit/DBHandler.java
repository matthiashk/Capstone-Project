package com.matthiasko.scrollforreddit;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by matthiasko on 4/13/16.
 */
public class DBHandler extends SQLiteOpenHelper {

    public static final String TABLE_POSTS = "posts";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_SUBREDDIT = "subreddit";
    public static final String COLUMN_AUTHOR = "author";
    public static final String COLUMN_SOURCE = "source";
    public static final String COLUMN_THUMBNAIL = "thumbnail";
    public static final String COLUMN_POST_ID = "post_id";
    public static final String COLUMN_SOURCE_DOMAIN = "source_domain";
    public static final String COLUMN_FULLNAME = "fullname";

    // TODO: points and number of comments can change often, how should we handle these?

    private static final String DATABASE_NAME = "posts.db";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_POSTS + "(" +
            COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_TITLE + " text not null," +
            COLUMN_SUBREDDIT + " text not null," +
            COLUMN_AUTHOR + " text not null," +
            COLUMN_SOURCE + " text not null," +
            COLUMN_THUMBNAIL + " text," +
            COLUMN_POST_ID + " text not null," +
            COLUMN_SOURCE_DOMAIN + " text not null," +
            COLUMN_FULLNAME + " text not null" +
            ");";

    public DBHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_POSTS);
        onCreate(db);
    }


    public void addPost(Post post) {
        SQLiteDatabase db = this.getWritableDatabase();
        try{
            ContentValues values = new ContentValues();
            values.put(COLUMN_TITLE, post.getPostTitle());
            values.put(COLUMN_SUBREDDIT, post.getPostSubreddit());
            values.put(COLUMN_AUTHOR, post.getPostAuthor());
            values.put(COLUMN_SOURCE, post.getPostSource());
            values.put(COLUMN_THUMBNAIL, post.getPostThumbnail());
            values.put(COLUMN_POST_ID, post.getPostId());
            values.put(COLUMN_SOURCE_DOMAIN, post.getPostDomain());
            values.put(COLUMN_FULLNAME, post.getPostFullName());

            db.insert(TABLE_POSTS, null, values);
            db.close();
        }catch (Exception e){
            Log.e("problem", e + "");
        }
    }


    public ArrayList<Post> getAllPosts() {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<Post> postsArrayList = null;
        try{
            postsArrayList = new ArrayList<Post>();
            String QUERY = "SELECT * FROM "+ TABLE_POSTS;
            Cursor cursor = db.rawQuery(QUERY, null);
            if(!cursor.isLast())
            {
                while (cursor.moveToNext())
                {
                    Post post = new Post();
                    post.setId(cursor.getInt(0));
                    post.setPostTitle(cursor.getString(1));
                    post.setPostSubreddit(cursor.getString(2));
                    post.setPostAuthor(cursor.getString(3));
                    post.setPostSource(cursor.getString(4));
                    post.setPostThumbnail(cursor.getString(5));
                    post.setPostId(cursor.getString(6));
                    post.setPostDomain(cursor.getString(7));
                    post.setPostFullName(cursor.getString(8));
                    postsArrayList.add(post);
                }
            }
            db.close();
        }catch (Exception e){
            Log.e("error",e+"");
        }
        return postsArrayList;
    }


    public int getPostCount() {
        int num = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        try{
            String QUERY = "SELECT * FROM "+ TABLE_POSTS;
            Cursor cursor = db.rawQuery(QUERY, null);
            num = cursor.getCount();
            db.close();
            return num;
        }catch (Exception e){
            Log.e("error",e+"");
        }
        return 0;
    }

}
