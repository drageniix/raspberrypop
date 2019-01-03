package com.drageniix.raspberrypop.utilities.categories;


import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.SystemClock;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.activities.ListActivity;
import com.drageniix.raspberrypop.activities.NoteActivity;
import com.drageniix.raspberrypop.activities.ScanActivity;
import com.drageniix.raspberrypop.dialog.DatabaseDialogHelper;
import com.drageniix.raspberrypop.dialog.Dice;
import com.drageniix.raspberrypop.dialog.Event;
import com.drageniix.raspberrypop.dialog.Form;
import com.drageniix.raspberrypop.fragments.BaseFragment;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.media.MediaMetadata;
import com.drageniix.raspberrypop.services.StopwatchService;
import com.drageniix.raspberrypop.services.AlarmReceiver;
import com.drageniix.raspberrypop.services.AlarmSoundService;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.Logger;
import com.drageniix.raspberrypop.utilities.TimeManager;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.NOTIFICATION_SERVICE;

public enum AuxiliaryApplication implements ApplicationCategory{
    WIFI_CONNECTION("WiFi Connection"){ //user ~!~ pw
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_network, true);
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata) {
            super.setMetadata(media, metadata);
            if (media.getAlternateID().length() == 3){
                media.setSummary(media.getStreamingID().split("~!~", -1)[1]);
            }
        }

        @Override
        public boolean scan(BaseActivity activity, DBHandler handler, Media media, ScanActivity.mediaPlayback scanner) {
            WifiManager wifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null && (wifiManager.isWifiEnabled() || wifiManager.setWifiEnabled(true))) {
                int netId = -1;
                String[] loginDetails = media.getStreamingID().split("~!~", -1);

                if (media.getAlternateID().equalsIgnoreCase(activity.getString(R.string.network_1))
                        && !loginDetails[1].isEmpty()){
                    List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
                    if (configuredNetworks != null) {
                        for (WifiConfiguration existingConfig : configuredNetworks) {
                            if (existingConfig.SSID.contains(loginDetails[0])) {
                                netId = existingConfig.networkId;
                                break;
                            }
                        }
                    }
                } else {
                    WifiConfiguration conf = new WifiConfiguration();
                    conf.SSID = "\"" + loginDetails[0] + "\"";
                    conf.status = WifiConfiguration.Status.ENABLED;

                    conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);

                    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);

                    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);

                    conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                    conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);

                    conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                    conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);

                    if (media.getAlternateID().equalsIgnoreCase(activity.getString(R.string.network_3))) {
                        conf.wepKeys[0] = "\"" + loginDetails[1] + "\"";
                        conf.wepTxKeyIndex = 0;
                    } else if (media.getAlternateID().equalsIgnoreCase(activity.getString(R.string.network_2))) {
                        conf.preSharedKey = "\"" + loginDetails[1] + "\"";
                    }

                    netId = wifiManager.addNetwork(conf);
                    wifiManager.saveConfiguration();

                    if (netId == -1) {
                        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
                        if (configuredNetworks != null) {
                            for (WifiConfiguration existingConfig : configuredNetworks) {
                                if (existingConfig.SSID.contains(loginDetails[0])) {
                                    netId = existingConfig.networkId;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (netId != -1) {
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(netId, true);
                    wifiManager.reconnect();
                    return true;
                }
            }
            return false;
        }

        @Override
        public String getQR(BaseActivity activity, Media media) {
            String[] loginDetails = media.getStreamingID().split("~!~", -1);
            String authentication = media.getAlternateID().toUpperCase();
            String password = loginDetails[1];
            if (authentication.length() != 3){
                authentication = "nopass";
                password = "";
            }

            return "WIFI:"
                    + "T:" + authentication + ";"
                    + "S:" + loginDetails[0]  + ";"
                    + "P:" + password  + ";"
                    + ";";
        }

        @Override
        public int getColor() {
            return StreamingApplication.DEVICE.getColor();
        }

    },
    WIFI("WiFi Toggle") { //boolean
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_network, true);
        }

        @Override
        public boolean scan(BaseActivity activity, DBHandler handler, Media media, ScanActivity.mediaPlayback scanner) {
            WifiManager wifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            return wifiManager != null && wifiManager.setWifiEnabled(Boolean.parseBoolean(media.getStreamingID()));
        }

        @Override
        public int getColor() {
            return StreamingApplication.DEVICE.getColor();
        }
    },
    BLUETOOTH("Bluetooth Toggle"){ //boolean
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_bluetooth, true);
        }

        @Override
        public boolean scan(BaseActivity activity, DBHandler handler, Media media, ScanActivity.mediaPlayback scanner) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            return bluetoothAdapter != null && (Boolean.parseBoolean(media.getStreamingID()) ? bluetoothAdapter.enable() : bluetoothAdapter.disable());
        }

        @Override
        public int getColor() {
            return StreamingApplication.DEVICE.getColor();
        }

    },
    VOLUME("Volume"){ //7 floats
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_volume, true);
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata) {
            super.setMetadata(media, metadata);
            StringBuilder summary = new StringBuilder();
            final String[] titles = new String[]{
                    "Ringer",
                    "Notification",
                    "Media",
                    "System",
                    "Alarm",
                    "Voice Call",
                    "DTMF",
                    "Do Not Disturb"
            };

            final String[] dndTitles = new String[]{
                    "Off",
                    "Silence",
                    "Priority",
                    "Alarm"
            };

            String[] oldData = media.getStreamingID().split("~!~", -1);
            float[] oldValues = new float[8];

            oldValues[7] = (int)Float.parseFloat(oldData[7]);
            if (oldValues[7] != -1){
                summary.append(titles[7]).append(" (").append(dndTitles[(int)oldValues[7]]).append(")\n");
            }

            for (int i = 0; i < 7; i++) {
                oldValues[i] = (int) (100 * Float.parseFloat(oldData[i]));
                if (oldValues[i] != -100) {
                    if (i == 0 && oldValues[i] < 0) {
                        summary.append(titles[i]).append(" (Vibrate)");
                    } else if (i == 0 && oldValues[i] == 0) {
                        summary.append(titles[i]).append(" (Mute)");
                    } else {
                        summary.append(titles[i]).append(" ").append((int)oldValues[i]).append("%");
                    }
                    summary.append("\n");
                }
            }

            media.setSummary(summary.toString().trim());
        }

        @Override
        public boolean scan(BaseActivity activity, DBHandler handler, Media media, ScanActivity.mediaPlayback scanner) {
            String percentStrings[] = media.getStreamingID().split("~!~", -1);
            float[] percent = new float[8];
            for(int i = 0; i < 8; i++){
                percent[i] = Float.parseFloat(percentStrings[i]);
            }

            AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
            if (percent[0] != -1) {
                if (percent[0] == 0){
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                } else if (percent[0] < 0) {
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                } else {
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, (int) (percent[0] * audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)), 0);
                }
            }

            if (percent[1] != -1) audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, (int)(percent[1] * audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)), 0);
            if (percent[2] != -1) audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int)(percent[2] * audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)), 0);
            if (percent[3] != -1) audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, (int)(percent[3] * audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM)), 0);
            if (percent[4] != -1) audioManager.setStreamVolume(AudioManager.STREAM_ALARM, (int)(percent[4] * audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)), 0);
            if (percent[5] != -1) audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, (int)(percent[5] * audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)), 0);
            if (percent[6] != -1) audioManager.setStreamVolume(AudioManager.STREAM_DTMF, (int)(percent[6] * audioManager.getStreamMaxVolume(AudioManager.STREAM_DTMF)), 0);

            if (percent[7] != -1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                NotificationManager mNotificationManager = (NotificationManager) activity.getSystemService(NOTIFICATION_SERVICE);
                switch ((int)percent[7]){
                    case 0: mNotificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                        break;
                    case 1: mNotificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
                        break;
                    case 2: mNotificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                        break;
                    case 3: mNotificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS);
                        break;

                }
            }

            return true;
        }

        @Override
        public int getColor() {
            return StreamingApplication.DEVICE.getColor();
        }

    },
    ORIENTATION("Auto Rotate"){ //boolean
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_orientation, true);
        }

        @Override
        public boolean scan(BaseActivity activity, DBHandler handler, Media media, ScanActivity.mediaPlayback scanner) {
            Settings.System.putInt(activity.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, Boolean.parseBoolean(media.getStreamingID()) ? 1 : 0);
            return true;
        }

        @Override
        public int getColor() {
            return StreamingApplication.DEVICE.getColor();
        }
    },
    BRIGHTNESS("Brightness"){ //integer 0 - 255
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_brightness, true);

        }

        @Override
        public boolean scan(BaseActivity activity, DBHandler handler, Media media, ScanActivity.mediaPlayback scanner) {
            Settings.System.putInt(activity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(activity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, Integer.parseInt(media.getStreamingID()));
            return true;
        }

        @Override
        public int getColor() {
            return StreamingApplication.DEVICE.getColor();
        }

    },
    FLASHLIGHT("Flashlight"){ //boolean
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_flashlight, true);
        }

        @Override
        public boolean scan(BaseActivity activity, DBHandler handler, Media media, ScanActivity.mediaPlayback scanner) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                CameraManager camManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
                try {
                    String cameraId = camManager.getCameraIdList()[0];
                    camManager.setTorchMode(cameraId, Boolean.parseBoolean(media.getStreamingID()));
                    return true;
                } catch (Exception e) {
                    Logger.log(Logger.CAST, e);
                }
            }
            return false;
        }

        @Override
        public int getColor() {
            return StreamingApplication.DEVICE.getColor();
        }
    },
    LIST("Checklist"){
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_checklist, true);
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata) {
            super.setMetadata(media, metadata);
            if (media.getStreamingID().isEmpty()) {
                media.setStreamingID("BEGIN:VLIST\nEND:VLIST");
            }

            media.setSummary(ListActivity.getSummary(media.getStreamingID()));
        }

        @Override
        public boolean scan(BaseActivity activity, DBHandler handler, Media media, ScanActivity.mediaPlayback scanner) {
            activity.openIntent(BaseActivity.OVERLAY_REQUEST_CODE, media);
            return true;
        }

        @Override
        public String getQR(BaseActivity activity, Media media) {
            return media.getStreamingID();
        }

        @Override
        public int getColor() {
            return StreamingApplication.OTHER.getColor();
        }
    },
    SIMPLE_NOTE("Note") {
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_notes, true);
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata) {
            media.setType(this.name());

            if (metadata != null) {
                media.setTitle(metadata.get(MediaMetadata.Type.INPUT_TITLE));
                media.setSummary(metadata.get(MediaMetadata.Type.SUMMARY)); //barcode
                if (!metadata.get(MediaMetadata.Type.INPUT_CUSTOM).isEmpty()){
                    media.setSummary(metadata.get(MediaMetadata.Type.INPUT_CUSTOM));}
            }

            media.getHandler().getParser().getThumbnailAPI().setThumbnailText(media);
            if(!media.getSummary().isEmpty()){
                media.setStreamingID(media.getSummary());
            } else {
                media.setStreamingID("n/a");
            }
        }

        @Override
        public boolean scan(BaseActivity activity, DBHandler handler, Media media, ScanActivity.mediaPlayback scanner) {
            activity.openIntent(BaseActivity.OVERLAY_REQUEST_CODE, media);
            return true;
        }

        @Override
        public String getQR(BaseActivity activity, Media media) {
            return media.getSummary();
        }

        @Override
        public int getColor() {
            return StreamingApplication.OTHER.getColor();
        }
    },
    COUNTER("Equation"){
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_counter, true);
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata) {
            media.setType(this.name());

            DecimalFormat format = new DecimalFormat();
            format.setDecimalSeparatorAlwaysShown(false);

            if (metadata != null && !metadata.get(MediaMetadata.Type.INPUT_CUSTOM).isEmpty()) {
                media.setStreamingID(metadata.get(MediaMetadata.Type.INPUT_CUSTOM));
                media.setAlternateID(metadata.get(MediaMetadata.Type.INPUT_TITLE));
            }

            if (media.getAlternateID().isEmpty()) {
                media.setAlternateID("0.0");}
            if (media.getStreamingID().isEmpty()) {
                media.setStreamingID("x + 1.0");}

            BigDecimal current = new BigDecimal(media.getAlternateID());
            media.setSummary("f(x) = " + DatabaseDialogHelper.getCalculator().format(media.getStreamingID()));
            media.setTitle("Value: " + format.format(current));
            media.setAlternateID(String.valueOf(current.doubleValue()));
        }

        @Override
        public boolean scan(final BaseActivity activity, DBHandler handler, final Media media, ScanActivity.mediaPlayback scanner) {
            DecimalFormat format = new DecimalFormat();
            format.setDecimalSeparatorAlwaysShown(false);

            BigDecimal result = DatabaseDialogHelper.getCalculator().handleCalculation(media.getAlternateID(), media.getStreamingID());
            if (result != null) {
                String resultString = format.format(result);
                media.setTitle("Value: " + resultString);

                final NotificationManager mNotifyMgr = (NotificationManager) activity.getSystemService(NOTIFICATION_SERVICE);
                NotificationCompat.Builder notification = BaseActivity.createNotification(activity, media,
                        R.drawable.ic_action_counter,
                        "Value: " + resultString,
                        media.getSummary() + ", when x is " + media.getAlternateID());

                media.setAlternateID(result.toPlainString());
                handler.addOrUpdateMedia(media);

                mNotifyMgr.notify((int) media.getId(), notification.build());
            } else {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, "Check your equation! " + media.getTitle(), Toast.LENGTH_LONG).show();
                    }
                });
            }
            return true;
        }

        @Override
        public int getColor() {
            return StreamingApplication.OTHER.getColor();
        }


        @Override
        public MediaMetadata getCSV(MediaMetadata baseCSV, Media media) {
            return baseCSV.set(MediaMetadata.Type.CSV_TITLE, media.getAlternateID());
        }
    },
    FORM("Custom Form"){
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_form, true);
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata) {
            super.setMetadata(media, metadata);
            StringBuilder summary = new StringBuilder();
            Set<String> foundTexts = new HashSet<>();
            Matcher matcher = Form.getPattern().matcher(media.getStreamingID());
            while (matcher.find()) {
                String param = matcher.group(1);
                String option = matcher.group(2);
                String detail = matcher.group(3);
                if (foundTexts.add(param + option + detail)) {
                    String[] details = option.replaceFirst(";", "").split(";");
                    summary.append(details[0]).append(":");
                    if (details.length > 2){
                        String[] savedChecks = detail.split(",");
                        for (int i = 0; i < savedChecks.length; i++){
                            if (savedChecks[i].equalsIgnoreCase("true")){
                                summary.append(" ").append(details[2 + i]).append(",");
                            }
                        }
                        if (summary.toString().endsWith(",")){
                            summary.deleteCharAt(summary.length()-1);
                        }
                    } else {
                        summary.append(" ").append(detail);
                    }
                    summary.append("\n");
                }
            }
            media.setSummary(summary.toString().trim().replace("?:", "?"));
        }

        @Override
        public boolean scan(final BaseActivity activity, final DBHandler handler, final Media media, ScanActivity.mediaPlayback scanner) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final Form form = new Form(false, activity, handler, media.getTitle(), media.getStreamingID());
                    new AlertDialog.Builder(activity)
                            .setTitle(media.getTitle())
                            .setView(form.getDialog())
                            .create()
                            .show();
                }
            });
            return true;
        }

        @Override
        public String getQR(BaseActivity activity, Media media) {
            return media.getStreamingID();
        }

        @Override
        public int getColor() {
            return StreamingApplication.OTHER.getColor();
        }
    },
    DICE("Roll Dice"){
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_dice, true);
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata) {
            super.setMetadata(media, metadata);
            Pattern pattern = Pattern.compile(
                    "H([0-9]+)" +
                            "L([0-9]+)" +
                            "R([0-9]+)" +
                            "M([-+]?[0-9]+)");

            Matcher matcher = pattern.matcher(media.getStreamingID());
            if (matcher.find()) {
                List<String> summary = new LinkedList<>();
                String highest = matcher.group(1),
                    lowest = matcher.group(2),
                    reroll = matcher.group(3),
                    modifier = matcher.group(4);

                if (!highest.equals("0")){
                    summary.add("Drop Highest: " + highest);}
                if (!lowest.equals("0")){
                    summary.add("Drop Lowest: " + lowest);}
                if (!reroll.equals("0")){
                    summary.add("Re-roll Below: " + reroll);}
                if (!modifier.equals("0")){
                    summary.add("Modifier: " + modifier);}
                media.setSummary(TextUtils.join("\n", summary));
            }
        }

        @Override
        public boolean scan(final BaseActivity activity, final DBHandler handler, final Media media, final ScanActivity.mediaPlayback scanner) {
            int sum = 0;
            List<String> finalRolls = new LinkedList<>();
            String options = "";

            Pattern pattern = Pattern.compile(
                    "((?:[0-9]+d[0-9]+[^0-9]?)+)+" +
                            "H([0-9]+)" +
                            "L([0-9]+)" +
                            "R([0-9]+)" +
                            "M([-+]?[0-9]+)");

            Matcher matcher = pattern.matcher(media.getStreamingID());
            while (matcher.find()) {
                int high = Integer.parseInt(matcher.group(2));
                int low = Integer.parseInt(matcher.group(3));
                int reRoll = Integer.parseInt(matcher.group(4));
                int modifier = Integer.parseInt(matcher.group(5));

                if (reRoll == 0) reRoll = Integer.MIN_VALUE;
                sum += modifier;

                List<String> summary = new LinkedList<>();
                if (high != 0){
                    summary.add("Drop Highest: " + high);}
                if (low != 0){
                    summary.add("Drop Lowest: " + low);}
                if (reRoll != Integer.MIN_VALUE){
                    summary.add("Re-roll Below: " + reRoll);}
                if (modifier != 0){
                    summary.add("Modifier: " + modifier);}
                options = TextUtils.join("\n", summary);

                List<Dice> resultRolls = new LinkedList<>();

                Pattern patternDie = Pattern.compile("([0-9]+)d([0-9]+)");
                Matcher matcherDie = patternDie.matcher(matcher.group(1));
                while(matcherDie.find()) {
                    int amount = Integer.parseInt(matcherDie.group(1));
                    int die = Integer.parseInt(matcherDie.group(2));
                    for (int i = 0; i < amount; i++) {
                        Dice rolled = new Dice(die, true);
                        resultRolls.add(rolled);
                        if (reRoll <= die){
                            while (rolled.getRoll() < reRoll){
                                rolled.roll();
                            }
                        }
                    }
                }

                List<Dice> amendedRolls = new LinkedList<>(resultRolls);
                Collections.sort(amendedRolls);
                if (low < amendedRolls.size()) {
                    for (int i = 0; i < low; i++) {
                        amendedRolls.remove(i);
                    }
                }

                if (high < amendedRolls.size()) {
                    for (int i = 0; i < high; i++) {
                        amendedRolls.remove(amendedRolls.size() - 1 - i);
                    }
                }

                for (int i = 0; i < resultRolls.size(); i++){
                    Dice roll = resultRolls.get(i);
                    if (amendedRolls.contains(roll)) {
                        sum += roll.getRoll();
                        finalRolls.add(roll.getDisplay());
                    }
                }
            }

            String extra = "\n";
            if (new HashSet<>(finalRolls).size() == 1 && finalRolls.size() >= 2 && finalRolls.size() <= 10){
                String[] tuples = new String[]{"", "", "Doubles", "Triples", "Quadruples", "Quintuples", "Sextuples", "Septuples", "Octuples", "Nonuples", "Decuples"};
                extra =  tuples[finalRolls.size()] + "!" + extra;
            }

            if (!finalRolls.isEmpty()) {
                String display = (extra + TextUtils.join(", ", finalRolls)).trim();
                media.setSummary((options + "\n\nLast Roll: " + sum + "\n" + display).trim());

                final NotificationManager mNotifyMgr = (NotificationManager) activity.getSystemService(NOTIFICATION_SERVICE);
                NotificationCompat.Builder notification = BaseActivity.createNotification(activity, media,
                        R.drawable.ic_action_dice,
                        "Dice Roll: " + sum,
                        display);

                mNotifyMgr.notify((int) media.getId(), notification.build());
            }
            return true;
        }

        @Override
        public int getColor() {
            return StreamingApplication.OTHER.getColor();
        }
    },
    VCARD("Insert or Edit"){
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_vcard, true);
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata) {
            super.setMetadata(media, metadata);
            media.setAlternateID("contact.vcf");
            media.getHandler().getFileHelper().saveMediaDataFile(media, media.getStreamingID());

            if (!metadata.get(MediaMetadata.Type.SUMMARY).isEmpty()) {
                media.setSummary(metadata.get(MediaMetadata.Type.SUMMARY));
            } else {
                Pattern pattern = Pattern.compile(
                        "([A-Z\\-]+)" +
                                "(?:[0-9A-Z,;=()\\-]+)*:" +
                                "(.*)" +
                                "(?=(?s:\n(?:[0-9A-Z,;=()\\-]+):))");

                Matcher matcher = pattern.matcher(media.getAlternateID());
                Set<String> details = new LinkedHashSet<>();
                while (matcher.find()) {
                    String param = matcher.group(1);
                    if (!param.equals("BEGIN") && !param.equals("END")
                            && !param.equals("VERSION") && !param.equals("N")) {
                        details.add(matcher.group(2).replace(";", " ").trim());
                    }
                }
                StringBuilder display = new StringBuilder();
                for (String item : details) {
                    display.append(item).append("\n");
                }
                media.setSummary(display.toString().trim());
            }
        }

        @Override
        public boolean scan(BaseActivity activity, DBHandler handler, Media media, ScanActivity.mediaPlayback scanner) {
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(Uri.parse(media.getStreamingID()), "text/vcard")
                    .putExtra(Intent.EXTRA_TITLE, media.getTitle())
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            return true;
        }

        @Override
        public String getQR(BaseActivity activity, Media media) {
            return media.getAlternateID();
        }

        @Override
        public int getColor() {
            return StreamingApplication.CONTACT.getColor();
        }
    },
    VIEW_CONTACT("View Contact"){
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_view_contact, true);
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata) {
            media.setType(this.name());
            media.setTitle(metadata.get(MediaMetadata.Type.TITLE));
            media.setStreamingID(metadata.get(MediaMetadata.Type.STREAMING));
        }

        @Override
        public int getColor() {
            return StreamingApplication.CONTACT.getColor();
        }
    },
    CALL("Dial"){
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_call, true);
        }

        @Override
        public int getColor() {
            return StreamingApplication.CONTACT.getColor();
        }
    },
    SMS("Send SMS"){
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_sms, true);
        }

        @Override
        public int getColor() {
            return StreamingApplication.CONTACT.getColor();
        }
    },
    EMAIL("Email"){
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_email, true);
        }
        @Override
        public int getColor() {
            return StreamingApplication.CONTACT.getColor();
        }
    },
    TIMER("Timer") {
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_timer, true);
        }

        @Override
        public boolean scan(BaseActivity activity, DBHandler handler, Media media, ScanActivity.mediaPlayback scanner) {
            Intent intent = new Intent(AlarmClock.ACTION_SET_TIMER)
                    .putExtra(AlarmClock.EXTRA_MESSAGE, media.getFigureName())
                    .putExtra(AlarmClock.EXTRA_LENGTH, Integer.parseInt(media.getStreamingID()))
                    .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            return true;
        }

        @Override
        public int getColor() {
            return StreamingApplication.CLOCK.getColor();
        }
    },
    STOPWATCH("Stopwatch") {
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_stopwatch, true);
        }

        @Override
        public boolean scan(final BaseActivity activity, final DBHandler handler, final Media media, ScanActivity.mediaPlayback scanner) {
            final NotificationManager mNotifyMgr = (NotificationManager) activity.getSystemService(NOTIFICATION_SERVICE);
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    long time = SystemClock.elapsedRealtime();
                    String uid = media.getScanUID();
                    StopwatchService.handler = handler;
                    if (!media.getStreamingID().equalsIgnoreCase("0L")) {
                        activity.startService(new Intent(activity, StopwatchService.class)
                                .setAction(StopwatchService.STOP)
                                .putExtra(ScanActivity.UID, uid));
                    } else {
                        RemoteViews view = new RemoteViews(activity.getPackageName(), R.layout.notification_stopwatch);
                        view.setChronometer(R.id.chronometer, time, null, true);
                        view.setTextViewText(R.id.title, ((media.getTitle().isEmpty() ? media.getFigureName() : media.getTitle()) + " Stopwatch").trim());
                        view.setOnClickPendingIntent(R.id.control, PendingIntent.getService(activity, 0, new Intent(activity, StopwatchService.class)
                                .setAction(StopwatchService.PLAY)
                                .putExtra(ScanActivity.UID, uid), PendingIntent.FLAG_UPDATE_CURRENT));
                        view.setOnClickPendingIntent(R.id.lap, PendingIntent.getService(activity, 0, new Intent(activity, StopwatchService.class)
                                .setAction(StopwatchService.LAP)
                                .putExtra(ScanActivity.UID, uid), PendingIntent.FLAG_UPDATE_CURRENT));

                        BaseActivity.getNotificationChannel(activity);
                        NotificationCompat.Builder notification = new NotificationCompat.Builder(activity, BaseActivity.NOTIFICATION_CHANNEL_ID)
                                .setColor(activity.getResourceColor(R.color.light_colorPrimary))
                                .setSmallIcon(R.drawable.ic_launcher_notification)
                                .setDeleteIntent(PendingIntent.getService(activity, 0, new Intent(activity, StopwatchService.class)
                                        .setAction(StopwatchService.STOP)
                                        .putExtra(ScanActivity.UID, uid), PendingIntent.FLAG_UPDATE_CURRENT))
                                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                                .setCustomContentView(view);

                        media.setSummary("");
                        media.setExternalID("");
                        media.setStreamingID(String.valueOf(time));
                        media.setTempDetails(new Object[]{view, notification});
                        if (!BaseFragment.addOrUpdate(media)) handler.addOrUpdateMedia(media);
                        mNotifyMgr.notify((int) media.getId(), notification.build());
                    }
                }
            });
            return true;
        }

        @Override
        public int getColor() {
            return StreamingApplication.CLOCK.getColor();
        }
    },
    ALARM("Alarm"){
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_alarm, true);
        }

        @Override
        public boolean scan(BaseActivity activity, DBHandler handler, Media media, ScanActivity.mediaPlayback scanner) {
            int hour = (Integer.parseInt(media.getStreamingID())) / 60 / 60;
            int minutes = (Integer.parseInt(media.getStreamingID()) - (hour * 60 * 60)) / 60;
            Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM)
                    .putExtra(AlarmClock.EXTRA_MESSAGE, media.getFigureName())
                    .putExtra(AlarmClock.EXTRA_HOUR, hour)
                    .putExtra(AlarmClock.EXTRA_MINUTES, minutes)
                    .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            return true;
        }

        @Override
        public int getColor() {
            return StreamingApplication.CLOCK.getColor();
        }
    },
    SCAN_ALARM("Scannable Alarm"){
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_scan_alarm, true);
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata) {
            super.setMetadata(media, metadata);
            media.setDetail(media.getStreamingID() + " - Inactive");
            if(media.getTitle().isEmpty()){
                media.setTitle("Untitled Alarm");
            }
        }

        @Override
        public boolean scan(final BaseActivity activity, DBHandler handler, final Media media, ScanActivity.mediaPlayback scanner) {
            AlarmManager manager = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
            Intent alarmIntent = new Intent(activity, AlarmReceiver.class)
                    .putExtra(ScanActivity.UID, media.getId())
                    .putExtra(AlarmReceiver.TITLE, media.getTitle() + " (" + media.getStreamingID() + " Alarm)")
                    .putExtra(AlarmReceiver.CONTENT, "Scan Tag to Stop Alarm")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(activity.getApplicationContext(), (int)media.getId(), alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT | Intent.FILL_IN_DATA);
            manager.cancel(pendingIntent);

            if (!media.getDetail().equals(media.getStreamingID() + " - Active")){
                media.setDetail(media.getStreamingID() + " - Active");
                handler.addOrUpdateMedia(media);

                manager.set(AlarmManager.RTC_WAKEUP, Event.parseTime(handler, media.getStreamingID()).getTimeInMillis(), pendingIntent);
            } else {
                media.setDetail(media.getStreamingID() + " - Inactive");
                handler.addOrUpdateMedia(media);
                activity.stopService(new Intent(activity, AlarmSoundService.class));

                NotificationManager mNotifyMgr = (NotificationManager) activity.getSystemService(NOTIFICATION_SERVICE);
                mNotifyMgr.cancel((int)media.getId());
            }
            return true;
        }

        @Override
        public int getColor() {
            return StreamingApplication.CLOCK.getColor();
        }
    },
    VEVENT("Event"){
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_event, true);
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata) {
            super.setMetadata(media, metadata);
            media.setSummary(metadata.get(MediaMetadata.Type.SUMMARY));
        }

        @Override
        public boolean scan(BaseActivity activity, DBHandler handler, Media media, ScanActivity.mediaPlayback scanner) {
            if (media.getStreamingID().startsWith("BEGIN:VCALENDAR")) {
                String[] data = media.getStreamingID().split("\n(.*?):", -1);
                if (media.getStreamingID().startsWith("BEGIN:VCALENDAR")) {
                    Intent intent = new Intent(Intent.ACTION_EDIT)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .setType("vnd.android.cursor.item/event")
                            .putExtra(CalendarContract.Events.TITLE, media.getTitle())
                            .putExtra(CalendarContract.Events.DESCRIPTION, data[3])
                            .putExtra(CalendarContract.Events.EVENT_LOCATION, data[4]);
                    if (!data[5].isEmpty()) {
                        Calendar start = Calendar.getInstance(TimeZone.getDefault());
                        start.set(
                                Integer.parseInt(data[5].substring(0, 4)),
                                Integer.parseInt(data[5].substring(4, 6)),
                                Integer.parseInt(data[5].substring(6, 8)),
                                Integer.parseInt(data[5].substring(9, 11)),
                                Integer.parseInt(data[5].substring(11, 13)),
                                Integer.parseInt(data[5].substring(13, 15)));
                        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start.getTimeInMillis());
                    }
                    if (!data[6].isEmpty()) {
                        Calendar end = Calendar.getInstance(TimeZone.getDefault());
                        end.set(
                                Integer.parseInt(data[6].substring(0, 4)),
                                Integer.parseInt(data[6].substring(4, 6)),
                                Integer.parseInt(data[6].substring(6, 8)),
                                Integer.parseInt(data[6].substring(9, 11)),
                                Integer.parseInt(data[6].substring(11, 13)),
                                Integer.parseInt(data[6].substring(13, 15)));
                        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end.getTimeInMillis());
                    }
                    activity.startActivity(intent);
                }
                return true;
            }
            return false;
        }

        @Override
        public String getQR(BaseActivity activity, Media media) {
            return media.getStreamingID();
        }

        @Override
        public int getColor() {
            return StreamingApplication.CLOCK.getColor();
        }
    },
    COUNTDOWN("Countdown"){
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_countdown, true);
        }

        @Override
        public boolean scan(BaseActivity activity, DBHandler handler, Media media, ScanActivity.mediaPlayback scanner) {
            String[] details = media.getStreamingID().split("~!~");
            if (details.length > 1) {
                Calendar dayOne = Calendar.getInstance(), dayTwo = Calendar.getInstance(); dayOne.set(Calendar.SECOND, 0);
                dayTwo.set(Integer.parseInt(details[0]), Integer.parseInt(details[1]), Integer.parseInt(details[2]), Integer.parseInt(details[3]), Integer.parseInt(details[4]), 0);
                String title = "Countdown " + handler.getTimeManager().format(TimeManager.DATE, dayTwo.getTime()).replace("-", "/");

                boolean past = false;
                if (dayTwo.before(dayOne)) {
                    past = true;
                    Calendar temp = dayOne;
                    dayOne = dayTwo;
                    dayTwo = temp;
                }

                String daysRemaining = "", remaining = "";
                if (!past && dayOne.get(Calendar.YEAR) == dayTwo.get(Calendar.YEAR)
                         && dayOne.get(Calendar.DAY_OF_YEAR) == dayTwo.get(Calendar.DAY_OF_YEAR)){
                    daysRemaining = "Today!";
                }

                long totalMilliseconds = dayTwo.getTimeInMillis() - dayOne.getTimeInMillis();
                long totalDays = TimeUnit.DAYS.convert(totalMilliseconds, TimeUnit.MILLISECONDS);
                totalMilliseconds -= TimeUnit.MILLISECONDS.convert(totalDays, TimeUnit.DAYS);

                long hours = TimeUnit.HOURS.convert(totalMilliseconds, TimeUnit.MILLISECONDS);
                long minutes = TimeUnit.MINUTES.convert(totalMilliseconds - TimeUnit.MILLISECONDS.convert(hours, TimeUnit.HOURS), TimeUnit.MILLISECONDS);

                dayOne.set(Calendar.HOUR_OF_DAY, 0);
                dayOne.set(Calendar.MINUTE, 0);
                dayTwo.set(Calendar.HOUR_OF_DAY, 0);
                dayTwo.set(Calendar.MINUTE, 0);

                int years = 0, months = 0, days = 0;
                while (dayOne.get(Calendar.YEAR) + 1 < dayTwo.get(Calendar.YEAR)) {
                    dayOne.add(Calendar.YEAR, 1);
                    years++;
                }

                while (dayOne.get(Calendar.MONTH) != dayTwo.get(Calendar.MONTH)) {
                    if (dayOne.get(Calendar.MONTH) + 1 == dayTwo.get(Calendar.MONTH)
                            && dayTwo.get(Calendar.DAY_OF_MONTH) < dayOne.get(Calendar.DAY_OF_MONTH)) {
                        break;
                    }
                    dayOne.add(Calendar.MONTH, 1);
                    months++;
                }

                if (dayOne.get(Calendar.MONTH) != dayTwo.get(Calendar.MONTH)
                        || dayTwo.get(Calendar.DAY_OF_MONTH) < dayOne.get(Calendar.DAY_OF_MONTH)) {

                    if (dayTwo.get(Calendar.DAY_OF_MONTH) >= dayOne.get(Calendar.DAY_OF_MONTH)) {
                        dayOne.add(Calendar.MONTH, 1);
                        months++;
                    }

                    while (dayOne.get(Calendar.MONTH) + 1 != dayTwo.get(Calendar.MONTH)) {
                        dayOne.add(Calendar.MONTH, 1);
                        months++;
                    }

                    if (months >= 12) {
                        months -= 12;
                        years++;
                    }
                }

                while (dayOne.before(dayTwo)) {
                    dayOne.add(Calendar.DAY_OF_YEAR, 1);
                    days++;

                    if (days >= dayOne.getActualMaximum(Calendar.DAY_OF_YEAR)) {
                        days -= dayOne.getActualMaximum(Calendar.DAY_OF_YEAR);
                        years++;
                    }
                }

                if (years != 0 || months != 0) {
                    if (totalDays != 0) daysRemaining = totalDays + " total days.";
                    if (years != 0) {
                        remaining += years + (years != 1 ? " years" : " year");
                    }
                    if (months != 0) {
                        if (years != 0) remaining += ", ";
                        remaining += months + (months != 1 ? " months" : " month");
                    }
                    if (days != 0) {
                        remaining += ", " + days + (days != 1 ? " days" : " day");
                    }
                } else if (totalDays != 0) {
                    remaining += days + (days != 1 ? " days" : " day");
                }

                if (!(daysRemaining.contains("Today") && minutes == 0 && hours == 0)) {
                    if (!remaining.isEmpty() && !remaining.contains("Today")) {
                        remaining += ", ";}
                    if (hours != 0){
                        remaining += hours + (hours != 1 ? " hours" : " hour");}
                    if (minutes != 0){
                        if (hours != 0){remaining += ", ";}
                        remaining += minutes + (minutes != 1 ? " minutes" : " minute");}
                    remaining += (past ? " ago." : " from now.");
                } else {
                    daysRemaining = "";
                    remaining = "Right now!";}

                daysRemaining = daysRemaining.isEmpty() ? "" : "\n" + daysRemaining;

                final NotificationManager mNotifyMgr = (NotificationManager) activity.getSystemService(NOTIFICATION_SERVICE);
                NotificationCompat.Builder notification = BaseActivity.createNotification(activity, media,
                        R.drawable.ic_action_countdown,
                        title,
                        remaining.trim())
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(
                                media.getTitle() + ":\n" + remaining.trim() + daysRemaining ));

                mNotifyMgr.notify((int) media.getId(), notification.build());
                return true;
            } return false;
        }

        @Override
        public int getColor() {
            return StreamingApplication.CLOCK.getColor();
        }
    };

    public void setMetadata(Media media, MediaMetadata metadata) {
        media.setType(this.name());
        if (metadata != null) {
            media.setAlternateID(metadata.get(MediaMetadata.Type.INPUT_OPTION));
            media.setStreamingID(metadata.get(MediaMetadata.Type.INPUT_CUSTOM));
        }
    }

    @Override
    public boolean scan(BaseActivity activity, DBHandler handler, Media media, ScanActivity.mediaPlayback scanner) {
        activity.startActivity(new Intent(Intent.ACTION_VIEW)
                .setData(Uri.parse(media.getStreamingID()))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        return false;
    }

    public MediaMetadata getCSV(MediaMetadata baseCSV, Media media){return baseCSV;}
    public String getQR(BaseActivity activity, Media media) {
        return null;
    }

    protected String name;
    protected Drawable icon;

    AuxiliaryApplication(String name){
        this.name = name;
    }

    public static AuxiliaryApplication valueOf(Media media){
        if (media != null && media.getEnabled().isFolder()) {
            for (AuxiliaryApplication application : values()) {
                if (media.getType().equalsIgnoreCase(application.name())) {
                    return application;
                }
            }
        }
        return null;
    }

    public String getName(){return name;}
    public String getAppPackage(){return "";}
    public String getActivityName(){return "";}
    public String getDownloadLink(){return "";}
    public String getUriData(){return "";}
    public boolean isInstalled() {return icon != null;}
    public Drawable getIcon(){return icon;}
}
