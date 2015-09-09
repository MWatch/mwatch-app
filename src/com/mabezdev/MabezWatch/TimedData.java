package com.mabezdev.MabezWatch;

import java.util.TimerTask;

/**
 * Created by Scott on 09/09/2015.
 */
public class TimedData extends TimerTask {
    @Override
    public void run() {
        new BTConnection(Main.getDeviceToConnect());
    }
}
