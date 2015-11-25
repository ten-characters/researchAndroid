package com.ten_characters.researchAndroid.server;

import org.json.JSONObject;



/**
 * Created by austin on 24/06/15.
 * A simple Callback for working with Async Tasks
 */
public interface OnTaskCompleted {
    void onTaskCompleted(JSONObject result);

}


