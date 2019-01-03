package com.drageniix.raspberrypop.utilities.categories;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.activities.ScanActivity;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.media.MediaMetadata;
import com.drageniix.raspberrypop.servers.kodi_servers.KodiServer;
import com.drageniix.raspberrypop.servers.plex_servers.PlexServer;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.Logger;
import com.drageniix.raspberrypop.utilities.TimeManager;
import com.drageniix.raspberrypop.utilities.api.APIBase;
import com.drageniix.raspberrypop.utilities.api.APIParser;
import com.drageniix.raspberrypop.utilities.api.KodiAPI;
import com.drageniix.raspberrypop.utilities.api.PlexAPI;
import com.drageniix.raspberrypop.utilities.api.ThumbnailAPI;
import com.drageniix.raspberrypop.utilities.custom.CaseInsensitiveMap;
import com.google.android.gms.actions.SearchIntents;

import java.net.URLDecoder;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.drageniix.raspberrypop.utilities.categories.ScanApplication.ROKU;

public enum StreamingApplication implements ApplicationCategory {
    PLEX("Plex", R.color.plex, "com.plexapp.android", "https://play.google.com/store/apps/details?id=com.plexapp.android"){
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            super.setIcon(context, handler);
            if (!isInstalled()) {
                this.icon = context.getIcon(R.drawable.plex, false);
                this.appPackage = "";
                this.downloadLink = "";
            }
        }

        @Override
        public void search(DBHandler handler, Media editMedia, CaseInsensitiveMap<String, MediaMetadata> searchTitles, String title, String option) {
            searchTitles.clear();
            Set<PlexServer> servers = handler.getPlexServers();
            for(PlexServer server : servers) {
                searchTitles.putAll(handler.getParser().getPlexAPI().search(server, title, servers.size() > 1));
            }

            if (searchTitles.size() > 1 && searchTitles.containsKey("")) searchTitles.remove("");
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata, APIParser parser){
            if (metadata.recentSuccess()) {
                media.setDetail(metadata.get(MediaMetadata.Type.DETAIL));
                media.setSummary((media.getSummary()  + "\n\n" + metadata.get(MediaMetadata.Type.SUMMARY)).trim());
                media.setThumbnailString(metadata.get(MediaMetadata.Type.THUMBNAIL));
                media.setType(metadata.get(MediaMetadata.Type.TYPE));
                media.setTitle(metadata.get(MediaMetadata.Type.TITLE));
                media.setStreamingID(metadata.get(MediaMetadata.Type.STREAMING));
                media.setAlternateID(metadata.get(MediaMetadata.Type.SERVER_ID));
            } else {
                media.clearMetadata();
                media.setTitle(metadata.get(MediaMetadata.Type.INPUT_TITLE));
            }
        }

