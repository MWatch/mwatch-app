package com.mabezdev.MabezWatch.Util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import com.mabezdev.MabezWatch.Activities.Main;
import com.mabezdev.MabezWatch.R;

/**
 * Created by Mabez on 07/05/2016.
 */
public class NotificationUtils {

    private static NotificationCompat.Builder mBuilder;
    private static NotificationManager mNotificationManager;

    public static void showNotification(Context ctx, String text, boolean canClear,boolean removeOnClick,int id){
        mBuilder =
                new NotificationCompat.Builder(ctx)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("MabezWatch")
                        .setContentText(text)
                        .setOngoing(!canClear)
                        .setTicker(null)
                        .setAutoCancel(removeOnClick);
//        // Creates an explicit intent for an Activity in your app
//        Intent resultIntent = new Intent(ctx, Main.class);
//
//        TaskStackBuilder stackBuilder = TaskStackBuilder.create(ctx);
//        stackBuilder.addParentStack(Main.class);
//
//        // Adds the Intent that starts the Activity to the top of the stack
//        stackBuilder.addNextIntent(resultIntent);
//        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT);
//        mBuilder.setContentIntent(resultPendingIntent);

        //  this code correctly resumes after tapping the notification
        final Intent notificationIntent = new Intent(ctx, Main.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.

        // Adds the back stack for the Intent (but not the Intent itself)
        PendingIntent resultPendingIntent = PendingIntent.getActivity(ctx,0,notificationIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(resultPendingIntent);


        mNotificationManager =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(id, mBuilder.build());
    }

    public static void updateNotification(String text,int id){//take in time here and connected status
        mBuilder.setSubText(text);
        mNotificationManager.notify(id,mBuilder.build());
    }



    public static void removeNotification(int id){
        if(mNotificationManager!=null) {
            mNotificationManager.cancel(id);
        }
    }
}
