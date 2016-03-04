package com.mabezdev.MabezWatch;


import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;

import java.util.ArrayList;
import java.util.Set;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;


public class Main extends Activity {

    private static final int REQUEST_ENABLE_BT = 1;
    private Button onBtn;
    private Button offBtn;
    private Button listBtn;
    private Button findBtn;
    private Button dcButton;
    private Button enablePush;
    private TextView text;
    private BluetoothAdapter myBluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private ListView myListView;
    private ArrayAdapter<String> BTArrayAdapter;
    private static BluetoothDevice chosenBT;
    private NotificationReceiver nReceiver;

    private static ArrayList<Bundle> notificationQueue;


    /*
    todo: Add the notificationListenerService to grab my notifications to my phone and push them to my phone http://stackoverflow.com/questions/3030626/android-get-all-the-notifications-by-code
    todo: make the app look nicer
    todo: stop the app fro starting again on format change (portrait to landscape and visa versa)
     */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        notificationQueue = new ArrayList<Bundle>();

        startService(new Intent(getBaseContext(),myNotificationListener.class));

        // take an instance of BluetoothAdapter - Bluetooth radio
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(myBluetoothAdapter == null) {
            onBtn.setEnabled(false);
            offBtn.setEnabled(false);
            listBtn.setEnabled(false);
            findBtn.setEnabled(false);
            text.setText("Status: not supported");

            Toast.makeText(getApplicationContext(),"Your device does not support Bluetooth",
                    Toast.LENGTH_LONG).show();
        } else {
            on();
        }

        setupUI();

        nReceiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.mabez.GET_NOTIFICATIONS");
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

    public static BluetoothDevice getDeviceToConnect(){
        return chosenBT;
    }

    public void on(){
        if (!myBluetoothAdapter.isEnabled()) {
            Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);

            Toast.makeText(getApplicationContext(),"Bluetooth turned on" ,
                    Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(getApplicationContext(),"Bluetooth is already on",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        if(requestCode == REQUEST_ENABLE_BT){
            if(myBluetoothAdapter.isEnabled()) {
                text.setText("Status: Enabled");
            } else {
                text.setText("Status: Disabled");
            }
        }
    }

    public void list(View view){
        // get paired devices
        pairedDevices = myBluetoothAdapter.getBondedDevices();

        // put it's one to the adapter
        for(BluetoothDevice device : pairedDevices)
            BTArrayAdapter.add(device.getName()+ "\n" + device.getAddress());

        Toast.makeText(getApplicationContext(),"Show Paired Devices",
                Toast.LENGTH_SHORT).show();

    }

    final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent

                // add the name and the MAC address of the object to the arrayAdapter
                BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                BTArrayAdapter.notifyDataSetChanged();
            }
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equalsIgnoreCase( action ) )    {//this needs testing
                System.out.println("Device Disconnected change var");
                pairedDevices.remove(device);
            }
        }
    };

    public void find(View view) {
        if (myBluetoothAdapter.isDiscovering()) {
            // the button is pressed when it discovers, so cancel the discovery
            System.out.println("Not searching");
            findBtn.setText("Search For Devices.");
            myBluetoothAdapter.cancelDiscovery();
        }
        else {
            BTArrayAdapter.clear();
            myBluetoothAdapter.startDiscovery();
            System.out.println("Searching");
            findBtn.setText("Tap to stop searching...");

            registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        }
    }

    public void off(){
        myBluetoothAdapter.disable();
        text.setText("Status: Disconnected");

        Toast.makeText(getApplicationContext(),"Bluetooth turned off",
                Toast.LENGTH_LONG).show();
    }

    public void setupUI(){
        text = (TextView) findViewById(R.id.text);

        listBtn = (Button)findViewById(R.id.paired);
        listBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                list(v);
            }
        });

        findBtn = (Button)findViewById(R.id.search);
        findBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                find(v);
            }
        });

        dcButton = (Button) findViewById(R.id.dcButton);
        dcButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(new Intent(getBaseContext(), BTBGService.class));
            }
        });
        enablePush = (Button) findViewById(R.id.notification);
        enablePush.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("NOTICE","Requesting Push notifications");
                Intent i = new Intent("com.mabez.GET_NOTIFICATIONS");
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra("command","list");
                sendBroadcast(i);
            }
        });

        myListView = (ListView)findViewById(R.id.listView1);

        // create the arrayAdapter that contains the BTDevices, and set it to the ListView
        BTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        myListView.setAdapter(BTArrayAdapter);

        // setup onclick listener for items in list view so that we connect from there we want to run bg program feeding the smart watch info
        myListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                for(BluetoothDevice d:pairedDevices){
                    if(d.getAddress().equals(BTArrayAdapter.getItem(position).split("\n")[1])){
                        System.out.println("Found BT Device trying to connect");
                        //start new service to keep in contact with watch in background
                        chosenBT = d;// set the device to connect to
                        startService(new Intent(getBaseContext(),BTBGService.class));

                    }
                }

            }
        });
    }


    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        try {
            unregisterReceiver(bReceiver);
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
                Main.this.addNotification(extras);
            } else {
                System.out.println("Null Notification received.");
            }
        }

    }

}
