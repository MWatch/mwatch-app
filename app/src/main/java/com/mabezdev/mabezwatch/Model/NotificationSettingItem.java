package com.mabezdev.mabezwatch.Model;

import android.graphics.drawable.Drawable;

/**
 * Created by mabez on 30/01/17.
 */
public class NotificationSettingItem {

    private String displayName = "NAME";
    private AppInfoItem appInfoItem;
    private Drawable icon;

    public NotificationSettingItem(String name, Drawable icon, AppInfoItem appInfoItem){
        this.displayName = name;
        this.icon = icon;
        this.appInfoItem = appInfoItem;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        if(displayName == null) return;
        this.displayName = displayName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public String getPackageName() {
        return appInfoItem.getPackageName();
    }


    public boolean isChatApp() {
        return appInfoItem.isChatApp();
    }

    public void setChatApp(boolean chatApp) {
        appInfoItem.setChatApp(chatApp);
    }

    public boolean isBlacklisted() {
        return appInfoItem.isBlacklisted();
    }

    public void setBlacklisted(boolean blacklisted) {
        appInfoItem.setChatApp(blacklisted);
    }
}
