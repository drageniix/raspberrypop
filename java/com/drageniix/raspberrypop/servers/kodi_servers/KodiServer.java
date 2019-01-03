package com.drageniix.raspberrypop.servers.kodi_servers;

import com.drageniix.raspberrypop.dialog.DatabaseDialog;
import com.drageniix.raspberrypop.servers.ServerBase;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.Logger;
import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


public class KodiServer extends ServerBase {
    //Manual Addition
    KodiServer(DBHandler handler, String serverHost, int serverPort){
        setType(StreamingApplication.KODI);
        this.handler = handler;
        this.chosenLibraries = new LinkedList<>();
        setServerPort(serverPort);
        setServerHost(serverHost);
        setClient("");
        setOnline(true);
    }

    //From Database
    public KodiServer(DBHandler handler, String name, String serverHost, String serverPort, String key, String libraries) {
        setNeedsUpdating(false);
        setType(StreamingApplication.KODI);
        this.handler = handler;
        if (!libraries.isEmpty()) {this.chosenLibraries = new LinkedList<>(Arrays.asList(libraries.split("~!~")));}
        else this.chosenLibraries = new LinkedList<>();
        setName(name);
        setServerPort(Integer.parseInt(serverPort));
        setServerHost(serverHost);
        setKey(key);
        setOnline(true);
        setClient("");
        updateServer(Category.STATUS, 2);
    }

    @Override
    public void setChosenLibraries(List<String> chosenLibraries) {
        super.setChosenLibraries(chosenLibraries);
        updateServer(Category.LIBRARY, 3);
    }

    @Override
    public void setServerHost(String serverHost) {
        setServerID(serverHost + "-" + getType().name());
        super.setServerHost(serverHost);
    }

    @Override
    public boolean authenticate(String key) {
        setKey(key);
        setNeedsUpdating(false);
        updateServer(Category.STATUS, 3);
        return isOnline() && !handler.getKodiServers().contains(this);
    }

    @Override
    public boolean isAuthenticated() {
        return getKey() != null;
    }

    @Override
    public void doUpdate(Category param) {
        try {
            switch (param) {
                case STATUS:
                    setOnline(handler.getParser().getKodiAPI().getInfo(this));
                    setUpdateTime(getChosenLibraries().isEmpty() ? error + " No Libraries Chosen" : "Connection Established at " + format(new Date()));
                    break;
                case LIBRARY:
                    DatabaseDialog.updateKodiOptions(handler);
                    setUpdateTime(getChosenLibraries().isEmpty() ? error + " No Libraries Chosen" : "Connection Established at " + format(new Date()));
                    break;
                case CLIENT:
                case CONTENT:
                    setNeedsUpdating(false);
                default:
                    break;
            }
        } catch (Exception e){
            setOnline(false);
            Logger.log(Logger.API, e);
        }

    }

    @Override
    public int hashCode() {
        return 1;
    }

}
