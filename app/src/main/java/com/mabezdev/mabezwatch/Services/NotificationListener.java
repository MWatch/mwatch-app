package com.mabezdev.mabezwatch.Services;

import android.app.Notification;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;

import static com.mabezdev.mabezwatch.Constants.NOTIFICATION_FILTER;

/**
 * Created by mabez on 25/01/17.
 */

public class NotificationListener extends NotificationListenerService {

    private ArrayList<String> packageFilter = new ArrayList<>();
    private ArrayList<String> chatAppFilter = new ArrayList<>();
    private ArrayList<Integer> idPool = new ArrayList<>();
    public static final String NOTIFICATION_NEW = "_NEW";
    public static final String NOTIFICATION_REMOVE = "_REMOVE";
    private static final String TAG = "NOTIFICATION_SERVICE";

    @Override
    public void onListenerConnected() {
        Log.i(TAG,"Started Notification Service");
        IntentFilter filter = new IntentFilter();
        filter.addAction(NOTIFICATION_FILTER);

        //stop system rubbish - eventually filled by a setting screen to choose when notifications
        packageFilter.add("android");
        packageFilter.add("com.mabezdev.MabezWatch");//stop our stuff
        packageFilter.add("clean");

        chatAppFilter.add("com.facebook.orca");
    }

    @Override
    public void onListenerDisconnected() {

    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Intent sendNewToApp = new Intent(NOTIFICATION_FILTER);
        Bundle extras = NotificationCompat.getExtras(sbn.getNotification());

        sendNewToApp.putExtra("TYPE",NOTIFICATION_NEW);
        /*
            Filters by package, then checks that it is not an update to a already posted notification (like our connection info notification) using isOnGoing()
         */
        if(sbn.getId() == 3333 || !packageFilter.contains(sbn.getPackageName()) && !sbn.isOngoing() ) { //3333 is our test notification ID, remove after debugging
//            System.out.println("Basic Notification text parsed:\n"+parseBasicText(extras)+"\n\n");
//            System.out.println("Extra Notification text parsed:\n"+parseExtraText(extras)+"\n\n");
            idPool.add(sbn.getId());
            sendNewToApp.putExtra("ID",sbn.getId()); // will be used to dismiss notifications on the watch if we have seen them on the phone
            sendNewToApp.putExtra("PKG", sbn.getPackageName());
            sendNewToApp.putExtra("TITLE", extras.getString(Notification.EXTRA_TITLE).toString());
            String nText;

            if(chatAppFilter.contains(sbn.getPackageName())){
                nText = parseBasicText(extras); // if its a messenger service make sure to only send updates not the full convo text over and over again
            } else {
                String extraText = parseExtraText(extras);
                nText = extraText == null ? parseBasicText(extras) : extraText;
            }
            sendNewToApp.putExtra("TEXT", nText); //
            sendBroadcast(sendNewToApp);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i(TAG,"Notification with id: "+sbn.getId()+" has been removed.");
        if(idPool.contains(sbn.getId())) { // we know it definitely could be on the watch as it has been transmitted during this session
            Log.i(TAG,sbn.getId()+" found in id pool, requesting removal on Watch.");
            idPool.remove((Integer)sbn.getId()); // remove from the pool after
            Intent sendNewToApp = new Intent(NOTIFICATION_FILTER);
            sendNewToApp.putExtra("TYPE", NOTIFICATION_REMOVE);
            sendNewToApp.putExtra("ID", sbn.getId());
            sendBroadcast(sendNewToApp);
        }
    }

    /*
        Util Methods
     */

    private String parseBasicText(Bundle extras){
        CharSequence chars =
                extras.getCharSequence(Notification.EXTRA_TEXT);
        if(!TextUtils.isEmpty(chars))
            return chars.toString();
        else if(!TextUtils.isEmpty((chars =
                extras.getString(Notification.EXTRA_SUMMARY_TEXT))))
            return chars.toString();
        else
            return null;
    }

    private String parseExtraText(Bundle extras){
        CharSequence[] lines =
                extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if(lines != null && lines.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (CharSequence msg : lines)
                if (!TextUtils.isEmpty(msg)) {
                    sb.append(msg.toString());
                    sb.append('\n');
                }
            return sb.toString().trim();
        }
        CharSequence chars =
                extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if(!TextUtils.isEmpty(chars))
            return chars.toString();
        return null;
    }
}
