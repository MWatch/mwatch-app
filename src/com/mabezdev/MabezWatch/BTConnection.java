package com.mabezdev.MabezWatch;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Created by Scott on 09/09/2015.
 */
public class BTConnection{

    private static OutputStream outputStream;
    private static BluetoothSocket socket;
    private static boolean canConnect = true;
    private static BluetoothDevice btdev;

    //title, text

    public BTConnection(BluetoothDevice b){
        btdev = b;
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
            canConnect = true;

            Log.d("EF-BTBee", ">>Client connected");


            outputStream = socket.getOutputStream();
            String date = "<d>"+DateFormat.getDateTimeInstance().format(new Date());
            outputStream.write(date.getBytes()); //replace with func to send all necessary data ie weather etc
            //once sent remember to remove them from the queue

            Thread.sleep(500);

            ArrayList toRemove = new ArrayList();
            for(Bundle extra: Main.getNotificationQueue()){
                String notification = "<n>"+extra.getString("PKG");
                for(int i = 0; i < 2; i++){
                    notification += "<i>"+extra.getString("TITLE");
                    notification += "<i>"+extra.getString("TEXT");
                }
                Log.d("EF-BTBee",notification);
                outputStream.write(notification.getBytes());
                toRemove.add(extra);
                //make sure they send separately
                Thread.sleep(500);
            }

            Main.removeNotifications(toRemove);


        } catch (IOException e) {
            Log.e("EF-BTBee", "", e);
            //retry connection
            //need timeout for when its actually dc'd for good

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            connect(btdev);
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

    public static boolean canConnect(){
        return canConnect;
    }
}
