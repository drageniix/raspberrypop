package com.drageniix.raspberrypop.utilities.api;

import android.content.Context;
import android.util.Base64;
import android.widget.Toast;

import com.drageniix.raspberrypop.media.MediaMetadata;
import com.drageniix.raspberrypop.servers.ServerBase;
import com.drageniix.raspberrypop.servers.plex_servers.PlexServer;
import com.drageniix.raspberrypop.utilities.FileHelper;
import com.drageniix.raspberrypop.utilities.Logger;

import com.drageniix.raspberrypop.utilities.custom.CaseInsensitiveMap;

import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Request;
import okhttp3.Response;

public class PlexAPI extends APIBase{
    private String prefix, url;
    private String[] data;

    public CaseInsensitiveMap<String, MediaMetadata> search(PlexServer server, String query, boolean includeServer){
        CaseInsensitiveMap<String, MediaMetadata> searchTitles = new CaseInsensitiveMap<>();

        if (server.isOnline()) {
            prefix = getUrl(server);

            //Videos
            data = new String[]{
                    "X-Plex-Token", server.getKey(),
                    "query", query};
            url = "/search";

            try {
                try (StringReader inStandard = getStringReader(getRequest(prefix + url, data, null))) {
                    if (inStandard != null) {
                        parser.setInput(inStandard);
                        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                            MediaMetadata metadata = new MediaMetadata();
                            if (event == XmlPullParser.START_TAG && parser.getName().equals("Video")) {
                                metadata.set(MediaMetadata.Type.DETAIL, parser.getAttributeValue(null, "year"));
                                metadata.set(MediaMetadata.Type.SUMMARY, parser.getAttributeValue(null, "summary"));
                                if (parser.getAttributeValue(null, "parentThumb") == null) {
                                    metadata.set(MediaMetadata.Type.THUMBNAIL, prefix + parser.getAttributeValue(null, "thumb"));
                                } else {
                                    metadata.set(MediaMetadata.Type.THUMBNAIL, prefix + parser.getAttributeValue(null, "parentThumb"));
                                }
                                metadata.set(MediaMetadata.Type.THUMBNAIL, metadata.get(MediaMetadata.Type.THUMBNAIL) + "?" + data[0] + "=" + data[1]);

                                String type = parser.getAttributeValue(null, "type");
                                metadata.set(MediaMetadata.Type.TYPE, type + "|plex");
                                switch (type) {
                                    case "movie":
                                    case "show":
                                        metadata.set(MediaMetadata.Type.TITLE, parser.getAttributeValue(null, "title"));
                                        break;
                                    case "episode":
                                        String showName, seasonName, title;
                                        title = "E" + String.format(Locale.getDefault(), "%02d", Integer.parseInt(parser.getAttributeValue(null, "index"))) + ": " + parser.getAttributeValue(null, "title");
                                        seasonName = " S" + String.format(Locale.getDefault(), "%02d", Integer.parseInt(parser.getAttributeValue(null, "parentIndex")));
                                        showName = parser.getAttributeValue(null, "grandparentTitle");
                                        metadata.set(MediaMetadata.Type.TITLE, showName + " - " + seasonName + title);
                                        break;
                                }

                                if (!metadata.get(MediaMetadata.Type.TITLE).isEmpty()) {
                                    metadata.set(MediaMetadata.Type.SUMMARY, server.getName() + "\n\n" + metadata.get(MediaMetadata.Type.SUMMARY));
                                    metadata.set(MediaMetadata.Type.STREAMING, parser.getAttributeValue(null, "ratingKey"));
                                    metadata.set(MediaMetadata.Type.SERVER_ID, server.getServerID());
                                    searchTitles.put(includeServer ? metadata.get(MediaMetadata.Type.TITLE) + " (" + server.getName() + ")" : metadata.get(MediaMetadata.Type.TITLE), metadata);
                                }
                            }
                        }
                    }
                }

                //Audio Tracks
                data = new String[]{
                        "X-Plex-Token", server.getKey(),
                        "query", query,
                        "type", "10"
                };

                try (StringReader inAudio = getStringReader(getRequest(prefix + url, data, null))) {
                    if (inAudio != null) {
                        parser.setInput(inAudio);
                        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                            MediaMetadata metadata = new MediaMetadata();
                            if (event == XmlPullParser.START_TAG && parser.getName().equals("Track")) {
                                metadata.set(MediaMetadata.Type.DETAIL, parser.getAttributeValue(null, "parentTitle"));
                                metadata.set(MediaMetadata.Type.SUMMARY, parser.getAttributeValue(null, "summary"));
                                if (parser.getAttributeValue(null, "grandparentThumb") != null) {
                                    metadata.set(MediaMetadata.Type.THUMBNAIL, prefix + parser.getAttributeValue(null, "grandparentThumb"));
                                    metadata.set(MediaMetadata.Type.THUMBNAIL, metadata.get(MediaMetadata.Type.THUMBNAIL) + "?" + data[0] + "=" + data[1]);
                                }
                                metadata.set(MediaMetadata.Type.TYPE, parser.getAttributeValue(null, "type") + "|plex");
                                metadata.set(MediaMetadata.Type.TITLE, parser.getAttributeValue(null, "grandparentTitle") + " - " +
                                        parser.getAttributeValue(null, "title"));

                                if (!metadata.get(MediaMetadata.Type.TITLE).isEmpty()) {
                                    metadata.set(MediaMetadata.Type.SUMMARY, server.getName() + "\n\n" + metadata.get(MediaMetadata.Type.SUMMARY));
                                    metadata.set(MediaMetadata.Type.STREAMING, parser.getAttributeValue(null, "ratingKey"));
                                    metadata.set(MediaMetadata.Type.SERVER_ID, server.getServerID());
                                    searchTitles.put(includeServer ? metadata.get(MediaMetadata.Type.TITLE) + " (" + server.getName() + ")" : metadata.get(MediaMetadata.Type.TITLE), metadata);
                                }
                            }
                        }
                    }
                }

                //Playlists
                url = "/playlists/all";
                data = new String[]{
                        "X-Plex-Token", server.getKey()};

                try(StringReader inPlaylist = getStringReader(getRequest(prefix + url, data, null))) {
                    if (inPlaylist != null) {
                        parser.setInput(inPlaylist);
                        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                            MediaMetadata metadata = new MediaMetadata();
                            if (event == XmlPullParser.START_TAG && parser.getName().equals("Playlist")) {
                                metadata.set(MediaMetadata.Type.TITLE, parser.getAttributeValue(null, "title"));
                                if (!metadata.get(MediaMetadata.Type.TITLE).isEmpty() && FileHelper.containsIgnoreCase(metadata.get(MediaMetadata.Type.TITLE), false, query)) {
                                    metadata.set(MediaMetadata.Type.SUMMARY, server.getName());
                                    metadata.set(MediaMetadata.Type.STREAMING, parser.getAttributeValue(null, "ratingKey"));
                                    metadata.set(MediaMetadata.Type.SERVER_ID, server.getServerID());
                                    searchTitles.put(includeServer ? metadata.get(MediaMetadata.Type.TITLE) + " (" + server.getName() + ")" : metadata.get(MediaMetadata.Type.TITLE), metadata);
                                }
                            }
                        }
                    }
                }

