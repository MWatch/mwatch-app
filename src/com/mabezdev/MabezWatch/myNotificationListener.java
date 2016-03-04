package com.mabezdev.MabezWatch;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

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
        filter.addAction("com.mabez.GET_NOTIFICATIONS");
        registerReceiver(nlservicereciver,filter);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        //works perfectly
        Intent sendNewToApp = new Intent("com.mabez.GET_NOTIFICATIONS");
        Bundle extras = sbn.getNotification().extras;
        sendNewToApp.putExtra("PKG",sbn.getPackageName());
        sendNewToApp.putExtra("TITLE",extras.getString("android.title"));
        sendNewToApp.putExtra("TEXT",extras.getCharSequence("android.text").toString());
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
                    StatusBarNotification[] sbnArray =  myNotificationListener.this.getActiveNotifications();
                        for (int i = 0; i< sbnArray.length; i++) {
                            StatusBarNotification sbn = sbnArray[i];
                            Intent sendItBackToApp = new Intent("com.mabez.GET_NOTIFICATIONS");
                            if(sbn.getPackageName()!=null) {
                                sendItBackToApp.putExtra("PKG", sbn.getPackageName());
                                Bundle content = sbn.getNotification().extras;
                                try {
                                    sendItBackToApp.putExtra("TITLE", content.getString("android.title"));
                                    if(content.getCharSequence("android.text").toString()==null){
                                        sendItBackToApp.putExtra("TEXT", "ERROR: Notification with "+content.getString("android.title")+"Contains no text");
                                    } else {
                                        sendItBackToApp.putExtra("TEXT", content.getCharSequence("android.text").toString());
                                    }

                                    //sendItBackToApp.putExtra("TEXT", sbn.getNotification().tickerText.toString());
                                    sendItBackToApp.putExtra("LENGTH", Integer.toString(myNotificationListener.this.getActiveNotifications().length));
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                                sendBroadcast(sendItBackToApp);
                            } else {
                                Log.i("DEBUG","Notification is null.");
                            }
                        }
                }

        }
    }

}
