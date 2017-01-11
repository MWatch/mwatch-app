package com.mabezdev.MabezWatch.Activities;



import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.*;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.app.Activity;

import java.util.ArrayList;
import java.util.List;


import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import android.widget.*;
import com.mabezdev.MabezWatch.Bluetooth.BTBGService;
import com.mabezdev.MabezWatch.Bluetooth.BluetoothHandler;
import com.mabezdev.MabezWatch.Bluetooth.BluetoothUtil;

import com.mabezdev.MabezWatch.R;
import com.mabezdev.MabezWatch.Util.NotificationUtils;
import com.mabezdev.MabezWatch.myNotificationListener;




public class Main extends Activity { // extend AppCompatActivity when we need

    private Button enablePush, pushWeather;
    private Button addFilter;
    private Button scanButton;
    private TextView statusText;
    private EditText filterBox;
    private static ArrayList<String> filter;
    public static String BLUETOOTH_FILE;
    public static final String NOTIFICATION_FILTER = "com.mabez.GET_NOTIFICATIONS";
    private boolean isConnected = false;
    private BTBGService myBTService;
    private BluetoothAdapter myBluetoothAdapter;
    private boolean isBound = false;
    private boolean isFound = false;
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothHandler bluetoothHandler;

    private static final String TAG = "MAIN_ACTIVITY";

    private Handler timerHandler;
    private long startTime = 0;
    public static final int NOTIFICATION_ID = 4444;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        BLUETOOTH_FILE = getFilesDir()+"/preffered_device.bin";

        BluetoothManager mng = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        // take an instance of BluetoothAdapter - Bluetooth radio
        myBluetoothAdapter = BluetoothUtil.getDefaultAdapter(mng);