        @Override
        public boolean scan(BaseActivity activity, DBHandler handler, Media media, ScanActivity.mediaPlayback scanner) {
            PlexServer server = (PlexServer)handler.getServer(media.getAlternateID());
            if (server != null && server.isOnline()) {
                PlexAPI parser = handler.getParser().getPlexAPI();

                List<String[]> clients = parser.getAvailableClients(server);
                if (clients.isEmpty()){return false;}

                for(String[] client : clients){
                    String url = client[0];
                    String[] data = new String[]{
                            "key", "/library/metadata/" + media.getStreamingID(),
                            "offset", "0",
                            "X-Plex-Client-Identifier", client[1],
                            "machineIdentifier", server.getServerID(),
                            "address", server.getServerHost(),
                            "port", String.valueOf(server.getServerPort()),
                            "protocol", "http",
                            "path",  APIBase.getUrl(server) + "/library/metadata/" + media.getStreamingID(),
                            "X-Plex-Token", server.getKey()};

                    if (parser.attemptConnection(server, url, data)) return true;
                }
            }
            return false;
        }
    },
    KODI("Kodi", R.color.kodi, "org.xbmc.kore", "https://play.google.com/store/apps/details?id=org.xbmc.kore"){
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            super.setIcon(context, handler);
            if (!isInstalled()) {
                this.appPackage = "org.leetzone.android.yatsewidgetfree";
                this.downloadLink = "https://play.google.com/store/apps/details?id=org.leetzone.android.yatsewidgetfree";
                super.setIcon(context, handler);
                if (!isInstalled()) {
                    this.icon = context.getIcon(R.drawable.kodi, false);
                    this.appPackage = "";
                    this.downloadLink = "";
                }
            }
        }

        @Override
        public void search(DBHandler handler, Media editMedia, CaseInsensitiveMap<String, MediaMetadata> searchTitles, String title, String option) {
            searchTitles.clear();
            Set<KodiServer> servers = handler.getKodiServers();
            for(KodiServer server : servers) {
                searchTitles.putAll(handler.getParser().getKodiAPI().searchContent(server, option, title, servers.size() > 1));
            }

            if (searchTitles.size() > 1 && searchTitles.containsKey("")) searchTitles.remove("");
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata, APIParser parser){
            media.setType(metadata.get(MediaMetadata.Type.INPUT_OPTION));

            if (metadata.recentSuccess()) {
                media.setAlternateID(metadata.get(MediaMetadata.Type.SERVER_ID));
                media.setStreamingID(metadata.get(MediaMetadata.Type.STREAMING));
                media.setTitle(metadata.get(MediaMetadata.Type.TITLE));
                media.setDetail(metadata.get(MediaMetadata.Type.DETAIL));
                media.setSummary(metadata.get(MediaMetadata.Type.SUMMARY));
                media.setThumbnailString(metadata.get(MediaMetadata.Type.THUMBNAIL));

                KodiServer server = (KodiServer) media.getHandler().getServer(media.getAlternateID());
                media.setTempDetails(parser.getKodiAPI().getHeaders(server));
            }
        }

        @Override
        public boolean scan(BaseActivity activity, DBHandler handler, Media media, ScanActivity.mediaPlayback scanner) {
            KodiServer server = (KodiServer)handler.getServer(media.getAlternateID());
            if (server != null && server.isOnline()) {
                KodiAPI parser = handler.getParser().getKodiAPI();
                parser.playContent(server, media);
                if (!KODI.getAppPackage().isEmpty()) {
                    Intent storeIntent = activity.getPackageManager().getLaunchIntentForPackage(KODI.getAppPackage())
                            .addCategory(Intent.CATEGORY_LAUNCHER)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(storeIntent);
                }
                return true;
            }
            return false;
        }
    },
    GOOGLE("Google Play", R.color.google, "com.google.android.videos", "https://play.google.com/store/apps/details?id=com.google.android.videos", "", "vnd.youtube:"){
        @Override
        public void search(DBHandler handler, Media editMedia, CaseInsensitiveMap<String, MediaMetadata> searchTitles, String title, String option) {
            searchTitles.clear();
            searchTitles.putAll(handler.getParser().getYoutubeAPI().searchYoutube(title, option));
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata, APIParser parser) {
            media.setType(metadata.get(MediaMetadata.Type.INPUT_OPTION));
            if (!metadata.recentSuccess() && media.getType().equals("movie")){
                if (metadata.putAll(parser.getYoutubeAPI().searchYoutube(media.getTitle(), media.getType()).entrySet().iterator().next().getValue())){
                    media.setStreamingID(metadata.get(MediaMetadata.Type.ALTERNATE));
                    media.setTitle(metadata.get(MediaMetadata.Type.TITLE));
                    metadata.putAll(parser.getOmDbAPI().getOMDb(media.getTitle(), "t"));
                }
            } else if (metadata.recentSuccess()) {
                media.setStreamingID(metadata.get(MediaMetadata.Type.ALTERNATE));
                media.setTitle(metadata.get(MediaMetadata.Type.TITLE));
                media.setDetail(metadata.get(MediaMetadata.Type.DETAIL));
                media.setSummary(metadata.get(MediaMetadata.Type.SUMMARY));
                media.setThumbnailString(metadata.get(MediaMetadata.Type.THUMBNAIL));
                metadata.putAll(parser.getOmDbAPI().getOMDb(media.getTitle(), "t"));
            }

            if (metadata.recentSuccess()) {
                media.setTitle(metadata.get(MediaMetadata.Type.TITLE));
                media.setDetail(metadata.get(MediaMetadata.Type.DETAIL));
                media.setSummary(metadata.get(MediaMetadata.Type.SUMMARY));
                media.setThumbnailString(metadata.get(MediaMetadata.Type.THUMBNAIL));
                media.setExternalID(metadata.get(MediaMetadata.Type.IMDB));
            }
        }
    },
    YOUTUBE("Youtube", R.color.youtube, "com.google.android.youtube", "http://www.youtube.com/watch?v="){
        @Override
        public void search(DBHandler handler, Media editMedia, CaseInsensitiveMap<String, MediaMetadata> searchTitles, String title, String option) {
            searchTitles.clear();
            searchTitles.putAll(handler.getParser().getYoutubeAPI().searchYoutube(title, option));
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata, APIParser parser){
            media.setType(metadata.get(MediaMetadata.Type.INPUT_OPTION));
            if (!metadata.recentSuccess() && metadata.get(MediaMetadata.Type.INPUT_TITLE).contains("http")){
                metadata.putAll(parser.getYoutubeAPI().parseLink(metadata.get(MediaMetadata.Type.INPUT_TITLE)));
            } else if (!metadata.recentSuccess() && media.getType().equals("channel")){
                metadata.putAll(parser.getYoutubeAPI().searchYoutube(media.getTitle(), media.getType()).entrySet().iterator().next().getValue());
            }

            if (metadata.recentSuccess()){
                media.setAlternateID(metadata.get(MediaMetadata.Type.ALTERNATE));
                media.setStreamingID(metadata.get(MediaMetadata.Type.STREAMING));
                media.setType(metadata.get(MediaMetadata.Type.TYPE));
                media.setTitle(metadata.get(MediaMetadata.Type.TITLE));
                media.setDetail(metadata.get(MediaMetadata.Type.DETAIL));
                media.setSummary(metadata.get(MediaMetadata.Type.SUMMARY));
                media.setThumbnailString(metadata.get(MediaMetadata.Type.THUMBNAIL));
            }
        }
    },
    SPOTIFY("Spotify", R.color.spotify, "com.spotify.music", "https://play.google.com/store/apps/details?id=com.spotify.music"){
        @Override
        public void search(DBHandler handler, Media editMedia, CaseInsensitiveMap<String, MediaMetadata> searchTitles, String title, String option) {
            searchTitles.clear();
            searchTitles.putAll(handler.getParser().getSpotifyAPI().searchSpotify(title, option));
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata, APIParser parser){
            media.setType(metadata.get(MediaMetadata.Type.INPUT_OPTION));
            if (!metadata.recentSuccess()&& !metadata.putAll(parser.getSpotifyAPI().parseLink(media.getTitle()))){
                metadata.putAll(parser.getSpotifyAPI().searchSpotify(media.getTitle(), media.getType()).entrySet().iterator().next().getValue());
            }

            if (metadata.recentSuccess()) {
                media.setType(metadata.get(MediaMetadata.Type.TYPE).isEmpty() ?
                        metadata.get(MediaMetadata.Type.INPUT_OPTION) :
                        metadata.get(MediaMetadata.Type.TYPE));

                media.setStreamingID(metadata.get(MediaMetadata.Type.STREAMING));
                media.setTitle(metadata.get(MediaMetadata.Type.TITLE));
                media.setSummary(metadata.get(MediaMetadata.Type.SUMMARY));
                media.setThumbnailString(metadata.get(MediaMetadata.Type.THUMBNAIL));
            }
        }
    },
    TWITCH("Twitch", R.color.twitch, "tv.twitch.android.app", "https://play.google.com/store/apps/details?id=tv.twitch.android.app"){
        @Override
        public void search(DBHandler handler, Media editMedia, CaseInsensitiveMap<String, MediaMetadata> searchTitles, String title, String option) {
            searchTitles.clear();
            searchTitles.putAll(handler.getParser().getTwitchAPI().searchChannels(title));
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata, APIParser parser) {
            media.setType("channel|twitch");
            if (!metadata.recentSuccess()){
                if (media.getTitle().contains("twitch.tv/")){
                    media.setTitle(media.getTitle().substring(media.getTitle().lastIndexOf("/")+1));}
                metadata.putAll(parser.getTwitchAPI().searchChannels(media.getTitle()).entrySet().iterator().next().getValue());
            }

            if (metadata.recentSuccess() && metadata.putAll(parser.getTwitchAPI().getChannel(metadata.get(MediaMetadata.Type.STREAMING)))){
                media.setAlternateID(metadata.get(MediaMetadata.Type.ALTERNATE));
                media.setStreamingID(metadata.get(MediaMetadata.Type.STREAMING));
                media.setTitle(metadata.get(MediaMetadata.Type.TITLE));
                media.setDetail(metadata.get(MediaMetadata.Type.DETAIL));
                media.setSummary(metadata.get(MediaMetadata.Type.SUMMARY));
                media.setThumbnailString(metadata.get(MediaMetadata.Type.THUMBNAIL));
            }
        }
    },
    PANDORA("Pandora", R.color.pandora, "com.pandora.android", "https://play.google.com/store/apps/details?id=com.pandora.android", "", "pandorav2://createStation?stationID="){
        @Override
        public void search(DBHandler handler, Media editMedia, CaseInsensitiveMap<String, MediaMetadata> searchTitles, String title, String option) {
            searchTitles.putAll(handler.getParser().getPandoraAPI().getStations());
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata, APIParser parser) {
            if (metadata.recentSuccess()){
                media.setTitle(metadata.get(MediaMetadata.Type.TITLE));
                media.setStreamingID(metadata.get(MediaMetadata.Type.STREAMING));
                media.setThumbnailString(metadata.get(MediaMetadata.Type.THUMBNAIL));
                media.setType("radio|saved");
            } else {
                media.setEnabled(LAUNCH);
                media.setTitle(getName());
                media.setStreamingID(getAppPackage());
                media.setDetail(metadata.get(MediaMetadata.Type.INPUT_TITLE));
                media.setAlternateID(metadata.get(MediaMetadata.Type.INPUT_TITLE));
                media.setType(SearchIntents.ACTION_SEARCH);
                parser.getThumbnailAPI().setThumbnailBitmap(
                        parser.getURIAPI().getPackageIcon(media, media.getStreamingID()),
                        media, ThumbnailAPI.Type.AUXILIARY);

            }
        }
    },
    MAPS("Location", R.color.maps){
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(
                    new Intent(Intent.ACTION_VIEW).setData(Uri.parse("geo:")), PackageManager.MATCH_DEFAULT_ONLY);

            if (resolveInfo != null) {
                this.icon = context.getIcon(R.drawable.ic_action_navigation, true);
                ActivityInfo activityInfo = resolveInfo.activityInfo;
                if (!activityInfo.packageName.equals("android")) {
                    this.appPackage = activityInfo.packageName;
                    this.downloadLink = "https://play.google.com/store/apps/details?id=" + appPackage;
                }
            }
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata, APIParser parser) {
            media.setType("geo");
            if (metadata == null) {
                media.setThumbnailString("https://maps.googleapis.com/maps/api/staticmap?"
                        + "markers=" + media.getStreamingID()
                        + "&size=200x250&scale=2&key=" + parser.getGoogleAPI().getAPIKey());

                if (getAppPackage().equals("com.google.android.apps.maps")) {
                    media.setStreamingID("https://www.google.com/maps/dir/?api=1&dir_action=navigate&destination=" + media.getStreamingID());
                } else {
                    media.setStreamingID("geo:0,0?q=" + media.getStreamingID() + "(" + media.getTitle() + ")");
                }

                parser.getThumbnailAPI().setThumbnailBitmap(
                        parser.getURIAPI().getUriIcon(media, "geo:0,0"),
                        media, ThumbnailAPI.Type.AUXILIARY);

                parser.getThumbnailAPI().setThumbnailURL(
                        media, ThumbnailAPI.Type.THUMBNAIL);
            } else {
                media.setTitle(metadata.get(MediaMetadata.Type.INPUT_TITLE));
                if (media.getStreamingID().contains("geo:")) {
                    media.setStreamingID(media.getStreamingID().substring(0, media.getStreamingID().lastIndexOf("(")) + "(" + media.getTitle() + ")");
                }
            }
        }

        @Override
        public String getQR(BaseActivity activity, Media media) {
            return media.getExternalID();
        }
    },
    URI("Website Link/URI", R.color.uri){
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_uri, true);
        }

        @Override
        public void search(DBHandler handler, Media editMedia, CaseInsensitiveMap<String, MediaMetadata> searchTitles, String title, String option) {
            searchTitles.clear();
            searchTitles.put("skype:", null);
            searchTitles.put("whatsapp://send", null);
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata, APIParser parser){
            media.setType(metadata.get(MediaMetadata.Type.INPUT_OPTION));
            if (media.getType().equalsIgnoreCase("Website Link")){
                Pattern pattern = Pattern.compile("([^\\s]*).([a-zA-Z0-9]*)(/|$)([^?&$\\s]*)");
                Matcher matcher = pattern.matcher(metadata.get(MediaMetadata.Type.INPUT_TITLE));
                if (matcher.find()){
                    media.setStreamingID((!matcher.group(0).toLowerCase().contains("http") ? "http://" : "") + matcher.group(0));
                    media.setStreamingID(media.getStreamingID().replace("//mobile.", "//"));
                    if (media.getStreamingID().startsWith("https://www.google.com/url?")){
                        try {
                            String url = media.getStreamingID().substring(media.getStreamingID().indexOf("&url=") + 5);
                            if (url.contains("&")) {url = url.substring(0, url.indexOf("&"));}
                            media.setStreamingID(URLDecoder.decode(url, "UTF-8"));
                        } catch (Exception e){
                            Logger.log(Logger.API, e);
                        }
                    }
                }
            }

            if (media.getStreamingID().isEmpty()){
                Pattern pattern = Pattern.compile("([^\\s]*)([A-Za-z0-9]*):([^$\\s]*)");
                Matcher matcher = pattern.matcher(metadata.get(MediaMetadata.Type.INPUT_TITLE));
                if (matcher.find()){
                    media.setStreamingID(matcher.group(0).replace(" ", "+"));
                }
            }

            if (!metadata.get(MediaMetadata.Type.SUMMARY).isEmpty()){ //barcode
                media.setSummary(metadata.get(MediaMetadata.Type.SUMMARY));
                media.setTitle(metadata.get(MediaMetadata.Type.TITLE));
            }

            if (media.getType().equalsIgnoreCase("Website Link")){
                String input;
                if (metadata.putAll(parser.getYoutubeAPI().parseLink(input = metadata.get(MediaMetadata.Type.INPUT_TITLE)))
                    || metadata.putAll(parser.getSpotifyAPI().parseLink(input = media.getStreamingID()))
                    || metadata.putAll(parser.getURLAPI().parseAmazon(input = media.getStreamingID()))
                    || metadata.putAll(parser.getURLAPI().parsePocketCasts(input = media.getStreamingID()))
                    || metadata.putAll(parser.getURLAPI().openGraph(input = media.getStreamingID(), metadata.get(MediaMetadata.Type.INPUT_TITLE)))) {

                    media.setTitle(metadata.get(MediaMetadata.Type.TITLE));
                    media.setDetail(metadata.get(MediaMetadata.Type.DETAIL));
                    media.setSummary(metadata.get(MediaMetadata.Type.SUMMARY));
                    media.setThumbnailString(metadata.get(MediaMetadata.Type.THUMBNAIL));
                    media.setStreamingID(metadata.get(MediaMetadata.Type.STREAMING));
                    media.setAlternateID(input);

                    parser.getURIAPI().checkURI(media);
                    if (!media.getTitle().contains(metadata.get(MediaMetadata.Type.INPUT_TITLE))
                            && !media.getTitle().contains(input)
                            && !media.getTitle().contains(media.getStreamingID())
                            && !media.getSummary().contains(metadata.get(MediaMetadata.Type.INPUT_TITLE))
                            && !media.getSummary().contains(input)
                            && !media.getSummary().contains(media.getStreamingID())){
                        media.setSummary(media.getStreamingID() + "\n\n" + media.getSummary());
                    }

                    media.setType(media.getStreamingID().toLowerCase().startsWith("http") ?
                            "Website Link" : "URI");
                }
            }

            parser.getThumbnailAPI().setThumbnailBitmap(
                    parser.getURIAPI().getUriIcon(media, media.getStreamingID()),
                    media,
                    ThumbnailAPI.Type.AUXILIARY);
        }

        @Override
        public String getQR(BaseActivity activity, Media media) {
            return media.getStreamingID();
        }
    },
    LOCAL("Local File", R.color.local){
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_file, true);
        }

        @Override
        public void search(DBHandler handler, Media editMedia, CaseInsensitiveMap<String, MediaMetadata> searchTitles, String title, String option) {
            searchTitles.clear();
            if (editMedia.getAlternateID().endsWith(title)) {
                title = handler.getFileHelper().normalizeTitle(editMedia.getAlternateID());
            }
            if (editMedia.getType().contains("audio")) {
                searchTitles.putAll(handler.getParser().getSpotifyAPI().searchSpotify(title, "track"));
            } else if (editMedia.getType().contains("video")) {
                searchTitles.putAll(handler.getParser().getOmDbAPI().searchOMDb(title));
            } else if (handler.getFileHelper().isGame(editMedia.getAlternateID())) {
                searchTitles.putAll(handler.getParser().getgDbAPI().getGame(title));
            } else if (handler.getFileHelper().isBook(editMedia.getAlternateID())) {
                searchTitles.putAll(handler.getParser().getGoogleAPI().getBook(title));
            }
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata, APIParser parser){
            if (metadata != null && !metadata.get(MediaMetadata.Type.MEDIA_ALTERNATE).isEmpty()){
                media.setAlternateID(metadata.get(MediaMetadata.Type.MEDIA_ALTERNATE));
                media.setType(metadata.get(MediaMetadata.Type.MEDIA_TYPE));
                media.setAuxiliaryString(metadata.get(MediaMetadata.Type.MEDIA_AUXILIARY));
                media.setStreamingID(metadata.get(MediaMetadata.Type.MEDIA_STREAMING));

                if (metadata.recentSuccess()){
                    if (media.getType().contains("audio")){
                        media.setTitle(metadata.get(MediaMetadata.Type.TITLE));
                        media.setSummary(metadata.get(MediaMetadata.Type.SUMMARY));
                        media.setThumbnailString(metadata.get(MediaMetadata.Type.THUMBNAIL));
                    } else if (media.getType().contains("video") && metadata.putAll(parser.getOmDbAPI().getOMDb(metadata.get(MediaMetadata.Type.IMDB), "i"))) {
                        media.setTitle(metadata.get(MediaMetadata.Type.TITLE));
                        media.setDetail(metadata.get(MediaMetadata.Type.DETAIL));
                        media.setSummary(metadata.get(MediaMetadata.Type.SUMMARY));
                        media.setThumbnailString(metadata.get(MediaMetadata.Type.THUMBNAIL));
                        media.setExternalID(metadata.get(MediaMetadata.Type.IMDB));
                    } else if (media.getHandler().getFileHelper().isBook(media.getAlternateID())) {
                        media.setTitle(metadata.get(MediaMetadata.Type.TITLE));
                        media.setDetail(metadata.get(MediaMetadata.Type.DETAIL));
                        media.setSummary(metadata.get(MediaMetadata.Type.SUMMARY));
                        media.setThumbnailString(metadata.get(MediaMetadata.Type.THUMBNAIL));
                    } else if (media.getHandler().getFileHelper().isGame(media.getAlternateID())){
                        media.setTitle(metadata.get(MediaMetadata.Type.TITLE));
                        media.setDetail(metadata.get(MediaMetadata.Type.DETAIL));
                        media.setSummary(metadata.get(MediaMetadata.Type.SUMMARY));
                        media.setThumbnailString(metadata.get(MediaMetadata.Type.THUMBNAIL));
                    }
                }
            }
        }
    },
    LAUNCH("Search/Launch App", R.color.launch) {
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_launch, true);
        }

        @Override
        public void search(DBHandler handler, Media editMedia, CaseInsensitiveMap<String, MediaMetadata> searchTitles, String title, String option) {
            if (searchTitles.isEmpty()) {
                searchTitles.putAll(handler.getParser().getURIAPI().getInstalledApps());
            }
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata, APIParser parser) {
            if (metadata.recentSuccess()) {
                media.setStreamingID(metadata.get(MediaMetadata.Type.PACKAGE_NAME));
                media.setExternalID(metadata.get(MediaMetadata.Type.PACKAGE_CLASS));
                media.setType(metadata.get(MediaMetadata.Type.PACKAGE_INTENT));
                media.setAlternateID(metadata.get(MediaMetadata.Type.INPUT_CUSTOM));
                media.setDetail(metadata.get(MediaMetadata.Type.INPUT_CUSTOM));

                if (!media.getAlternateID().isEmpty() && (
                        media.getStreamingID().equals("com.netflix.mediaclient") ||
                                media.getStreamingID().equals("com.google.android.videos"))
                        && metadata.putAll(parser.getOmDbAPI().getOMDb(media.getAlternateID(),"t"))){
                    media.setDetail(metadata.get(MediaMetadata.Type.TITLE) + " " + metadata.get(MediaMetadata.Type.DETAIL));
                    media.setSummary(metadata.get(MediaMetadata.Type.SUMMARY));
                    media.setThumbnailString(metadata.get(MediaMetadata.Type.THUMBNAIL));
                }

                parser.getThumbnailAPI().setThumbnailBitmap(
                        parser.getURIAPI().getPackageIcon(media, media.getStreamingID()),
                        media, ThumbnailAPI.Type.AUXILIARY);
            } else {
                media.setDetail("Web Search");
                media.setStreamingID("query");
            }
        }

        @Override
        public boolean scan(BaseActivity activity, DBHandler handler, Media media, ScanActivity.mediaPlayback scanner) {
            Intent intent;
            if (media.getType().isEmpty()){
                intent = new Intent(Intent.ACTION_WEB_SEARCH)
                        .putExtra("query", media.getTitle());
            } else if (!media.getExternalID().isEmpty() && !media.getAlternateID().isEmpty()){
                intent = new Intent(media.getType())
                        .setComponent(new ComponentName(media.getStreamingID(), media.getExternalID()))
                        .putExtra("query", media.getAlternateID());
            } else if (!media.getAlternateID().isEmpty()) {
                intent = new Intent(media.getType())
                        .setPackage(media.getStreamingID())
                        .putExtra("query", media.getAlternateID());
            } else {
                intent = activity.getPackageManager()
                        .getLaunchIntentForPackage(media.getStreamingID())
                        .addCategory(Intent.CATEGORY_LAUNCHER);
            }

            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivity(intent
                        .addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                        Intent.FLAG_ACTIVITY_NO_ANIMATION |
                                        Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS));
                return true;
            } else return false;
        }
    },
    CLOCK("Time", R.color.timer){
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_time, true);
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata, APIParser parser) {
            AuxiliaryApplication.valueOf(metadata.get(MediaMetadata.Type.TYPE)).setMetadata(media, metadata);
        }

        @Override
        public boolean scan(BaseActivity activity, DBHandler handler, Media media, ScanActivity.mediaPlayback scanner) {
            AuxiliaryApplication clock = AuxiliaryApplication.valueOf(media);
            return clock.scan(activity, handler, media, scanner);
        }

        @Override
        public String getQR(BaseActivity activity, Media media) {
            AuxiliaryApplication clock = AuxiliaryApplication.valueOf(media);
            return clock.getQR(activity, media);
        }

        @Override
        public MediaMetadata getCSV(Media media) {
            AuxiliaryApplication other = AuxiliaryApplication.valueOf(media);
            return other.getCSV(super.getCSV(media), media);
        }

        @Override public boolean isFolder(){return true;}
    },
    CONTACT("Contact", R.color.contact){
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_contact, true);
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata, APIParser parser) {
            //<uses-permission android:name="android.permission.READ_CONTACTS" />, move to non URL setter
            //media.setThumbnailString(metadata.get(MediaMetadata.Type.THUMBNAIL));
            //parser.getThumbnailAPI().setThumbnailURI(media, ThumbnailAPI.Type.THUMBNAIL);

            AuxiliaryApplication contact = AuxiliaryApplication.valueOf(metadata.get(MediaMetadata.Type.TYPE));
            if (contact == AuxiliaryApplication.CALL || contact == AuxiliaryApplication.SMS || contact == AuxiliaryApplication.EMAIL) {
                String prefix = "", prefix2 = "";
                switch (contact) {
                    case CALL:
                        prefix = "tel:";
                        break;
                    case SMS:
                        prefix = "sms:";
                        prefix2 = "smsto:";
                        break;
                    case EMAIL:
                        prefix = "mailto:";
                        break;
                }

                media.setType(contact.name());
                String title, custom, alternate = "";
                if (!metadata.get(MediaMetadata.Type.STREAMING).isEmpty()) { //chosen contact or barcode
                    custom = metadata.get(MediaMetadata.Type.STREAMING);
                    String[] splitInfo = custom.replace(prefix, "").replace(prefix2, "").split("\\?");
                    title = splitInfo[0];
                    media.setTitle(title);
                    if (!metadata.get(MediaMetadata.Type.SUMMARY).isEmpty()) {
                        media.setSummary(metadata.get(MediaMetadata.Type.SUMMARY));
                    } else if (!metadata.get(MediaMetadata.Type.DETAIL).equals(metadata.get(MediaMetadata.Type.STREAMING))) {
                        media.setDetail(metadata.get(MediaMetadata.Type.DETAIL));
                    }

                    if (splitInfo.length > 1) {
                        alternate = splitInfo[1];
                        media.setAlternateID(alternate);
                        media.setStreamingID(custom);
                    } else if (!metadata.get(MediaMetadata.Type.ALTERNATE).isEmpty() && (contact == AuxiliaryApplication.SMS || contact == AuxiliaryApplication.EMAIL)){
                        alternate = metadata.get(MediaMetadata.Type.ALTERNATE);
                        media.setAlternateID(alternate);
                        media.setSummary((media.getSummary() + "\n" + alternate).trim());
                        if (!alternate.contains("?") && !alternate.contains("=")){
                            alternate = "?body=" + alternate;
                        }
                        media.setStreamingID(custom + alternate);
                    } else {
                        media.setStreamingID(custom);
                    }

                } else {
                    custom = metadata.get(MediaMetadata.Type.INPUT_TITLE);
                    title = custom.replace(prefix, "").replace(prefix2, "").split("\\?")[0];
                    media.setTitle(title);
                    if (!custom.startsWith(prefix) && (prefix2.isEmpty() || !custom.startsWith(prefix2))) {
                        custom = prefix + custom;
                    }
                    if (!metadata.get(MediaMetadata.Type.INPUT_CUSTOM).isEmpty() && (contact == AuxiliaryApplication.SMS || contact == AuxiliaryApplication.EMAIL)){
                        alternate = metadata.get(MediaMetadata.Type.INPUT_CUSTOM);
                        media.setAlternateID(alternate);
                        media.setSummary(alternate);
                        if (!alternate.contains("?") && !alternate.contains("=")){
                            alternate = "?body=" + alternate;
                        }
                    }
                    media.setStreamingID(custom + alternate);
                }

                parser.getThumbnailAPI().setThumbnailBitmap(
                        parser.getURIAPI().getUriIcon(media, media.getStreamingID()),
                        media,
                        ThumbnailAPI.Type.AUXILIARY);
            } else {
                contact.setMetadata(media, metadata);
            }
        }

        @Override
        public boolean scan(BaseActivity activity, DBHandler handler, Media media, ScanActivity.mediaPlayback scanner) {
            AuxiliaryApplication contact = AuxiliaryApplication.valueOf(media);
            return contact.scan(activity, handler, media, scanner);
        }

        @Override
        public String getQR(BaseActivity activity, Media media) {
            AuxiliaryApplication contact = AuxiliaryApplication.valueOf(media);
            return  contact == AuxiliaryApplication.CALL || contact == AuxiliaryApplication.SMS || contact == AuxiliaryApplication.EMAIL ?
                    media.getStreamingID() : contact.getQR(activity, media);
        }

        @Override
        public MediaMetadata getCSV(Media media) {
            AuxiliaryApplication other = AuxiliaryApplication.valueOf(media);
            return other.getCSV(super.getCSV(media), media);
        }

        @Override public boolean isFolder(){return true;}

    },
    DEVICE("Device", R.color.device){
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_device, true);
        }

        @Override
        public void search(DBHandler handler, Media editMedia, CaseInsensitiveMap<String, MediaMetadata> searchTitles, String title, String option) {
            searchTitles.clear();
            if (AuxiliaryApplication.valueOf(option) == AuxiliaryApplication.WIFI_CONNECTION){
                searchTitles.putAll(handler.getParser().getDeviceAPI().getWifiConfigurations());
            }
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata, APIParser parser) {
            AuxiliaryApplication.valueOf(metadata.get(MediaMetadata.Type.TYPE)).setMetadata(media, metadata);
        }

        @Override
        public boolean scan(BaseActivity activity, DBHandler handler, Media media, ScanActivity.mediaPlayback scanner) {
            AuxiliaryApplication setting = AuxiliaryApplication.valueOf(media);
            if (setting == AuxiliaryApplication.BRIGHTNESS || setting == AuxiliaryApplication.ORIENTATION){
                activity.openIntent(BaseActivity.SETTING_REQUEST_CODE, media);
                return false;
            } else if (setting == AuxiliaryApplication.VOLUME){
                activity.openIntent(BaseActivity.VOLUME_REQUEST_CODE, media);
                return false;
            } else if (setting == AuxiliaryApplication.FLASHLIGHT){
                activity.openIntent(BaseActivity.FLASHLIGHT_REQUEST_CODE, media);
                return false;
            } else return setting.scan(activity, handler, media, scanner);
        }

        @Override
        public String getQR(BaseActivity activity, Media media) {
            AuxiliaryApplication setting = AuxiliaryApplication.valueOf(media);
            return setting.getQR(activity, media);
        }


        @Override
        public MediaMetadata getCSV(Media media) {
            AuxiliaryApplication other = AuxiliaryApplication.valueOf(media);
            return other.getCSV(super.getCSV(media), media);
        }

        @Override public boolean isFolder(){return true;}
    },
    OTHER("Other", R.color.note) {
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_note, true);
        }

        @Override
        public void setMetadata(Media media, MediaMetadata metadata, APIParser parser) {
            if (metadata == null && media != null && media.getEnabled() == this && !media.getType().isEmpty()){
                AuxiliaryApplication.valueOf(media).setMetadata(media, metadata);
            } else if (metadata != null){
                AuxiliaryApplication.valueOf(metadata.get(MediaMetadata.Type.TYPE)).setMetadata(media, metadata);
            }
        }

        @Override
        public boolean scan(BaseActivity activity, DBHandler handler, Media media, ScanActivity.mediaPlayback scanner) {
            AuxiliaryApplication other = AuxiliaryApplication.valueOf(media);
            return other.scan(activity, handler, media, scanner);
        }

        @Override
        public String getQR(BaseActivity activity, Media media) {
            AuxiliaryApplication other = AuxiliaryApplication.valueOf(media);
            return other.getQR(activity, media);
        }

        @Override
        public MediaMetadata getCSV(Media media) {
            AuxiliaryApplication other = AuxiliaryApplication.valueOf(media);
            return other.getCSV(super.getCSV(media), media);
        }

        @Override public boolean isFolder(){return true;}
    },
    COPY("Copy Existing", R.color.off){
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_action_tag, true);
        }

        @Override
        public void search(DBHandler handler, Media editMedia, CaseInsensitiveMap<String, MediaMetadata> searchTitles, String title, String option) {
            if (searchTitles.isEmpty()) {
                for (Media media : handler.getMediaList(handler.getDefaultCollection())) {
                    if ((option != null && media.getCyclePosition() == 0 && !editMedia.getCycleString().equals(media.getCycleString()))
                            || (option == null && !media.equals(editMedia))) {
                        searchTitles.put(
                            media.getFigureName() + " (" + media.getTitle() + " - " + media.getEnabled().getName() + ")",
                            new MediaMetadata()
                                .set(MediaMetadata.Type.MEDIA_UID, media.getScanUID())
                                .set(MediaMetadata.Type.MEDIA_CYCLE, media.getCycleString()));
                    }
                }
            }
        }

        @Override public void setMetadata(Media media, MediaMetadata metadata, APIParser parser){
            Media original = media.getHandler().readMedia(metadata.get(MediaMetadata.Type.INPUT_CUSTOM));
            if (original != null) {
                media.setEnabled(original.getEnabled());
                media.setStreamingID(original.getStreamingID());
                media.setAlternateID(original.getAlternateID());
                media.setType(original.getType());
                media.setTitle(original.getTitle());
                media.setExternalID(original.getExternalID());
                media.setDetail(original.getDetail());
                media.setSummary(original.getSummary());
                media.setThumbnailString(original.getThumbnailString());
                media.setAuxiliaryString(original.getAuxiliaryString());
                media.getHandler().getFileHelper().saveMediaFile(media, original.getThumbnailPath(), media.getThumbnailPath(), false);
                media.getHandler().getFileHelper().saveMediaFile(media, original.getAuixiliaryPath(), media.getAuixiliaryPath(), false);
                if (original.getEnabled() == StreamingApplication.LOCAL){
                    media.getHandler().getFileHelper().saveMediaContentFile(media, original.getStreamingID(), media.getFilePath());}
            } else {
                media.setEnabled(StreamingApplication.OFF);
                media.clearMetadata();
            }
        }
    }, OFF("Disabled", R.color.off){
        @Override
        public void setIcon(BaseActivity context, DBHandler handler) {
            this.icon = context.getIcon(R.drawable.ic_stream, true);
        }

        @Override public void setMetadata(Media media, MediaMetadata metadata, APIParser parser){}
    };

    protected String name, appPackage, activityName, downloadLink, uriData;
    protected Drawable icon;
    protected int color;

    StreamingApplication(String name, int color, String...details){
        this.name = name;
        this.color = color;
        this.appPackage = "";
        this.downloadLink = "";
        this.activityName = "";
        this.uriData = "";

        if (details != null && details.length != 0) {
            this.appPackage = details[0];
            this.downloadLink = details[1];
            if (details.length > 2) {
                this.activityName = details[2];
                this.uriData = details[3];
            }
        }
    }

    @Override
    public boolean scan(BaseActivity activity, DBHandler handler, Media media, ScanActivity.mediaPlayback scanner) {
        try {
            if (!ROKU.scan(activity, handler, media, scanner)){
                Intent intent = new Intent(Intent.ACTION_VIEW);
                if (!media.getEnabled().getActivityName().isEmpty()) {
                    intent.setClassName(media.getEnabled().getAppPackage(), media.getEnabled().getActivityName());}
                String mimeType;
                if ((media.getEnabled() == StreamingApplication.LOCAL)
                        && (mimeType = handler.getFileHelper().getMimeTypeForFile(media.getStreamingID())) != null && !mimeType.isEmpty()) {
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    intent.setDataAndType(Uri.parse(media.getStreamingID()), mimeType);
                } else if (media.getEnabled() == StreamingApplication.TWITCH) {
                    intent.setData(Uri.parse(handler.getParser().getTwitchAPI().getAppropriateUri(media.getAlternateID(), media.getStreamingID())));
                } else {
                    intent.setData(Uri.parse(media.getEnabled().getUriData() + media.getStreamingID()));
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            } else if (ROKU.isInstalled()) {
                Intent storeIntent = activity.getPackageManager().getLaunchIntentForPackage(ROKU.getAppPackage())
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(storeIntent);
            }
            return true;
        } catch (Exception e) {
            if (!media.getEnabled().getDownloadLink().isEmpty()) {
                Intent storeIntent = new Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse(media.getEnabled().getDownloadLink()))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(storeIntent);
            }
            return false;
        }
    }

    public void setIcon(BaseActivity context, DBHandler handler){
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(getAppPackage(), PackageManager.GET_META_DATA);
            icon = pm.getApplicationIcon(ai);
        } catch (PackageManager.NameNotFoundException e) {
            icon = null;
        }
    }

    public MediaMetadata getCSV(Media media){
        MediaMetadata metadata = new MediaMetadata();

        final List<String> scanDates = new LinkedList<>();
        for (int time = 0; time < media.getScanHistory().length; time++) {
            String date = media.getScanHistory()[time];
            long milli = Long.parseLong(date);
            Calendar entry = Calendar.getInstance();
            entry.setTimeInMillis(milli);
            scanDates.add(media.getHandler().getTimeManager().format(TimeManager.FULL_TIME, entry.getTime()).replace("-", "/"));
        }

        AuxiliaryApplication application = AuxiliaryApplication.valueOf(media);

        metadata.set(MediaMetadata.Type.CSV_FIGURE_NAME, media.getFigureName())
                .set(MediaMetadata.Type.CSV_ORIGINAL, media.getOriginal())
                .set(MediaMetadata.Type.CSV_SCAN_HISTORY, TextUtils.join("\n", scanDates))
                .set(MediaMetadata.Type.CSV_COMMENTS, media.getComments())
                .set(MediaMetadata.Type.CSV_SOURCE, application == null ? media.getEnabled().getName() : application.getName())
                .set(MediaMetadata.Type.CSV_TITLE, media.getTitle())
                .set(MediaMetadata.Type.CSV_DETAIL, media.getDetail())
                .set(MediaMetadata.Type.CSV_SUMMARY, media.getSummary())
                .set(MediaMetadata.Type.CSV_LABEL, media.getHandler().getPreferences().getColorString(media.getLabel()));

        return metadata;
    }

    public String getQR(BaseActivity activity, Media media) {
        return null;
    }

    public  void search(DBHandler handler, Media editMedia, CaseInsensitiveMap<String, MediaMetadata> searchTitles, String title, String option){}
    public abstract void setMetadata(Media media, MediaMetadata metadata, APIParser parser);

    public boolean isFolder(){return false;}
    public int getColor() {return color;}
    public String getName(){return name;}
    public String getAppPackage(){return appPackage;}
    public String getActivityName(){return activityName;}
    public String getDownloadLink(){return downloadLink;}
    public String getUriData(){return uriData;}
    public boolean isInstalled() {return icon != null;}
    public Drawable getIcon(){return icon;}
}