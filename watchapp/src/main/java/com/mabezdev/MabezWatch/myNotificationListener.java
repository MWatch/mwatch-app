package com.mabezdev.MabezWatch;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.mabezdev.MabezWatch.Activities.Main;
import com.mabezdev.MabezWatch.Bluetooth.BTBGService;

import java.util.ArrayList;

/**
 * Created by Mabez on 03/03/2016.
 */
public class myNotificationListener extends NotificationListenerService {

    private ArrayList<String> packageFilter = new ArrayList<>();
    private ArrayList<String> chatAppFilter = new ArrayList<>();
    private ArrayList<Integer> idPool = new ArrayList<>();
    public static final String NOTIFICATION_NEW = "_NEW";
    public static final String NOTIFICATION_REMOVE = "_REMOVE";
    private static final String TAG = "NOTIFICATION_SERVICE";

    @Override
    public void onCreate(){
        super.onCreate();
        Log.i("NOTIFICATION_SERVICE","Started Notification Service");
        IntentFilter filter = new IntentFilter();
        filter.addAction(Main.NOTIFICATION_FILTER);

        //stop system rubbish - eventually filled by a setting screen to choose when notifications
        packageFilter.add("android");
        packageFilter.add("com.mabezdev.MabezWatch");//stop our stuff
        packageFilter.add("clean");

        chatAppFilter.add("com.facebook.orca");
    }

    @Override
    public void onDestroy() {
        //unregisterReceiver(nlServiceReceiver);
        super.onDestroy();
    }

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

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // http://iamrobj.com/how-floatifications-captures-android-notification-content-part-1/ read this
        Intent sendNewToApp = new Intent(Main.NOTIFICATION_FILTER);
        Bundle extras = sbn.getNotification().extras;

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
            sendNewToApp.putExtra("TITLE", extras.getString(Notification.EXTRA_TITLE));
            String nText;
            if(chatAppFilter.contains(sbn.getPackageName())){
                Log.d(TAG,"Found a chat app, only pushing basic text.");
                nText = parseBasicText(extras); // if its a messenger service make sure to only send updates not the full convo text over and over again
            } else {
                nText = parseExtraText(extras) == null ? parseBasicText(extras) : parseExtraText(extras);
            }
            sendNewToApp.putExtra("TEXT", nText); //
            sendBroadcast(sendNewToApp);
        } /* else {
            if(!packageFilter.contains(sbn.getPackageName()))
                Log.i("NOTIFICATION_SERVICE","Not pushing notification with package name: "+sbn.getPackageName());
        } */
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i(TAG,"Notification with id: "+sbn.getId()+" has been removed.");
        if(idPool.contains(sbn.getId())) { // we know it definitely could be on the watch as it has been transmitted during this session
            Log.i(TAG,sbn.getId()+" found in id pool, requesting removal on Watch.");
            idPool.remove((Integer)sbn.getId()); // remove from the pool after
            Intent sendNewToApp = new Intent(Main.NOTIFICATION_FILTER);
            sendNewToApp.putExtra("TYPE", NOTIFICATION_REMOVE);
            sendNewToApp.putExtra("ID", sbn.getId());
            sendBroadcast(sendNewToApp);
        }
    }

}
