package com.mabezdev.MabezWatch.Bluetooth;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import com.mabezdev.MabezWatch.Activities.Main;
import com.mabezdev.MabezWatch.Util.NotificationUtils;
import com.mabezdev.yahooweather.WeatherInfo;
import com.mabezdev.yahooweather.YahooWeather;
import com.mabezdev.yahooweather.YahooWeatherInfoListener;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.mabezdev.MabezWatch.myNotificationListener.NOTIFICATION_NEW;

/**
 * Created by Scott on 09/09/2015.
 */
public class BTBGService extends Service {

    private static final int SEND_DELAY = 10; //delay between each message in ms
    private NotificationReceiver notificationReceiver;
    private final static String NOTIFICATION_TAG = "n";
    private final static String DATE_TAG = "d";
    private final static String WEATHER_TAG = "w";
    private final static String REMOVAL_TAG = "r";
    private final static String INTERVAL_TAG = "<i>";
    private final static String CLOSE_TAG = "<e>";
    private final static String END_TAG = "*";  // was *
    private final static int CHUNK_SIZE = 64; //64 bytes of data
    private final static int WEATHER_REFRESH_TIME = 300000; // 5 mins (300000/1000 = 300/60)
    private String[] data = null;
    private boolean isConnected = false;
    private static final String TAG = "ASYNC_TRANSMIT";
    private YahooWeatherInfoListener yahooWeatherInfoListener;
    private YahooWeather yahooWeather;
    private Handler weatherHandler;
    private Handler queueHandler;
    private BluetoothHandler bluetoothHandler;
    private Queue<String[]> transmitQueue;
    private boolean isTransmitting;
    private static final int DATA_LENGTH = 500;
    private final IBinder myBinder = new MyLocalBinder();
    private OnConnectedListener connectionListener;
    private boolean shouldSendNotifications = true;
    private boolean ackReceived = false;
    private boolean transmissionSuccess = false;
    private boolean transmissionError = false;
    private boolean timeOutFailure = false;
    private int retries = 0; // counts time we have retied to send a packet
    private TransmitTask currentTransmitTask;


    public interface OnConnectedListener{
        public void onConnected();
        public void onDisconnected();
    }

    /*
    BLE HM-11 SERVICES:

    0000ffe0-0000-1000-8000-00805f9b34fb

    BLE HM-11 CHARACTERISTICS:

    0000ffe1-0000-1000-8000-00805f9b34fb
     */

    @Override
    public int onStartCommand(Intent i,int flags,int srtID){

        Log.i(TAG,"BTBG service initialized.");

        transmitQueue = new LinkedList<String[]>();

        //register our receiver to listen to our other service
        notificationReceiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Main.NOTIFICATION_FILTER);
        registerReceiver(notificationReceiver,filter);
        //init bt handler
        bluetoothHandler = new BluetoothHandler(this);

        bluetoothHandler.setOnConnectedListener(new BluetoothHandler.OnConnectedListener() {
            @Override
            public void onConnected(boolean isConnected) {
                if (isConnected) {
                    Log.i("TRANSMIT", "Connected.");
                    BTBGService.this.isConnected = true;
                    //save device for quick connect
                    BluetoothUtil.storeDevice(BluetoothUtil.getChosenDeviceName(),BluetoothUtil.getChosenDeviceMac());
                    if(connectionListener!=null){
                        connectionListener.onConnected();
                    }

                } else {
                    if(connectionListener!=null){
                        connectionListener.onDisconnected();
                    }
                    Log.i("TRANSMIT","Disconnected.");
                    BTBGService.this.isConnected = false;
                    stopSelf();
                }
            }});

        bluetoothHandler.setOnReadyForTransmissionListener(new BluetoothHandler.OnReadyForTransmissionListener() {
            @Override
            public void OnReady(boolean isReady) {
                // all data that needs to be sent at the start done here
                Log.i(TAG,"Ready for transmission.");
                //yahooWeather.queryYahooWeatherByGPS(getBaseContext(),yahooWeatherInfoListener);
                transmitQueue.add(formatDateData());
            }
        });

