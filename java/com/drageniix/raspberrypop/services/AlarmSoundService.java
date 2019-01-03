package com.drageniix.raspberrypop.services;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;

public class AlarmSoundService extends Service {
    private MediaPlayer mediaPlayer;
    private PowerManager.WakeLock screenOn;

    public static int id;
    public static String title, content;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        NotificationCompat.Builder notification = BaseActivity.createNotification(this, null,
                R.drawable.ic_action_scan_alarm,
                title,
                content)
                .setOngoing(true)
                .setAutoCancel(false);

        startForeground(id, notification.build());

        try {
            screenOn = ((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, title);
            screenOn.acquire(5000);

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mediaPlayer.setDataSource(this, Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.alarm));
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e){
            mediaPlayer = null;
            screenOn = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
        }

        if (screenOn != null && screenOn.isHeld()){
            screenOn.release();
        }
    }
}
