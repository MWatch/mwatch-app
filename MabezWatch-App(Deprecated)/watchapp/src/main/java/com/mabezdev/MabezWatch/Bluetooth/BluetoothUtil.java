package com.mabezdev.MabezWatch.Bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import com.mabezdev.MabezWatch.Activities.Main;
import com.mabezdev.MabezWatch.Util.ObjectWriter;

import java.util.Set;

/**
 * Created by Mabez on 19/03/2016.
 */
public class BluetoothUtil {

    private static Set<BluetoothDevice> pairedDevices;
    private static String chosenDeviceMac;
    private static String chosenDeviceName;
    private static BTBGService service;

    private BluetoothUtil(){

    }

    public static Set<BluetoothDevice> getBluetoothDevices(BluetoothAdapter myBluetoothAdapter){
        pairedDevices = myBluetoothAdapter.getBondedDevices();
        return pairedDevices;
    }

    @Deprecated
    public static BluetoothAdapter getDefaultAdapter(){
        return BluetoothAdapter.getDefaultAdapter();
    }

    public static BluetoothAdapter getDefaultAdapter(BluetoothManager mng){
        return mng.getAdapter();
    }

    @Deprecated
    public static BluetoothDevice getDeviceFromAddress(String address, BluetoothAdapter bluetoothAdapter){
        if(pairedDevices==null){
            pairedDevices = bluetoothAdapter.getBondedDevices();
        }
        for(BluetoothDevice bt : pairedDevices){
            if(bt.getAddress().equals(address)){
                return bt;
            }
        }
        return null;
    }

    public static void setBoundService(BTBGService services){
        service = services;
    }

    public static BTBGService getBoundService(){
        return service;
    }

    public static void setChosenDeviceMac(String mac){
        chosenDeviceMac = mac;
    }

    public static String getChosenDeviceMac(){
        return chosenDeviceMac;
    }

    public static String getChosenDeviceName() {
        return chosenDeviceName;
    }

    public static void setChosenDeviceName(String chosenDeviceName) {
        BluetoothUtil.chosenDeviceName = chosenDeviceName;
    }

    public static void turnOnBluetooth(BluetoothAdapter b){
        if (!b.isEnabled()) {
            b.enable();
        }
    }
    public  static void turnOffBluetooth(BluetoothAdapter b){
        if (b.isEnabled()) {
            b.disable();
        }
    }

    public static void storeDevice(String name, String mac){
        //save bluetoothDevice
        DeviceSave store = new DeviceSave(name,mac);

        boolean done = ObjectWriter.writeObject(Main.BLUETOOTH_FILE,store);
        if(!done){
            System.out.println("Write failed! The preffered device will not be stored.");
        }
    }
}
