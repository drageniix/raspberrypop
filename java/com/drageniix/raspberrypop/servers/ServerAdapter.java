package com.drageniix.raspberrypop.servers;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;

import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.fragments.ServerFragment;
import com.drageniix.raspberrypop.servers.kodi_servers.KodiDialog;
import com.drageniix.raspberrypop.servers.kodi_servers.KodiServer;
import com.drageniix.raspberrypop.servers.plex_servers.PlexServer;
import com.drageniix.raspberrypop.servers.plex_servers.PlexDialog;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.Logger;
import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ServerAdapter extends RecyclerView.Adapter<ServerAdapter.ServerHolder> {
    private List<ServerBase> servers;
    private int lastPosition;
    private BaseActivity context;
    private DBHandler handler;

    public ServerAdapter(BaseActivity context, DBHandler handler) {
        this.context = context;
        this.handler = handler;
        refresh(false);
    }

    public void refresh(boolean database) {
        lastPosition = -1;
        ServerFragment.switchLoading(true);
        ServerFragment.setEmpty(false);
        if (database) handler.getAllServers();
        servers = new ArrayList<>(handler.getServers());
        new GetServerInfo().execute();
    }

    public void addOrUpdate(ServerBase server){
        if (server.isAuthenticated()) {
            handler.addOrUpdateServer(server);
            if (!servers.contains(server)) {
                servers.add(server);
                int position = servers.indexOf(server);
                notifyItemInserted(position);
                ServerFragment.setEmpty(servers.isEmpty());
            } else {
                int position = servers.indexOf(server);
                notifyItemChanged(position);
            }
        }
    }

    private void remove(ServerBase server){
        handler.deleteServer(server);
        int position = servers.indexOf(server);
        lastPosition -= 1;
        servers.remove(server);
        notifyItemRemoved(position);
        ServerFragment.setEmpty(servers.isEmpty());
    }

    @Override
    public void onBindViewHolder(final ServerHolder holder, int position) {
        ServerBase initialServer = servers.get(position);

        holder.icon.setImageDrawable(initialServer.getType().isInstalled() ? initialServer.getType().getIcon() : context.getIcon(R.drawable.ic_action_warning, true));
        holder.name.setText(initialServer.getName());
        holder.address.setText(handler.getPreferences().debugDatabase() ? initialServer.toString() :
                initialServer.getServerHost() + "\n\n" +  initialServer.getUpdateTime());

        holder.setMenu(initialServer);

        position = holder.getAdapterPosition();
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left);
            holder.card.startAnimation(animation);
            lastPosition = position;
        }
    }

    @Override
    public ServerHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.server_list_item, parent, false);
        return new ServerHolder(v);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public int getItemCount() {
        return servers.size();
    }

    private class GetServerInfo extends AsyncTask<Void, ServerBase, List<ServerBase>> {
        protected List<ServerBase> doInBackground(Void... param) {
            for (ServerBase server : servers){
                if (server.getType().isInstalled()) {
                    server.doUpdate(ServerBase.Category.STATUS);
                    if (server.isOnline()){
                        server.doUpdate(ServerBase.Category.CONTENT);
                    }
                    publishProgress(server);
                } else if (server.isOnline()){
                    server.setOnline(false);
                    publishProgress(server);
                }
            }

            List<ServerBase> plexServers = new LinkedList<>();
            if (StreamingApplication.PLEX.isInstalled()) {
                try {
                    DatagramSocket socket = new DatagramSocket(32414);
                    socket.setBroadcast(true);
                    String data = "M-SEARCH * HTTP/1.1\r\n\r\n";
                    DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(), InetAddress.getByName("239.0.0.250"), 32414);
                    socket.send(packet);
                    byte[] buf = new byte[256];
                    packet = new DatagramPacket(buf, buf.length);
                    socket.setSoTimeout(1500);

                    boolean listening = true;
                    while (listening) {
                        try {
                            socket.receive(packet);
                            String packetData = new String(packet.getData());
                            if (packetData.contains("HTTP/1.0 200 OK")) {
                                String message = packetData.trim();
                                int nameSPos = message.indexOf("Name: ") + 6;
                                int nameEPos = message.indexOf("\r", nameSPos);

                                int portSPos = message.indexOf("Port: ") + 6;
                                int portEPos = message.indexOf("\r", portSPos);

                                int idSPos = message.indexOf("Resource-Identifier: ") + 21;
                                int idEPos = message.indexOf("\r", idSPos);

                                String serverName = message.substring(nameSPos, nameEPos);
                                String serverId = message.substring(idSPos, idEPos);
                                String ipAddress = packet.getAddress().toString().substring(1);
                                String port = message.substring(portSPos, portEPos);

                                boolean found = false;
                                for (PlexServer server : handler.getPlexServers()) {
                                    if (server.getServerHost().equals(ipAddress)) {
                                        found = true;
                                        break;
                                    }
                                }

                                if (!found) {
                                    plexServers.add(new PlexServer(handler, serverName, serverId, ipAddress, port));
                                }
                            }
                        } catch (SocketTimeoutException e) {
                            socket.close();
                            listening = false;
                        }
                    }
                } catch (IOException e) {
                    Logger.log(Logger.CAST, e);
                }
            }
            return plexServers;
        }

        @Override
        protected void onProgressUpdate(ServerBase...updatedServers) {
            super.onProgressUpdate(updatedServers);
            if (updatedServers != null){
                for(ServerBase server : updatedServers){
                    if (servers.contains(server)){
                        notifyItemChanged(servers.indexOf(server));
                    } else {
                        servers.add(server);
                        notifyItemInserted(servers.indexOf(server));
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(List<ServerBase> foundServers) {
            if (!foundServers.isEmpty()) {
                int librarySize = servers.size();
                String token = handler.getPreferences().getPlexToken();
                for (ServerBase server : foundServers) {
                    servers.add(server);
                    if (server instanceof PlexServer && !token.isEmpty()) {
                        server.authenticate(token);
                    }
                }
                notifyItemRangeInserted(librarySize, foundServers.size());
            }
            ServerFragment.setEmpty(servers.isEmpty());
            ServerFragment.switchLoading(false);
        }
    }

    class ServerHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView address;
        View card;
        ImageView icon;

        ServerHolder(View serverCard){
            super(serverCard);
            this.card = serverCard;
            name = itemView.findViewById(R.id.title);
            address = itemView.findViewById(R.id.description);
            icon = itemView.findViewById(R.id.icon);
        }

        void setMenu(ServerBase initialServer){
            if (initialServer.getType() == StreamingApplication.KODI) {
                final KodiServer server = (KodiServer) initialServer;
                card.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                    @Override
                    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                        menu.setHeaderTitle(server.getName() + "'s Settings");
                        if (server.isOnline()) {
                            menu.add("Choose Libraries").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem menuItem) {
                                    KodiDialog.editServer(context.getSupportFragmentManager(), handler, server, false);
                                    return false;
                                }
                            });
                        } else {
                            menu.add("Edit Server Details").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem menuItem) {
                                    KodiDialog.editServer(context.getSupportFragmentManager(), handler, server, true);
                                    return false;
                                }
                            });
                        }

                        menu.add("Delete Server").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem) {
                                AlertDialog.Builder confirmationDialog = new AlertDialog.Builder(context);
                                confirmationDialog.setMessage(context.getString(R.string.confirm));
                                confirmationDialog.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        remove(server);
                                    }
                                });
                                confirmationDialog.setNegativeButton(context.getString(R.string.cancel), null);
                                confirmationDialog.create().show();
                                return false;
                            }
                        });
                    }
                });

            } else if (initialServer.getType() == StreamingApplication.PLEX) {
                final PlexServer server = (PlexServer)initialServer;
                card.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                    @Override
                    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                        menu.setHeaderTitle(server.getName() + "'s Settings");
                        if (server.isOnline() && !server.isAuthenticated()) {
                            PlexDialog.editServer(context.getSupportFragmentManager(), handler, server);
                        } else if (server.isOnline() && server.isAuthenticated()) {
                            menu.add("Refresh Server").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem menuItem) {
                                    server.updateServer(ServerBase.Category.CONTENT, 3);
                                    address.setText(server.isOnline() ?
                                            (server.getServerHost() + "\n\n" + server.getUpdateTime()) :
                                            ("Offline"));
                                    return false;
                                }
                            });
                            menu.add("Choose Preferred Player").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem menuItem) {
                                    final Map<String, String> foundClients = server.getClients();
                                    if (foundClients != null && !foundClients.isEmpty()) {
                                        int selected = -1;
                                        final String[] clientIPs = new String[foundClients.size()];
                                        final String[] clientNames = new String[foundClients.size()];
                                        int i = 0;

                                        for (Map.Entry<String, String> pair : foundClients.entrySet()) {
                                            clientNames[i] = pair.getValue();
                                            clientIPs[i] = pair.getKey();
                                            if (server.getClient().equals(clientIPs[i])) {
                                                selected = i;
                                            }
                                        }

                                        final AlertDialog.Builder serverDialog = new AlertDialog.Builder(context);
                                        serverDialog.setSingleChoiceItems(clientNames, selected, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int checked) {
                                                server.setClient(clientIPs[checked]);
                                            }
                                        });

                                        serverDialog.setTitle("Choose a Player");
                                        serverDialog.setPositiveButton(context.getString(R.string.submit), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                addOrUpdate(server);
                                            }
                                        });
                                        serverDialog.setNegativeButton(context.getString(R.string.cancel), null);
                                        serverDialog.create().show();
                                    } else {
                                        Toast.makeText(context, "No players available.", Toast.LENGTH_SHORT).show();
                                    }
                                    return false;
                                }
                            });
                        } else if (!server.isOnline()){
                            menu.add("Edit Server Details").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem menuItem) {
                                    PlexDialog.editServer(context.getSupportFragmentManager(), handler, server);
                                    return false;
                                }
                            });
                        }

                        if (server.isAuthenticated()) {
                            menu.add("Delete Server").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem menuItem) {
                                    AlertDialog.Builder confirmationDialog = new AlertDialog.Builder(context);
                                    confirmationDialog.setMessage(context.getString(R.string.confirm));
                                    confirmationDialog.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            remove(server);
                                        }
                                    });
                                    confirmationDialog.setNegativeButton(context.getString(R.string.cancel), null);
                                    confirmationDialog.create().show();
                                    return false;
                                }
                            });
                        }
                    }
                });

            }
        }
    }
}


