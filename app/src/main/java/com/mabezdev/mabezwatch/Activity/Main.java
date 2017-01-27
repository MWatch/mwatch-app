package com.mabezdev.mabezwatch.Activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.mabezdev.mabezwatch.R;
import com.mabezdev.mabezwatch.Service.WatchConnection;

public class Main extends AppCompatActivity {


    private TextView tvConnectionStatus;
    private Button bConnect, bPushNotification;
    private WatchConnection watchConnection;
    private boolean isBound = false;
    private boolean isConnected = false;

    private final String TAG = Main.class.getSimpleName();

    private static final int PERMISSION_COARSE_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeComponents();

        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},PERMISSION_COARSE_REQUEST_CODE);

        requestNotificationListenerPermissions();

    }

    private void requestNotificationListenerPermissions(){
        String notificationListenerString = Settings.Secure.getString(this.getContentResolver(),"enabled_notification_listeners");
        //Check notifications access permission
        if (notificationListenerString == null || !notificationListenerString.contains(getPackageName()))
        {
            Log.i(TAG, "Notification listening permission denied!.");
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Functionality limited");
            builder.setMessage("Notification listening required for full interaction with MabezWatch, do you want to enable it now?");
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                }
            });
            builder.show();
        }else{
            Log.i(TAG,"Notification listener permissions already granted.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for(String p : permissions){
            System.out.println(p);
        }
        switch (requestCode) {
            case PERMISSION_COARSE_REQUEST_CODE: {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Coarse location permission denied.");
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Coarse location is required for Bluetooth Low Energy!");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {

        }
        if (resultCode == RESULT_CANCELED) {
            Toast.makeText(getApplicationContext(), "Bluetooth is required for this app!", Toast.LENGTH_LONG).show();
        }
    }

    private void initializeComponents(){
        bConnect = (Button) findViewById(R.id.buttonConnect);

        bConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("MAIN","Button clicked!");
                // make sure bluetooth is enabled before connecting
                if (!((BluetoothManager)Main.this.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent , 0);
                } else {
                    System.out.println("isBound = "+isBound);
                    if(!isBound) {
                        // start the service
                        Intent btIntent = new Intent(getBaseContext(), WatchConnection.class);
                        bindService(btIntent, btServiceConnection, Context.BIND_AUTO_CREATE);
                    } else {
                        if(isConnected){
                            watchConnection.disconnect();
                        } else {
                            watchConnection.start(); // re log
                        }
                    }
                }
            }
        });

        bPushNotification = (Button) findViewById(R.id.buttonPushNotification);

        tvConnectionStatus = (TextView) findViewById(R.id.textViewConnectionState);


    }

    private ServiceConnection btServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            WatchConnection.MyLocalBinder binder = (WatchConnection.MyLocalBinder) service;
            watchConnection = binder.getService();
            Log.i("MAIN","Service is connected.");
            isBound = true;
            watchConnection.start();

            watchConnection.setOnConnectionListener(new WatchConnection.OnConnectionListener() {
                @Override
                public void onConnection(boolean connected) {
                    String text;
                    String bText;
                    if(connected){
                        isConnected = true;
                        Log.i(TAG,"Connected to MabezWatch");
                        text = getResources().getString(R.string.connected);
                        bText = getResources().getString(R.string.bDisconnect);

                    } else {
                        isConnected = false;
                        text = getResources().getString(R.string.disconnected);
                        bText = getResources().getString(R.string.bConnect);
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvConnectionStatus.setText(text);
                            bConnect.setText(bText);
                        }
                    });
                }
            });
        }

        public void onServiceDisconnected(ComponentName arg0){
            isBound = false;
            Log.i("MAIN","Service disconnected.");
        }

    };


}
