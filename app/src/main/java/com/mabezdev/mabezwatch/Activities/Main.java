package com.mabezdev.mabezwatch.Activities;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.mabezdev.mabezwatch.R;
import com.mabezdev.mabezwatch.Services.WatchConnection;
import com.mabezdev.mabezwatch.Util.AppInfoHandler;
import com.mabezdev.mabezwatch.Util.NotificationTimer;

import java.io.File;

import static com.mabezdev.mabezwatch.Constants.APP_INFO_STORE_FNAME;
import static com.mabezdev.mabezwatch.Constants.CONNECTION_TIMEOUT;
import static java.lang.System.currentTimeMillis;


public class Main extends AppCompatActivity {


    private TextView tvConnectionStatus;
    private Button bConnect, bPushNotification;
    private WatchConnection watchConnection;
    private boolean isBound = false;
    private boolean isConnected = false;
    private ActionBar mActionBar;
    private long startConnectionMillis = -1;

    private NotificationTimer notificationTimer;
    private Handler notificationUpdateHandler;

    private final String TAG = Main.class.getSimpleName();

    private static final int PERMISSION_COARSE_REQUEST_CODE = 1;

    private AppInfoHandler appInfoHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar mainToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(mainToolbar);

        mActionBar = getSupportActionBar();

        initializeComponents();

        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},PERMISSION_COARSE_REQUEST_CODE);

        requestNotificationListenerPermissions();

        notificationTimer = new NotificationTimer(this);

        notificationUpdateHandler = new Handler();

        if(!AppInfoHandler.loadFromFile(new File(getFilesDir().getPath() + File.separator + APP_INFO_STORE_FNAME))){
            Log.i(TAG,"AppInfo Failed to load, this may be due to permissions or this is a first time run.");
        }

        AppInfoHandler.discoverNewApps(getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA));
    }

    private Runnable notificationUpdater = new Runnable() {
        @Override
        public void run() {
            notificationTimer.update();
            if(startConnectionMillis != -1){
                if((currentTimeMillis() - startConnectionMillis) > CONNECTION_TIMEOUT){
                    Log.i(TAG,"Bluetooth connection has timed out, resetting.");
                    startConnectionMillis = -1;
                    watchConnection.disconnect();
                    runOnUiThread(() -> {
                        tvConnectionStatus.setTextColor(getColor(R.color.disconnected));
                        tvConnectionStatus.setText(getString(R.string.disconnected));
                    });
                }
            }
            notificationUpdateHandler.postDelayed(this, 1000);
        }
    };

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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_toolbar_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_open_settings:
                Intent i = new Intent(Main.this, Preferences.class);
                startActivity(i);
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

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
                if (!((BluetoothManager) Main.this.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent , 0);
                } else {
                    if(isConnected){
                        watchConnection.disconnect();
                    } else {
                        tvConnectionStatus.setTextColor(getResources().getColor(R.color.searching));
                        tvConnectionStatus.setText(getResources().getString(R.string.searching));
                        if(isBound){
                            Log.i(TAG,"Service already bound, starting new BLE connection.");
                            watchConnection.start();
                        } else {
                            Log.i(TAG,"Starting service, then starting new BLE connection.");
                            // start the service
                            Intent btIntent = new Intent(getBaseContext(), WatchConnection.class);
                            bindService(btIntent, btServiceConnection, Context.BIND_AUTO_CREATE);
                        }
                        // start the timeout
                        startConnectionMillis = currentTimeMillis();
                    }
                }

                // handle button spamming - once pressed cannot be activated for 2 seconds
                bConnect.setEnabled(false);
                bConnect.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        bConnect.setEnabled(true);
                        bConnect.removeCallbacks(this);
                    }
                }, 2000);
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
                public void onConnection(boolean connected) { //TODO change from boolean to Int connection state connecting, connected, disconneceted
                    String text;
                    String bText;
                    int colorId;

                    if(connected){
                        startConnectionMillis = -1; //reset timeout var
                        isConnected = true;
                        Log.i(TAG,"Connected to MabezWatch");
                        text = getResources().getString(R.string.connected);
                        bText = getResources().getString(R.string.bDisconnect);
                        colorId = R.color.connected;
                        notificationUpdateHandler.postDelayed(notificationUpdater,1000);
                        notificationTimer.start(currentTimeMillis());

                    } else {
                        isConnected = false;
                        text = getResources().getString(R.string.disconnected);
                        bText = getResources().getString(R.string.bConnect);
                        colorId = R.color.disconnected;
                        notificationUpdateHandler.removeCallbacks(notificationUpdater); // stop updating the notification
                        notificationTimer.stop();
                    }

                    runOnUiThread(() -> {
                        tvConnectionStatus.setTextColor(getResources().getColor(colorId));
                        tvConnectionStatus.setText(text);
                        bConnect.setText(bText);
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
