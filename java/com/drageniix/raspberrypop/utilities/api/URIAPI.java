package com.drageniix.raspberrypop.utilities.api;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.media.MediaMetadata;
import com.drageniix.raspberrypop.utilities.Logger;


import com.drageniix.raspberrypop.utilities.custom.CaseInsensitiveMap;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class URIAPI extends APIBase {
    private PackageManager packageManager;
    URIAPI(Context context){this.packageManager = context.getPackageManager();}

    public CaseInsensitiveMap<String, MediaMetadata> getInstalledApps(){
        CaseInsensitiveMap<String, MediaMetadata> searchTitles = new CaseInsensitiveMap<>();
        Set<String> duplicates = new LinkedHashSet<>();
        try {
            List<ApplicationInfo> packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo packageInfo : packages) {
                String label = String.valueOf(packageInfo.loadLabel(packageManager));
                String packageName = packageInfo.packageName;
                if (!label.equals(packageName) &&
                        ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                        ||(packageManager.getLaunchIntentForPackage(packageName) != null))) {
                    MediaMetadata metadata = new MediaMetadata()
                            .set(MediaMetadata.Type.PACKAGE_NAME, packageName)
                            .set(MediaMetadata.Type.PACKAGE_LABEL, label)
                            .set(MediaMetadata.Type.PACKAGE_INTENT, "application");

                    if (metadata.get(MediaMetadata.Type.PACKAGE_NAME).equalsIgnoreCase("com.netflix.mediaclient")) {
                        metadata.set(MediaMetadata.Type.PACKAGE_CLASS, "com.netflix.mediaclient.ui.search.SearchActivity");
                    }

                    if (duplicates.contains(label) || searchTitles.containsKey(label)){
                        MediaMetadata old = searchTitles.get(label);
                        if (old != null) {
                            searchTitles.put(old.get(MediaMetadata.Type.PACKAGE_LABEL) + " (" + old.get(MediaMetadata.Type.PACKAGE_NAME) + ")", old);
                            searchTitles.remove(label);
                        }
                        searchTitles.put(label + " (" + packageName + ")", metadata);
                        duplicates.add(label);
                    } else {
                        searchTitles.put(label, metadata);
                    }


                }
            }
        } catch (Exception e) {
            searchTitles.clear();
            Logger.log(Logger.API, e);
        }
        if (searchTitles.isEmpty()) searchTitles.put("", null);
        return searchTitles;
    }

    public Bitmap getPackageIcon(Media media, String source){
        Drawable icon;
        try {
            ApplicationInfo ai = packageManager.getApplicationInfo(source, PackageManager.GET_META_DATA);
            icon = packageManager.getApplicationIcon(ai);
            media.setAuxiliaryString(ai.packageName);
        } catch (Exception e) {
            icon = null;
        }
        if (icon == null) return null;
        else return ((BitmapDrawable)icon).getBitmap();
    }

    public void checkURI(Media media) {
        String uri = media.getStreamingID();
        if (new Intent(Intent.ACTION_VIEW)
                .setData(Uri.parse(media.getStreamingID()))
                .resolveActivity(packageManager) == null){
            media.setStreamingID(media.getAlternateID());
            media.setAlternateID(uri);
        }
    }

    public Bitmap getUriIcon(Media media, String uri){
        Drawable icon = null;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setData(Uri.parse(uri));

            ResolveInfo resolveInfo = packageManager.resolveActivity(
                    intent, PackageManager.MATCH_DEFAULT_ONLY);

            if (resolveInfo != null) {
                ActivityInfo activityInfo = resolveInfo.activityInfo;
                String appPackage = activityInfo.packageName;
                if (!appPackage.equals("android")) {
                    media.setAuxiliaryString(appPackage);
                    icon = activityInfo.applicationInfo.loadIcon(packageManager);
                }
            }
        } catch (Exception e) {
            Logger.log(Logger.API, e);
            icon = null;
        }

        if (icon == null || (BitmapDrawable)icon == null) return null;
        else return ((BitmapDrawable)icon).getBitmap();
    }
}
