package com.drageniix.raspberrypop.utilities.api;

import com.drageniix.raspberrypop.media.MediaMetadata;
import com.drageniix.raspberrypop.utilities.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import com.drageniix.raspberrypop.utilities.custom.CaseInsensitiveMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.drageniix.raspberrypop.utilities.categories.StreamingApplication.YOUTUBE;

public class YoutubeAPI extends APIBase {

    private static final String youtube_baseURL = "https://www.googleapis.com/youtube/v3/";
    private String youtube_apiKey;

    YoutubeAPI(String key){
        this.youtube_apiKey = key;
    }

    public CaseInsensitiveMap<String, MediaMetadata> searchYoutube(String query, String type) {
        CaseInsensitiveMap<String, MediaMetadata> searchTitles = new CaseInsensitiveMap<>();
        try {
            String url = youtube_baseURL + "search";
            String[] data;
            if (type.equals("movie") || type.equals("show")){
                String videoType = type.equals("show") ? "episode" : "movie";
                type = "video";
                data = new String[]{
                        "key", youtube_apiKey,
                        "part", "snippet",
                        "type", type,
                        "videoType", videoType,
                        "q", query
                };
            } else {
                data = new String[]{
                        "key", youtube_apiKey,
                        "part", "snippet",
                        "type", type,
                        "q", query
                };
            }

            JSONObject response = getJSON(getRequest(url, data, null));
            if (response != null) {
                JSONArray responseArray = response.getJSONArray("items");
                if (responseArray != null && responseArray.length() > 0) {
                    for (int i = 0; i < responseArray.length(); i++) {
                        MediaMetadata metadata = new MediaMetadata();
                        JSONObject media = responseArray.getJSONObject(i);
                        JSONObject snippet = media.getJSONObject("snippet");
                        String title = snippet.getString("title") + (type.equals("channel") ? "" : " - " + snippet.getString("channelTitle"));
                        metadata.set(MediaMetadata.Type.TYPE, type);
                        if (type.equals("channel") && media.getJSONObject("id").getString("kind").endsWith("playlist")){
                            metadata.set(MediaMetadata.Type.TYPE, "playlist");
                            metadata.set(MediaMetadata.Type.ALTERNATE, media.getJSONObject("id").getString("playlistId"));
                        } else {
                            metadata.set(MediaMetadata.Type.ALTERNATE, media.getJSONObject("id").getString(type + "Id"));
                        }
                        metadata.set(MediaMetadata.Type.TITLE, snippet.getString("title"));
                        metadata.set(MediaMetadata.Type.DETAIL, type.equals("channel") ? "" : snippet.getString("channelTitle"));
                        metadata.set(MediaMetadata.Type.SUMMARY, snippet.getString("publishedAt").substring(0, snippet.getString("publishedAt").indexOf("-")));
                        String description = snippet.getString("description");
                        if (!description.isEmpty())
                            metadata.set(MediaMetadata.Type.SUMMARY, metadata.get(MediaMetadata.Type.SUMMARY) + " - " + snippet.getString("description"));
                        metadata.set(MediaMetadata.Type.THUMBNAIL, snippet.getJSONObject("thumbnails").getJSONObject("medium").getString("url"));

                        switch (metadata.get(MediaMetadata.Type.TYPE)){
                            case "channel":
                                metadata.set(MediaMetadata.Type.STREAMING, "https://www.youtube.com/playlist?list=" + getChannelUploads(metadata.get(MediaMetadata.Type.ALTERNATE)));
                                break;
                            case "playlist":
                                metadata.set(MediaMetadata.Type.STREAMING, "https://www.youtube.com/playlist?list=" + metadata.get(MediaMetadata.Type.ALTERNATE));
                                break;
                            case "video":
                                if (YOUTUBE.isInstalled()) {
                                    metadata.set(MediaMetadata.Type.STREAMING, "vnd.youtube:" + metadata.get(MediaMetadata.Type.ALTERNATE));
                                } else {
                                    metadata.set(MediaMetadata.Type.STREAMING, "http://www.youtube.com/watch?v=" + metadata.get(MediaMetadata.Type.ALTERNATE));
                                    break;
                                }
                        }

                        searchTitles.put(title, metadata);
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

    private String getChannelUploads(String query) {
        String id = "";
        try {
            String url = youtube_baseURL + "channels";
            String[] data = new String[]{
                    "key", youtube_apiKey,
                    "part", "contentDetails",
                    "id", query
            };

            JSONObject response = getJSON(getRequest(url, data, null));

            if (response != null) {
                JSONArray responseArray = response.getJSONArray("items");
                if (responseArray != null && responseArray.length() > 0) {
                    JSONObject media = responseArray.getJSONObject(0);
                    id = media.getJSONObject("contentDetails").getJSONObject("relatedPlaylists").getString("uploads");
                }
            }
        } catch (Exception e) {
            Logger.log(Logger.API, e);
        }

        return id;
    }

    public MediaMetadata parseLink(String input){
        if (input.contains("youtu.be") || input.contains("youtube.com") ) {
            MediaMetadata metadata = null;
            if (input.contains("list=")) {
                Pattern pattern = Pattern.compile("list=(.*?)(\\?|$|\\s)");
                Matcher matcher = pattern.matcher(input);
                if (matcher.find()) {
                    String id = matcher.group(1);
                    metadata = searchYoutube(id, "playlist").entrySet().iterator().next().getValue();
                }
            } else if (input.contains("v=")) {
                Pattern pattern = Pattern.compile("v=(.*?)(\\?|$|\\s)");
                Matcher matcher = pattern.matcher(input);
                if (matcher.find()) {
                    String id = matcher.group(1);
                    metadata = searchYoutube(id, "video").entrySet().iterator().next().getValue();
                }
            } else if (input.contains("/v/")) {
                Pattern pattern = Pattern.compile("/v/(.*?)(\\?|$|\\s)");
                Matcher matcher = pattern.matcher(input);
                if (matcher.find()) {
                    String id = matcher.group(1);
                    metadata = searchYoutube(id, "video").entrySet().iterator().next().getValue();
                }
            } else {
                Pattern pattern = Pattern.compile("/([^/]*?)(\\?|$|\\s)");
                Matcher matcher = pattern.matcher(input);
                if (matcher.find()) {
                    String id = matcher.group(1);
                    metadata = searchYoutube(id, "video").entrySet().iterator().next().getValue();
                }
            }
            return metadata;
        }
        return null;
    }
}
