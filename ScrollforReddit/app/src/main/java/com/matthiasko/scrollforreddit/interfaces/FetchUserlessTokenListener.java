package com.matthiasko.scrollforreddit.interfaces;

/**
 * Created by matthiasko on 4/25/16.
 */
public interface FetchUserlessTokenListener {
    void onUserlessTokenFetched();
    void onSubredditNotFound();

}
