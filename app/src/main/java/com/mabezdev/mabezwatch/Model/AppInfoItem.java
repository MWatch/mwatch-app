package com.mabezdev.mabezwatch.Model;

import java.io.Serializable;

/**
 * Created by mabez on 31/01/17.
 */
public class AppInfoItem implements Serializable {

    private String packageName = "__PKG";
    private boolean isChatApp = false;
    private boolean isBlacklisted = false;

    public AppInfoItem(String packageName, boolean isChatApp, boolean isBlacklisted){
        this.packageName = packageName;
        this.isBlacklisted = isBlacklisted;
        this.isChatApp = isChatApp;
    }

    public String getPackageName() {
        return packageName;
    }

    public boolean isChatApp() {
        return isChatApp;
    }

    public void setChatApp(boolean chatApp) {
        isChatApp = chatApp;
    }

    public boolean isBlacklisted() {
        return isBlacklisted;
    }

    public void setBlacklisted(boolean blacklisted) {
        isBlacklisted = blacklisted;
    }
}
