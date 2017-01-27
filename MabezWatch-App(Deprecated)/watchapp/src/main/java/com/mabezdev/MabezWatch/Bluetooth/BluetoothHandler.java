package com.mabezdev.MabezWatch.Bluetooth;

import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import com.mabezdev.MabezWatch.Activities.Main;

public class BluetoothHandler {
    // scan bluetooth device
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mEnabled = false;
    private boolean mScanning = false;
    private static final long SCAN_PERIOD = 5000; //scan for 5 seconds
    private String mCurrentConnectedBLEAddr;

    // connect bluetooth device
    private BLEService mBLEService;
    private String mDeviceAddress = null;
    private boolean mConnected = false;
    private onReceivedDataListener onRecListener;
    private OnConnectedListener onConnectedListener;
    private OnScanListener onScanListener;
    private OnReadyForTransmissionListener onReadyForTransmissionListener;

    private List<BluetoothGattService> gattServices = null;
    private UUID targetServiceUuid = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");//cahnged this to ours
    private UUID targetCharacterUuid = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private UUID readUUID =
            UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb");
    private BluetoothGattCharacteristic targetGattCharacteristic = null;
    // 00001800-0000-1000-8000-00805f9b34fb service test
    private Context context;

    public interface onReceivedDataListener {
        public void onReceivedData(String data);
    };

    public interface OnReadyForTransmissionListener{
        public void OnReady(boolean isReady);
    }

    public interface OnConnectedListener{
        public void onConnected(boolean isConnected);
    };

    public interface OnScanListener{
        public void onScan(BluetoothDevice device, int rssi, byte[] scanRecord);
        public void onScanFinished();
    };

    public void setOnScanListener(OnScanListener l){
        onScanListener = l;
    }
    public void setOnReadyForTransmissionListener(OnReadyForTransmissionListener l){
        onReadyForTransmissionListener = l;
    }

    public BluetoothHandler(Context context) {
        // TODO Auto-generated constructor stub
        this.context = context;
        //mDevListAdapter = new BLEDeviceListAdapter(context);
        mBluetoothAdapter = null;
        mCurrentConnectedBLEAddr = null;

        if(!isSupportBle()){
            Toast.makeText(context, "your device not support BLE!", Toast.LENGTH_SHORT).show();
            return ;
        }
        // open bluetooth
        if (!getBluetoothAdapter().isEnabled()) {
            Intent mIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if(context instanceof Activity) {
                ((Main) context).startActivityForResult(mIntent, 1);
            }
        }else{
            setEnabled(true);
        }
    }

    /*public BLEDeviceListAdapter getDeviceListAdapter(){
        return mDevListAdapter;
    }*/

    public void connect(String deviceAddress){
        mDeviceAddress = deviceAddress;
        Intent gattServiceIntent = new Intent(context, BLEService.class);

        if(!(context).bindService(gattServiceIntent, mServiceConnection, (context).BIND_AUTO_CREATE)){
            System.out.println("bindService failed!");
        }

        (context).registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBLEService != null) {
            final boolean result = mBLEService.connect(mDeviceAddress);
        }else{
            System.out.println("mBLEService = null");
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBLEService = ((BLEService.LocalBinder) service).getService();
            if (!mBLEService.initialize()) {
                Log.e("onServiceConnected", "Unable to initialize Bluetooth");
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBLEService.connect(mDeviceAddress);
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {

            mBLEService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BLEService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                mCurrentConnectedBLEAddr = mDeviceAddress;
                if(onConnectedListener != null){
                    onConnectedListener.onConnected(true);
                }
            } else if (BLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                mCurrentConnectedBLEAddr = null;
                if(onConnectedListener != null){
                    onConnectedListener.onConnected(false);
                }
            } else if (BLEService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //displayGattServices(mBluetoothLEService.getSupportedGattServices());
                if(mBLEService != null)
                    getCharacteristic(mBLEService.getSupportedGattServices());
            } else if (BLEService.ACTION_DATA_AVAILABLE.equals(action)) {
                String data = intent.getStringExtra(BLEService.EXTRA_DATA);
                if(onRecListener != null)
                    onRecListener.onReceivedData(data);
            }
        }
    };

    public void setOnReceivedDataListener(onReceivedDataListener l){
        onRecListener = l;
    }

    public void setOnConnectedListener(OnConnectedListener l){
        onConnectedListener = l;
    }

    public void getCharacteristic(List<BluetoothGattService> gattServices){
        this.gattServices = gattServices;
        String uuid = null;
        BluetoothGattCharacteristic characteristic = null;
        BluetoothGattService targetGattService = null;
        // get target gattservice
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            Log.i("Services",uuid);
            if(uuid.equals(targetServiceUuid.toString())){
                targetGattService = gattService;
                Log.i("HANDLER","Found our service.");
                break;
            }
        }