        /*
            Here we will provide a API for the watch top request data
         */
        bluetoothHandler.setOnReceivedDataListener(new BluetoothHandler.onReceivedDataListener() {
            @Override
            public void onReceivedData(String data) {
                //data will be received in 20 byte payloads so we will need to stitch the data together if its longer than that
                if(data.equals("<n>")){
                    Log.i(TAG,"MabezWatch Wants notifications again!");
                    shouldSendNotifications = true;
                } else if(data.equals("<e>")){
                    Log.i(TAG,"MabezWatch Out of Space, Hold notifications in queue.");
                    shouldSendNotifications = false;
                } else if(data.equals(WEATHER_TAG)){
                    //add the 5 day forecast to the queue
                } else if(data.equals("<c>")){
                    // clear the notification queue
                } else if(data.equals("<r>")){
                    // resend the current notification
                } else if(data.equals("<OK>")){
                    // the last sent item was successfully recieved, remove from queue
                    Log.i(TAG, "[Success] <OK> received, transmission success!");
                    transmissionSuccess = true;
                } else if(data.equals("<ACK>")){
                    // send the rest of data
                    ackReceived = true;
                    Log.i(TAG, "[Success] <ACK> received from watch, sending data.");
                } else if(data.equals("<FAIL>")){
                    // the watch did not recieve the full data or there was data corruption, start again
                    transmissionError = true;
                    Log.i(TAG, "[Error] <FAIL> packet received from watch, resending.");

                } else if(data.equals("<RESET>")){
                    Log.i(TAG,"[Error] <RESET> is currently unimplemented, the message will be discarded.");
                    //TODO: cancel the transmit and resend eventually. [NOPE]
                    // currently if something timed out we try to resend again, this should only be used for resetting after the global retries reach a certain amount
                } else {
                    Log.i(TAG,"Data from the Watch: "+data);
                }
            }
        });


        //init data listener
        yahooWeatherInfoListener = new YahooWeatherInfoListener() {
            @Override
            public void gotWeatherInfo(WeatherInfo weatherInfo) {
                if(weatherInfo!=null){
                    //add data to the transmit queue
                    transmitQueue.add(formatWeatherData(weatherInfo.getForecastInfo1().getForecastDay(),
                            Integer.toString(weatherInfo.getCurrentTemp()),
                            weatherInfo.getForecastInfo1().getForecastText()));
                } else {
                    Toast.makeText(BTBGService.this,"Failed to retrieve weather information.",Toast.LENGTH_SHORT).show();
                }
            }
        };

        //init weather client
        yahooWeather = new YahooWeather();
        // set temp to celsius
        yahooWeather.setUnit(YahooWeather.UNIT.CELSIUS);

        weatherHandler = new Handler();
        weatherHandler.postDelayed(weatherRunnable,15000); //wait for device to connect before trying the first time

        queueHandler = new Handler();
        queueHandler.postDelayed(queueRunnable,500);

        bluetoothHandler.connect(BluetoothUtil.getChosenDeviceMac());

