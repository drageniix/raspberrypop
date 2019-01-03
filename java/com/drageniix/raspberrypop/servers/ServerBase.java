package com.drageniix.raspberrypop.servers;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Looper;

import com.drageniix.raspberrypop.fragments.ServerFragment;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.TimeManager;
import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class ServerBase {
    protected static String error = "⚠";
    protected StreamingApplication type;
    protected DBHandler handler;
    private boolean isOnline;
    private boolean needsUpdating = true;
    private String updateTime;
    private String name, serverHost, serverID, key, client;
    private int serverPort;
    protected List<String> chosenLibraries;

    public StreamingApplication getType() {return type;}
    public void setType(StreamingApplication type) {this.type = type;}
    protected void setNeedsUpdating(boolean needsUpdating) {this.needsUpdating = needsUpdating;}
    public void setServerHost(String serverHost) {this.serverHost = serverHost;}
    public String getServerHost() {
        return serverHost;
    }
    public void setServerPort(int serverPort) {this.serverPort = serverPort;}
    public int getServerPort() {
        return serverPort;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
    public void setKey(String key) {this.key = key;}
    public String getKey() {return key;}
    public String getServerID() {
        return serverID;
    }
    public void setServerID(String serverID) {
        this.serverID = serverID;
    }
    public String getClient() {return client;}
    protected void setClient(String client) {this.client = client;}
    public boolean isOnline(){return isOnline;}
    protected void setOnline(boolean online){this.isOnline = online;}
    public String getUpdateTime() {
        if (!isOnline()) {setUpdateTime("Offline");}
        return updateTime;}
    protected void setUpdateTime(String updateTime) {this.updateTime = updateTime;}
    protected String format(Date date){return handler.getTimeManager().format(TimeManager.BASIC, date);}

    public List<String> getChosenLibraries() {return chosenLibraries;}
    public void setChosenLibraries(List<String> chosenLibraries) {this.chosenLibraries = chosenLibraries;}
    public String getChosenLibrariesString(){
        StringBuilder libraries = new StringBuilder();
        if (getChosenLibraries() != null){
            for(String s : getChosenLibraries()){
                libraries.append(s);
                libraries.append("~!~");}}
        return libraries.toString();}

    public abstract boolean authenticate(String key);
    public abstract boolean isAuthenticated();
    protected abstract void doUpdate(Category param);

    protected void updateServer(Category search, int wait) {
        if (getType().isInstalled() && isOnline() & isAuthenticated()){
            if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? Looper.getMainLooper().isCurrentThread() : Thread.currentThread() == Looper.getMainLooper().getThread())){
                doUpdate(search);
                if (needsUpdating && !ServerFragment.addOrUpdate(this)){
                    handler.addOrUpdateServer(this);
                }
                setNeedsUpdating(true);
            } else if (wait > 0) {
                try {
                    new GetServerInfo(this).execute(search).get(wait, TimeUnit.SECONDS);
                } catch (Exception e) {
                    setOnline(false);
                }
            } else {
                new GetServerInfo(this).execute(search);
            }
        } else if (!getType().isInstalled()){
            setOnline(false);
        }
    }

    private static class GetServerInfo extends AsyncTask<Category, Void, Void> {
        ServerBase server;

        GetServerInfo(ServerBase server){
            this.server = server;
        }

        protected Void doInBackground(Category...param) {
            server.doUpdate(param[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (server.needsUpdating && !ServerFragment.addOrUpdate(server)){
                server.handler.addOrUpdateServer(server);
            }
            server.setNeedsUpdating(true);
        }
    }

    public enum Category{
        STATUS, CONTENT, LIBRARY, CLIENT
    }

    public String toString(){
        return (
            "{Host}\t\t\t\t" +
            getServerHost() +
            "\n{Port}\t\t\t\t" +
            getServerPort() +
            "\n{Name}\t\t\t\t" +
            getName() +
            "\n{Key}\t\t\t\t" +
            getKey() +
            "\n{ID}\t\t\t\t" +
            getServerID() +
            "\n{Client}\t\t\t" +
            getClient() +
            "\n{Status}\t\t\t" +
            (isOnline() ? "Online" : "Offline") +
            "\n{Updated}\t\t\t" +
            getUpdateTime() +
            "\n{Libraries}\t\t\t" +
            getChosenLibraries() +
            "\n{Authenticated}\t\t" +
            isAuthenticated());
    }

    @Override
    public boolean equals(Object object){
        if (!(object instanceof ServerBase)) return false;
        if (object == this) return true;
        ServerBase server = (ServerBase) object;
        return getServerID().equals(server.getServerID());
    }
}