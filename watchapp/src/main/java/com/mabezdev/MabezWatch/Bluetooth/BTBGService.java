package com.mabezdev.MabezWatch.Bluetooth;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;
import com.mabezdev.MabezWatch.Activities.Main;
import com.mabezdev.MabezWatch.R;
import com.mabezdev.MabezWatch.Util.NotificationUtils;
import com.mabezdev.yahooweather.Test;
import com.mabezdev.yahooweather.WeatherInfo;
import com.mabezdev.yahooweather.YahooWeather;
import com.mabezdev.yahooweather.YahooWeatherInfoListener;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Scott on 09/09/2015.
 */
public class BTBGService extends Service {

    private static final int SEND_DELAY = 50; //delay between
    private NotificationReceiver notificationReceiver;
    private final static String NOTIFICATION_TAG = "<n>";
    private final static String DATE_TAG = "<d>";
    private final static String WEATHER_TAG = "<w>";
    private final static String TITLE_TAG = "<t>";
    private final static String DATA_INTERVAL_TAG = "<i>";
    private final static String CLOSE_TAG = "<e>";
    private final static String END_TAG = "<f>";
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
    private static final int notificationID = 1234;
    private static final int DATA_LENGTH = 170;
    private final IBinder myBinder = new MyLocalBinder();
    private OnConnectedListener connectionListener;
    public static final int NOTIFICATION_ID = 4444;
    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotificationManager;
    private long startTime = 0;
    private Handler timerHandler;
    private boolean shouldSendNotifications = true;


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

    //todo might need this to be a bound service so we can get data like if its connected or not

    @Override
    public int onStartCommand(Intent i,int flags,int srtID){

        Log.i(TAG,"BTBG service initialized.");

        transmitQueue = new LinkedList<String[]>();

        //register our receiver to listen to our other service
        notificationReceiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Main.NOTIFICATION_FILTER);
        registerReceiver(notificationReceiver,filter);
        Test t = new Test();
        System.out.println(t.test());
        //init bt handler
        bluetoothHandler = new BluetoothHandler(this);

        bluetoothHandler.setOnConnectedListener(new BluetoothHandler.OnConnectedListener() {
            @Override
            public void onConnected(boolean isConnected) {
                if (isConnected) {
                    Log.i("TRANSMIT", "Connected.");
                    BTBGService.this.isConnected = true;
                    NotificationUtils.showNotification(BTBGService.this,"Connected to MabezWatch ("+BluetoothUtil.getChosenDeviceMac()+").",false,false,NOTIFICATION_ID);
                    //save device for quick connect
                    BluetoothUtil.storeDevice(BluetoothUtil.getChosenDeviceName(),BluetoothUtil.getChosenDeviceMac());
                    if(connectionListener!=null){
                        connectionListener.onConnected();
                        startTime = System.currentTimeMillis();
                        timerHandler = new Handler();
                        timerHandler.postDelayed(timerRunnable,0);
                    }

                } else {
                    if(connectionListener!=null){
                        connectionListener.onDisconnected();
                        if(timerHandler!=null) {
                            timerHandler.removeCallbacks(timerRunnable);
                            timerHandler = null;
                        }
                    }
                    NotificationUtils.removeNotification(NOTIFICATION_ID);
                    long time = calculateTime();
                    long hours = time / 3600;
                    long minutes = (time % 3600) / 60;
                    long seconds = time % 60;
                    NotificationUtils.showNotification(BTBGService.this,"Disconnected, connection lasted:\n"+String.format("%02d:%02d:%02d", hours, minutes, seconds),true,true,1111);
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
                yahooWeather.queryYahooWeatherByGPS(getBaseContext(),yahooWeatherInfoListener);
                transmitQueue.add(formatDateData());
            }
        });

        /*
            Here we will provide a API for the watch top request data
         */
        bluetoothHandler.setOnReceivedDataListener(new BluetoothHandler.onReceivedDataListener() {
            @Override
            public void onReceivedData(String data) {
                Log.i(TAG,"Data from the Watch: "+data);
                //data will be received in 20 byte payloads so we will need to stitch the data together if its longer than that
                if(data.equals("<n>")){
                    // this means we have run out of space on the smart watch an we should keep the rest in a queue to send when we get notified again
                    Log.i(TAG,"MabezWatch Wants notifications again!");
                    shouldSendNotifications = true;
                } else if(data.equals("<e>")){
                    Log.i(TAG,"MabezWatch Out of Space, Hold notifications in queue.");
                    shouldSendNotifications = false;
                } else if(data.equals(WEATHER_TAG)){
                    //add the 5 day forecast to the queue
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
                }
            }
        };

        //init weather client
        yahooWeather = new YahooWeather();
        // set temp to celcius
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



    private void transmit(String[] formattedData){
        //delay between each statement it received individual
        for(int i=0; i < formattedData.length; i++){
            if(bluetoothHandler!=null) {
                bluetoothHandler.sendData(formattedData[i].getBytes());
            } else {
                Log.i(TAG,"Handler is null stopping transmission.");
                stopSelf();
            }
            SystemClock.sleep(SEND_DELAY);
        }
    }

    private Runnable weatherRunnable = new Runnable() {
        @Override
        public void run() {
            if(isConnected) {
                yahooWeather.queryYahooWeatherByGPS(getBaseContext(), yahooWeatherInfoListener);
            } else {
                Log.i("WEATHER", "Not querying as the device is not connected.");
            }
            weatherHandler.postDelayed(this, WEATHER_REFRESH_TIME);//get new data every @WEATHER_REFRESH_TIME seconds.
        }
    };

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long time = calculateTime();
            long hours = time / 3600;
            long minutes = (time % 3600) / 60;
            long seconds = time % 60;
            NotificationUtils.updateNotification(String.format("%02d:%02d:%02d", hours, minutes, seconds),NOTIFICATION_ID);
            timerHandler.postDelayed(timerRunnable,1000);
        }

    };

