package com.mabezdev.MabezWatch.Activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.mabezdev.MabezWatch.Bluetooth.BTBGService;
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
    private TextView status;
    private BluetoothAdapter myBluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private ListView myListView;
    private ArrayAdapter<String> BTArrayAdapter;
    private static BluetoothDevice chosenBT;
    private static final int REQUEST_ENABLE_BT = 1;

    @Override
    public void onCreate(Bundle onSavedInstance){
        super.onCreate(onSavedInstance);
        setContentView(R.layout.connect);
        setupUI();

        // take an instance of BluetoothAdapter - Bluetooth radio
        myBluetoothAdapter = BluetoothUtil.getDefaultAdapter();
        if(myBluetoothAdapter == null) {
            listBtn.setEnabled(false);

            Toast.makeText(getApplicationContext(),"Your device does not support Bluetooth",
                    Toast.LENGTH_LONG).show();
        } else {
            BluetoothUtil.turnOnBluetooth(myBluetoothAdapter);
        }
    }

    private void setupUI(){
        listBtn = (Button)findViewById(R.id.paired);
        listBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                list(v);
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
                        BluetoothUtil.setChosenDevice(d);// set the device to connect to
                        startService(new Intent(getBaseContext(),BTBGService.class));

                    }
                }

            }
        });
        status = (TextView) findViewById(R.id.text);

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
