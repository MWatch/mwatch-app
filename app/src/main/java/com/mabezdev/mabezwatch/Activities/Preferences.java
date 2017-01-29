package com.mabezdev.mabezwatch.Activities;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.mabezdev.mabezwatch.R;

import java.util.List;

/**
 * Created by mabez on 29/01/17.
 */

public class Preferences extends AppCompatActivity {

    private static final String TAG = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        final PackageManager pm = getPackageManager();
        //get a list of installed apps.
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        Log.i(TAG,"Installed apps that might push notifications: ");
        for (ApplicationInfo packageInfo : packages) {
            Log.d(TAG, "Installed package :" + packageInfo.packageName);
            //Log.d(TAG, "Source dir : " + packageInfo.sourceDir);
            //Log.d(TAG, "Launch Activity :" + pm.getLaunchIntentForPackage(packageInfo.packageName));
        }
    }
}
