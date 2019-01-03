package com.drageniix.raspberrypop.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;

import com.drageniix.raspberrypop.activities.ScanActivity;
import com.drageniix.raspberrypop.utilities.DBHandler;

public class AlarmReceiver extends BroadcastReceiver {
    public static String TITLE = "TITLE", CONTENT = "CONTENT";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context != null) {
            if (intent.getAction() != null && intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
                new DBHandler(context);
            } else {
                AlarmSoundService.id = (int) intent.getLongExtra(ScanActivity.UID, 0);
                AlarmSoundService.title = intent.getStringExtra(TITLE);
                AlarmSoundService.content = intent.getStringExtra(CONTENT);

                ContextCompat.startForegroundService(context, new Intent(context, AlarmSoundService.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        }
    }
}
