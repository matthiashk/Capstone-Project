package com.matthiasko.scrollforreddit.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.matthiasko.scrollforreddit.models.ScrollComment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Created by matthiasko on 5/11/16.
 * Database handler for comments items
 *
 */
public class CommentsDBHandler extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "comments.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "comments";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_BODY = "body";
    public static final String COLUMN_AUTHOR = "author";
    public static final String COLUMN_SCORE = "score";
    public static final String COLUMN_DEPTH = "depth";
    public static final String COLUMN_POST_ID = "post_id";
    public static final String COLUMN_DATE_ADDED = "date_added";
    public static final String COLUMN_COMMENT_ID = "comment_id";

    // note the placement of the commas here
    private static final String DATABASE_CREATE = "create table "
            + TABLE_NAME + "(" +
            COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_BODY + " text not null," +
            COLUMN_AUTHOR + " text not null," +
            COLUMN_SCORE + " integer," +
            COLUMN_DEPTH + " integer," +
            COLUMN_POST_ID + " text not null," +
            COLUMN_DATE_ADDED + " datetime," +
            COLUMN_COMMENT_ID + " text not null" +
            ");";

    public CommentsDBHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // added datetime to database
    // using methods from http://tips.androidhive.info/2013/10/android-insert-datetime-value-in-sqlite-database/
    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    public void addComment(ScrollComment comment) {
        SQLiteDatabase db = this.getWritableDatabase();
        try{
            ContentValues values = new ContentValues();
            values.put(COLUMN_BODY, comment.getBody());
            values.put(COLUMN_AUTHOR, comment.getAuthor());
            values.put(COLUMN_SCORE, comment.getScore());
            values.put(COLUMN_DEPTH, comment.getDepth());
            values.put(COLUMN_POST_ID, comment.getPostId());
            values.put(COLUMN_DATE_ADDED, getDateTime());
            values.put(COLUMN_COMMENT_ID, comment.getId());

            db.insert(TABLE_NAME, null, values);
            db.close();
        }catch (Exception e){
            Log.e("problem", e + "");
        }
    }

    public ArrayList<ScrollComment> getAllComments(String postId) {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<ScrollComment> commentsList = null;
        try{
            commentsList = new ArrayList<>();
            String QUERY = "SELECT * FROM " + TABLE_NAME + " WHERE post_id=?";
            Cursor cursor = db.rawQuery(QUERY, new String[] { postId });
            if(!cursor.isLast())
            {
                while (cursor.moveToNext())
                {
                    ScrollComment comment = new ScrollComment();

                    comment.setBody(cursor.getString(1));
                    comment.setAuthor(cursor.getString(2));
                    comment.setScore(cursor.getInt(3));
                    comment.setDepth(cursor.getInt(4));
                    comment.setPostId(cursor.getString(5));
                    // date not used yet
                    comment.setId(cursor.getString(7));

                    commentsList.add(comment);
                }
            }
            db.close();
        }catch (Exception e){
            Log.e("error",e+"");
        }
        return commentsList;
    }

    public int getCommentsCount(String postId) {
        int num = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        try{
            //String QUERY = "SELECT * FROM "+TABLE_NAME + "WHERE post_id=" + postId;
            String[] columns = {"body", "author", "score", "depth", "post_id"};
            Cursor cursor = db.query(TABLE_NAME, columns, "post_id=?", new String[]{postId}, null, null, null);
            num = cursor.getCount();
            db.close();
            return num;
        }catch (Exception e){
            Log.e("error",e+"");
        }
        return 0;
    }
}
