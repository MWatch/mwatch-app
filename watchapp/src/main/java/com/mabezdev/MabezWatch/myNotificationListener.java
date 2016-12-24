package com.mabezdev.MabezWatch;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;
import com.mabezdev.MabezWatch.Activities.Main;
import com.mabezdev.MabezWatch.Bluetooth.BTBGService;

import java.util.ArrayList;

/**
 * Created by Mabez on 03/03/2016.
 */
public class myNotificationListener extends NotificationListenerService {

    private NLServiceReceiver nlServiceReceiver;
    //private final IBinder mBinder = new LocalBinder();
    private ArrayList<String> packageFilter = new ArrayList<String>();

    @Override
    public void onCreate(){
        super.onCreate();
        Log.i("NOTIFICATION_SERVICE","Started Notification Service");
        nlServiceReceiver = new NLServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Main.NOTIFICATION_FILTER);
        registerReceiver(nlServiceReceiver,filter);

        //stop system crap
        packageFilter.add("android");
        packageFilter.add("com.mabezdev.MabezWatch");//stop our stuff
        packageFilter.add("clean");
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(nlServiceReceiver);
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        //todo check EXTRA_BIG_TEXT, EXTRA_TEXT,EXTRA_TEXT_LINES and all other possibles to get all the data we can
        // http://iamrobj.com/how-floatifications-captures-android-notification-content-part-1/ read this
        Intent sendNewToApp = new Intent(Main.NOTIFICATION_FILTER);
        Bundle extras = sbn.getNotification().extras;
        if((!(packageFilter.contains(sbn.getPackageName())) && sbn.isClearable()) || sbn.getId() == 3333) { //3333 is our test notification ID
            sendNewToApp.putExtra("PKG", sbn.getPackageName());
            if(extras.getString(Notification.EXTRA_TITLE)!=null) {
                sendNewToApp.putExtra("TITLE", extras.getString(Notification.EXTRA_TITLE)+""); // +"" is used to try and stop spannable string cast excetioonms
            } else {
                sendNewToApp.putExtra("TITLE", sbn.getNotification().tickerText+"");
            }
            if (extras.getCharSequence(Notification.EXTRA_TEXT) != null) {
                sendNewToApp.putExtra("TEXT", extras.getCharSequence(Notification.EXTRA_TEXT)+""); // add toStrings once we know what ones to use
            } else {
                sendNewToApp.putExtra("TEXT", extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)+"");
            }
            sendBroadcast(sendNewToApp);
        } else {
            if(!sbn.getPackageName().equals("com.mabezdev.MabezWatch")){
                Log.i("NOTIFICATION_SERVICE","Not pushing notification with package name: "+sbn.getPackageName());
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i("NOTIFICATION_SERVICE","Notification Removed.");
    }


    class NLServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //only relevant for testing
                if (intent.hasExtra("command") && intent.getStringExtra("command").equals("list")) {
                    StatusBarNotification[] sbnArray = myNotificationListener.this.getActiveNotifications();
                    if (sbnArray != null) {
                        for (int i = 0; i < sbnArray.length; i++) {
                            StatusBarNotification sbn = sbnArray[i];
                            Intent sendItBackToApp = new Intent(Main.NOTIFICATION_FILTER);
                            if (sbn.getPackageName() != null) {
                                sendItBackToApp.putExtra("PKG", sbn.getPackageName());
                                Bundle content = sbn.getNotification().extras;
                                try {
                                    sendItBackToApp.putExtra("TITLE", content.getString("android.title"));
                                    if (content.getCharSequence("android.text") != null) {
                                        sendItBackToApp.putExtra("TEXT", content.getCharSequence("android.text").toString());
                                    } else {
                                        sendItBackToApp.putExtra("TEXT", content.getCharSequence("android.textLines").toString());
                                    }

                                    //sendItBackToApp.putExtra("TEXT", sbn.getNotification().tickerText.toString());
                                    sendItBackToApp.putExtra("LENGTH", Integer.toString(myNotificationListener.this.getActiveNotifications().length));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                sendBroadcast(sendItBackToApp);
                            } else {
                                Log.i("DEBUG", "Notification is null.");
                            }
                        }
                    }
                }

        }
    }

}
