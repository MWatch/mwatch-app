package com.mabezdev.mabezwatch.Util;

/**
 * Created by mabez on 29/01/17.
 */
public class WatchUtil {

    public static void sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
