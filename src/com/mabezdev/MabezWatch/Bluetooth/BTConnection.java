package com.mabezdev.MabezWatch.Bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;
import com.mabezdev.MabezWatch.Activities.Main;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by Scott on 09/09/2015.
 */
public class BTConnection{

    private static OutputStream outputStream;
    private static BluetoothSocket socket;
    private static boolean isConnected;
    private static BluetoothDevice btdev;
    private static int retries = 0;
    private static Service service;

    //title, text

    public BTConnection(BluetoothDevice b){
        btdev = b;
        //service = s;
        connect(btdev);
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

            Log.d("EF-BTBee", ">>Client connected");

            // todo : remember to uncomment the date stuff once debugging is done!
            outputStream = socket.getOutputStream();
            //String date = "<d>"+DateFormat.getDateTimeInstance().format(new Date())+"*";
            //outputStream.write(date.getBytes()); //replace with func to send all necessary data ie weather etc
            //once sent remember to remove them from the queue

            Thread.sleep(250);

            ArrayList toRemove = new ArrayList();
            for(Bundle extra: Main.getNotificationQueue()){
                String notification = "<n>"+extra.getString("PKG");
                for(int i = 0; i < 2; i++) {
                    notification += "<i>" + extra.getString("TITLE");
                    notification += "<i>" + extra.getString("TEXT");
                }
                String toSend = notification+"*";
                Log.d("EF-BTBee",toSend);
                outputStream.write(toSend.getBytes());
                toRemove.add(extra);
                //make sure they send separately
                Thread.sleep(250);
            }

            Main.removeNotifications(toRemove);


        } catch (IOException e) {
            Log.e("EF-BTBee", "", e);
            //retry 5 times then kill the service to save battery todo: this might not work as this class gets restarted every 5 seconds
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            if(retries <=5) {
                connect(btdev);
                retries++;
            } else {
                //service.stopSelf();
                System.out.println("Should now stop service!");
            }
        } catch(NullPointerException e1) {
            e1.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally{
            disconnect();
        }
    }

    public static void disconnect(){//called upon service onDestroy
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
