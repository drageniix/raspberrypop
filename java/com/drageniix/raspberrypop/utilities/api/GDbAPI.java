package com.drageniix.raspberrypop.utilities.api;

import com.drageniix.raspberrypop.media.MediaMetadata;
import com.drageniix.raspberrypop.utilities.Logger;

import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;

import com.drageniix.raspberrypop.utilities.custom.CaseInsensitiveMap;

public class GDbAPI extends APIBase {

    public CaseInsensitiveMap<String, MediaMetadata> getGame(String query){
        CaseInsensitiveMap<String, MediaMetadata> searchTitles = new CaseInsensitiveMap<>();
        String baseUrl = "http://thegamesdb.net/api/GetGame.php";
        String[] data = new String[]{
                "name", handler.getFileHelper().normalizeTitle(query)};

       try(StringReader in = getStringReader(getRequest(baseUrl, data, null))) {
           if (in != null) {
                parser.setInput(in);
                while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("Game")) {
                        String[] metadata = new String[5];
                        MediaMetadata resultMetadata = new MediaMetadata();
                        while (!((event = parser.next()) == XmlPullParser.END_TAG && parser.getName().equalsIgnoreCase("Game"))) {
                            if (event == XmlPullParser.START_TAG) {
                                String text;
                                switch (parser.getName()) {
                                    case "GameTitle":
                                        text = parser.nextText();
                                        resultMetadata.set(MediaMetadata.Type.TITLE, text);
                                        break;
                                    case "Platform":
                                        text = parser.nextText();
                                        metadata[0] = text;
                                        break;
                                    case "ReleaseDate":
                                        text = parser.nextText();
                                        metadata[1] = text.substring(text.lastIndexOf("/") + 1);
                                        break;
                                    case "Overview":
                                        text = parser.nextText();
                                        metadata[2] = text;
                                        break;
                                    case "Publisher":
                                        text = parser.nextText();
                                        metadata[3] = text;
                                        break;
                                    case "Developer":
                                        text = parser.nextText();
                                        metadata[4] = text;
                                        break;
                                    case "boxart":
                                        if (parser.getAttributeValue(null, "side").equals("front")) {
                                            text = parser.nextText();
                                            resultMetadata.set(MediaMetadata.Type.THUMBNAIL, "http://thegamesdb.net/banners/" + text);
                                        }
                                        break;
                                }
                            }
                        }

                        resultMetadata.set(MediaMetadata.Type.DETAIL,
                                (metadata[0] == null ? "" : metadata[0]) +
                                (metadata[1] == null ? "" : " - " + metadata[1]));
                        resultMetadata.set(MediaMetadata.Type.SUMMARY, (metadata[3] +
                                (metadata[4] == null ? "" : " - " + metadata[4]) +
                                (metadata[2] == null ? "" : "\n" + metadata[2])).trim());

                        searchTitles.put(resultMetadata.get(MediaMetadata.Type.TITLE) + " (" + metadata[1] +  ")", resultMetadata);
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
}
