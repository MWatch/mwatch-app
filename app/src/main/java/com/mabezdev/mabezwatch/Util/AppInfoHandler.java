package com.mabezdev.mabezwatch.Util;

import com.mabezdev.mabezwatch.Model.AppInfoItem;

import java.util.ArrayList;

/**
 * Created by mabez on 31/01/17.
 */
public class AppInfoHandler {

    private static ArrayList<AppInfoItem> appInfoItems = new ArrayList<>();
    private static boolean hasLoadedFromFile = false;
    private static final String filename = "AppInfoStore.bin";

    public static ArrayList<AppInfoItem> load(){
        return appInfoItems;
    }

    public static boolean save(ArrayList<AppInfoItem> arrayList){
        return true;
    }

}
