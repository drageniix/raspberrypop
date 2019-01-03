package com.drageniix.raspberrypop.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.support.v7.preference.PreferenceManager;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.dialog.adapter.media.MediaAdapter;

import java.util.HashSet;
import java.util.Map;

public class Preferences {
    private final static String
            LOGGER = "LOGGER",
            ASK_CAMERA = "ASK_CAMERA",
            ASK_PLACE = "ASK_PLACE",
            ASK_CONTACT = "ASK_CONTACT",
            PREMIUM = "PREMIUUM",
            PURCHASED_PREMIUM = "PURCHASED_PREMIUUM",
            DISPLAY_DEBUG = "DEBUG",
            COLLECTION = "COLLECTION",
            BARCODE = "BARCODE",
            CONTINUOUS = "CONTINUOUS",
            THUMBNAIL = "THUMBNAIL",
            VIEW = "VIEW",
            THEME = "THEME",
            UUID = "UUID",
            ORDER = "ORDER",
            REVERSE_ORDER = "REVERSE_ORDER",
            ROKU_IP = "ROKU_IP",
            ROKU_NAME = "ROKU_NAME",
            PLEX_TOKEN = "PLEX_TOKEN",
            PLEX_ACCOUNT = "PLEX_ACCOUNT",
            PLEX_PASSWORD = "PLEX_PASSWORD",
            SPOTIFY_ACCESS = "SPOTFY_ACCESS",
            SPOTIFY_REFRESH = "SPOTFY_REFRESH",
            SPOTIFY_ACCOUNT = "SPOTIFY_ACCOUNT",
            TWITCH_ACCOUNT = "TWITCH_ACCOUNT",
            TWITCH_ACCESS = "TWITCH_ACCESS",
            TWITCH_DISPLAY = "TWITCH_DISPLAY",
            PANDORA_ACCOUNT = "PANDORA_ACCOUNT",
            PANDORA_PASSWORD = "PANDORA_PASSWORD",
            COLLECTION_SIZE = "COLLECTION_SIZE",
            PRICES = "PRICES",
            CHECKLIST = "CHECKLIST";

    private SharedPreferences preferences, deviceSpecific;
    private Context context;

    Preferences(Context context){
        this.context = context.getApplicationContext();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.deviceSpecific = context.getSharedPreferences("DEVICE_SPECIFIC", Context.MODE_PRIVATE);
        if (getUUID().isEmpty()){
            deviceSpecific.edit().putString(UUID, java.util.UUID.randomUUID().toString()).apply();
        }
    }

    @SuppressWarnings({"unchecked"})
    void readAll(Object input){
        Map<String, ?> entries = (Map<String, ?>)input;
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();

        for (Map.Entry<String, ?> entry : entries.entrySet()) {
            Object v = entry.getValue();
            String key = entry.getKey();
            if (v instanceof Boolean)
                editor.putBoolean(key, (Boolean) v);
            else if (v instanceof Float)
                editor.putFloat(key, (Float) v);
            else if (v instanceof Integer)
                editor.putInt(key, (Integer) v);
            else if (v instanceof Long)
                editor.putLong(key, (Long) v);
            else if (v instanceof String)
                editor.putString(key, ((String) v));
        }

        editor.apply();
    }

    Map<String, ?> getAllAccounts(){
        return preferences.getAll();
    }
    public String getUUID() {
        return deviceSpecific.getString(UUID, "");
    }
    public boolean hasPremium(){return preferences.getBoolean(PREMIUM, false);}
    public void setPremium(boolean premium) {preferences.edit().putBoolean(PREMIUM, premium).apply();}
    public boolean hasPurchasedPremium(){return preferences.getBoolean(PURCHASED_PREMIUM, false);}
    void setPurchasedPremium() {preferences.edit().putBoolean(PURCHASED_PREMIUM, true).apply();}
    int getCollectionSize(){return preferences.getInt(COLLECTION_SIZE, 0);}
    void setCollectionSize(int size) {preferences.edit().putInt(COLLECTION_SIZE, size).apply();}

    public boolean hasThumbnail() {
        if (!preferences.getBoolean(THUMBNAIL, true)) {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            return wifiManager == null || (wifiManager != null && wifiManager.isWifiEnabled());
        } else {
            return true;
        }
    }

    public String getColorString(String key){return preferences.getString(key, "");}
    public void setColorString(String color, String value){preferences.edit().putString(color, value).apply();}

    public boolean hasPlex(){
        return !getPlexToken().isEmpty();
    }
    public boolean hasPandora(){return !getPandoraAccount().isEmpty();}
    public boolean hasSpotify(){
        return !getSpotifyAccess().isEmpty();
    }
    public boolean hasTwitch(){
        return !getTwitchAccess().isEmpty();
    }
    public boolean hasRoku(){
        return !getRokuIp().isEmpty();
    }
    public boolean openToBarcode(){return preferences.getBoolean(BARCODE, false);}
    public boolean continuousBarcode(){return preferences.getBoolean(CONTINUOUS, true);}
    public boolean showPrices(){return preferences.getBoolean(PRICES, true);}

    public boolean showChecklist(){return preferences.getBoolean(CHECKLIST, true);}
    public void setChecklist(boolean value) {preferences.edit().putBoolean(CHECKLIST, value).apply();}

    public String getLogger() {
        return deviceSpecific.getString(LOGGER, "");
    }
    public void setLogger(String value) {deviceSpecific.edit().putString(LOGGER, value).apply();}

