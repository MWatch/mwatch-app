package com.mabezdev.mabezwatch.Model;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by mabez on 31/01/17.
 */
public class AppInfoStore implements Serializable {

    private ArrayList<AppInfoItem> infoStore;

    public ArrayList<AppInfoItem> getInfoStore() {
        return infoStore;
    }

    public void setInfoStore(ArrayList<AppInfoItem> infoStore) {
        this.infoStore = infoStore;
    }
}
