package com.drageniix.raspberrypop.servers.kodi_servers;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.fragments.ServerFragment;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.api.KodiAPI;

import java.util.ArrayList;
import java.util.List;

public class KodiDialog extends DialogFragment {
    public static void editServer(FragmentManager manager, DBHandler handler, KodiServer server, boolean details) {
        newInstance(handler, server, details).show(manager, "edit_server");}

    public static void addServer(BaseActivity context, FragmentManager manager, DBHandler handler) {
        if (handler.getBilling().hasPremium() || handler.getSeversSize() < 1) {
            newInstance(handler, null, false).show(manager, "add_server");
        } else {
            context.advertisePremium(null);
        }
    }

    public static KodiDialog newInstance(DBHandler handler, KodiServer editableServer, boolean details){
        KodiDialog dialog = new KodiDialog();
        dialog.handler = handler;
        dialog.server = editableServer;
        dialog.details = details;
        return dialog;
    }

    private KodiServer server;
    private DBHandler handler;
    private boolean details;

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceStare) {
        if (handler == null){
            dismiss();
            return new Dialog(getActivity());
        } else if (server == null){
            return initialize();
        } else if (!details) {
            return chooseContent();
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

        ip.setHint(getString(R.string.kodi_ip));
        port.setHint(getString(R.string.kodi_port));
        user.setHint(getString(R.string.kodi_username));
        pw.setHint(getString(R.string.generic_password));

        user.setVisibility(View.GONE);
        pw.setVisibility(View.GONE);
        port.setVisibility(View.GONE);

        advanced.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b){
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

        final AlertDialog.Builder serverDialog = new AlertDialog.Builder(getActivity());
        serverDialog.setView(dialogView);
        serverDialog.setPositiveButton(getString(R.string.server_add), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String lPort = (port.getText().toString().trim().isEmpty()) ?
                        "8080" : port.getText().toString().trim();

                String ipString = (ip.getText().toString().trim().isEmpty() ?
                        "localhost" : ip.getText().toString().trim());

                server = new KodiServer(handler, ipString, Integer.parseInt(lPort));

                String username = user.getText().toString().trim();
                String password = pw.getText().toString().trim();

                if (username.isEmpty() && !password.isEmpty()){username = "kodi";}
                String key = (!username.isEmpty() && !password.isEmpty()) ?
                        "Basic " + Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP) : "";

                if (server.authenticate(key)){
                    chooseContent().show();
                } else {
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
        ip.setEnabled(false);


        port.setText(String.valueOf(server.getServerPort()));
        port.setHint(getString(R.string.kodi_port));

        user.setHint(getString(R.string.kodi_username));
        pw.setHint(getString(R.string.generic_password));

        user.setVisibility(View.GONE);
        pw.setVisibility(View.GONE);
        port.setVisibility(View.GONE);

        advanced.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b){
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

        final AlertDialog.Builder serverDialog = new AlertDialog.Builder(getActivity());
        serverDialog.setView(dialogView);
        serverDialog.setPositiveButton(getString(R.string.server_edit), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String lPort = (port.getText().toString().trim().isEmpty()) ?
                        "8080" : port.getText().toString().trim();

                String username = user.getText().toString().trim();
                String password = pw.getText().toString().trim();

                if (username.isEmpty() && !password.isEmpty()){username = "kodi";}
                String key = (!username.isEmpty() && !password.isEmpty()) ?
                        "Basic " + Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP) : "";

                int oldPort = server.getServerPort();
                server.setServerPort(Integer.parseInt(lPort));
                if (server.authenticate(key)){
                    ServerFragment.addOrUpdate(server);
                } else {
                    server.setServerPort(oldPort);
                    Toast.makeText(getContext(), getString(R.string.server_failed_edit), Toast.LENGTH_LONG).show();
                }
            }
        });
        serverDialog.setNegativeButton(getString(R.string.cancel), null);
        return serverDialog.create();
    }

    private Dialog chooseContent(){
        final List<String> preLibrary = new ArrayList<>();
        final String[] libraries = new String[KodiAPI.LibraryType.values().length];
        for (int i = 0; i < KodiAPI.LibraryType.values().length; i++){
            preLibrary.add(KodiAPI.LibraryType.values()[i].name());
            libraries[i] = KodiAPI.LibraryType.values()[i].getMenuName();}

        AlertDialog.Builder serverDialog = new AlertDialog.Builder(getActivity());
        final boolean[] chosenLibraries = new boolean[libraries.length];

        if (server.getChosenLibraries() != null && !server.getChosenLibraries().isEmpty()) {
            for (int i = 0; i < libraries.length; i++) {
                chosenLibraries[i] = server.getChosenLibraries().contains(preLibrary.get(i));
            }
        }

        serverDialog.setMultiChoiceItems(libraries, chosenLibraries, new DialogInterface.OnMultiChoiceClickListener() {
            @Override public void onClick(DialogInterface dialogInterface, int i, boolean b) {}});
        serverDialog.setTitle("Choose Libraries");
        serverDialog.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                List<String> finalChosen = new ArrayList<>();
                for(int j = 0; j < libraries.length; j++){
                    if (chosenLibraries[j]){
                        finalChosen.add(preLibrary.get(j));
                    }
                }
                server.setChosenLibraries(finalChosen);
            }
        });
        serverDialog.setNeutralButton("Select All", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                server.setChosenLibraries(preLibrary);
            }
        });

        return serverDialog.create();
    }

}
