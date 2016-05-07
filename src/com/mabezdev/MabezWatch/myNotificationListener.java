package com.mabezdev.MabezWatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
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
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(nlServiceReceiver);
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        //todo check EXTRA_BIG_TEXT, EXTRA_TEXT,EXTRA_TEXT,EXTRA_TEXT_LINES and all other possibles to get all the data we can
        Intent sendNewToApp = new Intent(Main.NOTIFICATION_FILTER);
        Bundle extras = sbn.getNotification().extras;
        if(sbn.getId()== BTBGService.NOTIFICATION_ID){
            //so we dont spam the log with out timer notification
        } else if(!packageFilter.contains(sbn.getPackageName()) && sbn.isClearable()) {
            sendNewToApp.putExtra("PKG", sbn.getPackageName());
            if(extras.getString("android.title")!=null) {
                sendNewToApp.putExtra("TITLE", extras.getString("android.title"));
            } else {
                sendNewToApp.putExtra("TITLE", sbn.getNotification().tickerText);
            }
            if (extras.getCharSequence("android.text") != null) {
                sendNewToApp.putExtra("TEXT", extras.getCharSequence("android.text"));
            } else {
                sendNewToApp.putExtra("TEXT", extras.getCharSequence("android.textLines"));
            }
            sendBroadcast(sendNewToApp);
        } else {
            Log.i("NOTIFICATION_SERVICE","Not pushing notification with package name: "+sbn.getPackageName());
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
