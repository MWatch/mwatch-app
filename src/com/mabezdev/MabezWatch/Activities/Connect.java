package com.mabezdev.MabezWatch.Activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.*;
import com.mabezdev.MabezWatch.Bluetooth.BTBGService;
import com.mabezdev.MabezWatch.Bluetooth.BluetoothHandler;
import com.mabezdev.MabezWatch.Bluetooth.BluetoothUtil;
import com.mabezdev.MabezWatch.Bluetooth.DeviceSave;
import com.mabezdev.MabezWatch.R;
import com.mabezdev.MabezWatch.Util.ObjectWriter;

import java.util.Set;

/**
 * Created by Mabez on 19/03/2016.
 */
public class Connect extends Activity {

    private Button listBtn;
    private Button searchBtn;
    private Button disconnectButton;
    private TextView status;
    private BluetoothAdapter myBluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private BluetoothHandler bluetoothHandler;
    private ListView myListView;
    private ArrayAdapter<String> BTArrayAdapter;
    private static BluetoothDevice chosenBT;
    private static final int REQUEST_ENABLE_BT = 1;
    private boolean deviceConnected = false;

    @Override
    public void onCreate(Bundle onSavedInstance){
        super.onCreate(onSavedInstance);
        setContentView(R.layout.connect);

        BluetoothManager mng = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        // take an instance of BluetoothAdapter - Bluetooth radio
        myBluetoothAdapter = BluetoothUtil.getDefaultAdapter(mng);


        if(myBluetoothAdapter == null) {
            listBtn.setEnabled(false);

            Toast.makeText(getApplicationContext(),"Your device does not support Bluetooth",
                    Toast.LENGTH_LONG).show();
        } else {
            BluetoothUtil.turnOnBluetooth(myBluetoothAdapter);
        }

        bluetoothHandler = new BluetoothHandler(this);

        setupUI();
    }



    private void setupUI(){
        listBtn = (Button)findViewById(R.id.paired);
        listBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                list(v);
            }
        });

        //todo remvoe this button completely s were never gunna use it
        listBtn.setVisibility(View.INVISIBLE);

        disconnectButton = (Button) findViewById(R.id.disconnectButton);
        disconnectButton.setVisibility(View.INVISIBLE);


        myListView = (ListView)findViewById(R.id.listView1);

        // create the arrayAdapter that contains the BTDevices, and set it to the ListView
        BTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        myListView.setAdapter(BTArrayAdapter);

        // setup onclick listener for items in list view so that we connect from there we want to run bg program feeding the smart watch info
        myListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //close this bt handler as we just used it to scan it
                BluetoothUtil.setChosenMac(BTArrayAdapter.getItem(position));
                startService(new Intent(getBaseContext(),BTBGService.class));
            }
        });
        status = (TextView) findViewById(R.id.text);

        searchBtn = (Button) findViewById(R.id.search);

        bluetoothHandler.setOnScanListener(new BluetoothHandler.OnScanListener() {
            @Override
            public void onScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                BTArrayAdapter.add(device.getAddress());
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

    private void list(View v){
        pairedDevices = BluetoothUtil.getBluetoothDevices(myBluetoothAdapter);

        for(BluetoothDevice device : pairedDevices)
            BTArrayAdapter.add(device.getName()+ "\n" + device.getAddress());

        Toast.makeText(getApplicationContext(),"Show Paired Devices",
                Toast.LENGTH_SHORT).show();
    }

    public static BluetoothDevice getDeviceToConnect(){
        return chosenBT;
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

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();

        BluetoothUtil.storeDevice(chosenBT);

    }
}
