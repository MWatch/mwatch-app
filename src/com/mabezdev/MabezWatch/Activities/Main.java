package com.mabezdev.MabezWatch.Activities;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.Bundle;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;

import java.util.ArrayList;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import com.mabezdev.MabezWatch.Bluetooth.BTBGService;
import com.mabezdev.MabezWatch.Bluetooth.BluetoothUtil;
import com.mabezdev.MabezWatch.Bluetooth.DeviceSave;
import com.mabezdev.MabezWatch.R;
import com.mabezdev.MabezWatch.Util.ObjectReader;
import com.mabezdev.MabezWatch.myNotificationListener;

import static android.R.attr.text;


public class Main extends Activity {

    private Button dcButton;
    private Button pair;
    private Button enablePush;
    private Button addFilter;
    private EditText filterBox;
    private Switch autoConnect;
    private NotificationReceiver nReceiver;
    private static ArrayList<Bundle> notificationQueue;
    private static ArrayList<String> filter;

    public static String BLUETOOTH_FILE;
    public static final String NOTIFICATION_FILTER = "com.mabez.GET_NOTIFICATIONS";


    /*
    todo: Add the notificationListenerService to grab my notifications to my phone and push them to my phone http://stackoverflow.com/questions/3030626/android-get-all-the-notifications-by-code
    todo: make the app look nicer
    todo: stop the app fro starting again on format change (portrait to landscape and visa versa)
     */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        BLUETOOTH_FILE = getFilesDir()+"/preffered_device.bin";

        notificationQueue = new ArrayList<Bundle>();
        filter = new ArrayList<String>();
        //startListening for notifications
        startService(new Intent(getBaseContext(),myNotificationListener.class));
        //setup UI components
        setupUI();

        //register custom receiver to communication with the notification service listener.
        nReceiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();

        filter.addAction(NOTIFICATION_FILTER);
        registerReceiver(nReceiver,filter);


    }

    public void addNotification(Bundle notification){
        notificationQueue.add(notification);
        printNotificationQueue();
    }

    public static ArrayList<Bundle> getNotificationQueue(){
        return notificationQueue;
    }

    public static void removeNotifications(ArrayList e){
        notificationQueue.removeAll(e);
    }


    public void printNotificationQueue(){
        for(Bundle extras : notificationQueue){
            System.out.println("Package: " + extras.getString("PKG"));
            System.out.println("Title: " + extras.getString("TITLE"));
            System.out.println("Text: " + extras.getString("TEXT"));
        }
        System.out.println("Number of Notifications: "+notificationQueue.size());
    }



    public void setupUI(){

        autoConnect = (Switch) findViewById(R.id.connectSwitch);
        autoConnect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Intent autoGo = new Intent(getBaseContext(),BTBGService.class);
                if(isChecked){
                    //load saved device address
                    DeviceSave store = (DeviceSave) ObjectReader.readObject(BLUETOOTH_FILE);
                    //connect from saved bluetooth device
                    BluetoothUtil.turnOnBluetooth(BluetoothUtil.getDefaultAdapter());
                    BluetoothUtil.setChosenDevice(BluetoothUtil.getDeviceFromAddress(store.getDeviceAddress(),BluetoothUtil.getDefaultAdapter()));
                    startService(autoGo);
                } else {
                    stopService(autoGo);
                }
            }
        });


        filterBox = (EditText) findViewById(R.id.editText);

        addFilter = (Button) findViewById(R.id.addFilter);
        addFilter.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                filter.add(filterBox.getText().toString().toLowerCase());
                Toast.makeText(getApplicationContext(),"Added filter for '"+filterBox.getText().toString()+"'", Toast.LENGTH_LONG).show();
                filterBox.setText("");
            }
        });


        pair = (Button) findViewById(R.id.connect);
        pair.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent goToConnect = new Intent(Main.this,Connect.class);
                startActivity(goToConnect);
            }
        });

        dcButton = (Button) findViewById(R.id.dcButton);
        dcButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(new Intent(getBaseContext(), BTBGService.class));
            }
        });
        /*
        Debugging tool - >
         */
        enablePush = (Button) findViewById(R.id.notification);
        enablePush.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                /*Log.i("NOTICE","Requesting Push notifications");
                Intent i = new Intent(NOTIFICATION_FILTER);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra("command","list");
                sendBroadcast(i);*/
                showNotification("The following text is over 128 chars: Contrary to popular belief, Lorem Ipsum is not simply random text. It has roots in a piece of classical Latin literature from 45 BC, making it over 2000 years old.",
                        Main.this);
            }
        });


    }

    private void showNotification(String eventtext, Context ctx) {



        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.ic_launcher,
                "Du hello", System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this
        // notification
        PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0,
                new Intent(ctx, Main.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(ctx, "Title", eventtext,
                contentIntent);

        // Send the notification.
        NotificationManager mng = (NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        mng.notify("Title", 0, notification);
    }


    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        try {
            unregisterReceiver(nReceiver);
        }catch (Exception e){
            System.out.println("bReceiver was never used therefore not unregistered");
        }

    }

    class NotificationReceiver extends BroadcastReceiver{
        @Override

        public void onReceive(Context context, Intent intent) {
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            Bundle extras = intent.getExtras();
            if(extras.getString("PKG")!=null) {
                if(!filter.contains(extras.getString("PKG"))) {
                    Main.this.addNotification(extras);
                } else {
                    System.out.println("Notification from package '"+extras.getString("PKG")+"' has been filtered.");
                }
            } else {
                System.out.println("Null Notification received.");
            }
        }

    }

}
