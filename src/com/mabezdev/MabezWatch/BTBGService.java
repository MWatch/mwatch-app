package com.mabezdev.MabezWatch;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import java.util.Timer;

/**
 * Created by Scott on 09/09/2015.
 */
public class BTBGService extends Service {

    private Thread CommunicationThread;
    private final static int DataRefreshTime = 5000;
    @Override
    public IBinder onBind(Intent intent) {
        //c

        return null;
    }

    @Override
    public int onStartCommand(Intent i,int flags,int srtID){
        //run code from here
        Toast.makeText(this,"Connecting to BT Device",Toast.LENGTH_LONG).show();
        CommunicationThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Timer timer = new Timer();
                timer.schedule(new TimedData(), 0, DataRefreshTime);
                //connect send ata then dc

            }
        });
        CommunicationThread.start();

        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        BTConnection.disconnect();
        CommunicationThread.interrupt();
        Toast.makeText(this,"BT Device disconnected",Toast.LENGTH_LONG).show();
        super.onDestroy();
    }
}
