package com.mabezdev.MabezWatch.Bluetooth;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import com.mabezdev.MabezWatch.Activities.Main;
import com.mabezdev.MabezWatch.R;
import zh.wang.android.apis.yweathergetter4a.WeatherInfo;
import zh.wang.android.apis.yweathergetter4a.YahooWeather;
import zh.wang.android.apis.yweathergetter4a.YahooWeatherInfoListener;

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

        //init bt handler
        bluetoothHandler = new BluetoothHandler(this);

        bluetoothHandler.setOnConnectedListener(new BluetoothHandler.OnConnectedListener() {
            @Override
            public void onConnected(boolean isConnected) {
                if (isConnected) {
                    Log.i("TRANSMIT", "Connected.");
                    BTBGService.this.isConnected = true;
                    showNotification("Connected to MabezWatch ("+BluetoothUtil.getChosenDeviceMac()+").");
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
                    showNotification("Disconnected from MabezWatch.");
                    stopSelf();
                }
            }});

        bluetoothHandler.setOnReadyForTransmissionListener(new BluetoothHandler.OnReadyForTransmissionListener() {
            @Override
            public void OnReady(boolean isReady) {
                // all data that needs to be sent at the start done here
                Log.i(TAG,"Ready for transmission.");
                yahooWeather.queryYahooWeatherByGPS(BTBGService.this,yahooWeatherInfoListener);

                transmitQueue.add(formatDateData());
            }
        });


        //init data listener
        yahooWeatherInfoListener = new YahooWeatherInfoListener() {
            @Override
            public void gotWeatherInfo(WeatherInfo weatherInfo) {
                if(weatherInfo!=null){
                    //add data to the transmit queue
                    transmitQueue.add(formatWeatherData(weatherInfo.getForecastInfo1().getForecastDay(),
                            String.format("%.2f", (weatherInfo.getCurrentTemp() - 32) / 1.8f),
                            weatherInfo.getForecastInfo1().getForecastText()));
                }
            }
        };

        //init weather client
        yahooWeather = new YahooWeather();


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

    private void showNotification(String eventText) {
        //todo replace with NotificationCompat
        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.ic_launcher,
                "MabezWatch", System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this
        // notification
        PendingIntent contentIntent = PendingIntent.getActivity(BTBGService.this, 0,
                new Intent(BTBGService.this, Main.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(BTBGService.this, "MabezWatch", eventText,
                contentIntent);
        // Send the notification.
        NotificationManager mng = (NotificationManager)BTBGService.this.getSystemService(Context.NOTIFICATION_SERVICE);
        mng.notify("Title", notificationID, notification);
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
            try {
                Thread.sleep(SEND_DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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

    private Runnable queueRunnable = new Runnable() {
        @Override
        public void run() {
            //check the queue if it has data
            if(!transmitQueue.isEmpty()){
                if(!isTransmitting) { //wait till we are not transmitting
                    //delay between transmissions
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    data = transmitQueue.poll();//remove from queue and put it here
                    new TransmitTask().execute();
                }
            }
            queueHandler.postDelayed(this, 1000);
        }
    };

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
        //todo need to check that the text is not too big and need to add a phase to read
        /*
            Data struct on teensy:
                -Package name = 15 characters
                -Title = 15 characters
                -Text = 150 characters
         */
        ArrayList<String> format = new ArrayList<String>();
        if(pkg.length() >= 15){
            pkg = pkg.substring(0,14);
        }
        if(title.length() >= 15){
            title = title.substring(0,14);
        }
        format.add(NOTIFICATION_TAG+pkg);
        format.add(TITLE_TAG+title+CLOSE_TAG);
        int charIndex = 0;
        String temp = "";
        if(text.length() > CHUNK_SIZE) {
            for (int i=0; i < DATA_LENGTH; i++) { //max 150 for message + 20 chars for tags
                if (charIndex >= CHUNK_SIZE) {//send in chunks of 64 chars
                    format.add(DATA_INTERVAL_TAG+temp);
                    temp = "";
                    charIndex = 0;
                } else {
                    temp += text.charAt(i);
                }
                charIndex++;
            }
            //this adds the last piece of data if it is under 64 characters
            format.add(DATA_INTERVAL_TAG+temp);
        } else {
            format.add(DATA_INTERVAL_TAG+text);
        }
        format.add(END_TAG);
        return format.toArray(new String[format.size()]);
    }

    private String[] formatDateData(){
        Date myDate = new Date();
        SimpleDateFormat ft =
                new SimpleDateFormat("dd MM yyyy HH:mm:ss");
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

        super.onDestroy();
    }

    /*
    Create a broadcast receiver to grab notifications and add them to a queue
    then the queue is processed one by one making sure data is received with a proper sending
    algorithm.
     */

    private class NotificationReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String pkgName = intent.getStringExtra("PKG");
            String title = intent.getStringExtra("TITLE");
            String text = intent.getStringExtra("TEXT");
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
            Log.i(TAG, "Transmitting data...");
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
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
