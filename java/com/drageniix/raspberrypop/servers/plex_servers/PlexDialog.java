package com.drageniix.raspberrypop.servers.plex_servers;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.fragments.ServerFragment;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.api.PlexAPI;

public class PlexDialog extends DialogFragment {

    public static void editServer(FragmentManager manager, DBHandler handler, PlexServer server) {
        newInstance(handler, server).show(manager, "edit_server");}

    public static void addServer(BaseActivity context, FragmentManager manager, DBHandler handler) {
        if (handler.getBilling().hasPremium() || handler.getSeversSize() < 1) {
            newInstance(handler, null).show(manager, "add_server");
        } else {
            context.advertisePremium(null);
        }
    }

    public static PlexDialog newInstance(DBHandler handler, PlexServer editableServer){
        PlexDialog dialog = new PlexDialog();
        dialog.handler = handler;
        dialog.server = editableServer;
        return dialog;
    }

    private PlexServer server;
    private DBHandler handler;

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceStare) {
        if (handler == null){
            dismiss();
            return new Dialog(getActivity());
        } else if (server == null){
            return initialize();
        } else if (!server.isAuthenticated()){
            return authenticate();
        } else {
            return editServerDetails();
        }
    }

    private Dialog initialize(){
        View dialogView = View.inflate(getContext(), R.layout.sever_popup_manual, null);

        final EditText ip = dialogView.findViewById(R.id.ip);
        final EditText port = dialogView.findViewById(R.id.port);
        final EditText user = dialogView.findViewById(R.id.user);
        final EditText pw = dialogView.findViewById(R.id.pw);
        final Switch advanced = dialogView.findViewById(R.id.advanced);
        final String token = handler.getPreferences().getPlexToken();

        ip.setHint(getString(R.string.plex_ip));
        port.setHint(getString(R.string.plex_port));
        pw.setHint(getString(R.string.generic_password));
        user.setHint(getString(R.string.plex_username));
        user.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        if (!token.isEmpty()){
            port.setVisibility(View.GONE);
            user.setVisibility(View.GONE);
            pw.setVisibility(View.GONE);

            advanced.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b) {
                        port.setVisibility(View.VISIBLE);
                        user.setVisibility(View.VISIBLE);
                        pw.setVisibility(View.VISIBLE);
                    } else {
                        port.setVisibility(View.GONE);
                        user.setVisibility(View.GONE);
                        pw.setVisibility(View.GONE);
                    }
                }
            });
        } else {
            advanced.setVisibility(View.GONE);
        }

        final AlertDialog.Builder serverDialog = new AlertDialog.Builder(getActivity());
        serverDialog.setView(dialogView);
        serverDialog.setPositiveButton(getString(R.string.server_add), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String username = user.getText().toString().trim();
                String password = pw.getText().toString().trim();

                String lPort = (port.getText().toString().isEmpty()) ?
                        "32400" : port.getText().toString().trim();

                server = new PlexServer(handler, ip.getText().toString().trim(), lPort);

                boolean success = false;
                if (!password.isEmpty() && !username.isEmpty()){
                    String token = PlexAPI.getTVToken(getContext(), username, password, false);
                    if (token != null) success = server.authenticate(token);
                } else if (!token.isEmpty()){
                    success = server.authenticate(token);
                } else {
                    Toast.makeText(getContext(), "Please enter a Plex.TV account.", Toast.LENGTH_LONG).show();
                    return;
                }

                if (!success){
                    Toast.makeText(getContext(), getString(R.string.server_failed_add), Toast.LENGTH_LONG).show();
                }
            }
        });
        serverDialog.setNegativeButton(getString(R.string.cancel), null);
        return serverDialog.create();
    }

    private Dialog editServerDetails(){
        View dialogView = View.inflate(getContext(), R.layout.sever_popup_manual, null);

        final EditText ip = dialogView.findViewById(R.id.ip);
        final EditText port = dialogView.findViewById(R.id.port);
        final EditText user = dialogView.findViewById(R.id.user);
        final EditText pw = dialogView.findViewById(R.id.pw);
        final Switch advanced = dialogView.findViewById(R.id.advanced);

        ip.setText(server.getServerHost());
        ip.setHint(getString(R.string.plex_username));

        port.setText(String.valueOf(server.getServerPort()));
        port.setHint(getString(R.string.plex_port));

        user.setVisibility(View.GONE);
        pw.setVisibility(View.GONE);
        advanced.setVisibility(View.GONE);

        final AlertDialog.Builder serverDialog = new AlertDialog.Builder(getActivity());
        serverDialog.setView(dialogView);
        serverDialog.setPositiveButton(getString(R.string.server_edit), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                int oldPort = server.getServerPort();
                String oldip = server.getServerHost();
                String lPort = (port.getText().toString().isEmpty()) ?
                        "32400" : port.getText().toString().trim();

                server.setServerPort(Integer.parseInt(lPort));
                server.setServerHost(ip.getText().toString().trim());
                if (server.authenticate(server.getKey())){
                    ServerFragment.addOrUpdate(server);
                } else {
                    server.setServerPort(oldPort);
                    server.setServerHost(oldip);
                    Toast.makeText(getContext(), getString(R.string.server_failed_edit), Toast.LENGTH_LONG).show();
                }
            }
        });
        serverDialog.setNegativeButton(getString(R.string.cancel), null);
        return serverDialog.create();
    }

    private Dialog authenticate(){
        View dialogView = View.inflate(getContext(), R.layout.sever_popup_manual, null);

        final EditText ip = dialogView.findViewById(R.id.ip);
        final EditText port = dialogView.findViewById(R.id.port);
        final EditText user = dialogView.findViewById(R.id.user);
        final EditText pw = dialogView.findViewById(R.id.pw);
        final Switch advanced = dialogView.findViewById(R.id.advanced);

        ip.setText(server.getServerHost());
        ip.setEnabled(false);

        port.setText(String.valueOf(server.getServerPort()));
        port.setEnabled(false);

        pw.setHint(getString(R.string.generic_password));

        user.setHint(getString(R.string.plex_username));
        user.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        advanced.setVisibility(View.GONE);

        AlertDialog.Builder serverDialog = new AlertDialog.Builder(getActivity());
        serverDialog.setView(dialogView);
        serverDialog.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String username = user.getText().toString().trim();
                String password = pw.getText().toString().trim();

                String token = PlexAPI.getTVToken(getContext(), username, password, false);
                if (token == null || !server.authenticate(token)) {
                    Toast.makeText(getContext(), getString(R.string.server_failed_auth), Toast.LENGTH_LONG).show();
                }
            }});

        serverDialog.setNegativeButton(getString(R.string.cancel), null);
        return serverDialog.create();
    }
}