    private Runnable queueRunnable = new Runnable() {
        @Override
        public void run() {
            //check the queue if it has data
            if(!transmitQueue.isEmpty()){
                if(!isTransmitting) { //wait till we are not transmitting
                    //delay between transmissions
                    SystemClock.sleep(1000);

                    if(transmitQueue.peek()[0].equals(NOTIFICATION_TAG) && !shouldSendNotifications) {
                        if(transmitQueue.size() > 1) { //only rotate the queue if there other item to send in its place else just wait
                            transmitQueue.add(transmitQueue.poll()); //move notification to the back of the queue
                        }
                    } else {
                        data = transmitQueue.poll();//remove from queue and send it
                        if(data!=null) {
                            new TransmitTask().execute();
                        } else {
                            Log.i(TAG,"Transmit data is null, not transmitting.");
                        }
                    }

                }
            }
            queueHandler.postDelayed(this, 1000);
        }
    };

    private long calculateTime(){
        if(startTime !=0){
            return ((System.currentTimeMillis() - startTime)/1000);
        } else {
            return 0;
        }
    }

    private String[] formatWeatherData(String day,String temp,String forecast){
        String[] data = new String[7];
        data[0] = WEATHER_TAG;
        data[1] = day;
        data[2] = TITLE_TAG;
        data[3] = temp;
        data[4] = TITLE_TAG;
        data[5] = forecast;
        data[6] = END_TAG;
        return data;
    }

    private String[] formatNotificationData(String pkg, String title, String text){
        /*
            Data struct on watch:
                -Package name = 15 characters
                -Title = 15 characters
                -Text = 150 characters
         */
        if(pkg!=null && title!=null && text!=null) {
            ArrayList<String> format = new ArrayList<String>();
            if (pkg.length() >= 15) {
                pkg = pkg.substring(0, 14);
            }
            if (title.length() >= 15) {
                title = title.substring(0, 14);
            }
            format.add(NOTIFICATION_TAG);
            format.add(pkg);
            format.add(TITLE_TAG + title + CLOSE_TAG);
            int charIndex = 0;
            String temp = "";
            //make sure we don't array out of bounds
            int len = text.length();
            if(len > DATA_LENGTH){
                len = DATA_LENGTH;
            }
            
            if (text.length() > CHUNK_SIZE) {
                for (int i = 0; i < len; i++) { //max 150 for message + 20 chars for tags
                    if (charIndex >= CHUNK_SIZE) {//send in chunks of 64 chars
                        format.add(DATA_INTERVAL_TAG + temp);
                        temp = "";
                        charIndex = 0;
                    } else {
                        temp += text.charAt(i);
                    }
                    charIndex++;
                }
                //this adds the last piece of data if it is under 64 characters
                format.add(DATA_INTERVAL_TAG + temp);
            } else {
                format.add(DATA_INTERVAL_TAG + text);
            }
            format.add(END_TAG);
            return format.toArray(new String[format.size()]);
        } else {
            return null;
        }
    }

    private String[] formatDateData(){
        Date myDate = new Date();
        SimpleDateFormat ft =
                new SimpleDateFormat("dd MM yyyy HH:mm:ss");
        String[] date = new String[3];
        date[0] = DATE_TAG;
        date[1] = ft.format(myDate)+':';
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
        NotificationUtils.removeNotification(NOTIFICATION_ID); // get rid of out notification
        System.out.println("Stopping BTBGService");
        try {
            unregisterReceiver(notificationReceiver);
        } catch (Exception e){
            Log.i(TAG,"Notification Receiver was never registered therefore cannot be unregistered.");
        }

        if(timerHandler!=null){
            timerHandler.removeCallbacks(timerRunnable);
            timerHandler = null;
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
            String pkgName = intent.getStringExtra("PKG").toString();
            String title = intent.getStringExtra("TITLE").toString();
            String text = intent.getStringExtra("TEXT").toString();
            //now package this up and add tot he transmit queue
            transmitQueue.add(formatNotificationData(pkgName,title,text));
        }
    }

    private class TransmitTask extends AsyncTask<Void, Void, Void>  // UI thread
    {

        @Override
        protected void onPreExecute()
        {
            isTransmitting = true;
            //Log.i(TAG, "Transmitting data with TAG: "+data[0]);
            printData();
        }

        @Override
        protected Void doInBackground(Void... devices)
        {
            transmit(data);
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);
            isTransmitting = false;
            Log.i(TAG,"Transmission complete.");
        }
    }
}
