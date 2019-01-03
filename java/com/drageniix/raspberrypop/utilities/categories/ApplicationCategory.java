package com.drageniix.raspberrypop.utilities.categories;

import android.graphics.drawable.Drawable;

import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.activities.ScanActivity;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.utilities.DBHandler;

public interface ApplicationCategory {
    String getAppPackage();
    String getActivityName();
    String getDownloadLink();
    String getUriData();
    String getName();
    Drawable getIcon();
    int getColor();
    boolean isInstalled();
    void setIcon(BaseActivity context, DBHandler handler);
    boolean scan(BaseActivity activity, DBHandler handler, Media media, ScanActivity.mediaPlayback scanner);
    String getQR(BaseActivity activity, Media media);
}
