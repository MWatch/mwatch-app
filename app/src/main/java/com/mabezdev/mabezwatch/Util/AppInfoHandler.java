package com.mabezdev.mabezwatch.Util;

import android.content.Context;
import android.util.Log;
import com.mabezdev.mabezwatch.Model.AppInfoItem;
import com.mabezdev.mabezwatch.Model.AppInfoStore;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by mabez on 31/01/17.
 */
public class AppInfoHandler {

    private static ArrayList<AppInfoItem> appInfoItems = new ArrayList<>();
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
        return true;
    }

    public static AppInfoItem get(String packageName){
        return null;
    }

    public AppInfoHandler getInstance(){
        return this;
    }

}
