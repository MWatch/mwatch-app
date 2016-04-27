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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import com.mabezdev.MabezWatch.Bluetooth.BTBGService;
import com.mabezdev.MabezWatch.Bluetooth.BluetoothUtil;
import com.mabezdev.MabezWatch.Bluetooth.DeviceSave;
import com.mabezdev.MabezWatch.R;
import com.mabezdev.MabezWatch.Util.ObjectReader;
import com.mabezdev.MabezWatch.myNotificationListener;


public class Main extends Activity {

    private Button connectButton;
    private Button enablePush;
    private Button addFilter;
    private Button pairButton;
    private EditText filterBox;
    private NotificationReceiver nReceiver;
    private static ArrayList<Bundle> notificationQueue;
    private static ArrayList<String> filter;
    public static String BLUETOOTH_FILE;
    public static final String NOTIFICATION_FILTER = "com.mabez.GET_NOTIFICATIONS";
    private boolean isConnected = false;


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

    @Deprecated
    public void addNotification(Bundle notification){
        notificationQueue.add(notification);
        printNotificationQueue();
    }
    @Deprecated
    public static ArrayList<Bundle> getNotificationQueue(){
        return notificationQueue;
    }

    @Deprecated
    public static void removeNotifications(ArrayList e){
        notificationQueue.removeAll(e);
    }

    @Deprecated
    public void printNotificationQueue(){
        for(Bundle extras : notificationQueue){
            System.out.println("Package: " + extras.getString("PKG"));
            System.out.println("Title: " + extras.getString("TITLE"));
            System.out.println("Text: " + extras.getString("TEXT"));
        }
        System.out.println("Number of Notifications: "+notificationQueue.size());
    }



    public void setupUI(){

        /*autoConnect = (Switch) findViewById(R.id.connectSwitch);
        autoConnect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Intent autoGo = new Intent(getBaseContext(),BTBGService.class);
                if(isChecked){
                    //load saved device address
                    DeviceSave store = (DeviceSave) ObjectReader.readObject(BLUETOOTH_FILE);
                    if(store != null) {
                        BluetoothUtil.setChosenDeviceName(store.getDeviceName());
                        BluetoothUtil.setChosenDeviceMac(store.getDeviceAddress());
                        startService(autoGo);
                        autoConnect.setText("Connected");
                    } else {
                        autoConnect.setChecked(false);
                        Toast.makeText(getBaseContext(),"No device stored! Connect manually first!",Toast.LENGTH_SHORT).show();
                    }
                } else {
                    stopService(autoGo);
                    autoConnect.setText("Quick Connect");
                }
            }
        });*/


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


        connectButton = (Button) findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isConnected){
                    connectButton.setText("Disconnect");
                } else {
                    connectButton.setText("Connect");
                }
                //stopService(new Intent(getBaseContext(), BTBGService.class));
            }
        });

        pairButton = (Button) findViewById(R.id.pairButton);
        pairButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Main.this,Connect.class));
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
        notification.setLatestEventInfo(ctx, "Titlethatsisover15characterslong", eventtext,
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
