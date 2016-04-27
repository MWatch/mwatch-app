package com.mabezdev.MabezWatch.Bluetooth;

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;


/**
 * Created by Mabez on 19/03/2016.
 */
public class DeviceSave implements Serializable {

    private String deviceAddress;
    private String deviceName;

    public DeviceSave(String name, String mac) {
        deviceName = name;
        deviceAddress = mac;
    }



    public String getDeviceAddress(){
        return deviceAddress;
    }

    public String getDeviceName(){
        return deviceName;
    }
}
