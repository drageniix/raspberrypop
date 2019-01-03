package com.drageniix.raspberrypop.utilities.api;

import android.content.Intent;
import android.net.Uri;
import com.drageniix.raspberrypop.fragments.SettingsFragment;
import com.drageniix.raspberrypop.media.MediaMetadata;
import com.drageniix.raspberrypop.utilities.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import com.drageniix.raspberrypop.utilities.custom.CaseInsensitiveMap;

import java.util.List;

public class TwitchAPI extends APIBase{

    private static final String twitch_base = "https://api.twitch.tv/kraken";
    private static final String twitch_version = "application/vnd.twitchtv.v5+json";
    private String twitch_client;
    private String twitch_redirectURI;
    private String[] headers;
    private SettingsFragment fragment;

    TwitchAPI(List<String> data){
        this.twitch_redirectURI = data.get(0);
        this.twitch_client = data.get(1);

        headers = new String[]{
                "Accept", twitch_version,
                "Client-ID", twitch_client,
        };
    }

    public void authenticateTwitch(SettingsFragment fragment){
        this.fragment = fragment;
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme("https")
                .authority("api.twitch.tv")
                .appendPath("kraken")
                .appendPath("oauth2")
                .appendPath("authorize")
                .appendQueryParameter("response_type", "token")
                .appendQueryParameter("client_id", twitch_client)
                .appendQueryParameter("redirect_uri", twitch_redirectURI)
                .appendQueryParameter("scope", "user_read")
                .appendQueryParameter("state", preferences.getUUID());

        Intent launchBrowser = new Intent("android.intent.action.VIEW", uriBuilder.build());
        fragment.getActivity().startActivity(launchBrowser);
    }

    public void setAccessCode(Uri uri){
        try {
            String accessCode = uri.getFragment().split("=")[1].split("&")[0];

            String url = twitch_base + "/user";
            String[] authHeaders = new String[]{
                    "Accept", twitch_version,
                    "Client-ID", twitch_client,
                    "Authorization", "OAuth " + accessCode
            };

            JSONObject response =  getJSON(getRequest(url, null, authHeaders));
            if (response == null) clear();

            preferences.setTwitchAccess(accessCode);
            preferences.setTwitchAccount(response.getString("_id"));
            preferences.setTwitchDisplay(response.getString("display_name"));
        } catch (Exception e){
            clear();
            Logger.log(Logger.API, e);
        }

        if (fragment != null && !fragment.isDetached()){
            fragment.updateValues();
            fragment = null;
        }
    }

    public void clear(){
        preferences.clearTwitch();
    }

    public CaseInsensitiveMap<String, MediaMetadata> getFollowedChannels(){
        CaseInsensitiveMap<String, MediaMetadata> searchTitles = new CaseInsensitiveMap<>();
        try {
            String url = twitch_base + "/users/" + preferences.getTwitchAccount() + "/follows/channels";
            String[] data = new String[]{
                "limit", "100"
            };

            JSONObject response =  getJSON(getRequest(url, data, headers));
            if (response != null) {
                JSONArray responseArray = response.getJSONArray("follows");
                if (responseArray != null && responseArray.length() > 0) {
                    for (int i = 0; i < responseArray.length(); i++) {
                        JSONObject media = responseArray.getJSONObject(i).getJSONObject("channel");

                        String title = media.getString("display_name");
                        String game = media.getString("game");
                        String gameName = game.isEmpty() ? "" : " (" + game +  ")";
                        searchTitles.put(title + gameName, new MediaMetadata()
                                .set(MediaMetadata.Type.STREAMING, media.getString("_id")));
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


    public CaseInsensitiveMap<String, MediaMetadata> searchChannels(String query){
        CaseInsensitiveMap<String, MediaMetadata> searchTitles = new CaseInsensitiveMap<>();
        try {
            String url = twitch_base + "/search/channels";
            String[] data = new String[]{
                    "query", query
            };

            JSONObject response =  getJSON(getRequest(url, data, headers));
            if (response != null) {
                JSONArray responseArray = response.getJSONArray("channels");
                if (responseArray != null && responseArray.length() > 0) {
                    for (int i = 0; i < responseArray.length(); i++) {
                        JSONObject media = responseArray.getJSONObject(i);
                        String title = media.getString("display_name");
                        String game = media.getString("game");
                        String gameName = game.isEmpty() ? "" : " (" + game +  ")";

                        searchTitles.put(title + gameName, new MediaMetadata()
                                .set(MediaMetadata.Type.STREAMING, media.getString("_id")));
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

    public MediaMetadata getChannel(String id){
        try {
            String url = twitch_base + "/channels/" + id;
            JSONObject response =  getJSON(getRequest(url, null, headers));
            if (response == null) return null;

            MediaMetadata mediaData = new MediaMetadata();
            mediaData.set(MediaMetadata.Type.ALTERNATE, response.getString("_id"));
            mediaData.set(MediaMetadata.Type.STREAMING, response.getString("name"));
            mediaData.set(MediaMetadata.Type.TITLE, response.getString("display_name"));
            mediaData.set(MediaMetadata.Type.DETAIL, response.getString("game"));
            mediaData.set(MediaMetadata.Type.SUMMARY, response.getString("status"));

            String logo = response.getString("logo");
            String profileBanner = response.getString("profile_banner");
            String videoBanner = response.getString("video_banner");
            mediaData.set(MediaMetadata.Type.THUMBNAIL, (!logo.equals("null")) ? logo : ((!profileBanner.equals("null")) ? profileBanner : videoBanner));
            return mediaData;
        } catch (Exception e){
            Logger.log(Logger.API, e);
        }
        return null;
    }

    public String getAppropriateUri(String id, String name){
        String uriString = "twitch://open";
        try {
            String url = twitch_base + "/streams/" + id;
            String[] data = new String[]{
                    "stream_type", "live"
            };

            JSONObject response =  getJSON(getRequest(url, data, headers));
            if (response != null){
                uriString = (!response.getString("stream").equals("null") ?
                        "twitch://stream/" :
                        "twitch://open?channel=")
                        + name;
            }
        } catch (Exception e){
            Logger.log(Logger.API, e);
        }

        return uriString;
    }
}
