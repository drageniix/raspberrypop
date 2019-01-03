package com.drageniix.raspberrypop.utilities.api;

import android.annotation.SuppressLint;

import com.drageniix.raspberrypop.media.MediaMetadata;
import com.drageniix.raspberrypop.utilities.Logger;
import com.drageniix.raspberrypop.utilities.custom.CaseInsensitiveMap;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;

public class PandoraAPI extends APIBase {
    private static final String baseUrl = "https://tuner.pandora.com/services/json/";

    private String partnerAuth;
    private String partnerId;
    private int syncTimeOffset;

    private void login(){
        try {
            @SuppressLint("GetInstance") Cipher d_cipher = Cipher.getInstance("Blowfish/ECB/NoPadding");
            d_cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec("R=U!LH$O2B#".getBytes(), "Blowfish"));

            String url = baseUrl + "?method=auth.partnerLogin";
            JSONObject data = new JSONObject()
                    .put("username", "android")
                    .put("password", "AC7IBG09A3DTSYM4R41UJWL07VLN8JI7")
                    .put("deviceModel", "android-generic")
                    .put("version", "5");

            int requestTime = (int) (System.currentTimeMillis() / 1000);

            JSONObject response = getJSON(postJSONRequest(url, data.toString())).getJSONObject("result");
            String syncTime = decrypt(d_cipher, response.getString("syncTime"));
            if (syncTime.length() > 10) syncTime = syncTime.substring(1);
            partnerAuth = response.getString("partnerAuthToken");
            partnerId = response.getString("partnerId");
            syncTimeOffset = Integer.parseInt(syncTime) - requestTime;
        } catch (IllegalBlockSizeException e){
            login();
        } catch (Exception e){
            Logger.log(Logger.API, e);
        }
    }

    public CaseInsensitiveMap<String, MediaMetadata> getStations(){
        CaseInsensitiveMap<String, MediaMetadata> searchTitles = new CaseInsensitiveMap<>();
        if (partnerAuth == null){login();}

        try {
            @SuppressLint("GetInstance") Cipher e_cipher = Cipher.getInstance("Blowfish/ECB/PKCS5Padding");
            e_cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec("6#26FRL$ZWD".getBytes(), "Blowfish"));

            String url = baseUrl + "?method=auth.userLogin&partner_id=" + partnerId + "&auth_token=" + partnerAuth;
            JSONObject data = new JSONObject()
                    .put("loginType", "user")
                    .put("username", handler.getPreferences().getPandoraAccount())
                    .put("password", handler.getPreferences().getPandoraPassword())
                    .put("partnerAuthToken", partnerAuth)
                    .put("returnStationList", true)
                    .put("includeStationArtUrl", true)
                    .put("syncTime", (int)(System.currentTimeMillis() / 1000) + syncTimeOffset);

            JSONObject response = getJSON(postJSONRequest(url, encrypt(e_cipher, data.toString())));
            if (response != null && response.has("result")) {
                JSONArray responseArray = response.getJSONObject("result").getJSONObject("stationListResult").getJSONArray("stations");
                if (responseArray != null && responseArray.length() > 0) {
                    for (int i = 0; i < responseArray.length(); i++) {
                        MediaMetadata metadata = new MediaMetadata();
                        JSONObject media = responseArray.getJSONObject(i);
                        metadata.set(MediaMetadata.Type.TITLE, media.getString("stationName"));
                        metadata.set(MediaMetadata.Type.STREAMING, media.getString("stationId"));
                        metadata.set(MediaMetadata.Type.THUMBNAIL, media.getString("artUrl"));
                        searchTitles.put(metadata.get(MediaMetadata.Type.TITLE), metadata);
                    }
                }
            } else if (response != null && response.has("code")
                    && response.getInt("code") == 1002){ //bad login
                preferences.clearPandora();
            } else if (response != null && response.has("code")
                    && response.getInt("code") == 1001){ //auth expired
                partnerAuth = null;
                return getStations();
            }
        } catch (IllegalBlockSizeException e){
            return getStations();
        } catch (Exception e){
            searchTitles.clear();
            Logger.log(Logger.API, e);
        }
        return searchTitles;
    }

    private String encrypt(Cipher cipher, String input) throws Exception{
        return new BigInteger((cipher.doFinal(input.getBytes()))).toString(16).toLowerCase();
    }

    private String decrypt(Cipher cipher, String input) throws Exception{
        String result = new String(reduceSize(cipher.doFinal(
                new BigInteger(input, 16).toByteArray())));

        StringBuilder parsedResult = new StringBuilder();
        for(char c : result.toCharArray()){
            if (Character.isDigit(c)){
                parsedResult.append(c);
            }
        }
        return parsedResult.toString();
    }

    private byte[] reduceSize(byte[] oldArray){
        byte[] newArray= new byte[oldArray.length-4];
        System.arraycopy(oldArray, 3, newArray, 0, newArray.length);
        return newArray;
    }
}