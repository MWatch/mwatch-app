package com.mabezdev.mabezwatch.Util;

import android.bluetooth.*;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import java.util.List;
import java.util.UUID;

import static com.mabezdev.mabezwatch.Constants.*;
import static com.mabezdev.mabezwatch.Util.WatchUtil.sleep;

/**
 * Created by mabez on 25/01/17.
 */
public class BluetoothLeHandler {

    private BluetoothAdapter bluetoothAdapter;
    private Context context;
    private final String TAG = BluetoothLeHandler.class.getSimpleName();

    private Handler scanHandler;
    private BluetoothLeScanner bleScanner;
    private BluetoothDevice watchDevice;
    private BluetoothGatt mBluetoothGatt;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private int mConnectionState = -1;

    private UUID targetServiceUuid = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"); // specific to hm-11
    private UUID targetCharacterUuid = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"); // specific to hm-11

    private BluetoothGattCharacteristic bluetoothGattCharacteristic = null;
    private BluetoothGattService bluetoothGattService = null;

    private boolean isScanning;
    private boolean isFound = false;
    private static final String DEVICE_NAME = "MabezWatch";

    public BluetoothLeHandler(Context ctx, BluetoothAdapter adapter){
        Log.i(TAG,"Initializing bluetooth Handler.");
        this.context = ctx;
        bluetoothAdapter = adapter;
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        scanHandler = new Handler();
    }

    public void scanLe(boolean enable){
        Log.i(TAG,"Scanning!");
        if(enable){
            // acts as timer to stop scanning
            scanHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isScanning) {
                        isScanning = false;
                        bleScanner.stopScan(mLeScanCallback);
                    }
                }
            }, 5000);

            isScanning = true;
            bleScanner.startScan(mLeScanCallback);

        } else {
            isScanning = false;
            bleScanner.stopScan(mLeScanCallback);
        }
    }

    /*
        Callbacks
     */

    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            String name = result.getDevice().getName();
            if(name != null) {
                if (name.equals(DEVICE_NAME)) {
                    Log.i(TAG, "Found MabezWatch! MAC: " + result.getDevice().getAddress());
                    isFound = true;
                    watchDevice = result.getDevice();
                    //stop scanning
                    scanLe(false);
                    // connect
                    connect(watchDevice);
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            System.out.println("Error: "+errorCode);
        }
    };

    private void connect(BluetoothDevice device){
        mConnectionState = STATE_CONNECTING;
        mBluetoothGatt = device.connectGatt(context,false,bluetoothGattCallback);
    }

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for(BluetoothGattService service : mBluetoothGatt.getServices()){
                    if(service.getUuid().equals(targetServiceUuid)){
                        Log.i(TAG,"Found our service on device with uuid: " + targetServiceUuid);
                        bluetoothGattService = service;
                        for(BluetoothGattCharacteristic c : service.getCharacteristics()){
                            if(c.getUuid().equals(targetCharacterUuid)){
                                Log.i(TAG,"Found our characteristic on device with uuid: " + targetCharacterUuid);
                                bluetoothGattCharacteristic = c;


                                Log.i(TAG, "Enabling data retrieval from the watch.");
                                mBluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic,true);

                                broadcastUpdate(ACTION_GATT_INITIALIZATION_COMPLETE); // notify Service that it can send data now
                            }
                        }

                    }
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }


    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        context.sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        // For all other profiles, writes the data formatted in HEX.
        String data = new String(characteristic.getValue());
        if (data.length() > 0) {
            intent.putExtra(EXTRA_DATA, data);
        }
        context.sendBroadcast(intent);
    }

    public void send(byte[] bytes){
        if(bluetoothGattCharacteristic != null && mConnectionState == STATE_CONNECTED) {
            int targetLen = 0;
            int offset = 0;
            for (int len = bytes.length; len > 0; len -= 20) {
                targetLen = len < 20 ? len : 20;
                byte[] targetByte = new byte[targetLen];
                System.arraycopy(bytes, offset, targetByte, 0, targetLen);
                offset += 20;
                bluetoothGattCharacteristic.setValue(targetByte);
                mBluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
                sleep(5);
            }
        }
    }

    public void disconnect(){
        mBluetoothGatt.disconnect();
    }

    public void close(){
        if (mBluetoothGatt == null) {
            return;
        }
        isFound = false;
        watchDevice = null;
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

}
