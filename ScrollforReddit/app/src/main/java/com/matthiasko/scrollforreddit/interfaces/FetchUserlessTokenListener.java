package com.matthiasko.scrollforreddit.interfaces;

/**
 * Created by matthiasko on 4/25/16.
 * Used to run methods after the asynctask has run
 *
 */
public interface FetchUserlessTokenListener {
    void onUserlessTokenFetched(); // hides loading animation
    void onSubredditNotFound(); // displays error dialog if needed
}
