package com.mabezdev.MabezWatch.Bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import com.mabezdev.MabezWatch.Activities.Main;
import zh.wang.android.apis.yweathergetter4a.WeatherInfo;
import zh.wang.android.apis.yweathergetter4a.YahooWeather;
import zh.wang.android.apis.yweathergetter4a.YahooWeatherInfoListener;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * Created by Scott on 09/09/2015.
 */
public class BTBGService extends Service {

    private NotificationReceiver notificationReceiver;
    private final static String NOTIFICATION_TAG = "<n>";
    private final static String DATE_TAG = "<d>";
    private final static String WEATHER_TAG = "<w>";
    private final static String TITLE_TAG = "<t>";
    private final static String DATA_INTERVAL_TAG = "<i>";
    private final static String CLOSE_TAG = "<e>";
    private final static String END_TAG = "<f>";
    private final static int CHUNK_SIZE = 64;
    private BTConnection connection;
    private boolean isConnected;
    private OutputStream outputStream;
    private BluetoothSocket socket;
    private BluetoothDevice btdev;
    private int retries = 0;
    private static UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private String[] data = null;
    private static final String TAG = "ASYNC_TRANSMIT";
    private YahooWeatherInfoListener yahooWeatherInfoListener;
    private YahooWeather yahooWeather;
    private Handler weatherHandler;

    @Override
    public int onStartCommand(Intent i,int flags,int srtID){

        //register our receiver to listen to our other service
        notificationReceiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Main.NOTIFICATION_FILTER);
        registerReceiver(notificationReceiver,filter);

        yahooWeatherInfoListener = new YahooWeatherInfoListener() {
            @Override
            public void gotWeatherInfo(WeatherInfo weatherInfo) {
                if(weatherInfo!=null){
                    //send data here as it is got once queried.
                    Log.i("TEMPERATURE (C): ",Float.toString((weatherInfo.getCurrentTemp() - 32)/1.8f));
                    for(WeatherInfo.ForecastInfo info: weatherInfo.getForecastInfoList()){
                        Log.i(info.getForecastDay(),info.getForecastText());
                    }

                    data = formatWeatherData(weatherInfo.getForecastInfo1().getForecastDay(),
                            String.format("%.2f", (weatherInfo.getCurrentTemp() - 32)/1.8f),
                            weatherInfo.getForecastInfo1().getForecastText());
                    new TransmitTask().execute();

                }
            }
        };

        yahooWeather = new YahooWeather();

        weatherHandler = new Handler();
        weatherHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                yahooWeather.queryYahooWeatherByGPS(getBaseContext(),yahooWeatherInfoListener);
                weatherHandler.postDelayed(this,30000);//get new data every ... seconds.
            }
        },100);




        /*
        Here we need to handle connection of bluetooth device
        and handle reconnection and eventual shutdown of service after a number of timeouts
         */

        //Async task to connect to device
        new ConnectBT().execute();


        return START_STICKY;
    }

    public void transmit(String[] formattedData){
        //delay between each statement it received individual
        for(int i=0; i < formattedData.length; i++){
            write(formattedData[i]);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
        //todo need to check that the text is not too big and need to add a phase to read
        //the rest on the phone!
        ArrayList<String> format = new ArrayList<String>();
        format.add(NOTIFICATION_TAG+pkg);
        format.add(TITLE_TAG+title+CLOSE_TAG);
        int charIndex = 0;
        String temp = "";
        if(text.length() > CHUNK_SIZE) {
            for (char c : text.toCharArray()) {
                if (charIndex >= CHUNK_SIZE) {//send in chunks of 64 chars
                    format.add(DATA_INTERVAL_TAG+temp);
                    temp = "";
                    charIndex = 0;
                } else {
                    temp += c;
                }
                charIndex++;
            }
        } else {
            format.add(DATA_INTERVAL_TAG+text);
        }
        format.add(END_TAG);
        temp ="";
        for(String item: format){
            temp+=item;
        }
        System.out.println("Sending: "+temp);
        return format.toArray(new String[format.size()]);
    }

    public void write(String data){
        try {
            outputStream.write(data.getBytes());
            outputStream.flush();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private String[] formatDateData(){
        String[] date = new String[3];
        date[0] = DATE_TAG;
        date[1] = DateFormat.getDateTimeInstance().format(new Date());
        date[2] = END_TAG;
        return date;
    }

    public void disconnect(){//called upon service onDestroy
        if (socket != null) {
            try {
                Log.d("EF-BTBee", ">>Client Close");
                // give a chance to send final message
                try {
                    Thread.sleep(250);
                }catch (Exception e){

                }
                //close streams
                outputStream.close();
                socket.close();
                socket=null;
            } catch (IOException e) {
                Log.e("EF-BTBee", "", e);
            }catch(NullPointerException e1){
                e1.printStackTrace();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        //c

        return null;
    }

    @Override
    public void onDestroy(){
        System.out.println("Stopping BTBGService");
        disconnect();
        Toast.makeText(this,"BT Device disconnected",Toast.LENGTH_LONG).show();
        unregisterReceiver(notificationReceiver);
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
            //now package this up and transmit
            data = formatNotificationData(pkgName,title,text);
            new TransmitTask().execute();
        }
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            Toast.makeText(getBaseContext(),"Connecting...",Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (socket == null || !isConnected)
                {
                    btdev = BluetoothUtil.getDeviceFromAddress(BluetoothUtil.getChosenMac(),
                            BluetoothUtil.getDefaultAdapter());
                    if (btdev != null) {
                        socket = btdev.createInsecureRfcommSocketToServiceRecord(myUUID);
                    }
                    socket.connect();//start connection
                    outputStream = socket.getOutputStream();
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                Toast.makeText(getBaseContext(),"Failed to connect retry!",Toast.LENGTH_SHORT).show();
                BTBGService.this.stopSelf();// kill the service
            }
            else {
                Toast.makeText(getBaseContext(),"Connected!",Toast.LENGTH_SHORT).show();
                isConnected = true;
                Log.i(TAG,"Sending time to watch.");
                transmit(formatDateData());
            }
        }
    }

    private class TransmitTask extends AsyncTask<Void, Void, Void>  // UI thread
    {

        @Override
        protected void onPreExecute()
        {
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
            Log.i(TAG,"Transmission complete.");
        }
    }
}
