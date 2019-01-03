package com.drageniix.raspberrypop.fragments;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.servers.ServerBase;
import com.drageniix.raspberrypop.servers.ServerAdapter;
import com.drageniix.raspberrypop.servers.kodi_servers.KodiDialog;
import com.drageniix.raspberrypop.servers.plex_servers.PlexDialog;
import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;

public class ServerFragment extends BaseFragment {

    private static ServerFragment serverFragment;
    public static synchronized ServerFragment getsInstance() {
        if (serverFragment == null) serverFragment = new ServerFragment();
        return serverFragment;
    }

    private ServerAdapter sAdapter;
    private TextView empty;
    private ProgressBar loading;
    private boolean fabMenuOpen;
    private FloatingActionButton addFab, plexFab, kodiFab;

    public static void setEmpty(boolean on){
        if (serverFragment != null && serverFragment.empty != null) {
            if (!on && serverFragment.empty.getVisibility() == View.VISIBLE) {
                serverFragment.empty.setVisibility(View.GONE);
            } else if (on && serverFragment.empty.getVisibility() == View.GONE) {
                serverFragment.empty.setVisibility(View.VISIBLE);
            }
        }
    }


    public static void switchLoading(boolean on){
        if (serverFragment != null && serverFragment.loading != null) {
            if (on && serverFragment.loading.getVisibility() == View.GONE) {
                serverFragment.loading.setVisibility(View.VISIBLE);
            } else if (!on && serverFragment.loading.getVisibility() == View.VISIBLE) {
                serverFragment.loading.setVisibility(View.GONE);
            }
        }
    }

    public static boolean addOrUpdate(final ServerBase server){
        if (serverFragment != null && serverFragment.sAdapter != null){
            serverFragment.getBaseActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    serverFragment.sAdapter.addOrUpdate(server);
                }
            });
            return true;
        }
        return false;
    }

    public static boolean refresh(boolean database){
        if (serverFragment != null && serverFragment.sAdapter != null){
            serverFragment.sAdapter.refresh(database);
            return true;
        }
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup group, Bundle savedInstanceState) {
        setHandler();

        View view = inflater.inflate(R.layout.server_fragment, group, false);
        empty = view.findViewById(R.id.empty);
        loading = view.findViewById(R.id.progress);

        RecyclerView serverList = view.findViewById(R.id.serverRecycler);
        serverList.setHasFixedSize(false);
        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        lm.setOrientation(LinearLayoutManager.VERTICAL);
        serverList.setLayoutManager(lm);
        sAdapter = new ServerAdapter(getBaseActivity(), handler);
        serverList.setAdapter(sAdapter);

        empty.setVisibility(View.GONE);
        loading.setVisibility(View.VISIBLE);

        addFab = view.findViewById(R.id.addButton);
        plexFab = view.findViewById(R.id.addPlex);
        kodiFab = view.findViewById(R.id.addKodi);

        addFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!fabMenuOpen){
                    openFabMenu();
                } else {
                    closeFabMenu();
                }
            }
        });

        if (StreamingApplication.PLEX.isInstalled()) {
            plexFab.setImageDrawable(StreamingApplication.PLEX.getIcon());
            plexFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closeFabMenu();
                    PlexDialog.addServer(getBaseActivity(), getActivity().getSupportFragmentManager(), handler);
                }
            });
        } else {
            plexFab.setVisibility(View.GONE);
        }

        if (StreamingApplication.KODI.isInstalled()) {
            kodiFab.setImageDrawable(StreamingApplication.KODI.getIcon());
            kodiFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closeFabMenu();
                    KodiDialog.addServer(getBaseActivity(), getActivity().getSupportFragmentManager(), handler);
                }
            });
        } else {
            kodiFab.setVisibility(View.GONE);
        }

        return view;
    }

    private void openFabMenu(){
        fabMenuOpen = true;
        ViewCompat.animate(addFab).rotation(45.0F).withLayer().setDuration(300).setInterpolator(new OvershootInterpolator(10.0F)).start();
        if (StreamingApplication.PLEX.isInstalled()){
            plexFab.setClickable(true);
            plexFab.startAnimation(AnimationUtils.loadAnimation(getBaseActivity(), R.anim.fab_open));}
        if (StreamingApplication.KODI.isInstalled()){
            kodiFab.setClickable(true);
            kodiFab.startAnimation(AnimationUtils.loadAnimation(getBaseActivity(), R.anim.fab_open));}
    }

    private void closeFabMenu(){
        fabMenuOpen = false;
        ViewCompat.animate(addFab).rotation(0.0F).withLayer().setDuration(300).setInterpolator(new OvershootInterpolator(10.0F)).start();
        if (StreamingApplication.PLEX.isInstalled()){
            plexFab.setClickable(false);
            plexFab.startAnimation(AnimationUtils.loadAnimation(getBaseActivity(), R.anim.fab_close));}
        if (StreamingApplication.KODI.isInstalled()){
            kodiFab.setClickable(false);
            kodiFab.startAnimation(AnimationUtils.loadAnimation(getBaseActivity(), R.anim.fab_close));}
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.server_menu, menu);
        setTitle("Servers");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                getActivity().onBackPressed();
                break;
            case R.id.update:
                refresh(false);
                break;
            default:
                return super.onOptionsItemSelected(item);}
        return true;
    }
}