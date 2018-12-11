package com.mabezdev.mabezwatch.Services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.*;
import android.util.Log;
import com.mabezdev.mabezwatch.Util.BluetoothLeHandler;
import com.mabezdev.mabezwatch.Util.WatchApiFormatter;

import java.util.LinkedList;
import java.util.Queue;

import static com.mabezdev.mabezwatch.Constants.*;
import static com.mabezdev.mabezwatch.Services.NotificationListener.NOTIFICATION_NEW;
import static com.mabezdev.mabezwatch.Util.WatchApiFormatter.NOTIFICATION_TAG;
import static com.mabezdev.mabezwatch.Util.WatchApiFormatter.formatDateData;
import static com.mabezdev.mabezwatch.Util.WatchUtil.sleep;

/**
 * Created by mabez on 25/01/17.
 */
public class WatchConnection extends Service {

    private final IBinder myBinder = new MyLocalBinder();
    private final String TAG = WatchConnection.class.getSimpleName();
    private NotificationReceiver notificationReceiver;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mConnected = false;

    private Queue<String[]> transmitQueue;
    private Handler queueHandler;
    private TransmitTask currentTransmitTask;

    private BluetoothLeHandler bluetoothLeHandler;

    private OnConnectionListener connectionListener;

    private String[] data = null;
    private boolean isTransmitting = false;
    private boolean shouldSendNotifications = true;
    private boolean ackReceived = false;
    private boolean transmissionSuccess = false;
    private boolean transmissionError = false;
    private boolean timeOutFailure = false;
    private int retries = 0; // counts time we have retied to send a packet

    public interface OnConnectionListener{
        void onConnection(boolean connected);
    }

    @Override
    public IBinder onBind(Intent intent) {

        Log.i("WATCHSERVICE:onBind()","Binding new service!");
        // register our receiver so we can receive notification data
        notificationReceiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(NOTIFICATION_FILTER);
        registerReceiver(notificationReceiver,filter);

        IntentFilter gattFilter = new IntentFilter();
        gattFilter.addAction(ACTION_GATT_CONNECTED);
        gattFilter.addAction(ACTION_GATT_DISCONNECTED);
        gattFilter.addAction(ACTION_GATT_INITIALIZATION_COMPLETE);
        gattFilter.addAction(ACTION_DATA_AVAILABLE);
        gattFilter.addAction(EXTRA_DATA);
        registerReceiver(mGattUpdateReceiver,gattFilter);

        mBluetoothAdapter = ((BluetoothManager)this.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null) {
            Log.e(TAG,"No bluetooth adapter found, app will terminate.");
            // Terminate app as with no bluetooth it is useless
        } else if(!mBluetoothAdapter.isEnabled()) {
            Log.e(TAG,"Bluetooth adapter is disabled! Requesting permission from user.");
        }

        return myBinder;
    }

    public void start(){
        bluetoothLeHandler = new BluetoothLeHandler(this,mBluetoothAdapter);
        queueHandler = new Handler();
        transmitQueue = new LinkedList<>();
        resetConnectionVariables(); // reset anything we may have used from a previous connection

        // search for MabezWatch and connect to it
        bluetoothLeHandler.scanLe(true);
        queueHandler.postDelayed(queueRunnable,100);
    }

    private void resetConnectionVariables(){
        data = null;
        isTransmitting = false;
        shouldSendNotifications = true;
        ackReceived = false;
        transmissionSuccess = false;
        transmissionError = false;
        timeOutFailure = false;
        retries = 0;
    }

    public void disconnect(){ // for manual disconnects
        currentTransmitTask.cancel(true);
        bluetoothLeHandler.disconnect();
        transmitQueue = null;
        queueHandler.removeCallbacks(queueRunnable);
        queueHandler = null;
    }

    private Runnable queueRunnable = new Runnable() {
        @Override
        public void run() {
            //check the queue if it has data
            if(!transmitQueue.isEmpty()){
                if(!isTransmitting) { //wait till we are not transmitting
                    //delay between transmissions
//                    SystemClock.sleep(50);

                    if(transmitQueue.peek()[0].equals(NOTIFICATION_TAG) && !shouldSendNotifications) {
                        if(transmitQueue.size() > 1) { //only rotate the queue if there other item to send in its place else just wait
                            transmitQueue.add(transmitQueue.poll()); //move notification to the back of the queue
                        }
                    } else {
                        data = transmitQueue.peek();//remove from queue and send it
                        if(data!=null) {
                            currentTransmitTask = new TransmitTask();
                            currentTransmitTask.execute();
                        } else {
                            Log.i(TAG,"Transmit data is null, not transmitting.");
                        }
                    }

                }
            }
            queueHandler.postDelayed(this, 500);
        }
    };

