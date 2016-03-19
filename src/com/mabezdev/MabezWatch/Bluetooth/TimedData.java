package com.mabezdev.MabezWatch.Bluetooth;

import com.mabezdev.MabezWatch.Activities.Connect;

import java.util.TimerTask;

/**
 * Created by Scott on 09/09/2015.
 */
public class TimedData extends TimerTask {
    @Override
    public void run() {
        new BTConnection(BluetoothUtil.getDeviceToConnect());
    }
}
