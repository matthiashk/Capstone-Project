package com.matthiasko.scrollforreddit.interfaces;

import java.util.ArrayList;

/**
 * Created by matthiasko on 4/22/16.
 * Used for running createNavMenuItems after getting arrayList from the asynctask
 */
public interface AsyncListener {
    void createNavMenuItems( ArrayList<String> arrayList );
}
