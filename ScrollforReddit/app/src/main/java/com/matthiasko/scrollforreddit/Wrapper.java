package com.matthiasko.scrollforreddit;

import net.dean.jraw.models.Captcha;

/**
 * Created by matthiasko on 6/16/16.
 */
public class Wrapper {
    public Captcha captcha;
    public String title;
    public String userInput;
    public String selectedSubredditName;

    public Captcha getCaptcha() {
        return captcha;
    }

    public void setCaptcha(Captcha captcha) {
        this.captcha = captcha;
    }

    public String getSelectedSubredditName() {
        return selectedSubredditName;
    }

    public void setSelectedSubredditName(String selectedSubredditName) {
        this.selectedSubredditName = selectedSubredditName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }
}
