package com.drageniix.raspberrypop.utilities.api;

import com.drageniix.raspberrypop.media.MediaMetadata;
import com.drageniix.raspberrypop.utilities.Logger;

import org.json.JSONArray;
import org.json.JSONObject;


import com.drageniix.raspberrypop.utilities.custom.CaseInsensitiveMap;

public class OMDbAPI extends APIBase {
    private static String OMDbBase = "http://www.omdbapi.com/";
    private String omdb_key;

    OMDbAPI(String key){this.omdb_key = key;}

    public CaseInsensitiveMap<String, MediaMetadata> searchOMDb(String query) {
        CaseInsensitiveMap<String, MediaMetadata> searchTitles = new CaseInsensitiveMap<>();
        try {
            String[] data = new String[]{
                    "s", query,
                    "r", "json",
                    "apikey", omdb_key
            };

            JSONObject response = getJSON(getRequest(OMDbBase, data, null));
            if (response != null) {
                JSONArray responseArray = response.getJSONArray("Search");
                if (responseArray != null && responseArray.length() > 0) {
                    for (int i = 0; i < responseArray.length(); i++) {
                        JSONObject media = responseArray.getJSONObject(i);
                        if (!media.getString("Type").equalsIgnoreCase("game")){
                            String year = media.getString("Year");
                            if (year.isEmpty()) year = " (????)";
                            else year = " (" + year + ")";
                            searchTitles.put(media.getString("Title") + year, new MediaMetadata()
                                    .set(MediaMetadata.Type.IMDB, media.getString("imdbID")));
                        }
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

    public MediaMetadata getOMDb(String query, String type) {
        try {
            String[] data = new String[]{
                    type, query,
                    "plot", "full",
                    "r", "json",
                    "apikey", omdb_key
            };

            JSONObject response = getJSON(getRequest(OMDbBase, data, null));
            if (response != null && response.getString("Response").equalsIgnoreCase("True")) {
                MediaMetadata metadata = new MediaMetadata();
                metadata.set(MediaMetadata.Type.TITLE, response.getString("Title"));
                metadata.set(MediaMetadata.Type.DETAIL, response.getString("Year"));
                metadata.set(MediaMetadata.Type.SUMMARY, metadata.get(MediaMetadata.Type.SUMMARY) + "Rated: " + response.getString("Rated"));
                metadata.set(MediaMetadata.Type.SUMMARY, metadata.get(MediaMetadata.Type.SUMMARY) + "\nGenre: " + response.getString("Genre"));
                metadata.set(MediaMetadata.Type.SUMMARY, metadata.get(MediaMetadata.Type.SUMMARY) + "\nDirector(s): " + response.getString("Director"));
                metadata.set(MediaMetadata.Type.SUMMARY, metadata.get(MediaMetadata.Type.SUMMARY) + "\nProduction Studio: " + response.getString("Production"));
                metadata.set(MediaMetadata.Type.SUMMARY, metadata.get(MediaMetadata.Type.SUMMARY) + "\nActors: " + response.getString("Actors"));
                metadata.set(MediaMetadata.Type.SUMMARY, metadata.get(MediaMetadata.Type.SUMMARY) + "\n\nPlot: " + response.getString("Plot"));
                metadata.set(MediaMetadata.Type.THUMBNAIL, response.getString("Poster"));
                metadata.set(MediaMetadata.Type.IMDB, response.getString("imdbID"));
                metadata.set(MediaMetadata.Type.TYPE, response.getString("Type"));
                return metadata;
            }
        } catch (Exception e) {
            Logger.log(Logger.API, e);
        }
        return null;
    }
}
