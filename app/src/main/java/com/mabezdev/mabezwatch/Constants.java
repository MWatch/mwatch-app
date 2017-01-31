package com.mabezdev.mabezwatch;

/**
 * Created by mabez on 25/01/17.
 */
public class Constants {

    public static final String NOTIFICATION_FILTER = "mabezdev.api.GET_NOTIFICATIONS";

    // Gatt constants
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_INITIALIZATION_COMPLETE =
            "com.example.bluetooth.le.ACTION_GATT_INITIALIZATION_COMPLETE";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public static final int SEND_DELAY = 10; //delay between each message in ms

    public static final int CONNECTION_TIMEOUT = 6000; //wait time for connection

    public static final String APP_INFO_STORE_FNAME = "AppInfoStore.bin";
}
