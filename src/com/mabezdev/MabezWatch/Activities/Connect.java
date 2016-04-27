package com.mabezdev.MabezWatch.Activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.mabezdev.MabezWatch.Bluetooth.BTBGService;
import com.mabezdev.MabezWatch.Bluetooth.BluetoothHandler;
import com.mabezdev.MabezWatch.Bluetooth.BluetoothUtil;
import com.mabezdev.MabezWatch.R;


/**
 * Created by Mabez on 19/03/2016.
 */
public class Connect extends Activity {

    private Button searchBtn;
    private TextView status;
    private BluetoothAdapter myBluetoothAdapter;
    private BluetoothHandler bluetoothHandler;
    private ListView myListView;
    private ArrayAdapter<String> BTArrayAdapter;
    private BTBGService myBTService;
    private boolean isBound = false;
    private static final int REQUEST_ENABLE_BT = 1;

    @Override
    public void onCreate(Bundle onSavedInstance){
        super.onCreate(onSavedInstance);
        setContentView(R.layout.connect);

        BluetoothManager mng = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        // take an instance of BluetoothAdapter - Bluetooth radio
        myBluetoothAdapter = BluetoothUtil.getDefaultAdapter(mng);


        if(myBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),"Your device does not support Bluetooth",
                    Toast.LENGTH_LONG).show();
        } else {
            BluetoothUtil.turnOnBluetooth(myBluetoothAdapter);
        }

        bluetoothHandler = new BluetoothHandler(this);
        setupUI();
    }



    private void setupUI(){


        myListView = (ListView)findViewById(R.id.listView1);

        // create the arrayAdapter that contains the BTDevices, and set it to the ListView
        BTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        myListView.setAdapter(BTArrayAdapter);

        // setup onclick listener for items in list view so that we connect from there we want to run bg program feeding the smart watch info
        myListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //close this bt handler as we just used it to scan it
                //bluetoothHandler.close();
                //bluetoothHandler = null;
                if(!isBound) {
                    BluetoothUtil.setChosenDeviceMac(BTArrayAdapter.getItem(position).split("\n")[1]);
                    BluetoothUtil.setChosenDeviceName(BTArrayAdapter.getItem(position).split("\n")[0]);

                    Intent btIntent = new Intent(getBaseContext(), BTBGService.class);
                    startService(btIntent);

                    bindService(btIntent, myConnection, Context.BIND_AUTO_CREATE);
                } else {
                    unbindService(myConnection);
                }

            }
        });

        searchBtn = (Button) findViewById(R.id.search);

        bluetoothHandler.setOnScanListener(new BluetoothHandler.OnScanListener() {
            @Override
            public void onScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                boolean contains = false;
                for(int i=0; i < BTArrayAdapter.getCount(); i++){
                    if(BTArrayAdapter.getItem(i).equals(device.getName()+"\n"+device.getAddress())){
                        contains = true;
                    }
                }
                if(!contains) {
                    BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            }

            @Override
            public void onScanFinished() {

            }
        });

        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothHandler.scanLeDevice(true);
            }
        });



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        if(requestCode == REQUEST_ENABLE_BT){
            if(myBluetoothAdapter.isEnabled()) {
                status.setText("Status: Enabled");
            } else {
                status.setText("Status: Disabled");
            }
        }
    }

    public void off(){
        myBluetoothAdapter.disable();
        Toast.makeText(getApplicationContext(),"Bluetooth turned off",
                Toast.LENGTH_LONG).show();
    }

    private ServiceConnection myConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            BTBGService.MyLocalBinder binder = (BTBGService.MyLocalBinder) service;
            myBTService = binder.getService();
            isBound = true;
            Log.i("CONNECT","Service is connected");
        }

        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
            if (isBound) {
                unbindService(myConnection);
                isBound = false;
            }
        }

    };

    @Override
    protected void onDestroy() {
        if (isBound) {
            unbindService(myConnection);
            isBound = false;
        }
        super.onDestroy();
    }
}
