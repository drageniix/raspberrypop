package com.drageniix.raspberrypop.utilities.api;


import android.content.Intent;
import android.net.Uri;

import com.drageniix.raspberrypop.fragments.SettingsFragment;
import com.drageniix.raspberrypop.media.MediaMetadata;
import com.drageniix.raspberrypop.utilities.Logger;
import com.drageniix.raspberrypop.utilities.custom.CaseInsensitiveMap;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class GoogleAPI extends APIBase {

    private SettingsFragment fragment;
    private String key, client_id;

    GoogleAPI(List<String> data){
        this.key = data.get(0);
        this.client_id = data.get(1);
    }

    public CaseInsensitiveMap<String, MediaMetadata> getBook(String query){
        CaseInsensitiveMap<String, MediaMetadata> searchTitles = new CaseInsensitiveMap<>();
        String url = "https://www.googleapis.com/books/v1/volumes";
        String[] data = new String[]{
                "q", handler.getFileHelper().normalizeTitle(query),
                "key", key};

        try {
            JSONObject response = getJSON(getRequest(url, data, null));
            if (response != null && response.has("items")) {
                JSONArray responseArray = response.getJSONArray("items");
                if (responseArray != null && responseArray.length() > 0) {
                    for (int i = 0; i < responseArray.length(); i++) {
                        MediaMetadata metadata = new MediaMetadata();
                        JSONObject volume = responseArray.getJSONObject(i).getJSONObject("volumeInfo");
                        if (volume.has("title"))
                            metadata.set(MediaMetadata.Type.TITLE, volume.getString("title"));
                        if (volume.has("authors"))
                            metadata.set(MediaMetadata.Type.DETAIL, volume.getJSONArray("authors").join(", ").replace("\"", ""));
                        if (volume.has("imageLinks"))
                            metadata.set(MediaMetadata.Type.THUMBNAIL, volume.getJSONObject("imageLinks").getString("thumbnail"));
                        if (volume.has("publishedDate"))
                            metadata.set(MediaMetadata.Type.SUMMARY, metadata.get(MediaMetadata.Type.SUMMARY) + "\nPublished: " + volume.getString("publishedDate"));
                            if (volume.has("publisher")) {
                                metadata.set(MediaMetadata.Type.SUMMARY, metadata.get(MediaMetadata.Type.SUMMARY) + " (" + volume.getString("publisher") + ")");}
                        if (volume.has("pageCount"))
                            metadata.set(MediaMetadata.Type.SUMMARY, metadata.get(MediaMetadata.Type.SUMMARY) + "\nPage Count: " + volume.getString("pageCount"));
                        if (volume.has("description"))
                            metadata.set(MediaMetadata.Type.SUMMARY, metadata.get(MediaMetadata.Type.SUMMARY) + "\n\n" + volume.getString("description"));


                        searchTitles.put(metadata.get(MediaMetadata.Type.TITLE) + (!metadata.get(MediaMetadata.Type.DETAIL).isEmpty() ? " (" + metadata.get(MediaMetadata.Type.DETAIL) +  ")" : ""), metadata);
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

    public void setAccessCode(Uri uri){
        String url = "https://www.googleapis.com/oauth2/v4/token";
        String[] data = new String[]{
                "code", uri.getQueryParameter("code"),
                "grant_type", "authorization_code",
                "redirect_uri", "drageniix.auth://callback.google",
                "client_id", client_id
        };

        JSONObject response = getJSON(postRequest(url, data, null));
        //Logger.test(uri, response); todo:

        if (fragment != null && !fragment.isDetached()){
            fragment.updateValues();
            fragment = null;
        }
    }

    public void authenticateGoogle(SettingsFragment fragment){
        this.fragment = fragment;
        Uri.Builder uriBuilder =  new Uri.Builder()
                .scheme("https")
                .authority("accounts.google.com").appendPath("o").appendPath("oauth2").appendPath("v2").appendPath("auth")
                .encodedQuery("redirect_uri=drageniix.auth:/callback.google")
                .appendQueryParameter("client_id", client_id)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("scope", "https://www.googleapis.com/auth/youtube.readonly");

        Intent launchBrowser = new Intent("android.intent.action.VIEW", uriBuilder.build());
        fragment.getActivity().startActivity(launchBrowser);
    }

    public String getAPIKey(){return key;}
}
