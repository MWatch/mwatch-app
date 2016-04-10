package com.mabezdev.MabezWatch.Bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import com.mabezdev.MabezWatch.Activities.Main;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * Created by Scott on 09/09/2015.
 */
public class BTConnection{

    private  OutputStream outputStream;
    private BluetoothSocket socket;
    private BluetoothDevice btdev;
    private int retries = 0;

    //title, text

    public BTConnection(BluetoothSocket socket){
        this.socket = socket;
    }

    public boolean initialize(String address){
        while(btdev==null) {
            btdev = createDevice(address);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            retries++;
            Log.d("BTSERVICE","Attempting BT Connection... "+retries);
            if(retries >= 5){
                Log.e("BTSERVICE","Connection timed out.");
                return false;
            }
        }
        return true;
    }


    private BluetoothDevice createDevice(String mac){
        return BluetoothUtil.getDeviceFromAddress(mac, BluetoothAdapter.getDefaultAdapter());
    }

    public void connect() {
        try {
            if (socket != null) {
                socket = null;
            }
            //Create a Socket connection: need the server's UUID number of registered
            socket = btdev.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));//standard serial string ID

            socket.connect();

            outputStream = socket.getOutputStream();

            Log.d("EF-BTBee", ">>Client connected");


        } catch (IOException e) {
            Log.e("EF-BTBee", "", e);
        }
    }
}