    public boolean debugDatabase(){return preferences.getStringSet(DISPLAY_DEBUG, new HashSet<String>()).contains("0");}
    public boolean debugAmazon(){return preferences.getStringSet(DISPLAY_DEBUG, new HashSet<String>()).contains("1");}

    public boolean askedCamera() {
        if (!deviceSpecific.contains(ASK_CAMERA)) {
            deviceSpecific.edit().putBoolean(ASK_CAMERA, true).apply();
            return false;
        } else {
            return true;
        }
    }

    public boolean askedContact() {
        if (!deviceSpecific.contains(ASK_CONTACT)) {
            deviceSpecific.edit().putBoolean(ASK_CONTACT, true).apply();
            return false;
        } else {
            return true;
        }
    }
    public boolean askedLocation() {
        if (!deviceSpecific.contains(ASK_PLACE)) {
            deviceSpecific.edit().putBoolean(ASK_PLACE, true).apply();
            return false;
        } else {
            return true;
        }
    }

    //----------------------------------------------------------------------

    public void clearPandora() {
        setPandoraAccount("");
        setPandoraPassword("");
    }

    public void clearPlex(){
        setPlexAccount("");
        setPlexPassword("");
        setPlexToken("");
    }
    public void clearSpotify(){
        setSpotifyAccess("");
        setSpotifyRefresh("");
        setSpotifyAccount("");
    }
    public void clearRoku(){
        setRokuIp("");
        setRokuName("");
    }
    public void clearTwitch(){
        setTwitchDisplay("");
        setTwitchAccess("");
        setTwitchAccount("");
    }
    //----------------------------------------------------------------------

    public String getCollection(String defaultCollection){return preferences.getString(COLLECTION, defaultCollection);}
    public void setCollection(String collection){preferences.edit().putString(COLLECTION, collection).apply();}

    public int getTheme() {return preferences.getInt(THEME, R.style.LightTheme);}
    public void setTheme(int theme){preferences.edit().putInt(THEME, theme).apply();}

    public MediaAdapter.ViewType getView() {return MediaAdapter.ViewType.valueOf(preferences.getString(VIEW, "CARD"));}

    public MediaAdapter.Order getLastOrder() {return MediaAdapter.Order.values()[preferences.getInt(ORDER, 0)];}
    public void setLastOrder(int value) {preferences.edit().putInt(ORDER, value).apply();}
    public boolean getReverseOrder() {return preferences.getBoolean(REVERSE_ORDER, false);}
    public void setReverseOrder(boolean value) {preferences.edit().putBoolean(REVERSE_ORDER, value).apply();}

    //----------------------------------------------------------------------

    public String getRokuIp() {
        return preferences.getString(ROKU_IP, "");
    }
    public void setRokuIp(String value) {
        preferences.edit().putString(ROKU_IP, value).apply();
    }
    public String getRokuName() {
        return preferences.getString(ROKU_NAME, "");
    }
    public void setRokuName(String value) {
        preferences.edit().putString(ROKU_NAME, value).apply();
    }

    public String getPlexToken() {
        return preferences.getString(PLEX_TOKEN, "");
    }
    public void setPlexToken(String value) {
        preferences.edit().putString(PLEX_TOKEN, value).apply();
    }
    public String getPlexAccount() {
        return preferences.getString(PLEX_ACCOUNT, "");
    }
    public void setPlexAccount(String value) {
        preferences.edit().putString(PLEX_ACCOUNT, value).apply();
    }
    public String getPlexPassword() {
        return preferences.getString(PLEX_PASSWORD, "");
    }
    public void setPlexPassword(String value) {
        preferences.edit().putString(PLEX_PASSWORD, value).apply();
    }

    public String getSpotifyAccess() {
        return preferences.getString(SPOTIFY_ACCESS, "");
    }
    public void setSpotifyAccess(String value) {
        preferences.edit().putString(SPOTIFY_ACCESS, value).apply();
    }
    public String getSpotifyRefresh() {
        return preferences.getString(SPOTIFY_REFRESH, "");
    }
    public void setSpotifyRefresh(String value) {
        preferences.edit().putString(SPOTIFY_REFRESH, value).apply();
    }
    public String getSpotifyAccount(){return preferences.getString(SPOTIFY_ACCOUNT, "");}
    public void setSpotifyAccount(String value) {
        preferences.edit().putString(SPOTIFY_ACCOUNT, value).apply();
    }

    public String getTwitchAccount() {
        return preferences.getString(TWITCH_ACCOUNT, "");
    }
    public void setTwitchAccount(String value) {
        preferences.edit().putString(TWITCH_ACCOUNT, value).apply();
    }
    private String getTwitchAccess(){return preferences.getString(TWITCH_ACCESS, "");}
    public void setTwitchAccess(String value) {
        preferences.edit().putString(TWITCH_ACCESS, value).apply();
    }
    public String getTwitchDisplay(){return preferences.getString(TWITCH_DISPLAY, "");}
    public void setTwitchDisplay(String value) {
        preferences.edit().putString(TWITCH_DISPLAY, value).apply();
    }

    public String getPandoraAccount() {
        return preferences.getString(PANDORA_ACCOUNT, "");
    }
    public void setPandoraAccount(String value) {
        preferences.edit().putString(PANDORA_ACCOUNT, value).apply();
    }
    public String getPandoraPassword() {
        return preferences.getString(PANDORA_PASSWORD, "");
    }
    public void setPandoraPassword(String value) {
        preferences.edit().putString(PANDORA_PASSWORD, value).apply();
    }
}