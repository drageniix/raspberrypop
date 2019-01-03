package com.drageniix.raspberrypop.utilities.categories;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.activities.ScanActivity;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.Logger;
import com.drageniix.raspberrypop.utilities.api.RokuAPI;
import com.drageniix.raspberrypop.utilities.api.TaskerIntent;

import java.util.List;

public enum ScanApplication implements ApplicationCategory {

    TASKER("net.dinglisch.android.taskerm", "https://play.google.com/store/apps/details?id=net.dinglisch.android.taskerm&hl=en", "", "content://net.dinglisch.android.tasker/tasks") {
        @Override
        public boolean scan(final BaseActivity activity, final DBHandler handler, final Media media, final ScanActivity.mediaPlayback scanner) {
            String task = scanner.isTaskerWait() ? media.getTaskerTaskB() : media.getTaskerTaskA();
            if (media.useTasker() && !task.isEmpty() && TaskerIntent.testStatus(activity).equals(TaskerIntent.Status.OK)) {
                TaskerIntent i = new TaskerIntent(task);

                for (String param : media.getTaskerParams()) {i.addParameter(param.trim());}
                i.addLocalVariable("%uid", media.getScanUID());
                i.addLocalVariable("%title", media.getTitle());
                i.addLocalVariable("%name", media.getFigureName());

                if (scanner.isTaskerWait()) {
                    BroadcastReceiver br = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            context.unregisterReceiver(this);
                            if (intent.getBooleanExtra(TaskerIntent.EXTRA_SUCCESS_FLAG, false)) {
                                media.getEnabled().scan(activity, handler, media, scanner);
                                TASKER.scan(activity, handler, media, scanner);
                                scanner.updateProgress(activity, media);
                            }
                        }
                    };
                    activity.registerReceiver(br, i.getCompletionFilter());
                }
                activity.sendBroadcast(i);
                return true;
            }
            return false;
        }

        @Override
        public void search(Context context, List<String> searchTitles) {
            try (Cursor cursor = context.getContentResolver().query(Uri.parse(ScanApplication.TASKER.getUriData()), null, null, null, null)) {
                if (cursor != null) {
                    int nameCol = cursor.getColumnIndex("name");
                    while (cursor.moveToNext()) {
                        searchTitles.add(cursor.getString(nameCol));
                    }
                }
            } catch (Exception e) {
                Logger.log(Logger.DB, e);
            }
        }

        @Override
        public int getColor() {return 0;}
    },
    ROKU("com.roku.remote", "https://play.google.com/store/apps/details?id=com.roku.remote&hl=en","","") {
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            super.setIcon(context, handler);
            if (!isInstalled()) {
                this.appPackage = "com.tinybyteapps.robyte";
                this.downloadLink = "https://play.google.com/store/apps/details?id=com.tinybyteapps.robyte";
                super.setIcon(context, handler);
            }
        }

        @Override
        public boolean scan(BaseActivity activity, DBHandler handler, Media media, ScanActivity.mediaPlayback scanner) {
            String roku = handler.getPreferences().getRokuIp();
            boolean success = false;
            if (!roku.isEmpty()){
                switch (media.getEnabled()){
                    case URI:
                        if (media.getStreamingID().contains("youtu")) {
                            String youtube = null;
                            if (media.getStreamingID().contains(":")) {
                                youtube = media.getStreamingID().substring(media.getStreamingID().lastIndexOf(":") + 1);
                            } else if (!media.getStreamingID().contains("=") && media.getStreamingID().contains("/")) {
                                youtube = media.getStreamingID().substring(media.getStreamingID().lastIndexOf("/") + 1);}

                            if (youtube != null){
                                success = RokuAPI.launchApp(roku,
                                        "?contentID=" + youtube + "&mediaType=series",
                                        "837");}
                        } else if (media.getStreamingID().contains("netflix")) {
                            String netflix = null;
                            if (media.getStreamingID().contains("/")) {
                                netflix = media.getStreamingID().substring(media.getStreamingID().lastIndexOf("/") + 1);}

                            if (netflix != null){
                                success = RokuAPI.launchApp(roku,
                                        "?contentID=" + media.getStreamingID() + "&mediaType=series",
                                        "12");}
                        }
                        break;
                    case YOUTUBE:
                        if (!media.getStreamingID().contains("=")) {
                            success = RokuAPI.launchApp(roku,
                                    "?contentID=" + media.getAlternateID() + "&mediaType=series",
                                    "837");
                        }
                        break;
                }
            }
            return success;
        }

        @Override
        public void search(Context context, List<String> searchTitles) {}

        @Override
        public int getColor() {return 0;}

    };

    protected String appPackage, activityName, downloadLink, uriData;
    private Drawable icon;
    private boolean isInstalled;

    ScanApplication(String appPackage, String downloadLink, String activityName, String uriData){
        this.appPackage = appPackage;
        this.downloadLink = downloadLink;
        this.activityName = activityName;
        this.uriData = uriData;
    }

    public void setIcon(BaseActivity context, DBHandler handler){
        try {
            isInstalled = true;
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(getAppPackage(), PackageManager.GET_META_DATA);
            icon = pm.getApplicationIcon(ai);
        } catch (PackageManager.NameNotFoundException e) {
            isInstalled = false;
            icon = null;
        }
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public String getQR(BaseActivity activity, Media media) {
        return null;
    }

    public abstract void search(Context context, List<String> searchTitles);
    public String getAppPackage(){return appPackage;}
    public String getActivityName(){return activityName;}
    public String getDownloadLink(){return downloadLink;}
    public String getUriData(){return uriData;}
    public Drawable getIcon(){return icon;}
    public boolean isInstalled() {return isInstalled;}
}
