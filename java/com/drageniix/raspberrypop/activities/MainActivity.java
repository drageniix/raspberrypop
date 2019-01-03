package com.drageniix.raspberrypop.activities;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.TextView;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.fragments.DatabaseFragment;
import com.drageniix.raspberrypop.fragments.ScannerFragment;
import com.drageniix.raspberrypop.fragments.ServerFragment;
import com.drageniix.raspberrypop.fragments.SettingsFragment;
import com.drageniix.raspberrypop.fragments.StatisticsFragment;
import com.drageniix.raspberrypop.fragments.SupportFragment;
import com.drageniix.raspberrypop.dialog.DatabaseDialog;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.media.MediaMetadata;
import com.drageniix.raspberrypop.utilities.Logger;
import com.drageniix.raspberrypop.utilities.categories.AuxiliaryApplication;
import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;

import java.util.UUID;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static MenuItem lastNavigation;
    private static boolean settingsNavigation;

    private int currentTheme;
    private boolean recreate;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private DrawerLayout drawerLayout;
    private NfcAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(currentTheme = handler.getPreferences().getTheme());
        super.setContentView(R.layout.base_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.activity_container);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);

        if (lastNavigation != null){
            updateMenu();
            onNavigationItemSelected(lastNavigation);
        } else try {
            String homePage = updateMenu();
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.activity_content, handler.getPreferences().openToBarcode() ?
                        ScannerFragment.getsInstance(homePage) : DatabaseFragment.getsInstance(homePage))
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
        } catch (Exception e){
            Logger.log(Logger.FRAG, e);
        }
    }

    public String updateMenu(){
        SubMenu collectionMenu = navigationView.getMenu().findItem(R.id.tags).getSubMenu();
        collectionMenu.clear();
        String defaultCollection = handler.getPreferences().getCollection(handler.getDefaultCollection());
        String homePage = handler.getDefaultCollection();
        for(String collection : handler.getCollections()){
            if (collection.equals(defaultCollection)){homePage = defaultCollection;}
            collectionMenu.add(collection).setIcon(R.drawable.ic_action_tag);
        }
        return homePage;
    }


    private void readDisplay(String uid, String display){
        if (display != null && !(uid.isEmpty()) && handler.readMedia(uid) == null) {
            if (handler.getBilling().canAddMedia()) {
                final Media result = new Media(handler, StreamingApplication.OTHER, uid, "New Tag", handler.getDefaultCollection(), uid,
                    false, "", "", new String[]{""},
                    new MediaMetadata()
                        .set(MediaMetadata.Type.INPUT_TITLE, uid)
                        .set(MediaMetadata.Type.TYPE, AuxiliaryApplication.SIMPLE_NOTE.name())
                        .set(MediaMetadata.Type.INPUT_CUSTOM, display), false);

                final View view = View.inflate(this, R.layout.scan_display, null);
                final TextView textView = view.findViewById(R.id.code_summary);
                textView.setText(display);

                new AlertDialog.Builder(this)
                        .setView(view)
                        .setTitle("NFC Tag Scanned")
                        .setPositiveButton(getString(R.string.submit), null)
                        .setNegativeButton(getString(R.string.reassign), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                DatabaseDialog.editMedia(
                                        getSupportFragmentManager(),
                                        handler, result);
                            }
                        }).show();
            } else {
                advertisePremium(null);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) != null) {
            String[] display = ScanActivity.scanTag(intent);
            boolean[] scanResult = ScanActivity.scan(display[0], this, handler);
            if (!scanResult[1]) {
                readDisplay(display[0], display[1]);
            }
        }

        Uri uri = intent.getData();
        if (uri != null && uri.getAuthority() != null) {
            if (uri.getAuthority().contains("spotify")) {
               handler.getParser().getSpotifyAPI().setAccessCode(uri);
            } else if (uri.getAuthority().contains("twitch")){
                handler.getParser().getTwitchAPI().setAccessCode(uri);
            } else if (uri.getAuthority().contains("google")){
                handler.getParser().getGoogleAPI().setAccessCode(uri);
            }
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        setTheme(currentTheme = handler.getPreferences().getTheme());
        drawerLayout.closeDrawer(Gravity.START, false);

        adapter = NfcAdapter.getDefaultAdapter(this);
        if (adapter != null && adapter.isEnabled()) {
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
            adapter.enableForegroundDispatch(this, pendingIntent, null, null);

            Intent intent = getIntent();
            if (intent.getStringExtra(ScanActivity.UID) != null && intent.getStringExtra(ScanActivity.CONTENTS) != null){
                readDisplay(intent.getStringExtra(ScanActivity.UID), intent.getStringExtra(ScanActivity.CONTENTS));
                intent.removeExtra(ScanActivity.UID);
                intent.removeExtra(ScanActivity.CONTENTS);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (adapter != null && adapter.isEnabled()) {
            adapter.disableForegroundDispatch(this);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        toggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        toggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return (toggle.onOptionsItemSelected(item)) || super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        drawerLayout.closeDrawers();

        if (item.getItemId() == R.id.nav_creation){
            DatabaseDialog.addMedia(this, getSupportFragmentManager(), handler, true, UUID.randomUUID().toString());
        } else {
            lastNavigation = item;
            if (recreate) {
                recreate = false;
                if (currentTheme != handler.getPreferences().getTheme()) {
                    recreate();
                    return true;
                }
            }

            Fragment fragment;
            switch (item.getItemId()) {
                case R.id.nav_servers:
                    BaseActivity.setCameraOpen(false);
                    fragment = ServerFragment.getsInstance();
                    break;
                case R.id.nav_appear:
                    BaseActivity.setCameraOpen(false);
                    if (settingsNavigation){
                        settingsNavigation = false;
                        fragment = new StatisticsFragment();
                    } else {
                        recreate = true;
                        fragment = new SettingsFragment();
                    }
                    break;
                case R.id.nav_help:
                    BaseActivity.setCameraOpen(false);
                    fragment = new SupportFragment();
                    break;
                default:
                    handler.getPreferences().setCollection(String.valueOf(item.getTitle()));
                    fragment = DatabaseFragment.getsInstance(String.valueOf(item.getTitle()));
                    break;
            }

            if (fragment != null) {
                try {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.activity_content, fragment)
                            .addToBackStack(null)
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .commit();
                } catch (Exception e) {
                    Logger.log(Logger.FRAG, e);
                }
            }
        }
        return true;
    }

    public boolean updateRecreate(){
        if (currentTheme != handler.getPreferences().getTheme()) {
            settingsNavigation = true;
            recreate();
            return true;
        }
        return false;
    }
}