        if(targetGattService != null){
            //Toast.makeText(context, "get service ok", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(context, "Failed to retrieve Service.", Toast.LENGTH_SHORT).show();
            return ;
        }
        List<BluetoothGattCharacteristic> gattCharacteristics =
                targetGattService.getCharacteristics();
        // get targetGattCharacteristic
        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
            uuid = gattCharacteristic.getUuid().toString();
            Log.i("Characteristics","UUID: "+uuid);
            if(uuid.equals(targetCharacterUuid.toString())){
                targetGattCharacteristic = gattCharacteristic;
                Log.i("HANDLER","Found our characteristic.");
                break;
            }
        }
        targetGattCharacteristic = targetGattService.getCharacteristic(targetCharacterUuid);
        BluetoothGattCharacteristic readGattCharacteristic = targetGattService.getCharacteristic(readUUID);
        if(readGattCharacteristic != null)
            mBLEService.setCharacteristicNotification(readGattCharacteristic, true);

        if(targetGattCharacteristic != null){
            //Toast.makeText(context, "get character ok", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(context, "Failed to retrieve characteristic.", Toast.LENGTH_SHORT).show();
            return ;
        }

        if(targetGattService!=null && targetGattCharacteristic!=null){
            Toast.makeText(context,
                    "           Connected.\nService UUID: "+targetGattService.getUuid().toString().substring(0,8)
                            + "\nCharacteristic UUID: "+targetGattCharacteristic.getUuid().toString().substring(0,8),
                    Toast.LENGTH_SHORT).show();
            if(onReadyForTransmissionListener!=null){
                onReadyForTransmissionListener.OnReady(true);
            }

            //enable notifications for gatt characteristic
            mBLEService.setCharacteristicNotification(targetGattCharacteristic,true);
        } else {
            Log.i("HANDLER","Failed to find characteristic, retry.");
            mBLEService.disconnect();
            mBLEService.close();
        }
    }

    public void close(){
        // kill service
        if(mBLEService!=null) {
            mBLEService.close();
            mBLEService = null;
        }
        try {
            context.unregisterReceiver(mGattUpdateReceiver);
            context.unbindService(mServiceConnection);
        } catch (IllegalArgumentException e){
            e.printStackTrace();
        }


        mConnected = false;
        //mHandler.removeCallbacks();
        mHandler = null;
    }

    public void onPause() {
        if(mConnected){
            ((BTBGService) context).unregisterReceiver(mGattUpdateReceiver);
        }
    }

    public void onDestroy(){
        if(mConnected){
            //mDevListAdapter.clearDevice();
            //mDevListAdapter.notifyDataSetChanged();
            ((BTBGService) context).unbindService(mServiceConnection);
            mBLEService = null;
            mConnected = false;
        }
    }

    public void onResume(){
        if(!mConnected || mBLEService == null)
            return ;
        ((BTBGService)context).registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBLEService != null) {
            final boolean result = mBLEService.connect(mDeviceAddress);
            Log.d("registerReceiver", "Connect request result=" + result);
        }
    }

    public synchronized void sendData(byte[] value){
        if(targetGattCharacteristic != null && mBLEService != null && mConnected == true){
            int targetLen = 0;
            int offset=0;
            for(int len = (int)value.length; len > 0; len -= 20){
                if(len < 20)
                    targetLen = len;
                else
                    targetLen = 20;
                byte[] targetByte = new byte[targetLen];
                System.arraycopy(value, offset, targetByte, 0, targetLen);
                offset += 20;
                targetGattCharacteristic.setValue(targetByte);
                mBLEService.writeCharacteristic(targetGattCharacteristic);
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } else {
            Log.i("ERROR","FAILED TO SEND");
            this.disconnect();
            this.close();
        }
    }

    public synchronized void sendData(String value){
        if(targetGattCharacteristic != null && mBLEService != null && mConnected == true){
            targetGattCharacteristic.setValue(value);
            mBLEService.writeCharacteristic(targetGattCharacteristic);
        }
    }

    public boolean isSupportBle(){
        // is support 4.0 ?
        final BluetoothManager bluetoothManager = (BluetoothManager)
                context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null)
            return false;
        else
            return true;
    }

    public BluetoothAdapter getBluetoothAdapter(){
        return mBluetoothAdapter;
    }

    public void setEnabled(boolean enabled){
        mEnabled = enabled;
    }

    public boolean isEnabled(){
        return mEnabled;
    }

    Handler mHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            if(msg.obj != null){
                //mDevListAdapter.addDevice((BluetoothScanInfo) msg.obj);
                //mDevListAdapter.notifyDataSetChanged();
            }
        }
    };

    // scan device
    public void scanLeDevice(boolean enable) {
        if (enable) {
            //mDevListAdapter.clearDevice();
            //mDevListAdapter.notifyDataSetChanged();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    if(onScanListener != null){
                        onScanListener.onScanFinished();
                    }
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    public void disconnect(){
        if(onConnectedListener!=null){
            onConnectedListener.onConnected(false);
        }
        mBLEService.disconnect();
        mBLEService.close();
    }

    public boolean isScanning(){
        return mScanning;
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             final byte[] scanRecord) {

            if(onScanListener != null){
                onScanListener.onScan(device, rssi, scanRecord);
            }

            System.out.println("scan info:");
            System.out.println("rssi="+rssi);
            System.out.println("ScanRecord:");
            for(byte b:scanRecord)
                System.out.printf("%02X ", b);
            System.out.println("");
        }
    };

    public class BluetoothScanInfo{
        public BluetoothDevice device;
        public int rssi;
        public byte[] scanRecord;
    };

    public static double calculateAccuracy(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0; // if we cannot determine accuracy, return -1.
        }

        double ratio = rssi*1.0/txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio,10);
        }
        else {
            double accuracy =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;
            return accuracy;
        }
    }
}
