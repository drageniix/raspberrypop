package com.drageniix.raspberrypop.utilities.api;

import android.os.AsyncTask;

import com.drageniix.raspberrypop.utilities.Logger;

import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;
import java.io.StringReader;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RokuAPI extends APIBase {

    public static boolean launchApp(String roku, String parameters, String appID) {
        int event;
        roku = "http://" + roku + ":8060";
        if (isInstalled(roku, appID)) {
            if (parameters.isEmpty()) {
                String url = roku + "/query/active-app";
                try (StringReader in = getStringReader(getRequest(url, null, null))) {
                    if (in != null) {
                        parser.setInput(in);
                        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                            if (event == XmlPullParser.START_TAG && parser.getName().equals("app")) {
                                if (parser.getAttributeValue(null, "id") == null || !parser.getAttributeValue(null, "id").equals(appID)) {
                                    url = roku + "/launch/" + appID;
                                    client.newCall(postRequest(url, null, null)).execute();
                                    Thread.sleep(6000);
                                    return true;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Logger.log(Logger.API, e);
                }
            } else {
                try{
                    String url = roku + "/launch/" + appID +  parameters;
                    client.newCall(postRequest(url, null, null)).execute();
                    return true;
                } catch (Exception e) {
                    Logger.log(Logger.API, e);
                }

            }

        }
        return false;
    }

    private static boolean isInstalled(String roku, String appID){
        int event;
        try (StringReader in = getStringReader(getRequest(roku + "/query/apps", null, null))) {
            if (in != null) {
                parser.setInput(in);
                while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG && parser.getName().equals("app")) {
                        if (parser.getAttributeValue(null, "id").equals(appID)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e){
            Logger.log(Logger.API, e);
        }
        return false;
    }

    private static class RokuScan extends AsyncTask<Void, Void, List<String[]>> {
        protected List<String[]> doInBackground(Void...param) {
            List<String[]> rokus = new ArrayList<>();
            try {
                DatagramSocket clientSocket = new DatagramSocket(1900);
                clientSocket.setBroadcast(true);
                String sendData = "M-SEARCH * HTTP/1.1\nHost: 239.255.255.250:1900\nMan: \"ssdp:discover\"\nST: roku:ecp\n";
                DatagramPacket sendPacket = new DatagramPacket(sendData.getBytes(), sendData.length(), InetAddress.getByName("239.255.255.250"), 1900);
                clientSocket.send(sendPacket);
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                clientSocket.setSoTimeout(2000);

                boolean listening = true;
                while (listening) {
                    try {
                        clientSocket.receive(receivePacket);
                        String data = new String(receivePacket.getData());
                        String address = data
                                .toLowerCase()
                                .split("location:")[1]
                                .split("\n")[0]
                                .trim();

                        rokus.add(new String[]{address, null});
                    } catch (SocketTimeoutException e) {
                        clientSocket.close();
                        listening = false;
                    }
                }
            } catch (IOException e) {
                Logger.log(Logger.CAST, e);
            }

            for (String[] roku : rokus) {
                roku[1] = getName(roku[0]);
                roku[0] = roku[0].substring(7, roku[0].lastIndexOf(":"));
            }

            return rokus;
        }

        private String getName(String roku){
            String name = "";
            int event;
            try (StringReader in = getStringReader(getRequest(roku, null, null))) {
                if (in != null) {
                    parser.setInput(in);
                    while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                        if (event == XmlPullParser.START_TAG && parser.getName().equals("friendlyName")) {
                            return parser.nextText();
                        }
                    }
                }
            } catch (Exception e){
                Logger.log(Logger.API, e);
            }
            return name;
        }

    }

    public static List<String[]> scanForAllRokus() {
        try {
            return new RokuScan().execute().get(5, TimeUnit.SECONDS);
        } catch (Exception e){
            Logger.log(Logger.API, e);
            return null;
        }
    }

}