        return START_STICKY;
    }

    public void setOnConnectedListener(OnConnectedListener l){
        this.connectionListener = l;
    }

    private Runnable weatherRunnable = new Runnable() {
        @Override
        public void run() {
            if(isConnected) {
                //queryWeather();
            } else {
                Log.i("WEATHER", "Not querying as the device is not connected. Check internet connection permissions.");
            }
            weatherHandler.postDelayed(this, WEATHER_REFRESH_TIME);//get new data every @WEATHER_REFRESH_TIME seconds.
        }
    };

    public void queryWeather(){
        yahooWeather.queryYahooWeatherByGPS(getBaseContext(), yahooWeatherInfoListener);
    }

    private Runnable queueRunnable = new Runnable() {
        @Override
        public void run() {
            //check the queue if it has data
            if(!transmitQueue.isEmpty()){
                if(!isTransmitting) { //wait till we are not transmitting
                    //delay between transmissions
                    SystemClock.sleep(500);

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
            queueHandler.postDelayed(this, 1000);
        }
    };

    private String[] formatWeatherData(String day,String temp,String forecast){
        String[] data = new String[5];
        data[0] = WEATHER_TAG;
        data[1] = day;
        data[2] = INTERVAL_TAG + temp;
        data[3] = INTERVAL_TAG + forecast;
        data[4] = END_TAG;
        return data;
    }

    private String[] formatNotificationData(int id,String pkg, String title, String text){
        /*
            Data struct on watch:
                -Package name = 15 characters
                -Title = 15 characters
                -Text = up to 500 characters
         */
        if(pkg!=null && title!=null && text!=null) {
            ArrayList<String> format = new ArrayList<String>();
            if (pkg.length() >= 15) {
                pkg = pkg.substring(0, 14);
            }
            if (title.length() >= 15) {
                title = title.substring(0, 14);
            }
            format.add(NOTIFICATION_TAG); // only for app meta side sake, this never gets sent
            format.add(id + INTERVAL_TAG + pkg + INTERVAL_TAG + title + INTERVAL_TAG);
            int charIndex = 0;
            String temp = "";
            //make sure we don't array out of bounds on the watch
            int len = text.length();
            if(len > DATA_LENGTH){
                len = DATA_LENGTH;
            }

            if (text.length() > CHUNK_SIZE) {
                for (int i = 0; i < len; i++) { //max 150 for message + 20 chars for tags
                    if (charIndex >= CHUNK_SIZE) {//send in chunks of 64 chars
                        format.add(temp);
                        temp = "";
                        charIndex = 0;
                    } else {
                        temp += text.charAt(i);
                    }
                    charIndex++;
                }
                //this adds the last piece of data if it is under 64 characters
                format.add(temp);
            } else {
                format.add(text);
            }
            //format.add(text);
            format.add(END_TAG);
            return format.toArray(new String[format.size()]);
        } else {
            return null;
        }
    }

    private String[] formatDateData(){
        Date myDate = new Date();
        SimpleDateFormat ft =
                new SimpleDateFormat("dd MM yyyy HH mm ss");
        String[] date = new String[3];
        date[0] = DATE_TAG;
        date[1] = ft.format(myDate);
        date[2] = END_TAG;
        return date;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        //very IMPORTANT
        return myBinder;
    }

    public class MyLocalBinder extends Binder {
        public BTBGService getService() {
            return BTBGService.this;
        }
    }

    @Override
    public void onDestroy(){
        System.out.println("Stopping BTBGService");
        try {
            unregisterReceiver(notificationReceiver);
        } catch (Exception e){
            Log.i(TAG,"Notification Receiver was never registered therefore cannot be unregistered.");
        }

        queueHandler.removeCallbacks(queueRunnable);
        queueHandler = null;
        weatherHandler.removeCallbacks(weatherRunnable);
        weatherHandler = null;

        if(isConnected){
            bluetoothHandler.disconnect();
            Toast.makeText(BTBGService.this, "Disconnected.", Toast.LENGTH_SHORT).show();
        }
        bluetoothHandler.close();
        bluetoothHandler = null;

        transmitQueue = null;

        super.onDestroy();
    }

    private void printData(){
        System.out.println("Printing data for tag "+data[0]);
        for(int i=1; i < data.length; i++){
            System.out.print(data[i]);
            System.out.print(",");
        }
        System.out.println();
    }

    /*
    Create a broadcast receiver to grab notifications and add them to a queue
    then the queue is processed one by one making sure data is received with a proper sending
    algorithm.
     */

    private class NotificationReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {

            String type = intent.getStringExtra("TYPE");
            int id = intent.getIntExtra("ID", -1); //should never be -1

            if(type.equals(NOTIFICATION_NEW)) {
                String pkgName = intent.getStringExtra("PKG");
                String title = intent.getStringExtra("TITLE");
                String text = intent.getStringExtra("TEXT");

                Log.i(TAG, "[New notification]");
                Log.i(TAG, "\tPkg: " + pkgName);
                Log.i(TAG, "\tid: " + id);
                Log.i(TAG, "\ttitle: " + title);
                Log.i(TAG, "\ttext: " + text);
                //now package this up and add to the transmit queue
                transmitQueue.add(formatNotificationData(id, pkgName, title, text));
            } else {
                transmitQueue.add(formatRemoval(id));
            }
        }
    }
    private String[] formatRemoval(int id) {
        String[] removeNotification = new String[3];
        removeNotification[0] = REMOVAL_TAG;
        removeNotification[1] = Integer.toString(id);
        removeNotification[2] = END_TAG;
        return removeNotification;
    }


    private int calculateCheckSum(String[] data){
        int dataLen = 0;
        for(int i = 1; i < data.length; i++){ // skip the first tag as this is just for the app side
            dataLen += data[i].length();
        }
        return dataLen;
    }

    private void sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void transmit(String payload){
        if(bluetoothHandler!=null) {
            bluetoothHandler.sendData(payload.getBytes());
        } else {
            Log.i(TAG,"Handler is null stopping transmission.");
            stopSelf();
        }
    }

    private boolean isAckReceived(){
        int ackTimeout = 0;
        while(!ackReceived){ //wait till we recieve the ack packet, add increment time out here
            sleep(100);
            ackTimeout++;
            if(ackTimeout > 25){
                System.out.println("[Error]<ACK> timeout, retry!");
                //break;
                return false;
            }
        }
        return true;
    }

    private boolean isTransmissionSuccess(){
        int timeout = 0;
        while(!transmissionSuccess){ // while we haven't got the OKAY from the watch, check if there were any errors
            if(transmissionError){
                transmissionError = false; //reset flag
                Log.i(TAG,"Error set from watch returning false;");
                return false;
            }
            sleep(100);
            timeout++;
            if(timeout > 25){ //wait 2.5 seconds
                System.out.println("[Error] checkSum <OKAY> timeout, retry!");
                break;
            }
        }
        return true;
    }

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
            int packetIndex = 0;
            int timeOut = 0;

            while(packetIndex <= (data.length - 1) && timeOut < 10){
                // form new data initializer
                String metaData = packetIndex == 0 ? "<*>"+data[0]+Integer.toString(calculateCheckSum(data)) : "<+>"+data[packetIndex].length();
                // send it
                transmit(metaData);
                // wait for ack
                if(isAckReceived()) { // wait for ack or timeout
                    if(packetIndex == 0) {
                        // if its the first packet there is no data to send so just move on
                        packetIndex++;
                    } else {
                        Log.i(TAG, "Sending data at index "+packetIndex+" out of "+ (data.length - 1));
                        transmit(data[packetIndex]); // send the actual data
                        if (isTransmissionSuccess()) { // wait for okay or timeout
                            packetIndex++; // if it was successful we can move on to the next payload
                        } else {
                            Log.i(TAG, "Resending data at index "+packetIndex+" out of "+ (data.length - 1));
                        }
                    }
                } else { // try again
                    timeOut++;
                }

                // reset vars
                ackReceived = false;
                transmissionSuccess = false;

                // give the watch time to process information
                sleep(SEND_DELAY);
            }

            if(timeOut < 10){
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
                retries++;
                timeOutFailure = false; // reset for next retry
                if (retries > 10) {
                    NotificationUtils.showNotification(BTBGService.this, "[Error] A message failed to send more than 10 times!", true, true, 9999);
                }
            }

        }
    }
}

