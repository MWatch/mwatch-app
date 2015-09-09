package com.mabezdev.MabezWatch;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Created by Scott on 09/09/2015.
 */
public class BTConnection{

    private static OutputStream outputStream;
    private static BluetoothSocket socket;

    public BTConnection(BluetoothDevice b){
        connect(b);
    }

    public static void connect(BluetoothDevice device) {//doesnt really need to be stastic or public lean up later
        //BluetoothSocket socket = null;
        try {
            if(socket!=null){
                socket=null;
            }
            //Create a Socket connection: need the server's UUID number of registered
            socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));//standard serial string ID

            socket.connect();

            Log.d("EF-BTBee", ">>Client connectted");


            outputStream = socket.getOutputStream();
            outputStream.write(DateFormat.getDateTimeInstance().format(new Date()).getBytes()); //replace with func to send all necessary data ie weather etc

        } catch (IOException e) {
            Log.e("EF-BTBee", "", e);
        } finally {
            disconnect();
        }
    }

    public static void disconnect(){//called upon service ondestroy
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
            }
        }
    }
}
