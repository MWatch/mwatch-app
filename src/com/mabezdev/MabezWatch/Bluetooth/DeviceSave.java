package com.mabezdev.MabezWatch.Bluetooth;

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;


/**
 * Created by Mabez on 19/03/2016.
 */
public class DeviceSave implements Serializable {

    private transient BluetoothDevice btdev;
    private String deviceAddress;
    private String deviceName;

    public DeviceSave(BluetoothDevice bt) {
        btdev = bt;
        if(btdev!=null){
            deviceAddress = btdev.getAddress();
            deviceName = btdev.getName();
        }
    }



    public String getDeviceAddress(){
        return deviceAddress;
    }

    public String getDeviceName(){
        return deviceName;
    }
}
