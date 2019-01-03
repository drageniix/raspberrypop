package com.drageniix.raspberrypop.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.activities.ScanActivity;
import com.drageniix.raspberrypop.fragments.BaseFragment;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.categories.AuxiliaryApplication;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class StopwatchService extends IntentService {
    final public static String PLAY = "PLAY", LAP = "LAP", STOP = "STOP";
    public static DBHandler handler;

    public StopwatchService(){
        super("Stopwatch");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (handler == null){
            return;}

        final NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        NotificationCompat.Builder notification;
        Media media = handler.readMedia(intent.getStringExtra(ScanActivity.UID));
        if (media == null || AuxiliaryApplication.valueOf(media) != AuxiliaryApplication.STOPWATCH || media.getStreamingID().equalsIgnoreCase("0L")){
            stopSelf();
            return;}

        mNotifyMgr.cancel((int)media.getId());
        if (intent.getAction().equalsIgnoreCase(STOP) || media.getTempDetails() == null){
            int lap = media.getSummary().split("Lap ").length;
            String time = formatTime(getMediaMillis(media));
            String oneLine = "Time - " + time;

            media.setSummary(lap == 1 ? oneLine : media.getSummary() + "\nLap " + lap + " - " + time);
            media.setStreamingID("0L");
            media.setExternalID("");
            media.setTempDetails(null);

            if (lap != 1){
                oneLine = "Average Lap - " + formatTime(averageLap(media));
                media.setSummary(oneLine + "\n\n" + media.getSummary());}

            notification = BaseActivity.createNotification(this, media,
                    R.drawable.ic_action_stopwatch,
                    (media.getTitle() + " Stopwatch").trim(),
                    oneLine);

            if (lap != 1){
                notification.setStyle(new NotificationCompat.BigTextStyle().bigText(media.getSummary()));}

        } else {
            RemoteViews view = (RemoteViews) media.getTempDetails()[0];
            notification = (NotificationCompat.Builder) media.getTempDetails()[1];
            long baseTime = Long.parseLong(media.getStreamingID());
            long currentTime = SystemClock.elapsedRealtime();
            long time;

            switch (intent.getAction()) {
                case PLAY:
                    if (!media.getExternalID().isEmpty()) { //RESUME
                        time = -Long.parseLong(media.getExternalID()) + currentTime;
                        view.setChronometer(R.id.chronometer, time, null, true);
                        media.setStreamingID(String.valueOf(time));
                        media.setExternalID("");
                    } else { //PAUSE
                        time = baseTime - currentTime;
                        view.setChronometer(R.id.chronometer, baseTime, null, false);
                        media.setExternalID(String.valueOf(Math.abs(time)));
                    }
                    break;
                case LAP:
                    int lap = media.getSummary().split("Lap ").length;
                    view.setChronometer(R.id.chronometer, currentTime, null, true);
                    view.setTextViewText(R.id.title, ((media.getTitle().isEmpty() ? media.getFigureName() : media.getTitle()) + " Stopwatch - Lap " + (lap+1)).trim());
                    media.setSummary((media.getSummary() + "\nLap " + lap + " - " + formatTime(getMediaMillis(media))).trim());
                    media.setStreamingID(String.valueOf(currentTime));
                    media.setExternalID("");
                    break;
            }
        }

        mNotifyMgr.notify((int) media.getId(), notification.build());
        if (!BaseFragment.addOrUpdate(media)) handler.addOrUpdateMedia(media);
    }

    private String formatTime(long millis){
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        long milliseconds = millis % 1000;

        String time = String.format(Locale.getDefault(), "%02d:%02d:%02d.%03d",
                hours, minutes, seconds, milliseconds);

        while(time.startsWith("00:")){
            time = time.substring(3);
        }

        if (time.startsWith("0") && !time.startsWith("0.")){
            time = time.substring(1);
        }

        return time;
    }

    private long getMediaMillis(Media media){
        return media.getExternalID().isEmpty() ?
                SystemClock.elapsedRealtime() - Long.parseLong(media.getStreamingID()) :
                Long.parseLong(media.getExternalID());
    }

    private long averageLap(Media media){
        String[] splitSummary = media.getSummary()
                .replace("\n", " - ")
                .replace(".", "")
                .replace(":", "")
                .split(" - ");

        long sum = 0;
        int count = 0;

        for (int i = 1; i < splitSummary.length; i += 2, count++){
            String value = splitSummary[i];
            long finalValue;
            switch (value.length()){
                case 4: //millis
                case 5: //seconds
                    finalValue = Long.parseLong(value);
                    break;
                case 6:
                case 7: //minutes
                    finalValue = TimeUnit.MINUTES.toMillis(Long.parseLong(value.substring(0, value.length()-5)))
                            + (Long.parseLong(value.substring(value.length() - 5)));
                    break;
                default: //hours
                    finalValue = TimeUnit.HOURS.toMillis(Long.parseLong(value.substring(0, value.length()-7)))
                            + TimeUnit.MINUTES.toMillis(Long.parseLong(value.substring(value.length()-7, value.length()-5)))
                            + (Long.parseLong(value.substring(value.length() - 5)));
                    break;
            }

            sum += finalValue;
        }

        return sum/count;
    }
}