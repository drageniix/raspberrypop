package com.drageniix.raspberrypop.utilities.api;

import com.drageniix.raspberrypop.media.MediaMetadata;
import com.drageniix.raspberrypop.utilities.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLAPI extends APIBase {

    private String getWebsiteIcon(String input){
        try {
            return getWebsiteIcon(input, Jsoup.parse(getString(getRequest(input, null, null))));
        } catch (Exception e){
            Logger.log(Logger.API, e);
        }
        return "";
    }

    private String getWebsiteIcon(String url, Document document) throws Exception{
        Elements icon = document.head().select("link[href~=.*\\.(ico|png)]");
        if (icon.isEmpty()){
            return "";
        } else {
            String ico = icon.first().attr("href");
            if (ico.startsWith("/") && !ico.startsWith("//")){
                URI uri = new URI(url);
                ico = url.substring(0, url.indexOf("://")) + "://" + uri.getAuthority() + ico;
                return ico;
            } else if (ico.startsWith("//")){
                ico = url.substring(0, url.indexOf("://")) + ":" + ico;
                return ico;
            } else {
                return ico;
            }
        }
    }

    public MediaMetadata openGraph(String input, String original){
        try {
            MediaMetadata metadata = new MediaMetadata();
            Document webpage = Jsoup.parse(getString(getRequest(input, null, null)));
            Elements meta = webpage.getElementsByTag("meta");
            for (Element element : meta) {
                if (element.attr("property").equalsIgnoreCase("og:title")) {
                    metadata.set(MediaMetadata.Type.TITLE, URLDecoder.decode(element.attr("content"), "UTF-8"));
                } else if (element.attr("property").equalsIgnoreCase("og:description")) {
                    metadata.set(MediaMetadata.Type.SUMMARY, element.attr("content"));
                } else if (element.attr("property").equalsIgnoreCase("og:image")) {
                    metadata.set(MediaMetadata.Type.THUMBNAIL, element.attr("content"));
                } else if (element.attr("property").equalsIgnoreCase("og:url") && metadata.get(MediaMetadata.Type.STREAMING).isEmpty()) {
                    metadata.set(MediaMetadata.Type.STREAMING, element.attr("content")
                            .replace("www.audible.com", "mobile.audible.com"));
                } else if (element.attr("property").equalsIgnoreCase("al:android:url")) {
                    metadata.set(MediaMetadata.Type.STREAMING, element.attr("content"));
                }
            }

            if (original.contains(".ups.com") && original.contains("trackNums=")){
                String tracking = original.substring(original.indexOf("trackNums=") + 10);
                if (tracking.contains("&")) tracking = tracking.substring(0, tracking.indexOf("&"));
                metadata.set(MediaMetadata.Type.TITLE, "UPS - " + tracking);
                metadata.set(MediaMetadata.Type.THUMBNAIL, "https://pbs.twimg.com/profile_images/892500227558481920/TDebaxs8_400x400.jpg");
            } else if (original.contains(".usps.com") && original.contains("tLabels=")) {
                String tracking = original.substring(original.indexOf("tLabels=") + 8);
                if (tracking.contains("&")) tracking = tracking.substring(0, tracking.indexOf("&"));
                metadata.set(MediaMetadata.Type.TITLE, "USPS - " + tracking);
                metadata.set(MediaMetadata.Type.THUMBNAIL, "https://pbs.twimg.com/profile_images/864100712883597313/a1fN6W7g_400x400.jpg");
            } else if (original.contains(".fedex.com") && original.contains("trackingnumber=")){
                String tracking = original.substring(original.indexOf("trackingnumber=") + 15);
                if (tracking.contains("&")) tracking = tracking.substring(0, tracking.indexOf("&"));
                metadata.set(MediaMetadata.Type.TITLE, "FedEX - " + tracking);
                metadata.set(MediaMetadata.Type.THUMBNAIL, "https://pbs.twimg.com/profile_images/948961065731203072/vKb47R4c_400x400.jpg");
            }

            if (metadata.get(MediaMetadata.Type.TITLE).isEmpty() && !webpage.title().isEmpty()) {
                metadata.set(MediaMetadata.Type.TITLE, webpage.title());}
            if (metadata.get(MediaMetadata.Type.THUMBNAIL).isEmpty()) {
                metadata.set(MediaMetadata.Type.THUMBNAIL, getWebsiteIcon(input, webpage));}
            if (metadata.get(MediaMetadata.Type.STREAMING).isEmpty()) {
                metadata.set(MediaMetadata.Type.STREAMING, original);}
            if (metadata.get(MediaMetadata.Type.SUMMARY).isEmpty()) {
                metadata.set(MediaMetadata.Type.SUMMARY, input);}
            return metadata;
        } catch (Exception e) {
            Logger.log(Logger.API, e);
        }

        MediaMetadata metadata = new MediaMetadata();
        metadata.set(MediaMetadata.Type.STREAMING, original);
        metadata.set(MediaMetadata.Type.TITLE, original);
        return null;
    }


    public MediaMetadata parsePocketCasts(String input) {
        if (input.contains("pca.st") || input.contains("pocketcasts.com")) {
            try {
                Pattern podcastPattern = Pattern.compile("pca.st/(.*?)(\\?|$|\\s)");
                Matcher podcastMatcher = podcastPattern.matcher(input);
                if (podcastMatcher.find()) {
                    String id = podcastMatcher.group(1);
                    MediaMetadata metadata = new MediaMetadata();
                    Document podcast = Jsoup.parse(getString(getRequest(input, null, null)));
                    Elements meta = podcast.getElementsByTag("meta");
                    for (Element element : meta) {
                        if (element.attr("property").equalsIgnoreCase("og:title")) {
                            metadata.set(MediaMetadata.Type.TITLE, element.attr("content"));
                        } else if (element.attr("property").equalsIgnoreCase("og:description")) {
                            metadata.set(MediaMetadata.Type.SUMMARY, element.attr("content"));
                        } else if (element.attr("property").equalsIgnoreCase("og:image")) {
                            metadata.set(MediaMetadata.Type.THUMBNAIL, element.attr("content"));
                        }
                    }
                    //extra details: podcast name and air date for episode
                    Element titles = podcast.getElementById("content").getElementsByClass("section").first();
                    if (!titles.getElementsByTag("h2").isEmpty()) {
                        metadata.set(MediaMetadata.Type.DETAIL, titles.getElementsByTag("h2").first().text());
                    } else {
                        metadata.set(MediaMetadata.Type.SUMMARY, titles.getElementById("episode_date").text() + "\n" + metadata.get(MediaMetadata.Type.SUMMARY));
                    }
                    metadata.set(MediaMetadata.Type.STREAMING, "pktc://www.pocketcasts.com/social/share/show/" + id);
                    return metadata;

                }

                Pattern listPattern = Pattern.compile("lists.pocketcasts.com/(.*?)(\\?|$|\\s)");
                Matcher listMatcher = listPattern.matcher(input);
                if (listMatcher.find()) {
                    String id = listMatcher.group(1);
                    JSONObject response = getJSON(getRequest(input + ".json", null, null));
                    if (response != null) {
                        MediaMetadata metadata = new MediaMetadata();
                        metadata.set(MediaMetadata.Type.TITLE, response.getString("title"));
                        metadata.set(MediaMetadata.Type.SUMMARY, response.getString("description"));
                        JSONArray responseArray = response.getJSONArray("podcasts");
                        if (responseArray != null && responseArray.length() > 0) {
                            metadata.set(MediaMetadata.Type.DETAIL, responseArray.length() + " Podcasts");
                            for (int i = 0; i < responseArray.length(); i++) {
                                JSONObject podcast = responseArray.getJSONObject(i);
                                metadata.set(MediaMetadata.Type.SUMMARY,
                                        metadata.get(MediaMetadata.Type.SUMMARY)
                                                + (i == 0 ? "\n\n" : "\n") +
                                                podcast.getString("title") + " (" + podcast.getString("author") + ")");
                            }
                        }
                        metadata.set(MediaMetadata.Type.STREAMING, "pktc://sharelist/lists.pocketcasts.com/" + id);
                        return metadata;
                    }
                }
            } catch (Exception e) {
                Logger.log(Logger.API, e);
            }
        }
        return null;
    }

    String getAmazonPrice(String asin){
        if (handler.getPreferences().debugAmazon()) {
            try {
                Document webpage = Jsoup.parse(getString(getRequest("https://www.amazon.com/dp/" + asin, null, null)));
                Elements prices = webpage.getElementsByClass("a-color-price");
                if (!prices.isEmpty()) {
                    return prices.get(0).text().trim();
                }
            } catch (Exception e) {
                Logger.log(Logger.API, e);
            }
        }
        return "";
    }

    public MediaMetadata parseAmazon(String input){
        MediaMetadata metadata = new MediaMetadata();
        Pattern pattern = Pattern.compile("amazon(.+)(\\?|&|$|\\s)");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            try {
                Elements meta, asin;
                Document webpage = Jsoup.parse(getString(getRequest(input, null, null)));
                String rawTitle = webpage.title();
                if (rawTitle != null && rawTitle.contains(":")) {
                    if (!(meta = webpage.select("span#pageData")).isEmpty() &&
                        !(asin = webpage.getElementsByAttribute("data-asin")).isEmpty()) {

                        String link = "http://www.amazon.com/piv-apk-play?asin=" + asin.first().attr("data-asin");
                        String title = rawTitle.substring(rawTitle.indexOf(":") + 1, rawTitle.lastIndexOf(":")).trim();
                        if (title.contains(":") && meta.first().attr("data-sub-page-type").equalsIgnoreCase("Movie")){
                            title = title.substring(0, title.lastIndexOf(":")).trim();
                        } else if (meta.first().attr("data-sub-page-type").equalsIgnoreCase("TVSeason")
                            && !(meta = webpage.select("div.season-single-dark")).isEmpty()){
                            String season = meta.first().text().trim();
                            if (title.endsWith(season)){
                                title = title.substring(0, title.length() - season.length()).trim();
                            }
                        }

                        if (!metadata.putAll(handler.getParser().getOmDbAPI().getOMDb(title, "t"))) {
                            metadata.set(MediaMetadata.Type.TITLE, title);
                            metadata.set(MediaMetadata.Type.THUMBNAIL, getWebsiteIcon(input));
                            metadata.set(MediaMetadata.Type.SUMMARY, link);
                        }

                        metadata.set(MediaMetadata.Type.STREAMING, link);
                        return metadata;
                    } else if (!(meta = webpage.select("span#productTitle")).isEmpty()) { //product
                        metadata.set(MediaMetadata.Type.TITLE, meta.first().text().trim());
                        if (!(meta = webpage.select("div#productDescription")).isEmpty()){
                            metadata.set(MediaMetadata.Type.SUMMARY, meta.first().text().trim());}
                        if (!(meta = webpage.getElementsByAttribute("data-a-dynamic-image")).isEmpty()) {
                            String[] images = meta.first().attr("data-a-dynamic-image").split("\"");
                            if (images.length > 1) {
                                metadata.set(MediaMetadata.Type.THUMBNAIL, images[1]);
                            }
                        }
                        metadata.set(MediaMetadata.Type.STREAMING, input);
                        return metadata;
                    }
                }
            }  catch(Exception e){
                Logger.log(Logger.API, e);
            }
        }
        return null;
    }
}
