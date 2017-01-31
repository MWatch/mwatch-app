package com.mabezdev.mabezwatch.Util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.util.SparseArray;
import com.mabezdev.mabezwatch.Model.AppInfoItem;
import com.mabezdev.mabezwatch.Model.AppInfoStore;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by mabez on 31/01/17.
 */
public class AppInfoHandler {

    private static ArrayList<AppInfoItem> appInfoItems = new ArrayList<>();
    //todo Issue tokens (hasmap keys) to apps objects that need to see/modify data then they use this class to modify the array
    // i.e Search once, get token, use token over and over again to view modify that element
    private static HashMap<Integer, AppInfoItem> appInfoItemsHash = new HashMap<>();
    public static final String TAG = "AppInfoHandler";

    public static boolean saveToFile(File path){
        Log.i(TAG,"Saving AppInfoStore to: "+path.getPath());
        // create the object to be serialized
        AppInfoStore toSave = new AppInfoStore();
        toSave.setInfoStore(appInfoItems);
        //write to file as byte stream
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path));
            oos.writeObject(toSave);
            oos.flush();
            oos.close();
        } catch (IOException e) {
            //e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean loadFromFile(File path){
        Log.i(TAG,"Loading AppInfoStore from: "+path.getPath());
        // create the variable for data to be loaded into
        AppInfoStore toLoad;
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path));
            toLoad = (AppInfoStore) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            //e.printStackTrace();
            return false;
        }
        appInfoItems = toLoad.getInfoStore();
        System.out.println("Loaded the following");
//        for(AppInfoItem i : appInfoItems){
//            System.out.println(i.getPackageName());
//        }
        return true;
    }

    public static void discoverNewApps(List<ApplicationInfo> appList){
        int newAppCount = 0;
        for (ApplicationInfo packageInfo : appList) {
            for(AppInfoItem info : appInfoItems){
                if(!info.getPackageName().equals(packageInfo.packageName)){
                    // found a new installation add it to the array
                    appInfoItems.add(new AppInfoItem(packageInfo.packageName,false,false));
                    newAppCount++;
                }
            }
        }
        Log.i(TAG,"New App discovery complete. " + newAppCount + " app(s) discovered");
    }

    // shouldnt be used, as we should always modify the reference in the array
    public static AppInfoItem get(String packageName){
        return null;
    }

    public static void setBlackListed(int token, boolean isBlacklisted){

    }

    public static void setChatApp(int token, boolean isChatApp){

    }

    public AppInfoHandler getInstance(){
        return this;
    }

}