    private void handleWatchResponses(String data){
        switch (data) {
            case "<n>":
                Log.i(TAG, "MabezWatch Wants notifications again!");
                shouldSendNotifications = true;
                break;
            case "<e>":
                Log.i(TAG, "MabezWatch Out of Space, Hold notifications in queue.");
                shouldSendNotifications = false;
                break;
            case "w5day":
                //add the 5 day forecast to the queue
                break;
            case "<c>":
                // clear the notification queue
                break;
            case "<r>":
                // resend the current notification
                break;
            case "<OK>":
                // the last sent item was successfully recieved, remove from queue
                Log.i(TAG, "[Success] <OK> received, transmission success!");
                transmissionSuccess = true;
                break;
            case "<ACK>":
                // send the rest of data
                ackReceived = true;
                Log.i(TAG, "[Success] <ACK> received from watch, sending data.");
                break;
            case "<FAIL>":
                // the watch did not recieve the full data or there was data corruption, start again
                transmissionError = true;
                Log.i(TAG, "[Error] <FAIL> packet received from watch, resending.");

                break;
            case "<RESET>":
                Log.i(TAG, "[Error] <RESET> is currently unimplemented, the message will be discarded.");
                //TODO: cancel the transmit and resend eventually. [NOPE]
                // currently if something timed out we try to resend again, this should only be used for resetting after the global retries reach a certain amount
                break;
            default:
                Log.i(TAG, "Data from the Watch: " + data);
                break;
        }
    }

    /*
        Transmission async task and helper methods
     */

    private class TransmitTask extends AsyncTask<Void, Void, Void>  // UI thread
    {

        @Override
        protected void onPreExecute()
        {
            isTransmitting = true;
        }

        @Override
        protected Void doInBackground(Void... devices)

        {
            for(int i = 0; i < data.length; i++) {
                transmit(data[i]);
            }
//            int packetIndex = 0;
            int ackTimeOut = 0;
            int transmissionTimeOut = 0;

//            while(packetIndex <= (data.length - 1) && (ackTimeOut < 10) && (transmissionTimeOut < 10)){
//                // form new data initializer
//                String metaData = packetIndex == 0 ? "<*>"+data[0]+Integer.toString(calculateCheckSum(data)) : "<+>"+data[packetIndex].length();
//                // send it
//                transmit(metaData);
//                // wait for ack
//                if(isAckReceived()) { // wait for ack or timeout
//                    if(packetIndex == 0) {
//                        // if its the first packet there is no data to send so just move on
//                        packetIndex++;
//                    } else {
//                        Log.i(TAG, "Sending data at index "+packetIndex+" out of "+ (data.length - 1));
//                        transmit(data[packetIndex]); // send the actual data
//                        if (isTransmissionSuccess()) { // wait for okay or timeout
//                            packetIndex++; // if it was successful we can move on to the next payload
//                        } else {
//                            Log.i(TAG, "Resending data at index "+packetIndex+" out of "+ (data.length - 1));
//                            transmissionTimeOut++;
//                        }
//                    }
//                } else { // try again
//                    ackTimeOut++;
//                }
//
//                // reset vars
//                ackReceived = false;
//                transmissionSuccess = false;
//
//                // give the watch time to process information
//                sleep(SEND_DELAY);
//            }

            if(transmissionTimeOut < 10 && ackTimeOut < 10){
                transmitQueue.poll(); // remove from the queue as it was sent successfully
            } else {
                // failed find out why or just discard the message
                timeOutFailure = true;
            }





            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if(!timeOutFailure) {
                isTransmitting = false;
                retries = 0;
                Log.i(TAG, "Transmission complete. " + transmitQueue.size() + " items left in the sending queue.");
            } else {
                Log.i(TAG, "Transmission failed due to transmission timeout, Resending now. Attempt number " + retries + ".");
                Log.i(TAG,"numInQueue: "+transmitQueue.size());
                retries++;
                timeOutFailure = false; // reset for next retry
                if (retries > 10) {
                    //NotificationUtils.showNotification(BTBGService.this, "[Error] A message failed to send more than 10 times!", true, true, 9999);
                }
            }

        }
    }

