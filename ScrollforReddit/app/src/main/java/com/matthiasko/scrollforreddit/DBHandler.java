package com.matthiasko.scrollforreddit;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.matthiasko.scrollforreddit.PostContract.PostEntry;

import java.util.ArrayList;

/**
 * Created by matthiasko on 4/13/16.
 */
public class DBHandler extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "posts.db";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + PostEntry.TABLE_NAME + "(" +
            PostEntry._ID + " integer primary key autoincrement, " +
            PostEntry.COLUMN_TITLE + " text not null," +
            PostEntry.COLUMN_SUBREDDIT + " text not null," +
            PostEntry.COLUMN_AUTHOR + " text not null," +
            PostEntry.COLUMN_SOURCE + " text not null," +
            PostEntry.COLUMN_THUMBNAIL + " text," +
            PostEntry.COLUMN_POST_ID + " text not null," +
            PostEntry.COLUMN_SOURCE_DOMAIN + " text not null," +
            PostEntry.COLUMN_FULLNAME + " text not null," +
            PostEntry.COLUMN_SCORE + " integer," +
            PostEntry.COLUMN_NUMBER_OF_COMMENTS + " integer" +
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
        db.execSQL("DROP TABLE IF EXISTS " + PostEntry.TABLE_NAME);
        onCreate(db);
    }


    public void addPost(Post post) {
        SQLiteDatabase db = this.getWritableDatabase();
        try{
            ContentValues values = new ContentValues();
            values.put(PostEntry.COLUMN_TITLE, post.getPostTitle());
            values.put(PostEntry.COLUMN_SUBREDDIT, post.getPostSubreddit());
            values.put(PostEntry.COLUMN_AUTHOR, post.getPostAuthor());
            values.put(PostEntry.COLUMN_SOURCE, post.getPostSource());
            values.put(PostEntry.COLUMN_THUMBNAIL, post.getPostThumbnail());
            values.put(PostEntry.COLUMN_POST_ID, post.getPostId());
            values.put(PostEntry.COLUMN_SOURCE_DOMAIN, post.getPostDomain());
            values.put(PostEntry.COLUMN_FULLNAME, post.getPostFullName());
            values.put(PostEntry.COLUMN_SCORE, post.getPostPoints());
            values.put(PostEntry.COLUMN_NUMBER_OF_COMMENTS, post.getPostNumberOfComments());
            db.insert(PostEntry.TABLE_NAME, null, values);
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
            String QUERY = "SELECT * FROM "+ PostEntry.TABLE_NAME;
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
                    post.setPostPoints(cursor.getInt(9));
                    post.setPostNumberOfComments(cursor.getInt(10));
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
            String QUERY = "SELECT * FROM "+ PostEntry.TABLE_NAME;
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
