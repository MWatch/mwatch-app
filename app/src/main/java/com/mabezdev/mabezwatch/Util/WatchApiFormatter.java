package com.mabezdev.mabezwatch.Util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by mabez on 25/01/17.
 */
public class WatchApiFormatter {

    public final static byte[] STX = {0x2};
    public final static byte[] ETX = {0x3};
    public final static byte[] DELIM = {0x1F};
    public final static String NOTIFICATION_TAG = "N";
    public final static String DATE_TAG = "d";
    public final static String WEATHER_TAG = "w";
    public final static String REMOVAL_TAG = "r";

    public final static String INTERVAL_TAG = "<i>";
    public final static String END_TAG = "*";  // was *

    private final static int CHUNK_SIZE = 64; //64 bytes of data
    private static final int DATA_LENGTH = 500; // max size of data

    public static String[] formatWeatherData(String day,String temp,String forecast){
        String[] data = new String[5];
        data[0] = WEATHER_TAG;
        data[1] = day;
        data[2] = INTERVAL_TAG + temp;
        data[3] = INTERVAL_TAG + forecast;
        data[4] = END_TAG;
        return data;
    }

    public static String[] formatRemoval(int id) {
        String[] removeNotification = new String[3];
        removeNotification[0] = REMOVAL_TAG;
        removeNotification[1] = Integer.toString(id);
        removeNotification[2] = END_TAG;
        return removeNotification;
    }

    public static String[] formatNotificationData(int id,String pkg, String title, String text) {
        if(pkg!=null && title!=null && text!=null) {
            ArrayList<String> format = new ArrayList<String>();
            if (pkg.length() >= 15) {
                pkg = pkg.substring(0, 14);
            }
            if (title.length() >= 15) {
                title = title.substring(0, 14);
            }
            format.add(new String(STX));
            format.add(NOTIFICATION_TAG);
            format.add(new String(DELIM));
            format.add(title);
            //TODO TEXT
            format.add(new String(ETX));


            return format.toArray(new String[format.size()]);
        } else {
            return null;
        }
    }

    public static String[] formatNotificationDataOld(int id,String pkg, String title, String text){
        /*
            Data struct on watch:
                -Package name = 15 characters
                -Title = 15 characters
                -Text = up to 500 characters
         */
        if(pkg!=null && title!=null && text!=null) {
            ArrayList<String> format = new ArrayList<String>();
            if (pkg.length() >= 15) {
                pkg = pkg.substring(0, 14);
            }
            if (title.length() >= 15) {
                title = title.substring(0, 14);
            }
            format.add(NOTIFICATION_TAG); // only for app meta side sake, this never gets sent
            format.add(id + INTERVAL_TAG + pkg + INTERVAL_TAG + title + INTERVAL_TAG);
            int charIndex = 0;
            String temp = "";
            //make sure we don't array out of bounds on the watch
            int len = text.length();
            if(len > DATA_LENGTH){
                len = DATA_LENGTH;
            }

            if (text.length() > CHUNK_SIZE) {
                for (int i = 0; i < len; i++) { //max 150 for message + 20 chars for tags
                    if (charIndex >= CHUNK_SIZE) {//send in chunks of 64 chars
                        format.add(temp);
                        temp = "";
                        charIndex = 0;
                    } else {
                        temp += text.charAt(i);
                    }
                    charIndex++;
                }
                //this adds the last piece of data if it is under 64 characters
                format.add(temp);
            } else {
                format.add(text);
            }
            //format.add(text);
            format.add(END_TAG);
            return format.toArray(new String[format.size()]);
        } else {
            return null;
        }
    }

    public static String[] formatDateData(){
        Date myDate = new Date();
        SimpleDateFormat ft =
                new SimpleDateFormat("dd MM yyyy HH mm ss");
        String[] date = new String[3];
        date[0] = DATE_TAG;
        date[1] = ft.format(myDate);
        date[2] = END_TAG;
        return date;
    }
}