                //Photos
                url = "/library/sections";

                List<String> photoDirectories = new LinkedList<>();
                try(StringReader inPhotoDirectory = getStringReader(getRequest(prefix + url, data, null))) {
                    if (inPhotoDirectory != null) {
                        parser.setInput(inPhotoDirectory);
                        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                            if (event == XmlPullParser.START_TAG && parser.getName().equals("Directory") && parser.getAttributeValue(null, "type").equals("photo")) {
                                photoDirectories.add(parser.getAttributeValue(null, "key"));
                            }
                        }
                    }
                }

                for (String photo : photoDirectories) {
                    url = "/library/sections/" + photo + "/all";
                    try(StringReader inPhoto = getStringReader(getRequest(prefix + url, data, null))) {
                        if (inPhoto != null) {
                            parser.setInput(inPhoto);
                            while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                                MediaMetadata metadata = new MediaMetadata();
                                if (event == XmlPullParser.START_TAG && parser.getName().equals("Photo")) {
                                    metadata.set(MediaMetadata.Type.DETAIL, parser.getAttributeValue(null, "originallyAvailableAt"));
                                    metadata.set(MediaMetadata.Type.SUMMARY, parser.getAttributeValue(null, "summary"));
                                    metadata.set(MediaMetadata.Type.THUMBNAIL, prefix + parser.getAttributeValue(null, "thumb"));
                                    metadata.set(MediaMetadata.Type.THUMBNAIL, metadata.get(MediaMetadata.Type.THUMBNAIL) + "?" + data[0] + "=" + data[1]);
                                    metadata.set(MediaMetadata.Type.TYPE, parser.getAttributeValue(null, "type") + "|plex");
                                    metadata.set(MediaMetadata.Type.TITLE, parser.getAttributeValue(null, "title") + " (" + parser.getAttributeValue(null, "originallyAvailableAt") + ")");
                                    if (!metadata.get(MediaMetadata.Type.TITLE).isEmpty() && FileHelper.containsIgnoreCase(metadata.get(MediaMetadata.Type.TITLE), false, query)) {
                                        metadata.set(MediaMetadata.Type.SUMMARY, server.getName() + "\n\n" + metadata.get(MediaMetadata.Type.SUMMARY));
                                        metadata.set(MediaMetadata.Type.STREAMING, parser.getAttributeValue(null, "ratingKey"));
                                        metadata.set(MediaMetadata.Type.SERVER_ID, server.getServerID());
                                        searchTitles.put(includeServer ? metadata.get(MediaMetadata.Type.TITLE) + " (" + server.getName() + ")" : metadata.get(MediaMetadata.Type.TITLE), metadata);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Logger.log(Logger.API, e);
            }
        }

        if (searchTitles.isEmpty()) searchTitles.put("", null);
        return searchTitles;
    }

    public boolean getInfo(PlexServer server) throws Exception{
        boolean found = false;
        if (pingServer(server)) {
            prefix = getUrl(server);
            data = new String[]{"X-Plex-Token", server.getKey()};
            url = "/servers";

            try (StringReader in = getStringReader(getRequest(prefix + url, data, null))) {
                if (in != null) {
                    parser.setInput(in);
                    while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                        if (event == XmlPullParser.START_TAG && parser.getName().equals("Server")
                                && parser.getAttributeValue(null, "address").equals(server.getServerHost())) {
                            server.setName(parser.getAttributeValue(null, "name"));
                            server.setServerID(parser.getAttributeValue(null, "machineIdentifier"));
                            found = true;
                            break;
                        }
                    }
                }
            }
        }
        return found;
    }

    public Map<String, String> getPotentialClients(PlexServer server){
        prefix = getUrl(server);
        data = new String[]{"X-Plex-Token", server.getKey()};
        url = "/clients";

        Map<String, String> clients = new CaseInsensitiveMap<>();
        try(StringReader in = getStringReader(getRequest(prefix + url, data, null))) {
            if (in != null) {
                parser.setInput(in);
                while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG && parser.getName().equals("Server")) {
                        clients.put(
                                parser.getAttributeValue(null, "address"),
                                parser.getAttributeValue(null, "name"));
                    }
                }
            }
        } catch (Exception e) {
            Logger.log(Logger.API, e);
        }

        if (preferences.hasRoku()) {
            clients.put(
                    preferences.getRokuIp(),
                    preferences.getRokuName());
        }

        return clients;
    }

    public List<String[]> getAvailableClients(ServerBase server){
        if (!server.getClient().isEmpty() &&
                !preferences.hasRoku()
                    && server.getClient().equals(preferences.getRokuIp())){
            RokuAPI.launchApp(preferences.getRokuIp(), "", "13535");
        }

        prefix = getUrl(server);
        data = new String[]{"X-Plex-Token", server.getKey()};
        url = "/clients";

        List<String[]> clients = new ArrayList<>();
        try(StringReader in = getStringReader(getRequest(prefix + url, data, null))) {
            if (in != null) {
                parser.setInput(in);
                while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG && parser.getName().equals("Server")) {
                        String[] client = new String[]{
                                "http://" + parser.getAttributeValue(null, "address") + ":" + parser.getAttributeValue(null, "port") + "/player/playback/playMedia",
                                parser.getAttributeValue(null, "machineIdentifier"),
                                parser.getAttributeValue(null, "address")};
                        if (!server.getClient().isEmpty() && client[2].equals(server.getClient())){
                            clients.add(0, client);
                        } else clients.add(client);
                    }
                }
            }
        } catch (Exception e) {
            Logger.log(Logger.API, e);
        }
        return clients;
    }

    public static String getTVToken(Context context, String email, String password, boolean saveKey){
        String token = null;
        password = Base64.encodeToString((email + ":" + password).getBytes(), Base64.NO_WRAP);

        if (saveKey) {
            handler.getPreferences().setPlexAccount(email);
            handler.getPreferences().setPlexPassword(password);
        }

        String url = "https://plex.tv/users/sign_in.json";
        String[] headers = new String[]{
                "Authorization", "Basic " + password,
                "X-Plex-Client-Identifier", preferences.getUUID(),
                "X-Plex-Product", "Drageniix's RaspberryPOP",
                "X-Plex-Version", "1.0.0",
                "X-Plex-Device-Name","RaspberryPOP"
        };

        try {
            Request request = postRequest(url, null, headers);
            JSONObject response = getJSON(request);
            if (response != null) {
                if (response.has("user")) {
                    token = response.getJSONObject("user").getString("authentication_token");
                    if (saveKey) {
                        handler.getPreferences().setPlexToken(token);
                        Toast.makeText(context, "Login Successful!", Toast.LENGTH_LONG).show();
                    }
                } else if (saveKey){
                    handler.getPreferences().clearPlex();
                    Toast.makeText(context, "Login Failed. Check your credentials.", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e){
            Logger.log(Logger.API, e);
        }
        return token;
    }

    public boolean attemptConnection(ServerBase server, String url, String[] data){
        try{
            StringBuilder builder = new StringBuilder(url);
            builder.append("?");
            for(int i = 0; i < data.length; i++){
                if (i % 2 != 0) {
                    builder.append(URLEncoder.encode(data[i], "UTF-8"));
                    builder.append("&");
                } else {
                    builder.append(URLEncoder.encode(data[i], "UTF-8"));
                    builder.append("=");
                }
            }
            builder.deleteCharAt(builder.length()-1);
            url = builder.toString();

            Response response = client.newCall(getRequest(url, null, null)).execute();
            if (response.code() == 401 && handler.getPreferences().hasPlex()){
                String deviceUrl = "https://plex.tv/devices.xml";
                String[] headers  = new String[]{
                        "Authorization", "Basic " + handler.getPreferences().getPlexPassword()};

                try(StringReader in = getStringReader(getRequest(deviceUrl, null, headers))) {
                    if (in != null) {
                        parser.setInput(in);
                        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                            if (event == XmlPullParser.START_TAG && parser.getName().equals("Device")) {
                                if (parser.getAttributeValue(null, "clientIdentifier").equals(server.getServerID())){
                                    data[data.length - 1] = parser.getAttributeValue(null, "token");
                                    server.setKey(data[data.length-1]);
                                    handler.addOrUpdateServer(server);
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Logger.log(Logger.API, e);
                }
                response = client.newCall(getRequest(url, data, null)).execute();
            }
            return (response.code() == 200);
        } catch (Exception e){
            return false;
        }
    }
}