    private int calculateCheckSum(String[] data){
        int dataLen = 0;
        for(int i = 1; i < data.length; i++){ // skip the first tag as this is just for the app side
            dataLen += data[i].length();
        }
        return dataLen;
    }

    private void transmit(String payload){
        if(bluetoothLeHandler!=null) {
            bluetoothLeHandler.send(payload.getBytes());
        } else {
            Log.i(TAG,"bluetoothLeHandler is null stopping transmission.");
        }
    }

    private boolean isAckReceived(){
        return true; // TODO remove this
//        int ackTimeout = 0;
//        while(!ackReceived){ // wait till we receive the ack packet, add increment time out here
//            if(!isTransmitting) return false;
//            sleep(100);
//            ackTimeout++;
//            if(ackTimeout > 25){
//                System.out.println("[Error] Acknowledgement timeout.");
//                return false;
//            }
//        }
//        return true;
    }

    private boolean isTransmissionSuccess(){
//        int timeout = 0;
//        while(!transmissionSuccess && isTransmitting){ // while we haven't got the OKAY from the watch, check if there were any errors
//            if(transmissionError){
//                transmissionError = false; //reset flag
//                System.out.println("[Error] packet corruption token received from watch.");
//                return false;
//            }
//            if(!isTransmitting) return false;
//            sleep(100);
//            timeout++;
//            if(timeout > 25){ // wait 2.5 seconds
//                System.out.println("[Error] TransmissionSuccess timeout.");
//                return false;
//            }
//        }
        return true;
    }

    public void setOnConnectionListener(OnConnectionListener connectionListener){
        this.connectionListener = connectionListener;
    }

    /*
        Broadcast Receivers
     */
    private class NotificationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if(transmitQueue != null) {
                String type = intent.getStringExtra("TYPE");
                int id = intent.getIntExtra("ID", -1); //should never be -1

                if (type.equals(NOTIFICATION_NEW)) {
                    String pkgName = intent.getStringExtra("PKG");
                    String title = intent.getStringExtra("TITLE");
                    String text = intent.getStringExtra("TEXT");

                    Log.i(TAG, "[New notification]");
                    Log.i(TAG, "\tPkg: " + pkgName);
                    Log.i(TAG, "\tid: " + id);
                    Log.i(TAG, "\ttitle: " + title);
                    Log.i(TAG, "\ttext: " + text);
                    //now package this up and add to the transmit queue
                    String[] packet = WatchApiFormatter.formatNotificationData(id, pkgName, title, text);
//                    System.out.print("Data: ");
//                    System.out.println(packet);
                    transmitQueue.add(packet);
                } else {
                    transmitQueue.add(WatchApiFormatter.formatRemoval(id));
                }
            }
        }
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_INITIALIZATION_COMPLETE: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device. This can be a
    // result of read or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ACTION_GATT_CONNECTED.equals(action)) {
                Log.i("GATT","Connected!");
                mConnected = true;
            } else if (ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.i("GATT","Disonnected.");
                mConnected = false;

                if(connectionListener != null){
                    connectionListener.onConnection(false);
                }

                bluetoothLeHandler.close(); // release handler once we have disconnected!

            } else if (ACTION_GATT_INITIALIZATION_COMPLETE.equals(action)) {
                // as soon as we get to this part of code we can transmit
                if(connectionListener != null){
                    connectionListener.onConnection(true);
                }

                // send data data as soon as we can
//                transmitQueue.add(formatDateData());

            } else if (ACTION_DATA_AVAILABLE.equals(action)) {
                String data = intent.getStringExtra(EXTRA_DATA);
                handleWatchResponses(data);
            }
        }
    };


    public class MyLocalBinder extends Binder {
        public WatchConnection getService() {
            return WatchConnection.this;
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(notificationReceiver);
        unregisterReceiver(mGattUpdateReceiver);
        bluetoothLeHandler.close();
    }
}
