package com.drageniix.raspberrypop.utilities.api;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import com.drageniix.raspberrypop.media.MediaMetadata;
import com.drageniix.raspberrypop.utilities.custom.CaseInsensitiveMap;

import java.util.List;

public class DeviceAPI extends APIBase {
    private Context context;

    DeviceAPI(Context context){
        this.context = context;
    }

    public CaseInsensitiveMap<String, MediaMetadata> getWifiConfigurations() {
        CaseInsensitiveMap<String, MediaMetadata> searchTitles = new CaseInsensitiveMap<>();
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            List<WifiConfiguration> knownConfigs = wifiManager.getConfiguredNetworks();
            if (knownConfigs != null) {
                for (WifiConfiguration wifiConfiguration : knownConfigs) {
                    searchTitles.put(wifiConfiguration.SSID.substring(1, wifiConfiguration.SSID.length() - 1),
                            new MediaMetadata()
                                    .set(MediaMetadata.Type.INPUT_CUSTOM, String.valueOf(wifiConfiguration.networkId)));
                }
            }
        }
        return searchTitles;
    }
}
