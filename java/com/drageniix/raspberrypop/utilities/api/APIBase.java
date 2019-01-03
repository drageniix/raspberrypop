package com.drageniix.raspberrypop.utilities.api;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Looper;

import com.drageniix.raspberrypop.servers.ServerBase;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.Logger;
import com.drageniix.raspberrypop.utilities.Preferences;

import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class APIBase {
    static OkHttpClient client;
    static Preferences preferences;
    static XmlPullParser parser;
    static DBHandler handler;
    static int event;

    protected APIBase(){
        try {
            client = new OkHttpClient.Builder()
                .retryOnConnectionFailure(false)
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();

            parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        } catch (Exception e) {
            Logger.log(Logger.API, e);
        }
    }

    public static String getUrl(ServerBase server){
        return  "http://" + server.getServerHost() + ":" + server.getServerPort();
    }


    static boolean pingServer(ServerBase server) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(server.getServerHost(), server.getServerPort()), 150);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static Request postJSONRequest(String url, String data){
        try{
            MediaType JSON = MediaType.parse("application/json;charset=utf-8");
            return new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(JSON, data))
                    .build();
        } catch (Exception e) {
            Logger.log(Logger.API, e);
            return null;
        }
    }

    static Request postRequest(String url, String[] data, String[] headers){
        try {
            Request.Builder request = new Request.Builder();
            request.url(url);
            if (data != null) {
                FormBody.Builder requestBody = new FormBody.Builder();
                for (int i = 0; i < data.length - 1; i += 2) {
                    requestBody.add(data[i], data[i + 1]);
                }
                request.post(requestBody.build());
            } else {
                request.post(RequestBody.create(null, new byte[0]));
            }
            if (headers != null) {
                for (int i = 0; i < headers.length - 1; i += 2) {
                    request.addHeader(headers[i], headers[i + 1]);
                }
            }
            return request.build();
        } catch (Exception e) {
            Logger.log(Logger.API, e);
            return null;
        }
    }

    protected static Request getRequest(String url, String[] data, String[] headers){
        try {
            Request.Builder request = new Request.Builder();
            if (data != null) {
                HttpUrl.Builder builder = HttpUrl.parse(url).newBuilder();
                for(int i = 0; i < data.length-1; i+=2){
                    builder.addQueryParameter(data[i], data[i+1]);
                }
                request.url(builder.build());
            } else {
                request.url(url);
            }
            if (headers != null) {
                for (int i = 0; i < headers.length - 1; i += 2) {
                    request.addHeader(headers[i], headers[i + 1]);
                }
            }

            return request.build();
        } catch (Exception e) {
            Logger.log(Logger.API, e);
            return null;
        }
    }

    static String getString(Request request){
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? Looper.getMainLooper().isCurrentThread() : Thread.currentThread() == Looper.getMainLooper().getThread()){
                return new asyncString().execute(request).get(3, TimeUnit.SECONDS);
            } else {
                return getResponse(request).body().string();
            }
        } catch (Exception e) {
            return null;
        }
    }

    static InputStream getByteStream(Request request){
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? Looper.getMainLooper().isCurrentThread() : Thread.currentThread() == Looper.getMainLooper().getThread()){
                return new asyncStream().execute(request).get(3, TimeUnit.SECONDS);
            } else {
                return getResponse(request).body().byteStream();
            }
        } catch (Exception e) {
            return null;
        }
    }

    static StringReader getStringReader(Request request) {
        try {
            return new StringReader(getString(request));
        } catch (Exception e) {
            return null;
        }
    }

    static JSONObject getJSON(Request request) {
        try {
            return new JSONObject(getString(request));
        } catch (Exception e) {
            return null;
        }
    }

    static Response getResponse(Request request) throws Exception{
       return client.newCall(request).execute();
    }

    private static class asyncString extends AsyncTask<Request, Void, String> {
        protected String doInBackground(Request... request) {
            try {
                return getResponse(request[0]).body().string();
            } catch (Exception e) {
                return null;
            }
        }
    }

    private static class asyncStream extends AsyncTask<Request, Void, InputStream> {
        protected InputStream doInBackground(Request... request) {
            try {
                return getResponse(request[0]).body().byteStream();
            } catch (Exception e) {
                return null;
            }
        }
    }
}