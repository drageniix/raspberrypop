package com.drageniix.raspberrypop.servers.plex_servers;

import com.drageniix.raspberrypop.servers.ServerBase;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.Logger;
import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;

import java.util.Date;
import java.util.LinkedList;
import java.util.Map;

public class PlexServer extends ServerBase{
    private Map<String, String> clients;

    //From Database
    public PlexServer(DBHandler handler, String name, String serverID, String serverHost, String serverPort, String auth, String client) {
        setNeedsUpdating(false);
        setType(StreamingApplication.PLEX);
        this.handler = handler;
        this.chosenLibraries = new LinkedList<>();
        setName(name);
        setServerID(serverID);
        setServerHost(serverHost);
        setServerPort(Integer.parseInt(serverPort));
        setKey(auth);
        setOnline(true);
        setClient(client);
        updateServer(ServerBase.Category.STATUS, 3);
    }

    //From GDM Scan
    public PlexServer(DBHandler handler, String serverName, String serverID, String serverHost, String serverPort){
        setType(StreamingApplication.PLEX);
        this.handler = handler;
        this.chosenLibraries = new LinkedList<>();
        setName(serverName);
        setServerID(serverID);
        setServerHost(serverHost);
        setServerPort(Integer.parseInt(serverPort));
        setKey("");
        setOnline(true);
        setClient("");
        updateTime();
    }

    //Manual Addition
    PlexServer(DBHandler handler, String serverHost, String serverPort){
        setType(StreamingApplication.PLEX);
        this.handler = handler;
        this.chosenLibraries = new LinkedList<>();
        setName("Unknown Server");
        setServerID("");
        setServerHost(serverHost);
        setServerPort(Integer.parseInt(serverPort));
        setOnline(true);
        setClient("");
    }

    protected void doUpdate(Category param){
        try {
            switch (param) {
                case STATUS:
                    setOnline(handler.getParser().getPlexAPI().getInfo(PlexServer.this));
                    updateTime();
                    break;
                case CONTENT:
                case LIBRARY:
                    setNeedsUpdating(false);
                    break;
                case CLIENT:
                    setNeedsUpdating(false);
                    clients = handler.getParser().getPlexAPI().getPotentialClients(PlexServer.this);
                    break;
            }
        } catch (Exception e){
            setOnline(false);
            Logger.log(Logger.API, e);
        }
    }

    private void updateTime() {
        if (!isAuthenticated()){
            super.setUpdateTime(error + " Needs Authentication Key");
        } else {
            super.setUpdateTime("Connection Established at " + format(new Date()));
        }
    }

    @Override
    public boolean authenticate(String key){
        setKey(key);
        updateServer(Category.STATUS, 3);
        if (!isOnline()) {
            setOnline(true);
            setKey("");
            return false;
        } else {
            return !handler.getPlexServers().contains(this);
        }
    }

    @Override
    public boolean isAuthenticated(){
        return !getKey().isEmpty();
    }

    public Map<String, String> getClients(){
        updateServer(Category.CLIENT, 3);
        return clients;}

    @Override
    public int hashCode() {
        return 0;
    }
}