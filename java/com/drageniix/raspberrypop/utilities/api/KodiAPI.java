package com.drageniix.raspberrypop.utilities.api;

import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.media.MediaMetadata;
import com.drageniix.raspberrypop.servers.ServerBase;
import com.drageniix.raspberrypop.utilities.FileHelper;
import com.drageniix.raspberrypop.utilities.Logger;
import com.drageniix.raspberrypop.utilities.custom.CaseInsensitiveMap;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;

public class KodiAPI extends APIBase {
    private JSONObject schema;

    KodiAPI(JSONObject schema){
        this.schema = schema;
    }

    public void playContent(ServerBase server, Media media){
        try {
            getJSON(getRequest(
                    getBaseUrl(server),
                    getData(AuxiliaryType.Play,
                            LibraryType.getType(media.getType()).getIdName(),
                            media.getStreamingID()),
                    getHeaders(server)));


        } catch (Exception e){
            Logger.log(Logger.API, e);
        }
    }

    public boolean getInfo(ServerBase server){
        if (!pingServer(server)) return false;

        try {
            JSONObject response = getJSON(getRequest(
                    getBaseUrl(server),
                    getData(AuxiliaryType.Application),
                    getHeaders(server)));

            if (response != null) {
                response = response.getJSONObject("result");
                server.setName(response.getString("System.FriendlyName"));
                return true;
            }
        } catch (Exception e){
            Logger.log(Logger.API, e);
        }
        return false;
    }

