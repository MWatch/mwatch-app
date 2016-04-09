package com.mabezdev.MabezWatch.Bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
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
    private boolean isConnected;
    private BluetoothDevice btdev;
    private int retries = 0;

    //title, text

    public BTConnection(String address){
        btdev = createDevice(address);
    }

    private boolean init(String address){
        try{
            btdev = createDevice(address);
            if(btdev!=null){
                return true;
            }
        } catch (Exception e){
            retries++;
        }
        return false;
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

    public void write(String formattedData){
        try {
            outputStream.write(formattedData.getBytes());
            outputStream.flush();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void disconnect(){//called upon service onDestroy
        if (socket != null) {
            try {
                Log.d("EF-BTBee", ">>Client Close");
                // give a chance to send final message
                try {
                    Thread.sleep(1000);
                }catch (Exception e){

                }
                //close streams
                outputStream.close();
                socket.close();
                socket=null;

                return;
            } catch (IOException e) {
                Log.e("EF-BTBee", "", e);
            }catch(NullPointerException e1){
                e1.printStackTrace();
            }
        }
    }
}
