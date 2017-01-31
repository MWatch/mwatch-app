package com.mabezdev.mabezwatch.Util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;
import com.mabezdev.mabezwatch.Activities.Main;
import com.mabezdev.mabezwatch.R;

/**
 * Created by mabez on 29/01/17.
 */
public class NotificationTimer {


    private static NotificationCompat.Builder mBuilder;
    private static NotificationManager mNotificationManager;
    private static final int NOTIFICATION_ID_STATIC = 4444;
    private static final int NOTIFICATION_ID_DISMISSIBLE = 7777;

    private long startTime = 0;
    private Context context;

    public NotificationTimer(Context ctx){
        this.context = ctx;
    }

    public void start(long timeMillis){
        mBuilder = new NotificationCompat.Builder(context);
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.startTime = timeMillis;
        show(generateTimeStamp(), NOTIFICATION_ID_STATIC,true);
    }

    public void update(){
        show(generateTimeStamp(),NOTIFICATION_ID_STATIC,true);
    }

    public void stop(){
        if(mNotificationManager == null)
            return;
        show("Disconnected.\nConnection Lasted: " + generateTimeStamp(), NOTIFICATION_ID_DISMISSIBLE,false);
        mNotificationManager.cancel(NOTIFICATION_ID_STATIC);
        this.startTime = 0;
        mNotificationManager = null;
        mBuilder = null;
    }

    private void show(String text,int id,boolean setOnGoing){
        mBuilder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("MabezWatch")
                .setContentText(text)
                .setOngoing(setOnGoing)
                .setTicker(null);

        final Intent notificationIntent = new Intent(context, Main.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        // Adds the back stack for the Intent (but not the Intent itself)
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context,0,notificationIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        mNotificationManager.notify(id,mBuilder.build());
    }

    private String generateTimeStamp(){
        long time = calculateTime();
        long hours = time / 3600;
        long minutes = (time % 3600) / 60;
        long seconds = time % 60;
        return "Connected for " + String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private long calculateTime(){
        return startTime != 0 ? ((System.currentTimeMillis() - startTime)/1000) : 0;
    }
}
