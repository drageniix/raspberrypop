package com.drageniix.raspberrypop.utilities.api;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.Logger;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

public class APIParser {
    private PlexAPI plexAPI;
    private SpotifyAPI spotifyAPI;
    private YoutubeAPI youtubeAPI;
    private ThumbnailAPI thumbnailAPI;
    private OMDbAPI omDbAPI;
    private GDbAPI gDbAPI;
    private URIAPI URIAPI;
    private BarCodeAPI barCodeAPI;
    private TwitchAPI twitchAPI;
    private KodiAPI kodiAPI;
    private DeviceAPI deviceAPI;
    private GoogleAPI googleAPI;
    private URLAPI URLAPI;
    private PandoraAPI pandoraAPI;
    private JSONObject apiResource;
    private String billingKey;

    public APIParser(DBHandler handler, BaseActivity context) {
        APIBase.handler = handler;
        APIBase.preferences = handler.getPreferences();

        StringBuilder apiString = new StringBuilder();
        try (BufferedReader apiInput = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(R.raw.api)))) {
            String line;
            while((line = apiInput.readLine()) != null){
                apiString.append(line);
            }
            apiResource = new JSONObject(apiString.toString());
        } catch (Exception e){
            Logger.log(Logger.API, e);
        }

        JSONObject kodi = null;
        StringBuilder kodiString = new StringBuilder();
        try (BufferedReader apiInput = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(R.raw.kodi)))) {
            String line;
            while((line = apiInput.readLine()) != null){
                kodiString.append(line);
            }
            kodi = new JSONObject(kodiString.toString());
        } catch (Exception e){
            Logger.log(Logger.API, e);
        }

        List<String> googleData = getAPIValue("google", "key", "client_id", "iap_key");
        billingKey = googleData.get(2);
        googleAPI = new GoogleAPI(googleData);

        plexAPI = new PlexAPI();
        spotifyAPI = new SpotifyAPI(
                getAPIValue("spotify", "redirect", "client", "secret"));
        youtubeAPI = new YoutubeAPI(
                googleAPI.getAPIKey());
        thumbnailAPI = new ThumbnailAPI(context.getApplicationContext());
        omDbAPI = new OMDbAPI(
                getAPIValue("omdb", "key").get(0));
        gDbAPI = new GDbAPI();
        URIAPI = new URIAPI(context.getApplicationContext());
        barCodeAPI = new BarCodeAPI();
        twitchAPI = new TwitchAPI(
                getAPIValue("twitch", "redirect", "client"));
        kodiAPI = new KodiAPI(kodi);
        deviceAPI = new DeviceAPI(context.getApplicationContext());
        URLAPI = new URLAPI();
        pandoraAPI = new PandoraAPI();
        apiResource = null;
    }

    public PlexAPI getPlexAPI() {return plexAPI;}
    public SpotifyAPI getSpotifyAPI() {return spotifyAPI;}
    public YoutubeAPI getYoutubeAPI() {return youtubeAPI;}
    public ThumbnailAPI getThumbnailAPI() {return thumbnailAPI;}
    public OMDbAPI getOmDbAPI(){return omDbAPI;}
    public GDbAPI getgDbAPI() {return gDbAPI;}
    public URIAPI getURIAPI() {return URIAPI;}
    public BarCodeAPI getBarCodeAPI() {return barCodeAPI;}
    public TwitchAPI getTwitchAPI() {return twitchAPI;}
    public KodiAPI getKodiAPI() {return kodiAPI;}
    public DeviceAPI getDeviceAPI() {return deviceAPI;}
    public GoogleAPI getGoogleAPI() {return googleAPI;}
    public URLAPI getURLAPI() {return URLAPI;}
    public PandoraAPI getPandoraAPI() {return pandoraAPI;}

    public String getBillingKey() {return billingKey;}

    private List<String> getAPIValue(String source, String...keys) {
        try {
            List<String> results = new LinkedList<>();
            JSONObject object = apiResource.getJSONObject(source);
            for(String key : keys){
                results.add(object.getString(key)); }
            return results;
        } catch (Exception e) {
            Logger.log(Logger.API, e);
            return null;
        }
    }
}