package com.mabezdev.mabezwatch.Adapters;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.mabezdev.mabezwatch.Activities.PreferenceActivities.NotificationPreferences;
import com.mabezdev.mabezwatch.Model.NotificationSettingItem;
import com.mabezdev.mabezwatch.R;

import java.util.ArrayList;

/**
 * Created by mabez on 30/01/17.
 */
public class NotificationSettingAdapter extends ArrayAdapter<NotificationSettingItem> {

    private ArrayList<NotificationSettingItem> data;
    private NotificationPreferences context;

    private static final String TAG = "NotificationPrefAdapter";

    public NotificationSettingAdapter(Context context, int resource, ArrayList<NotificationSettingItem> data) {
        super(context, resource, data);
        this.context = (NotificationPreferences) context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final NotificationSettingItem currentRow = getItem(position);

        View myView = convertView;
        if(myView == null) {
            myView = LayoutInflater.from(getContext()).inflate(R.layout.view_item_notification_preferences, parent, false);
        }

        TextView name = (TextView) myView.findViewById(R.id.displayName);
        ToggleButton chatAppButton = (ToggleButton) myView.findViewById(R.id.chatAppToggle);
        ImageView icon = (ImageView) myView.findViewById(R.id.appIcon);

        name.setText(currentRow.getDisplayName());
        chatAppButton.setChecked(currentRow.isChatApp());
        icon.setImageDrawable(currentRow.getIcon());

        chatAppButton.setOnClickListener((view) -> currentRow.setChatApp(chatAppButton.isChecked()));

        myView.setOnLongClickListener(view -> {
            Log.i(TAG,"Long press detected.");
            return false;
        });

        return myView;
    }
}
