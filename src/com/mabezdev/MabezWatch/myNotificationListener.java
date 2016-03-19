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

/**
 * Created by Mabez on 03/03/2016.
 */
public class myNotificationListener extends NotificationListenerService {

    private NLServiceReceiver nlservicereciver;
    //private final IBinder mBinder = new LocalBinder();

    @Override
    public void onCreate(){
        super.onCreate();
        Log.i("NOTICE","Started Notification Service");
        nlservicereciver = new NLServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Main.NOTIFICATION_FILTER);
        registerReceiver(nlservicereciver,filter);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        //works perfectly
        Intent sendNewToApp = new Intent(Main.NOTIFICATION_FILTER);
        Bundle extras = sbn.getNotification().extras;
        sendNewToApp.putExtra("PKG",sbn.getPackageName());
        sendNewToApp.putExtra("TITLE",extras.getString("android.title"));
        if(extras.getCharSequence("android.text")!=null){
            sendNewToApp.putExtra("TEXT", extras.getCharSequence("android.text").toString());
        } else {
            sendNewToApp.putExtra("TEXT", extras.getCharSequence("android.textLines").toString());
        }
        sendBroadcast(sendNewToApp);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i("NOT","SOMETHING REMOVED");
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
