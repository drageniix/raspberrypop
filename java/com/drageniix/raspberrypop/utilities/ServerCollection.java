package com.drageniix.raspberrypop.utilities;

import com.drageniix.raspberrypop.servers.ServerBase;
import com.drageniix.raspberrypop.servers.kodi_servers.KodiServer;
import com.drageniix.raspberrypop.servers.plex_servers.PlexServer;

import java.util.LinkedHashSet;

class ServerCollection extends LinkedHashSet<ServerBase> {
    private LinkedHashSet<PlexServer> plexServers;
    private LinkedHashSet<KodiServer> kodiServers;

    ServerCollection(){
        plexServers = new LinkedHashSet<>();
        kodiServers = new LinkedHashSet<>();
    }

    LinkedHashSet<PlexServer> getPlexServers(){return plexServers;}
    LinkedHashSet<KodiServer> getKodiServers() {return kodiServers;}

    @Override
    public boolean add(ServerBase server){
        if (server == null) return false;
        if (server instanceof PlexServer) plexServers.add((PlexServer) server);
        else if (server instanceof KodiServer) kodiServers.add((KodiServer) server);
        return super.add(server);
    }

    @Override
    public boolean remove(Object server){
        if (server instanceof PlexServer) plexServers.remove(server);
        else if (server instanceof KodiServer) kodiServers.remove(server);
        return super.remove(server);
    }
}
