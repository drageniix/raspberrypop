package com.drageniix.raspberrypop.utilities.api;

import android.content.Intent;
import android.net.Uri;
import android.util.Base64;

import com.drageniix.raspberrypop.fragments.SettingsFragment;
import com.drageniix.raspberrypop.media.MediaMetadata;
import com.drageniix.raspberrypop.utilities.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;
import com.drageniix.raspberrypop.utilities.custom.CaseInsensitiveMap;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpotifyAPI extends APIBase {

    private String spotify_redirectURI;
    private String spotify_clientID;
    private String spotify_secret;
    private String access_code;
    private String refresh_token;
    private SettingsFragment fragment;
    private long lastRequestTime;
    private String clientAccessCode;


    SpotifyAPI(List<String> data){
        this.spotify_redirectURI = data.get(0);
        this.spotify_clientID = data.get(1);
        this.spotify_secret = data.get(2);
        this.access_code = preferences.getSpotifyAccess();
        this.refresh_token = preferences.getSpotifyRefresh();
    }

    public void authenticateSpotify(SettingsFragment fragment){
        this.fragment = fragment;
        Uri.Builder uriBuilder =  new Uri.Builder()
                .scheme("https")
                .authority("accounts.spotify.com")
                .appendPath("authorize")
                .appendQueryParameter("client_id", spotify_clientID)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("redirect_uri", spotify_redirectURI)
                .appendQueryParameter("show_dialog", "true")
                .appendQueryParameter("scope", "playlist-read-private playlist-read-collaborative");

        Intent launchBrowser = new Intent("android.intent.action.VIEW", uriBuilder.build());
        fragment.getActivity().startActivity(launchBrowser);
    }

    public void setAccessCode(Uri uri){
        try {
            String code = uri.getQueryParameter("code");
            if (code == null){
                clear();
            } else {
                String url = "https://accounts.spotify.com/api/token";
                String[] headers = new String[]{
                        "Authorization", "Basic " + Base64.encodeToString((spotify_clientID + ":" + spotify_secret).getBytes(), Base64.NO_WRAP)};
                String[] data = new String[]{
                        "grant_type", "authorization_code",
                        "code", code,
                        "redirect_uri", spotify_redirectURI};

                JSONObject response = getJSON(postRequest(url, data, headers));
                if (response != null) {
                    if (response.has("access_token")) {
                        this.access_code = response.getString("access_token");
                        this.refresh_token = response.getString("refresh_token");
                        preferences.setSpotifyAccess(this.access_code);
                        preferences.setSpotifyRefresh(this.refresh_token);

                        url = "https://api.spotify.com/v1/me";
                        headers = new String[]{"Authorization", "Bearer " + access_code};
                        response = getJSON(getRequest(url, null, headers));
                        if (response != null && !response.has("error")) {
                            handler.getPreferences().setSpotifyAccount(response.getString("display_name"));
                        } else {
                            clear();
                        }
                    } else {
                        clear();
                    }
                }
            }
        } catch (Exception e) {
            Logger.log(Logger.API, e);
            clear();
        }

        if (fragment != null && !fragment.isDetached()){
            fragment.updateValues();
            fragment = null;
        }
    }

    public void clear(){
        preferences.clearSpotify();
        access_code = "";
        refresh_token = "";
    }

    private void refreshToken() {
        try {
            String url = "https://accounts.spotify.com/api/token";
            String[] headers = new String[]{
                    "Authorization", "Basic " + Base64.encodeToString((spotify_clientID + ":" + spotify_secret).getBytes(), Base64.NO_WRAP)};
            String[] data = new String[]{
                    "grant_type", "refresh_token",
                    "refresh_token", refresh_token};

            JSONObject response = getJSON(postRequest(url, data, headers));
            if (response != null) {
                if (response.has("access_token")) {
                    this.access_code = response.getString("access_token");
                    preferences.setSpotifyAccess(access_code);
                } else {
                    authenticateSpotify(null);
                }
            }
        } catch (Exception  e) {
            Logger.log(Logger.API, e);
            clear();
        }
    }

    private String getClientAccessCode() throws Exception {
        long currentTime = System.currentTimeMillis();
        if (clientAccessCode == null || lastRequestTime + 3600000 < currentTime) {
            String url = "https://accounts.spotify.com/api/token";
            String[] headers = new String[]{
                    "Authorization", "Basic " + Base64.encodeToString((spotify_clientID + ":" + spotify_secret).getBytes(), Base64.NO_WRAP)};
            String[] data = new String[]{
                    "grant_type", "client_credentials"};

            JSONObject response = getJSON(postRequest(url, data, headers));
            if (response != null && response.has("access_token")) {
                clientAccessCode = response.getString("access_token");
                lastRequestTime = currentTime;
            }
        }
        return clientAccessCode;
    }

    public MediaMetadata parseLink(String input){
        if (access_code.isEmpty()){clear(); return null;}
        if (!input.contains("http") && !input.contains(".spotify.com")){return null;}

        try {
            MediaMetadata metadata = new MediaMetadata();

            Pattern playlistPattern = Pattern.compile("/user/(.*)/playlist/(.*?)(\\?|$|\\s)");
            Matcher playlistMatcher = playlistPattern.matcher(input);
            if (playlistMatcher.find()) {
                String user = playlistMatcher.group(1);
                String playlist = playlistMatcher.group(2);
                metadata.set(MediaMetadata.Type.TYPE, "playlist");

                String url = "https://api.spotify.com/v1/users/" + user + "/playlists/" + playlist;
                String[] headers = new String[]{
                        "Authorization", "Bearer " + access_code};

                JSONObject response = getJSON(getRequest(url, null, headers));
                if (response != null) {
                    if (!refresh_token.isEmpty() && response.has("error")) {
                        refreshToken();
                        if (!access_code.isEmpty()) {
                            headers[1] = "Bearer " + access_code;
                            response = getJSON(getRequest(url, null, headers));
                        }
                        if (response == null || response.has("error")) {
                            clear();
                            return null;
                        }
                    }

                    metadata.set(MediaMetadata.Type.TITLE, response.getString("name"));
                    metadata.set(MediaMetadata.Type.SUMMARY,
                            "Made by: " + response.getJSONObject("owner").getString("id")
                                    + " (" + response.getJSONObject("tracks").getInt("total") + " Tracks)"
                                    + "\n" + response.getString("description"));
                    metadata.set(MediaMetadata.Type.STREAMING, response.getString("uri"));
                    metadata.set(MediaMetadata.Type.THUMBNAIL, response.getJSONArray("images").length() >= 2 ?
                            response.getJSONArray("images").getJSONObject(1).getString("url") :
                            response.getJSONArray("images").getJSONObject(0).getString("url"));
                    if (!StreamingApplication.SPOTIFY.isInstalled()){
                        metadata.set(MediaMetadata.Type.STREAMING, input);
                    }
                    return metadata;
                }

                metadata.set(MediaMetadata.Type.TITLE, "Playlist by " + user);
                metadata.set(MediaMetadata.Type.STREAMING, "spotify:user:" + user + ":playlist:" + playlist);
                if (!StreamingApplication.SPOTIFY.isInstalled()){
                    metadata.set(MediaMetadata.Type.STREAMING, input);
                }
                return metadata;
            }

            Pattern trackPattern = Pattern.compile("/track/(.*?)(\\?|$|\\s)");
            Matcher trackMatcher = trackPattern.matcher(input);
            if (trackMatcher.find()) {
                String id = trackMatcher.group(1);
                metadata.set(MediaMetadata.Type.TYPE, "track");

                String url = "https://api.spotify.com/v1/tracks/" + id;
                String[] headers = new String[]{
                        "Authorization", "Bearer " + access_code};

                JSONObject response = getJSON(getRequest(url, null, headers));
                if (response != null) {
                    if (!refresh_token.isEmpty() && response.has("error")) {
                        refreshToken();
                        if (!access_code.isEmpty()) {
                            headers[1] = "Bearer " + access_code;
                            response = getJSON(getRequest(url, null, headers));
                        }
                        if (response == null || response.has("error")) {
                            clear();
                            return null;
                        }
                    }

                    metadata.set(MediaMetadata.Type.TITLE, response.getJSONArray("artists").getJSONObject(0).getString("name") + " - " + response.getString("name"));
                    metadata.set(MediaMetadata.Type.SUMMARY, response.getJSONObject("album").getString("name"));
                    metadata.set(MediaMetadata.Type.STREAMING, response.getString("uri"));
                    metadata.set(MediaMetadata.Type.THUMBNAIL, response.getJSONObject("album").getJSONArray("images").length() >= 2 ?
                            response.getJSONObject("album").getJSONArray("images").getJSONObject(1).getString("url") :
                            response.getJSONObject("album").getJSONArray("images").getJSONObject(0).getString("url"));

                    if (!StreamingApplication.SPOTIFY.isInstalled()){
                        metadata.set(MediaMetadata.Type.STREAMING, input);
                    }
                    return metadata;
                }

                metadata.set(MediaMetadata.Type.TITLE, "Unknown Track");
                metadata.set(MediaMetadata.Type.STREAMING, "spotify:track:" + id);

                if (!StreamingApplication.SPOTIFY.isInstalled()){
                    metadata.set(MediaMetadata.Type.STREAMING, input);
                }
                return metadata;
            }

            Pattern albumPattern = Pattern.compile("/album/(.*?)(\\?|$|\\s)");
            Matcher albumMatcher = albumPattern.matcher(input);
            if (albumMatcher.find()) {
                String album = albumMatcher.group(1);
                metadata.set(MediaMetadata.Type.TYPE, "album");

                String url = "https://api.spotify.com/v1/albums/" + album;
                String[] headers = new String[]{
                        "Authorization", "Bearer " + access_code};

                JSONObject response = getJSON(getRequest(url, null, headers));
                if (response != null) {
                    if (!refresh_token.isEmpty() && response.has("error")) {
                        refreshToken();
                        if (!access_code.isEmpty()) {
                            headers[1] = "Bearer " + access_code;
                            response = getJSON(getRequest(url, null, headers));
                        }
                        if (response == null || response.has("error")) {
                            clear();
                            return null;
                        }
                    }

                    metadata.set(MediaMetadata.Type.TITLE, response.getJSONArray("artists").getJSONObject(0).getString("name") + " - " + response.getString("name"));
                    metadata.set(MediaMetadata.Type.STREAMING, response.getString("uri"));
                    metadata.set(MediaMetadata.Type.THUMBNAIL, response.getJSONArray("images").length() >= 2 ?
                            response.getJSONArray("images").getJSONObject(1).getString("url") :
                            response.getJSONArray("images").getJSONObject(0).getString("url"));

                    if (!StreamingApplication.SPOTIFY.isInstalled()) {
                        metadata.set(MediaMetadata.Type.STREAMING, input);
                    }
                    return metadata;
                }

                metadata.set(MediaMetadata.Type.TITLE, "Unknown Album");
                metadata.set(MediaMetadata.Type.STREAMING, "spotify:album:" + album);

                if (!StreamingApplication.SPOTIFY.isInstalled()) {
                    metadata.set(MediaMetadata.Type.STREAMING, input);
                }
                return metadata;
            }

        } catch (Exception e) {
            Logger.log(Logger.API, e);
        }

        return null;
    }

    public CaseInsensitiveMap<String, MediaMetadata> loadSpotifyPlaylists(){
        CaseInsensitiveMap<String, MediaMetadata> searchTitles = new CaseInsensitiveMap<>();
        if (access_code.isEmpty()){clear(); return null;}

        try {
            String url = "https://api.spotify.com/v1/me/playlists";
            String[] headers = new String[]{
                    "Authorization", "Bearer " + access_code};

            JSONObject response = getJSON(getRequest(url, null, headers));
            if (response != null) {
                if (!refresh_token.isEmpty() && response.has("error")){
                    refreshToken();
                    if (!access_code.isEmpty()){
                        headers[1] = "Bearer " + access_code;
                        response = getJSON(getRequest(url, null, headers));}
                    if (response == null || !response.has("items")){
                        clear();
                        return searchTitles;}}

                JSONArray responseArray;
                responseArray = response.getJSONArray("items");
                if (responseArray != null && responseArray.length() > 0) {
                    for (int i = 0; i < responseArray.length(); i++) {
                        MediaMetadata metadata = new MediaMetadata();
                        JSONObject media = responseArray.getJSONObject(i);
                        metadata.set(MediaMetadata.Type.TITLE, media.getString("name"));
                        metadata.set(MediaMetadata.Type.SUMMARY, "Made by: " + media.getJSONObject("owner").getString("id") + " (" + media.getJSONObject("tracks").getInt("total") + " Tracks)");
                        metadata.set(MediaMetadata.Type.STREAMING, media.getString("uri"));
                        metadata.set(MediaMetadata.Type.THUMBNAIL, media.getJSONArray("images").length() >= 2 ?
                                media.getJSONArray("images").getJSONObject(1).getString("url") :
                                media.getJSONArray("images").getJSONObject(0).getString("url"));
                        searchTitles.put(metadata.get(MediaMetadata.Type.TITLE), metadata);
                    }
                }
            }
        } catch (Exception e) {
            Logger.log(Logger.API, e);
        }
        return searchTitles;
    }

    public CaseInsensitiveMap<String, MediaMetadata> searchSpotify(String query, String type) {
        CaseInsensitiveMap<String, MediaMetadata> searchTitles = new CaseInsensitiveMap<>();
        try {
            String url = "https://api.spotify.com/v1/search";
            String[] data = new String[]{
                    "type", type,
                    "q", query};
            String[] headers = new String[]{
                    "Authorization", "Bearer " + getClientAccessCode()};

            JSONObject response = getJSON(getRequest(url, data, clientAccessCode == null ? null : headers));
            if (response != null) {
                JSONArray responseArray;
                switch (type.toLowerCase()){
                    case "track":
                        responseArray = response.getJSONObject("tracks").getJSONArray("items");
                        if (responseArray != null && responseArray.length() > 0) {
                            for (int i = 0; i < responseArray.length(); i++) {
                                MediaMetadata metadata = new MediaMetadata();
                                JSONObject media = responseArray.getJSONObject(i);
                                metadata.set(MediaMetadata.Type.TITLE, media.getJSONArray("artists").getJSONObject(0).getString("name") + " - " + media.getString("name"));
                                metadata.set(MediaMetadata.Type.SUMMARY, media.getJSONObject("album").getString("name"));
                                metadata.set(MediaMetadata.Type.STREAMING, media.getString("uri"));
                                metadata.set(MediaMetadata.Type.THUMBNAIL, media.getJSONObject("album").getJSONArray("images").length() >= 2 ?
                                        media.getJSONObject("album").getJSONArray("images").getJSONObject(1).getString("url") :
                                        media.getJSONObject("album").getJSONArray("images").getJSONObject(0).getString("url"));
                                searchTitles.put(metadata.get(MediaMetadata.Type.TITLE), metadata);
                            }
                        }
                        break;
                    case "playlist":
                        responseArray = response.getJSONObject("playlists").getJSONArray("items");
                        if (responseArray != null && responseArray.length() > 0) {
                            for (int i = 0; i < responseArray.length(); i++) {
                                MediaMetadata metadata = new MediaMetadata();
                                JSONObject media = responseArray.getJSONObject(i);
                                metadata.set(MediaMetadata.Type.TITLE, media.getString("name"));
                                metadata.set(MediaMetadata.Type.SUMMARY, "Made by: " + media.getJSONObject("owner").getString("id") + " (" +  media.getJSONObject("tracks").getInt("total") + " Tracks)");
                                metadata.set(MediaMetadata.Type.STREAMING, media.getString("uri"));
                                metadata.set(MediaMetadata.Type.THUMBNAIL, media.getJSONArray("images").length() >= 2 ?
                                        media.getJSONArray("images").getJSONObject(1).getString("url") :
                                        media.getJSONArray("images").getJSONObject(0).getString("url"));
                                searchTitles.put(metadata.get(MediaMetadata.Type.TITLE), metadata);
                            }
                        }
                        break;
                    case "album":
                        responseArray = response.getJSONObject("albums").getJSONArray("items");
                        if (responseArray != null && responseArray.length() > 0) {
                            for (int i = 0; i < responseArray.length(); i++) {
                                MediaMetadata metadata = new MediaMetadata();
                                JSONObject media = responseArray.getJSONObject(i);
                                if (media.getString("album_type").equalsIgnoreCase("album")) {
                                    metadata.set(MediaMetadata.Type.TITLE, media.getJSONArray("artists").getJSONObject(0).getString("name") + " - " + media.getString("name"));
                                    metadata.set(MediaMetadata.Type.STREAMING, media.getString("uri"));
                                    metadata.set(MediaMetadata.Type.THUMBNAIL, media.getJSONArray("images").length() >= 2 ?
                                            media.getJSONArray("images").getJSONObject(1).getString("url") :
                                            media.getJSONArray("images").getJSONObject(0).getString("url"));
                                    searchTitles.put(metadata.get(MediaMetadata.Type.TITLE), metadata);
                                }
                            }
                        }
                        break;
                }
            }
        } catch (Exception e) {
            searchTitles.clear();
            Logger.log(Logger.API, e);
        }

        if (searchTitles.isEmpty()) searchTitles.put("", null);
        return searchTitles;
    }
}
