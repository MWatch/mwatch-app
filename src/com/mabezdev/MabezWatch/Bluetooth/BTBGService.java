package com.mabezdev.MabezWatch.Bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.widget.Toast;
import com.mabezdev.MabezWatch.Activities.Main;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;

/**
 * Created by Scott on 09/09/2015.
 */
public class BTBGService extends Service {

    private NotificationReceiver notificationReceiver;
    private final static String NOTIFICATION_TAG = "<n>";
    private final static String DATE_TAG = "<d>";
    private final static String TITLE_TAG = "<t>";
    private final static String DATA_INTERVAL_TAG = "<i>";
    private final static String CLOSE_TAG = "<e>";
    private final static String END_TAG = "<f>";
    private final static int CHUNK_SIZE = 64;
    private BTConnection connection;

    @Override
    public int onStartCommand(Intent i,int flags,int srtID){

        //register our receiver to listen to our other service
        notificationReceiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Main.NOTIFICATION_FILTER);
        registerReceiver(notificationReceiver,filter);

        /*
        Here we need to handle connection of bluetooth device
        and handle reconnection and eventual shutdown of service after a number of timeouts
         */
        //need to be on a thread

        connection = new BTConnection(BluetoothUtil.getChosenMac());
        connection.connect();

        return START_STICKY;
    }

    private String[] formatNotificationData(String pkg, String title, String text){
        //todo need to check that the text is not too big and need to add a phase to read
        //the rest on the phone!
        ArrayList<String> format = new ArrayList<String>();
        format.add(NOTIFICATION_TAG+pkg);
        format.add(TITLE_TAG+title+CLOSE_TAG);
        int charIndex = 0;
        String temp = "";
        if(text.length() > CHUNK_SIZE) {
            for (char c : text.toCharArray()) {
                if (charIndex >= CHUNK_SIZE) {//send in chunks of 64 chars
                    format.add(DATA_INTERVAL_TAG+temp);
                    temp = "";
                    charIndex = 0;
                } else {
                    temp += c;
                }
                charIndex++;
            }
        } else {
            format.add(DATA_INTERVAL_TAG+text);
        }
        format.add(END_TAG);
        temp ="";
        for(String item: format){
            temp+=item;
        }
        System.out.println("Sending: "+temp);
        return format.toArray(new String[format.size()]);
    }

    private String formatDateData(){
        return DATE_TAG+ DateFormat.getDateTimeInstance().format(new Date())+END_TAG;
    }

    //
    private void transmit(String[] formattedData){
        //delay between each statement it received individual
        for(int i=0; i < formattedData.length; i++){
            connection.write(formattedData[i]);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void transmit(String single){

    }

    @Override
    public IBinder onBind(Intent intent) {
        //c

        return null;
    }

    @Override
    public void onDestroy(){
        System.out.println("Stopping BTBGService");
        Toast.makeText(this,"BT Device disconnected",Toast.LENGTH_LONG).show();
        unregisterReceiver(notificationReceiver);
        super.onDestroy();
    }

    /*
    Create a broadcast receiver to grab notifications and add them to a queue
    then the queue is processed one by one making sure data is received with a proper sending
    algorithm.
     */

    private class NotificationReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String pkgName = intent.getStringExtra("PKG");
            String title = intent.getStringExtra("TITLE");
            String text = intent.getStringExtra("TEXT");
            //now package this up and transmit

            transmit(formatNotificationData(pkgName,title,text));
            Toast.makeText(context,"Notification sending",Toast.LENGTH_LONG).show();
        }
    }
}
