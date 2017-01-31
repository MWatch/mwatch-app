package com.mabezdev.mabezwatch.Activities;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ListViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import com.mabezdev.mabezwatch.Activities.PreferenceActivities.NotificationPreferences;
import com.mabezdev.mabezwatch.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mabez on 29/01/17.
 */

public class Preferences extends AppCompatActivity {

    private static final String TAG = "";
    private ActionBar prefToolbar;
    private ListViewCompat settingsListView;
    private ArrayList<String> settings = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);
        Toolbar toolbar = (Toolbar) findViewById(R.id.preferences_toolbar);
        setSupportActionBar(toolbar);

        prefToolbar = getSupportActionBar();
        if(prefToolbar != null) {
            prefToolbar.setTitle(R.string.app_settings);
            prefToolbar.setDisplayHomeAsUpEnabled(true);
        }

        initializeViews();
    }

    private void initializeViews(){
        settingsListView = (ListViewCompat) findViewById(R.id.settingsListView);

        settingsListView.setDividerHeight(4);

        settingsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                System.out.println("ListView clicked at index: "+i);
                Intent intent = null;
                switch (i){
                    case 0 :
                        intent = new Intent(Preferences.this, NotificationPreferences.class);
                }

                startActivity(intent);
            }
        });
    }
}
