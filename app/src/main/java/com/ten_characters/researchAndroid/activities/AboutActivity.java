package com.ten_characters.researchAndroid.activities;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.ten_characters.researchAndroid.R;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class AboutActivity extends ActionBarActivity {

    private static final String LOG_TAG = AboutActivity.class.getSimpleName();
    private ListView mListView;
    private String[] mListOptions;
    private ArrayAdapter<String> mListAdapter;

    /** This is used to implement our custom font, initialized in the GlobalApp.java file*/
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        // Populate the list with a ListView Adapter
        mListOptions = getResources().getStringArray(R.array.about_items);
        mListAdapter = new ArrayAdapter<>(
                this,
                R.layout.about_list_item,
                mListOptions);
        mListView = (ListView) findViewById(R.id.about_options_list);

        // Set the adapter to finish em off!
        mListView.setAdapter(mListAdapter);
    }
}