    public CaseInsensitiveMap<String, MediaMetadata> searchContent(ServerBase server, String typeString, String query, boolean includeServer) {
        CaseInsensitiveMap<String, MediaMetadata> searchTitles = new CaseInsensitiveMap<>();

        if (server.isOnline()) {
            LibraryType type = LibraryType.getType(typeString);

            try {
                JSONObject response = getJSON(getRequest(
                        getBaseUrl(server),
                        getData(type, query),
                        getHeaders(server)));

                if (response != null) {
                    JSONArray responseArray = response.getJSONObject("result").getJSONArray(type.getListName());
                    if (responseArray != null && responseArray.length() > 0) {
                        for (int i = 0; i < responseArray.length(); i++) {
                            JSONObject media = responseArray.getJSONObject(i);
                            String title = null;
                            MediaMetadata metadata = new MediaMetadata();
                            metadata.set(MediaMetadata.Type.STREAMING, media.getString("file"));
                            metadata.set(MediaMetadata.Type.SERVER_ID, server.getServerID());
                            if (type == LibraryType.Movies || type == LibraryType.Shows) {
                                metadata.set(MediaMetadata.Type.TITLE,  media.getString("title"));
                                metadata.set(MediaMetadata.Type.SUMMARY,  media.getString("plot"));
                                metadata.set(MediaMetadata.Type.THUMBNAIL,  getThumbnailUrl(server, media.getString("thumbnail")));
                                metadata.set(MediaMetadata.Type.DETAIL,  String.valueOf(media.getInt("year")));
                                String year = metadata.get(MediaMetadata.Type.DETAIL);
                                if (year.isEmpty()) year = " (????)";
                                else year = " (" + year + ")";
                                title = metadata.get(MediaMetadata.Type.TITLE) + year;
                            } else if (type == LibraryType.Songs || type == LibraryType.MusicVideos) {
                                metadata.set(MediaMetadata.Type.TITLE,  media.getString("title"));
                                metadata.set(MediaMetadata.Type.SUMMARY,  media.getString("album"));
                                metadata.set(MediaMetadata.Type.THUMBNAIL,  getThumbnailUrl(server, media.getString("thumbnail")));
                                metadata.set(MediaMetadata.Type.DETAIL,  media.getJSONArray("artist").join(", ").replace("\"", ""));
                                title = metadata.get(MediaMetadata.Type.TITLE) + " (" + metadata.get(MediaMetadata.Type.DETAIL) + ")";
                            } else if (type == LibraryType.Photographs) {
                                metadata.set(MediaMetadata.Type.TITLE,  media.getString("label"));
                                if (FileHelper.containsIgnoreCase(metadata.get(MediaMetadata.Type.TITLE), false, query)
                                        || FileHelper.containsIgnoreCase(metadata.get(MediaMetadata.Type.STREAMING), false, query)) {
                                    metadata.set(MediaMetadata.Type.DETAIL,  "");
                                    metadata.set(MediaMetadata.Type.SUMMARY,  "");
                                    metadata.set(MediaMetadata.Type.THUMBNAIL,  "");
                                }
                                title = metadata.get(MediaMetadata.Type.TITLE);
                                recursiveDirectorySearch(server, searchTitles, metadata.get(MediaMetadata.Type.STREAMING), query, includeServer);
                            } else if (type == LibraryType.Recordings) {
                                metadata.set(MediaMetadata.Type.TITLE,  media.getString("title"));
                                metadata.set(MediaMetadata.Type.DETAIL,  media.getString("channel"));
                                if (FileHelper.containsIgnoreCase(metadata.get(MediaMetadata.Type.TITLE), false, query)
                                        || FileHelper.containsIgnoreCase(metadata.get(MediaMetadata.Type.DETAIL), false, query)) {
                                    metadata.set(MediaMetadata.Type.SUMMARY,  media.getString("plot"));
                                    metadata.set(MediaMetadata.Type.THUMBNAIL,  getThumbnailUrl(server, media.getString("icon")));
                                    title = metadata.get(MediaMetadata.Type.TITLE) + " (" + metadata.get(MediaMetadata.Type.DETAIL) + ")";
                                }
                            }

                            if (title != null) {
                                metadata.set(MediaMetadata.Type.DETAIL, server.getName() + "\n\n" + metadata.get(MediaMetadata.Type.DETAIL));
                                if (includeServer) title += " (" + server.getName() + ")";
                                searchTitles.put(title, metadata);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                searchTitles.clear();
                Logger.log(Logger.API, e);
            }
        }

        if (searchTitles.isEmpty()) searchTitles.put("", null);
        return searchTitles;
    }

    private void recursiveDirectorySearch(ServerBase server, CaseInsensitiveMap<String, MediaMetadata> searchTitles, String directory, String query, boolean includeServer) throws Exception{
        JSONObject subFolder = getJSON(getRequest(
                getBaseUrl(server),
                getData(AuxiliaryType.Directory, directory),
                getHeaders(server)));

        if (subFolder != null) {
            JSONArray subFolderArray = subFolder.getJSONObject("result").getJSONArray("files");
            if (subFolderArray != null && subFolderArray.length() > 0) {
                for (int i = 0; i < subFolderArray.length(); i++) {
                    JSONObject file = subFolderArray.getJSONObject(i);
                    String label = file.getString("label");
                    String fileName = file.getString("file");
                    if (file.getString("filetype").equalsIgnoreCase("directory") &&
                            (FileHelper.containsIgnoreCase(fileName, false, query)
                                    || FileHelper.containsIgnoreCase(label, false, query))) {
                        MediaMetadata fileMetadata = new MediaMetadata();
                        fileMetadata.set(MediaMetadata.Type.TITLE,  label);
                        fileMetadata.set(MediaMetadata.Type.DETAIL,  server.getName());
                        fileMetadata.set(MediaMetadata.Type.SUMMARY,  "");
                        fileMetadata.set(MediaMetadata.Type.THUMBNAIL,  "");
                        fileMetadata.set(MediaMetadata.Type.STREAMING,  fileName);
                        fileMetadata.set(MediaMetadata.Type.SERVER_ID,  server.getServerID());

                        if (includeServer) label += " (" + server.getName() + ")";
                        searchTitles.put(label, fileMetadata);

                        recursiveDirectorySearch(server, searchTitles, fileMetadata.get(MediaMetadata.Type.STREAMING), query, includeServer);
                    }
                }
            }
        }
    }

    private String getBaseUrl(ServerBase server){
        return getUrl(server) + "/jsonrpc";
    }

    private String getThumbnailUrl(ServerBase server, String thumbnail) throws Exception{
        return getUrl(server) + "/image/" + URLEncoder.encode(thumbnail, "UTF-8");
    }

    private String[] getData(Method method, String...query) throws Exception{
        String request = method.getRequest(schema, query).toString();
        return new String[]{"request", request};
    }

    public String[] getHeaders(ServerBase server){
        if (server.getKey().isEmpty()) return null;
        else return new String[]{"Authorization", server.getKey()};
    }


    interface Method {JSONObject getRequest(JSONObject schema, String[] query) throws Exception;}
    private enum AuxiliaryType implements Method {
        Application {
            @Override
            public JSONObject getRequest(JSONObject schema, String[] query) throws Exception {
                return schema.getJSONObject("application");
            }
        },
        Play {
            @Override
            public JSONObject getRequest(JSONObject schema, String[] query) throws Exception {
                JSONObject player = schema.getJSONObject("play");
                player.getJSONObject("params").put("item", new JSONObject().put(query[0], query[1]));
                return player;
            }
        },
        Directory {
            @Override
            public JSONObject getRequest(JSONObject schema, String[] query) throws Exception {
                JSONObject directory = schema.getJSONObject("directory");
                directory.getJSONObject("params").put("directory", query[0]);
                return directory;
            }
        }
    }

    public enum LibraryType implements Method{
        Movies("Movie", "Movies", "movies", "file") {
            @Override
            public JSONObject getRequest(JSONObject schema, String[] query) throws Exception {
                JSONObject media = adjustFilter(schema, query[0], "videos");
                media.put("method", "VideoLibrary.getMovies");
                return media;
            }
        },
        Shows("Show", "Television Shows", "tvshows", "file") {
            @Override
            public JSONObject getRequest(JSONObject schema, String[] query) throws Exception {
                JSONObject media = adjustFilter(schema, query[0], "videos");
                media.put("method", "VideoLibrary.getTVShows");
                return media;
            }
        },
        MusicVideos("Music Video", "Music Videos", "musicvideos", "file") {
            @Override
            public JSONObject getRequest(JSONObject schema, String[] query) throws Exception {
                JSONObject media = adjustFilter(schema, query[0], "music");
                media.put("method", "VideoLibrary.getMusicVideos");
                media.getJSONObject("params").getJSONObject("sort").put("method", "artist");
                return media;
            }
        },
        Songs("Song", "Individual Tracks", "songs", "file") {
            @Override
            public JSONObject getRequest(JSONObject schema, String[] query) throws Exception {
                JSONObject media = adjustFilter(schema, query[0], "music");
                media.put("method", "AudioLibrary.getSongs");
                media.getJSONObject("params").getJSONObject("sort").put("method", "track");
                return media;
            }
        },
        Photographs("Images", "Photograph Albums", "sources", "directory"){
            @Override
            public JSONObject getRequest(JSONObject schema, String[] query) throws Exception {
                return schema.getJSONObject("sources");
            }
        },
        Recordings("Recording", "Recordings", "recordings", "file"){
            @Override
            public JSONObject getRequest(JSONObject schema, String[] query) throws Exception {
                return schema.getJSONObject("pvr_recording");
            }
        };

        String listName, idName, menuName, optionName;
        LibraryType(String optionName, String displayName, String listName, String idName){
            this.listName = listName;
            this.idName = idName;
            this.menuName = displayName;
            this.optionName = optionName;
        }

        public String getOptionName() {return optionName;}
        public String getMenuName() {return menuName;}
        String getIdName() {return idName;}
        String getListName(){return listName;}

        JSONObject adjustFilter(JSONObject schema, String query, String key) throws Exception{
            JSONObject media = schema.getJSONObject(key);
            JSONArray filters = media
                    .getJSONObject("params")
                    .getJSONObject("filter")
                    .getJSONArray("or");

            for(int i = 0; i < filters.length(); i++){
                filters.getJSONObject(i)
                        .put("value", query);
            }

            return media;
        }

        public static LibraryType getType(String name){
            for(LibraryType category : LibraryType.values()){
                if (name.equalsIgnoreCase(category.optionName)){
                    return category;
                }
            }
            return Movies;
        }
    }
}