        if(myBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),"Your device does not support Bluetooth",
                    Toast.LENGTH_LONG).show();
        } else {
            BluetoothUtil.turnOnBluetooth(myBluetoothAdapter);
        }

        //notificationQueue = new ArrayList<Bundle>();
        filter = new ArrayList<>();

        //startListening for notifications
        startService(new Intent(getBaseContext(),myNotificationListener.class));



        bluetoothHandler = new BluetoothHandler(this);

        //setup UI components
        setupUI();


        final PackageManager pm = getPackageManager();
        //get a list of installed apps.
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        Log.i(TAG,"Installed apps that might push notifications: ");
        for (ApplicationInfo packageInfo : packages) {
            Log.d(TAG, "Installed package :" + packageInfo.packageName);
            //Log.d(TAG, "Source dir : " + packageInfo.sourceDir);
            //Log.d(TAG, "Launch Activity :" + pm.getLaunchIntentForPackage(packageInfo.packageName));
        }


    }

    public void setupUI(){

        statusText = (TextView) findViewById(R.id.statusText);

        scanButton = (Button) findViewById(R.id.scanButton);

        scanButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
               // scan and look for MabezWatch in the name
                if(!myBluetoothAdapter.isEnabled()) { // check if bluetooth is enabled
                    //turn on bluetooth
                    myBluetoothAdapter.enable();
                }

                while(!myBluetoothAdapter.isEnabled());

                if (!isConnected) {
                    bluetoothHandler.scanLeDevice(true);
                    statusText.setText(R.string.searching);
                    statusText.setTextColor(getResources().getColor(R.color.searching));
                } else {
                    killService();
                }

            }
        });

        bluetoothHandler.setOnScanListener(new BluetoothHandler.OnScanListener() {
            @Override
            public void onScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                if(device!=null) {
                    if(device.getName()!=null){
                        if (device.getName().equals("MabezWatch")) {
                            if (!isFound) {
                                //bind service here
                                BluetoothUtil.setChosenDeviceMac(device.getAddress());
                                BluetoothUtil.setChosenDeviceName(device.getName());
                                isFound = true;
                                Intent btIntent = new Intent(getBaseContext(), BTBGService.class);
                                startService(btIntent);
                                bindService(btIntent, myConnection, Context.BIND_AUTO_CREATE);
                            }

                        }
                        Log.i("Main", "Found BLE Device with name: " + device.getName());
                    }
                }
            }

            @Override
            public void onScanFinished() {
                //if we found it say so if not say
                if (!isFound){
                    Toast.makeText(getBaseContext(), "No MabezWatch Found.", Toast.LENGTH_SHORT).show();
                    statusText.setText(R.string.Disconnected);
                    statusText.setTextColor(getResources().getColor(R.color.disconnected));
                }
            }
        });


        filterBox = (EditText) findViewById(R.id.editText);

        filterBox.setVisibility(View.INVISIBLE);

        addFilter = (Button) findViewById(R.id.addFilter);
        addFilter.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                filter.add(filterBox.getText().toString().toLowerCase());
                Toast.makeText(getApplicationContext(),"Added filter for '"+filterBox.getText().toString()+"'", Toast.LENGTH_LONG).show();
                filterBox.setText("");
            }
        });

        addFilter.setVisibility(View.INVISIBLE);




        /*
        Debugging tools - >
         */
        enablePush = (Button) findViewById(R.id.notification);
        enablePush.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotificationUtils.showNotification(Main.this,"The following text is over 128 chars: Contrary to popular belief, Lorem Ipsum is not simply random text. It has roots in a piece of classical Latin literature from 45 BC, making it over 2000 years old."
                        ,true,true,3333);
            }
        });

        pushWeather = (Button) findViewById(R.id.weather);
        pushWeather.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isConnected){
                    myBTService.queryWeather();
                } else {
                    Toast.makeText(Main.this,"Cannot push weather data when unconnected.", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    private void killService() {
        if(isBound) {
            Log.i("MAIN", "Disconnecting service.");
            myBTService.stopSelf();
            unbindService(myConnection);
            isFound = false;
            isBound = false;
        } else {
            Log.i("MAIN", "Can't disconnect, not connected.");
        }
    }

    private ServiceConnection myConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            BTBGService.MyLocalBinder binder = (BTBGService.MyLocalBinder) service;
            myBTService = binder.getService();
            isBound = true;
            Log.i("MAIN","Service is connected");

            myBTService.setOnConnectedListener(new BTBGService.OnConnectedListener() {
                @Override
                public void onConnected() {
                    isConnected = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scanButton.setText(R.string.buttonConnected);
                            statusText.setText(R.string.Connected); //"Connected\n         to\nMabezWatch."
                            statusText.setTextColor(getResources().getColor(R.color.connected));
                        }
                    });
                    NotificationUtils.showNotification(Main.this,"Connected to MabezWatch ("+BluetoothUtil.getChosenDeviceMac()+").",false,false,NOTIFICATION_ID);

                    startTime = System.currentTimeMillis();
                    timerHandler = new Handler();
                    timerHandler.postDelayed(timerRunnable,0);
                }

                @Override
                public void onDisconnected() {
                    isConnected = false;
                    killService();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scanButton.setText(R.string.buttonDisconnected);
                            statusText.setText(R.string.Disconnected); //"    Not\nConnected."
                            statusText.setTextColor(getResources().getColor(R.color.disconnected));
                        }
                    });

                    if(timerHandler!=null) {
                        timerHandler.removeCallbacks(timerRunnable);
                        timerHandler = null;
                    }

                    NotificationUtils.removeNotification(NOTIFICATION_ID);
                    long time = calculateTime();
                    long hours = time / 3600;
                    long minutes = (time % 3600) / 60;
                    long seconds = time % 60;
                    NotificationUtils.showNotification(Main.this,"Disconnected, connection lasted:\n"+String.format("%02d:%02d:%02d", hours, minutes, seconds),true,true,1111);

                }
            });
        }

        public void onServiceDisconnected(ComponentName arg0){
            killService();
        }

    };

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long time = calculateTime();
            long hours = time / 3600;
            long minutes = (time % 3600) / 60;
            long seconds = time % 60;
            NotificationUtils.updateNotification(String.format("%02d:%02d:%02d", hours, minutes, seconds),NOTIFICATION_ID);
            timerHandler.postDelayed(timerRunnable,1000);
        }

    };

    private long calculateTime(){
        if(startTime !=0){
            return ((System.currentTimeMillis() - startTime)/1000);
        } else {
            return 0;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        killService();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT){
            if(myBluetoothAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(),"Bluetooth activated.",Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getBaseContext(),"Bluetooth deactivated.",Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void off(){
        myBluetoothAdapter.disable();
        Toast.makeText(getApplicationContext(),"Bluetooth turned off",
                Toast.LENGTH_LONG).show();
    }

}
