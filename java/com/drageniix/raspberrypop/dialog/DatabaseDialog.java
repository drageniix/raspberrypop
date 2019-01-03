package com.drageniix.raspberrypop.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.activities.ListActivity;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.media.MediaMetadata;
import com.drageniix.raspberrypop.servers.kodi_servers.KodiServer;
import com.drageniix.raspberrypop.activities.NoteActivity;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.Logger;
import com.drageniix.raspberrypop.utilities.api.KodiAPI;
import com.drageniix.raspberrypop.utilities.api.ThumbnailAPI;
import com.drageniix.raspberrypop.utilities.categories.ApplicationCategory;
import com.drageniix.raspberrypop.utilities.categories.AuxiliaryApplication;
import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;
import com.drageniix.raspberrypop.utilities.custom.AutocompleteAdapter;
import com.drageniix.raspberrypop.utilities.custom.CaseInsensitiveMap;
import com.google.android.gms.actions.SearchIntents;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class DatabaseDialog extends DialogFragment {
    private static AutocompleteAdapter<String> alternateAdapter, searchAdapter;
    public static CaseInsensitiveMap<String, MediaMetadata> pandoraStations = new CaseInsensitiveMap<>();
    private static CaseInsensitiveMap<String, MediaMetadata> spotifyTitles = new CaseInsensitiveMap<>();
    private static CaseInsensitiveMap<String, MediaMetadata> twitchChannels = new CaseInsensitiveMap<>();
    private static CaseInsensitiveMap<String, MediaMetadata> installedApplications = new CaseInsensitiveMap<>();
    private static TreeMap<String, Drawable> installedApplicationsMenu = new TreeMap<>();
    private static CaseInsensitiveMap<String, MediaMetadata> searchTitles = new CaseInsensitiveMap<>();
    private static Set<String> kodiOptions = new LinkedHashSet<>();

    private static StreamingApplication enabled;
    private static AuxiliaryApplication enabledAuxiliary;

    private CaseInsensitiveMap<String, MediaMetadata> existingMediaMap = new CaseInsensitiveMap<>();
    private BaseActivity activity;
    private DBHandler handler;
    private String uid, cycle, name, inferredTitle, inferredCollection, eventDisplay;
    private boolean isQRCreation;
    private Media editMedia, existingMedia;
    private ImageButton streamSource, searchButton;
    private AutoCompleteTextView collectionName, titleSearch, taskerSearchA, taskerSearchB;
    private RadioGroup optionGroup;
    private Switch taskerSwitch;
    private PopupMenu popupMenu;
    private LinearLayout customIDHolder;
    private EditText customID, figureName;
    private AlertDialog sourceDialog;
    private MediaMetadata metadata = new MediaMetadata();
    private DatabaseDialogHelper dialogHelper;

    public static void addMedia(final BaseActivity context, FragmentManager manager, DBHandler handler, boolean qrCreation, String... info){
        if (handler.getBilling().canAddMedia()) {
            addDialog(context, manager, newInstance(null, handler, qrCreation, info));
        } else {
            context.advertisePremium(null);
        }
    }

    public static void editMedia(FragmentManager manager, DBHandler handler, Media m) {
        newInstance(m, handler, false, m.getScanUID()).show(manager, "update_media");}

    private static DatabaseDialog newInstance(Media editableMedia, DBHandler handler, boolean qrCreation, String... input){
        List<ApplicationCategory> allOptions = new LinkedList<>();

        for(StreamingApplication category : StreamingApplication.values()){
            if (category.isInstalled()
                    && !(category == StreamingApplication.PLEX && handler.getPlexServers().isEmpty())
                    && !(category == StreamingApplication.KODI && handler.getKodiServers().isEmpty())
                    && !(category == StreamingApplication.COPY && handler.getMediaList(handler.getDefaultCollection()).isEmpty()) //no media
                    && !(category == StreamingApplication.COPY && editableMedia != null && handler.getMediaList(handler.getDefaultCollection()).size() == 1)){ //only media
                allOptions.add(category);
            }
        }

        for(AuxiliaryApplication category : AuxiliaryApplication.values()){
            if (category.isInstalled()){
                allOptions.add(category);
            }
        }

        basicApplications.clear();
        for (StreamingApplication app : trueBasicApplications) {
            if (allOptions.contains(app)) {
                basicApplications.add(app);
            }
        }

        streamApplications.clear();
        for (StreamingApplication app : trueStreamApplications) {
            if (allOptions.contains(app)) {
                streamApplications.add(app);
            }
        }

        deviceApplications.clear();
        for (AuxiliaryApplication app : trueDeviceApplications) {
            if (allOptions.contains(app)) {
                deviceApplications.add(app);
            }
        }

        clockApplications.clear();
        for (AuxiliaryApplication app : trueClockApplications) {
            if (allOptions.contains(app)) {
                clockApplications.add(app);
            }
        }

        contactApplications.clear();
        for (AuxiliaryApplication app : trueContactApplications) {
            if (allOptions.contains(app)) {
                contactApplications.add(app);
            }
        }

        otherApplications.clear();
        for (AuxiliaryApplication app : trueOtherApplications) {
            if (allOptions.contains(app)) {
                otherApplications.add(app);
            }
        }

        DatabaseDialog.qrCreation = new LinkedHashMap<>();
        DatabaseDialog.qrCreation.put(AuxiliaryApplication.SIMPLE_NOTE, StreamingApplication.OTHER);
        DatabaseDialog.qrCreation.put(AuxiliaryApplication.LIST, StreamingApplication.OTHER);
        DatabaseDialog.qrCreation.put(AuxiliaryApplication.FORM, StreamingApplication.OTHER);
        DatabaseDialog.qrCreation.put(StreamingApplication.URI, null);
        DatabaseDialog.qrCreation.put(StreamingApplication.MAPS, null);
        DatabaseDialog.qrCreation.put(AuxiliaryApplication.VCARD, StreamingApplication.CONTACT);
        DatabaseDialog.qrCreation.put(AuxiliaryApplication.CALL, StreamingApplication.CONTACT);
        DatabaseDialog.qrCreation.put(AuxiliaryApplication.SMS, StreamingApplication.CONTACT);
        DatabaseDialog.qrCreation.put(AuxiliaryApplication.EMAIL, StreamingApplication.CONTACT);
        DatabaseDialog.qrCreation.put(AuxiliaryApplication.VEVENT, StreamingApplication.CLOCK);
        DatabaseDialog.qrCreation.put(AuxiliaryApplication.WIFI_CONNECTION, StreamingApplication.DEVICE);

        //Logged out, clear pre-filled data
        if (!spotifyTitles.isEmpty() && !handler.getPreferences().hasSpotify()){
            spotifyTitles.clear();}
        if (!twitchChannels.isEmpty() && !handler.getPreferences().hasTwitch()){
            twitchChannels.clear();}

        DatabaseDialog dialog = new DatabaseDialog();
        dialog.uid = input[0];
        dialog.inferredTitle = (input.length >= 2) ? input[1] : "";
        dialog.inferredCollection = (input.length) >= 3 ? input[2] : "";
        dialog.cycle = (input.length) >= 4 ? input[3] : input[0];
        dialog.name = (input.length) >= 5 ? input[4] : "";
        dialog.editMedia = editableMedia;
        dialog.handler = handler;
        dialog.isQRCreation = qrCreation;
        return dialog;
    }

    private static AlertDialog addSourceDialog;
    private static void addDialog(final BaseActivity activity, final FragmentManager manager, final DatabaseDialog dialog){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SourceAdapter sourceAdapter = new SourceAdapter(activity, dialog, manager, dialog.isQRCreation);
                GridView gridView = (GridView)View.inflate(activity, R.layout.media_popup_sourcegrid, null);
                gridView.setNumColumns(activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? 3 : 4);

                gridView.setAdapter(sourceAdapter);

                AlertDialog.Builder sourceDialogBuilder = new AlertDialog.Builder(activity);
                sourceDialogBuilder.setTitle(dialog.isQRCreation ? "Choose QR Source: " : "Choose Source: ");
                sourceDialogBuilder.setView(gridView);
                addSourceDialog = sourceDialogBuilder.create();
                addSourceDialog.show();
            }
        });
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (searchAdapter != null) searchAdapter.resetList();
        if (alternateAdapter != null) alternateAdapter.resetList();
        searchTitles.clear();
        enabled = null;
        enabledAuxiliary = null;
        super.onDismiss(dialog);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(final Bundle savedInstanceStare){
        if (handler == null){
            dismiss();
            return new Dialog(getActivity());
        }

        final View dialogView = View.inflate(getContext(), R.layout.media_popup, null);
        customID =  (EditText) View.inflate(getContext(), R.layout.media_popup_custom, null);
        activity = (BaseActivity) getActivity();

        streamSource = dialogView.findViewById(R.id.streamSource);
        searchButton = dialogView.findViewById(R.id.searchButton);
        taskerSwitch = dialogView.findViewById(R.id.advanced);
        titleSearch =  dialogView.findViewById(R.id.title);
        collectionName = dialogView.findViewById(R.id.collectionName);
        taskerSearchA = dialogView.findViewById(R.id.taskerSearchA);
        taskerSearchB = dialogView.findViewById(R.id.taskerSearchB);
        figureName = dialogView.findViewById(R.id.popName);
        customIDHolder = dialogView.findViewById(R.id.customHolder);
        optionGroup = dialogView.findViewById(R.id.radioGroup);

        dialogHelper = new DatabaseDialogHelper(activity, handler, editMedia, dialogView, customID);

        final SourceAdapter sourceAdapter = new SourceAdapter(activity, this, null, isQRCreation);
        streamSource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GridView gridView = (GridView)View.inflate(getContext(), R.layout.media_popup_sourcegrid, null);
                gridView.setNumColumns(activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? 3 : 4);

                gridView.setAdapter(sourceAdapter);

                AlertDialog.Builder sourceDialogBuilder = new AlertDialog.Builder(getContext());
                sourceDialogBuilder.setTitle(isQRCreation ? "Choose QR Source: " : "Choose Source: ");
                sourceDialogBuilder.setView(gridView);
                sourceDialog = sourceDialogBuilder.create();
                sourceDialog.show();
            }
            });


        if (handler.getBilling().hasPremium()) {
            ArrayAdapter<String> collectionAdapter = new AutocompleteAdapter<>(getContext(),new ArrayList<>(handler.getCollections()));
            collectionName.setAdapter(collectionAdapter);
        } else {
            collectionName.setVisibility(GONE);
        }

        searchAdapter = new AutocompleteAdapter<>(getContext(),new ArrayList<String>());
        alternateAdapter = new AutocompleteAdapter<>(getContext(),new ArrayList<String>());

        optionGroup.setOnCheckedChangeListener(getCheckListener());

        customID.setOnClickListener(new View.OnClickListener() {@Override public void onClick(View v) {
            switch (enabled) {
                case MAPS:
                    openIntent(BaseActivity.PLACE_REQUEST_CODE);
                    break;
                case LOCAL:
                    openIntent(BaseActivity.FILE_REQUEST_CODE);
                    break;
            }
        }});

        searchButton.setOnClickListener(new View.OnClickListener() {@Override public void onClick(View v) {
            switch (enabled) {
                case MAPS:
                    openIntent(BaseActivity.PLACE_REQUEST_CODE);
                    break;
                case CONTACT:
                    switch (enabledAuxiliary) {
                        case VIEW_CONTACT:
                            openIntent(BaseActivity.CONTACT_REQUEST_CODE);
                            break;
                        case VCARD:
                            //openIntent(BaseActivity.CONTACT_VCARD_REQUEST_CODE); <uses-permission android:name="android.permission.READ_CONTACTS" />
                            break;
                        case CALL:
                        case SMS:
                            openIntent(BaseActivity.CONTACT_NUMBER_REQUEST_CODE);
                            break;
                        case EMAIL:
                            openIntent(BaseActivity.CONTACT_EMAIL_REQUEST_CODE);
                            break;
                    }
                    break;
                case LAUNCH:
                    displayApps();
                    break;
                default:
                    search();
                    break;
            }
        }});

        titleSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = titleSearch.getText().toString().trim();
                switch (enabled) {
                    case COPY:
                        if (existingMediaMap.get(text) != null) {
                            existingMedia = handler.readMedia(existingMediaMap.get(text).get(MediaMetadata.Type.MEDIA_UID));
                            customID.setText(existingMedia.getScanUID());
                            dialogHelper.getTasker().loadTaskerFromMedia(existingMedia);
                        }
                        break;
                    case LAUNCH:
                        if (installedApplications.get(text) != null) {
                            MediaMetadata metadata = installedApplications.get(text);
                            Intent intent = new Intent().putExtra(SearchManager.QUERY, "");
                            if (!metadata.get(MediaMetadata.Type.PACKAGE_CLASS).isEmpty()) {
                                intent.setComponent(new ComponentName(metadata.get(MediaMetadata.Type.PACKAGE_NAME), metadata.get(MediaMetadata.Type.PACKAGE_CLASS)));
                            } else {
                                intent.setPackage(metadata.get(MediaMetadata.Type.PACKAGE_NAME));
                            }

                            boolean resolved = false;
                            if (!resolved) {
                                intent.setAction(Intent.ACTION_SEARCH);
                                resolved = intent.resolveActivity(activity.getPackageManager()) != null;
                                metadata.set(MediaMetadata.Type.PACKAGE_INTENT, Intent.ACTION_SEARCH);
                            }
                            if (!resolved) {
                                intent.setAction(SearchIntents.ACTION_SEARCH);
                                resolved = intent.resolveActivity(activity.getPackageManager()) != null;
                                metadata.set(MediaMetadata.Type.PACKAGE_INTENT, SearchIntents.ACTION_SEARCH);
                            }
                            if (!resolved) {
                                metadata.set(MediaMetadata.Type.PACKAGE_INTENT, "application");
                            }

                            if (resolved) {
                                customID.setVisibility(VISIBLE);
                            } else {
                                customID.setText("");
                                customID.setVisibility(GONE);
                            }
                        }
                        break;
                }
            }
        });

        titleSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))){
                    search(); return true;} else return false;}
        });

        AlertDialog.Builder mediaDialog = new AlertDialog.Builder(getActivity());
        mediaDialog.setView(dialogView);
        mediaDialog.setCancelable(false);
        if (editMedia != null) {
            enabled = editMedia.getEnabled();
            figureName.setText(editMedia.getFigureName());
            collectionName.setText(editMedia.getCollection());

        } else {
            collectionName.setText(!inferredCollection.isEmpty() ? inferredCollection : getDefault());
            figureName.setText(name);
        }


        mediaDialog.setPositiveButton(editMedia != null ?
                "Update Media" : "Add Tag",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        performClick();
                    }
                });

        chooseSource();
        if (titleSearch.isFocusableInTouchMode()) titleSearch.requestFocus();
        else figureName.requestFocus();

        return mediaDialog.create();
    }

    private void performClick(){
        if (editMedia != null){
            if (figureName.getText().toString().trim().isEmpty()){figureName.setText(String.valueOf("New Tag"));}
            if (collectionName.getText().toString().trim().isEmpty()){collectionName.setText(getDefault());}

            editMedia.update(enabled, figureName.getText().toString().trim(), collectionName.getText().toString().trim(),
                    taskerSwitch.isChecked(), taskerSearchA.getText().toString().trim(), taskerSearchB.getText().toString().trim(), dialogHelper.getTasker().getTaskerParams(),
                    matchTitle()
            );
            dismiss();
        } else {
            if (figureName.getText().toString().trim().isEmpty()){figureName.setText(String.valueOf("New Tag"));}
            if (collectionName.getText().toString().trim().isEmpty()){collectionName.setText(getDefault());}

            new Media(handler, enabled, uid, figureName.getText().toString().trim(), collectionName.getText().toString().trim(), cycle,
                    taskerSwitch.isChecked(), taskerSearchA.getText().toString().trim(), taskerSearchB.getText().toString().trim(), dialogHelper.getTasker().getTaskerParams(),
                    matchTitle(),
                    isQRCreation);
            dismiss();
        }
    }

    private void openIntent(int code) {
        if (figureName.getText().toString().trim().isEmpty()) {
            figureName.setText(String.valueOf("New Tag"));}
        if (collectionName.getText().toString().trim().isEmpty()) {
            collectionName.setText(getDefault());}

            boolean preserve = false;
        if (editMedia != null) {
            preserve = editMedia.getEnabled() == StreamingApplication.OTHER &&
                    (AuxiliaryApplication.valueOf(editMedia) == AuxiliaryApplication.SIMPLE_NOTE || AuxiliaryApplication.valueOf(editMedia) == AuxiliaryApplication.LIST);
            editMedia.update(enabled, figureName.getText().toString().trim(), collectionName.getText().toString().trim(),
                    taskerSwitch.isChecked(), taskerSearchA.getText().toString().trim(), taskerSearchB.getText().toString().trim(), dialogHelper.getTasker().getTaskerParams(), null);
        } else {
            editMedia = new Media(handler, enabled, uid, figureName.getText().toString().trim(), collectionName.getText().toString().trim(), cycle,
                    taskerSwitch.isChecked(), taskerSearchA.getText().toString().trim(), taskerSearchB.getText().toString().trim(), dialogHelper.getTasker().getTaskerParams(), null, isQRCreation);
        }

        if (enabledAuxiliary == AuxiliaryApplication.SIMPLE_NOTE || enabledAuxiliary == AuxiliaryApplication.LIST){
            if (!preserve){
                editMedia.clearMetadata();
                for (ThumbnailAPI.Type file : ThumbnailAPI.Type.values()) {
                    file.remove(editMedia);
                }
            }

            enabled.setMetadata(editMedia, matchTitle(), handler.getParser());
            String title = titleSearch.getText().toString().isEmpty() ? inferredTitle : titleSearch.getText().toString();
            editMedia.setTitle(title);
            editMedia.setInferredTitle(!title.isEmpty());

        } else if (enabled.isFolder()){
            editMedia.setTempDetails(new Object[]{enabledAuxiliary, customID.getText().toString().trim()});
        }

        ((BaseActivity) getActivity()).openIntent(code, editMedia);
        dismiss();
    }

    private void search(){
        InputMethodManager in = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        in.hideSoftInputFromWindow(titleSearch.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        if (titleSearch.getAdapter() == searchAdapter ) {
            String option = ((RadioButton)optionGroup.findViewById(optionGroup.getCheckedRadioButtonId())).getText().toString().toLowerCase();

            String title = titleSearch.getText().toString().trim();
            if (enabled == StreamingApplication.LOCAL && (editMedia == null || editMedia.getEnabled() != StreamingApplication.LOCAL)){
                openIntent(BaseActivity.FILE_REQUEST_CODE);
            } else if (!title.isEmpty()) {
                switch (enabled) {
                    case LOCAL:
                    case KODI:
                    case PLEX:
                    case GOOGLE:
                    case YOUTUBE:
                    case SPOTIFY:
                    case TWITCH:
                        enabled.search(handler, editMedia, searchTitles, title, option);
                        break;
                }
                searchAdapter.resetList();
                searchAdapter.addAll(searchTitles.keySet());
                searchAdapter.getFilter().filter("", titleSearch);
                titleSearch.showDropDown();
            }
        }
    }

    private RadioGroup.OnCheckedChangeListener getCheckListener(){
        return new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                searchAdapter.resetList();
                alternateAdapter.resetList();

                if (enabled.isFolder()){
                    deviceCheckListener(checkedId);
                    return;
                }

                if (checkedId == R.id.radio1){
                    switch (enabled) {
                        case URI:
                            titleSearch.setHint("Website Link");
                            break;
                    }
                } else if (checkedId == R.id.radio2){
                    switch (enabled) {
                        case SPOTIFY:
                            if (!spotifyTitles.isEmpty()){
                                searchAdapter.addAll(spotifyTitles.keySet());}
                            break;
                        case URI:
                            titleSearch.setHint("Application URI/URN");
                            alternateAdapter.addAll(searchTitles.keySet());
                            break;
                    }
                }
            }
        };
    }

    private void deviceCheckListener(int checkedId){
        if (checkedId == R.id.radio1){
            switch (enabledAuxiliary) {
                case WIFI_CONNECTION:
                    titleSearch.setAdapter(alternateAdapter);
                    if (!searchTitles.isEmpty()) {
                        alternateAdapter.clear();
                        alternateAdapter.addAll(searchTitles.keySet());
                    }
                    customID.setText("");
                    customID.setVisibility(GONE);
                    break;
            }
        } else if (checkedId == R.id.radio2){
            switch (enabledAuxiliary) {
                case WIFI_CONNECTION:
                    titleSearch.setAdapter(null);
                    customID.setText("");
                    customID.setVisibility(VISIBLE);
                    break;
            }
        } else if (checkedId == R.id.radio3){
            switch (enabledAuxiliary) {
                case WIFI_CONNECTION:
                    titleSearch.setAdapter(null);
                    customID.setText("");
                    customID.setVisibility(VISIBLE);
                    break;
            }
        }
    }

    private void chooseSource(){
        if (enabled.isFolder()){
            chooseAuxiliarySource();
            return;
        }

        enabledAuxiliary = null;
        titleSearch.setAdapter(searchAdapter);
        if (!inferredTitle.isEmpty() && titleSearch.getText().toString().isEmpty()) {
            titleSearch.setText(inferredTitle);}
        titleSearch.setHint(getString(R.string.media_title));
        streamSource.setImageDrawable(enabled.isInstalled() ? enabled.getIcon() : activity.getIcon(R.drawable.ic_action_warning, true));
        searchButton.setVisibility(VISIBLE);
        resetCustom();

        if (editMedia != null){
            if (!editMedia.getType().isEmpty() && editMedia.getEnabled() == enabled){
                if ((enabled == StreamingApplication.GOOGLE
                        && editMedia.getType().equalsIgnoreCase(getString(R.string.general_2)))
                        || ((enabled == StreamingApplication.YOUTUBE || enabled == StreamingApplication.SPOTIFY )
                        && editMedia.getType().equalsIgnoreCase("Playlist"))
                        || (enabled == StreamingApplication.URI
                        && editMedia.getType().equalsIgnoreCase(getContext().getString(R.string.uri_2)))){
                    optionGroup.check(R.id.radio2);
                } else if (enabled == StreamingApplication.YOUTUBE && editMedia.getType().equalsIgnoreCase("Channel")
                        || (enabled == StreamingApplication.SPOTIFY && editMedia.getType().equalsIgnoreCase("Album"))){
                    optionGroup.check(R.id.radio3);
                } else {
                    optionGroup.check(R.id.radio1);
                }
            } else {
                optionGroup.check(R.id.radio1);
            }

            if (enabled.equals(editMedia.getEnabled())){
                customID.setText(editMedia.getEnabled() == StreamingApplication.LOCAL ?
                        editMedia.getAlternateID() : editMedia.getStreamingID());
                titleSearch.setText(editMedia.getEnabled() == StreamingApplication.URI ?
                        editMedia.getStreamingID() : editMedia.getTitle());
            }
        } else {
            optionGroup.check(R.id.radio1);
        }

        switch(enabled){
            case KODI:
                if (kodiOptions.isEmpty()){updateKodiOptions(handler);}
                int optionIndex = 0;
                for(String option : kodiOptions) {
                    getOption(optionIndex).setVisibility(VISIBLE);
                    getOption(optionIndex).setText(option);
                    if (editMedia != null && editMedia.getEnabled() == enabled){
                        getOption(optionIndex).setChecked(editMedia.getType().equalsIgnoreCase(option));
                    }
                    optionIndex++;
                }
                break;
            case GOOGLE:
                getOption(0).setVisibility(VISIBLE);
                getOption(1).setVisibility(VISIBLE);
                getOption(0).setText(getContext().getString(R.string.general_1));
                getOption(1).setText(getContext().getString(R.string.general_2));
                customID.setHint(enabled.getName() + " Link/ID");
                break;
            case YOUTUBE:
                getOption(0).setVisibility(VISIBLE);
                getOption(0).setText(getContext().getString(R.string.youtube_1));
                getOption(1).setVisibility(VISIBLE);
                getOption(1).setText(getContext().getString(R.string.youtube_2));
                getOption(2).setVisibility(VISIBLE);
                getOption(2).setText(getContext().getString(R.string.youtube_3));
                break;
            case SPOTIFY:
                new AdapterPopulation(searchAdapter, spotifyTitles).execute();
                getOption(0).setVisibility(VISIBLE);
                getOption(0).setText(getContext().getString(R.string.spotify_1));
                getOption(1).setVisibility(VISIBLE);
                getOption(1).setText(getContext().getString(R.string.spotify_2));
                getOption(2).setVisibility(VISIBLE);
                getOption(2).setText(getContext().getString(R.string.spotify_3));
                break;
            case TWITCH:
                new AdapterPopulation(searchAdapter, twitchChannels).execute();
                titleSearch.setHint("Name of Channel");
                break;
            case PANDORA:
                searchButton.setVisibility(GONE);
                titleSearch.setHint("Station Search");
                new AdapterPopulation(alternateAdapter, pandoraStations).execute();
                titleSearch.setAdapter(alternateAdapter);
                break;
            case MAPS:
                titleSearch.setAdapter(null);
                if (editMedia == null || editMedia.getEnabled() != StreamingApplication.MAPS || editMedia.getAlternateID().isEmpty()) {
                    titleSearch.setHint("Touch to choose a location!");
                    titleSearch.setText("");
                    titleSearch.setFocusableInTouchMode(false);
                    titleSearch.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            openIntent(BaseActivity.PLACE_REQUEST_CODE);
                        }
                    });
                } else {
                    customID.setVisibility(VISIBLE);
                    customID.setText(editMedia.getAlternateID());
                    customID.setFocusableInTouchMode(false);
                }
                break;
            case URI:
                getOption(0).setVisibility(VISIBLE);
                getOption(0).setText(getContext().getString(R.string.uri_1));
                getOption(1).setVisibility(VISIBLE);
                getOption(1).setText(getContext().getString(R.string.uri_2));
                searchButton.setVisibility(GONE);
                new AdapterPopulation(alternateAdapter, searchTitles).execute();
                titleSearch.setAdapter(alternateAdapter);
                titleSearch.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS);
                break;
            case LAUNCH:
                searchButton.setVisibility(GONE);
                titleSearch.setHint("Name of Application");
                new AdapterPopulation(alternateAdapter, installedApplications).execute();
                titleSearch.setAdapter(alternateAdapter);
                customID.setHint(String.valueOf("optional: Search Query"));
                if (editMedia != null && editMedia.getEnabled() == StreamingApplication.LAUNCH && !editMedia.getAlternateID().isEmpty()){
                    customID.setText(editMedia.getAlternateID());
                    customID.setVisibility(VISIBLE);
                } else {
                    customID.setText("");
                    customID.setVisibility(GONE);
                }
                break;
            case LOCAL:
                if (editMedia == null || editMedia.getEnabled() != StreamingApplication.LOCAL || editMedia.getAlternateID().isEmpty()){
                    searchButton.setBackground(activity.getIcon(R.drawable.ic_action_file_search, false));
                    titleSearch.setText("");
                    titleSearch.setHint("Touch to choose a file!");
                    titleSearch.setFocusableInTouchMode(false);
                    titleSearch.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            openIntent(BaseActivity.FILE_REQUEST_CODE);
                        }
                    });
                } else {
                    customID.setVisibility(VISIBLE);
                    customID.setFocusableInTouchMode(false);
                    if (editMedia.getSummary().isEmpty()) search();
                }
                break;
            case COPY:
                searchButton.setVisibility(GONE);
                new AdapterPopulation(alternateAdapter, existingMediaMap).execute();
                titleSearch.setAdapter(alternateAdapter);
                titleSearch.setHint("Name of Tag to Copy");
                break;
            case OFF:
                searchButton.setVisibility(GONE);
                titleSearch.setAdapter(null);
                streamSource.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.blink));
                break;
            case PLEX:
            default:
                break;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class AdapterPopulation extends AsyncTask<String, Void, Void> {
        AutocompleteAdapter<String> adapter;
        CaseInsensitiveMap<String, MediaMetadata> map;
        StreamingApplication settingEnabled;

        AdapterPopulation(AutocompleteAdapter<String> adapter,CaseInsensitiveMap<String, MediaMetadata> map){
            this.map = map;
            this.adapter = adapter;
            this.settingEnabled = enabled;
        }

        protected Void doInBackground(String...details) {
            switch (enabled) {
                case TWITCH:
                    if (handler.getPreferences().hasTwitch() && map.isEmpty()){
                        map.putAll(handler.getParser().getTwitchAPI().getFollowedChannels());}
                    if (settingEnabled == enabled) adapter.addAll(map.keySet());
                    break;
                case SPOTIFY:
                    if (handler.getPreferences().hasSpotify() && map.isEmpty()) {
                        map.putAll(handler.getParser().getSpotifyAPI().loadSpotifyPlaylists());}
                    if (optionGroup.getCheckedRadioButtonId() == R.id.radio2 && settingEnabled == enabled){
                        adapter.addAll(map.keySet());}
                    break;
                case LAUNCH:
                    enabled.search(handler, editMedia, map, null, null);
                    if (settingEnabled == enabled) {
                        adapter.addAll(map.keySet());
                        if (installedApplicationsMenu.isEmpty()) {
                            try {
                                PackageManager pm = getContext().getPackageManager();
                                ApplicationInfo ai;
                                for (Map.Entry<String, MediaMetadata> application : installedApplications.entrySet()) {
                                    ai = pm.getApplicationInfo(application.getValue().get(MediaMetadata.Type.PACKAGE_NAME), PackageManager.GET_META_DATA);
                                    installedApplicationsMenu.put(application.getKey(), pm.getApplicationIcon(ai));
                                }
                            } catch (Exception e) {
                                Logger.log(Logger.FRAG, e);
                            }
                        }
                    }
                    break;
                case URI:
                case COPY:
                    enabled.search(handler, editMedia, map, null, null);
                    if ((enabled != StreamingApplication.URI || optionGroup.getCheckedRadioButtonId() == R.id.radio2)
                            && (settingEnabled == enabled)){
                        adapter.addAll(map.keySet());}
                    break;
                case PANDORA:
                    if (map.isEmpty() && handler.getPreferences().hasPandora()){
                        enabled.search(handler, editMedia, map, null, null);}
                    if (settingEnabled == enabled) adapter.addAll(map.keySet());
                    break;
                case DEVICE:
                    enabled.search(handler, editMedia, map, null, enabledAuxiliary.name());
                    adapter.addAll(map.keySet());
                    break;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            titleSearch.dismissDropDown();
            if (enabled == StreamingApplication.LAUNCH && settingEnabled == enabled){
                searchButton.setVisibility(VISIBLE);
            }
        }
    }

    private void chooseAuxiliarySource(){
        titleSearch.setAdapter(null);
        titleSearch.setText(inferredTitle);
        searchButton.setVisibility(GONE);
        resetCustom();

        if (editMedia != null && editMedia.getEnabled().equals(enabled) && (enabledAuxiliary == null || enabledAuxiliary == AuxiliaryApplication.valueOf(editMedia))) {
            enabledAuxiliary = AuxiliaryApplication.valueOf(editMedia);
            if (editMedia.getAlternateID().equalsIgnoreCase(getContext().getString(R.string.network_2))){
                optionGroup.check(R.id.radio2);
            } else if (editMedia.getAlternateID().equalsIgnoreCase(getContext().getString(R.string.network_3))){
                optionGroup.check(R.id.radio3);
            } else {
                optionGroup.check(R.id.radio1);
            }

            titleSearch.setText(editMedia.getTitle());

            switch (enabledAuxiliary){
                case WIFI_CONNECTION:
                    if (optionGroup.getCheckedRadioButtonId() != R.id.radio1) {
                        customID.setText(editMedia.getStreamingID().split("~!~", -1)[1]);}
                    break;
                case SIMPLE_NOTE:
                case LIST:
                    customID.setText(editMedia.getSummary());
                    break;
                case COUNTER:
                    customID.setText(editMedia.getStreamingID());
                    titleSearch.setText(editMedia.getAlternateID());
                    break;
                case SMS:
                case EMAIL:
                    customID.setText(editMedia.getAlternateID());
                    break;
                default:
                    customID.setText(editMedia.getStreamingID());
                    break;

            }
        } else {
            optionGroup.check(R.id.radio1);
            switch (enabledAuxiliary){
                case VEVENT:
                    customID.setText("Title: \nDescription: \nLocation: \nStart: \nEnd: ");
                    break;
                case VCARD:
                    customID.setText("Name: \nOrganization: \nTitle: \nPhone: \nEmail: \nAddress: \nNotes: ");
                    break;
            }
        }

        titleSearch.setHint("Touch to set " + enabledAuxiliary.getName());
        streamSource.setImageDrawable(enabledAuxiliary.getIcon());

        switch (enabledAuxiliary){
            case WIFI_CONNECTION:
                getOption(0).setVisibility(VISIBLE);
                getOption(0).setText(getContext().getString(R.string.network_1));
                getOption(1).setVisibility(VISIBLE);
                getOption(1).setText(getContext().getString(R.string.network_2));
                getOption(2).setVisibility(VISIBLE);
                getOption(2).setText(getContext().getString(R.string.network_3));
                new AdapterPopulation(alternateAdapter, searchTitles).execute();
                titleSearch.setHint("Network SSID");
                customID.setHint("Network Password");
                break;
            case WIFI:
            case BLUETOOTH:
            case FLASHLIGHT:
            case ORIENTATION:
                createSettingToggle();
                titleSearch.setFocusableInTouchMode(false);
                titleSearch.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        createSettingToggle();
                    }
                });
                break;
            case BRIGHTNESS:
                setBrightness();
                titleSearch.setFocusableInTouchMode(false);
                titleSearch.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setBrightness();
                    }
                });
                break;
            case VOLUME:
                setVolume();
                titleSearch.setFocusableInTouchMode(false);
                titleSearch.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setVolume();
                    }
                });
                break;
            case FORM:
                setForm();
                titleSearch.setFocusableInTouchMode(false);
                titleSearch.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setForm();
                    }
                });
                break;
            case DICE:
                titleSearch.setVisibility(GONE);
                setCustomID(dialogHelper.getDice().createDialog(customID.getText().toString()));
                break;
            case SIMPLE_NOTE:
                titleSearch.setHint("Title of Note");
                customID.setVisibility(VISIBLE);
                customID.setHint("Click to take a note.");
                customID.setFocusableInTouchMode(false);
                customID.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openIntent(BaseActivity.OVERLAY_REQUEST_CODE);
                    }
                });
                break;

            case LIST:
                titleSearch.setHint("Title of List");
                customID.setVisibility(VISIBLE);
                customID.setHint("Click to make a list");
                customID.setFocusableInTouchMode(false);
                customID.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openIntent(BaseActivity.OVERLAY_REQUEST_CODE);
                    }
                });
                break;
            case COUNTER:
                titleSearch.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                titleSearch.setHint("Set Initial Value (0)");
                customID.setVisibility(VISIBLE);
                customID.setHint("Set Equation (x + 1)");
                customID.setFocusableInTouchMode(false);
                customID.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DatabaseDialogHelper.getCalculator().setCalculator(activity, customID);
                    }
                });
                break;
            case VIEW_CONTACT:
                searchButton.setVisibility(VISIBLE);
                titleSearch.setFocusableInTouchMode(false);
                titleSearch.setHint("Touch to Choose Contact");
                titleSearch.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openIntent(BaseActivity.CONTACT_REQUEST_CODE);
                    }
                });
                break;
            case CALL:
                searchButton.setVisibility(VISIBLE);
                titleSearch.setInputType(InputType.TYPE_CLASS_PHONE);
                titleSearch.setHint("Phone Number");
                break;
            case SMS:
                customID.setVisibility(VISIBLE);
                customID.setHint("SMS Message (Optional)");
                searchButton.setVisibility(VISIBLE);
                titleSearch.setInputType(InputType.TYPE_CLASS_PHONE);
                titleSearch.setHint("Phone Number (Optional)");
                break;
            case EMAIL:
                customID.setVisibility(VISIBLE);
                customID.setHint("Email Body (Optional)");
                searchButton.setVisibility(VISIBLE);
                titleSearch.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                titleSearch.setHint("Email Address (Optional)");
                break;
            case VCARD:
                titleSearch.setFocusableInTouchMode(false);
                titleSearch.setHint("Touch to Select Details");
                titleSearch.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setContact();
                    }
                });
                break;
            case TIMER:
                titleSearch.setHint("Set Timer Length");
                titleSearch.setFocusableInTouchMode(false);
                titleSearch.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setTimer();
                    }
                });
                break;
            case STOPWATCH:
                titleSearch.setHint("Set Stopwatch Title");
                customID.setText(String.valueOf("0L"));
                break;
            case ALARM:
            case SCAN_ALARM:
                titleSearch.setHint("Alarm Title");
                customID.setVisibility(VISIBLE);
                customID.setHint("Set Alarm Time");
                customID.setFocusableInTouchMode(false);
                customID.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setTimer();
                    }
                });
                break;
            case COUNTDOWN:
                titleSearch.setHint("Set Countdown Date");
                titleSearch.setFocusableInTouchMode(false);
                titleSearch.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setTimer();
                    }
                });
                break;
            case VEVENT:
                eventDisplay = null;
                titleSearch.setHint("Set Event Details");
                titleSearch.setFocusableInTouchMode(false);
                titleSearch.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setTimer();
                    }
                });
                break;
        }
    }

    private void createSettingToggle() {
        getOption(0).setText(getContext().getString(R.string.on));
        getOption(1).setText(getContext().getString(R.string.off));
        if (getDialog() != null && getDialog().isShowing()) {
            final String[] toggleOptions = new String[]{"Enable", "Disable"};
            new AlertDialog.Builder(getActivity())
                    .setTitle(enabledAuxiliary.getName())
                    .setSingleChoiceItems(toggleOptions, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            titleSearch.setText(String.valueOf(enabledAuxiliary.getName() + " (" + toggleOptions[which] + "d)"));
                            switch (which) {
                                case 0:
                                    optionGroup.check(R.id.radio1);
                                    customID.setText(String.valueOf(true));
                                    return;
                                case 1:
                                    optionGroup.check(R.id.radio2);
                                    customID.setText(String.valueOf(false));
                                    return;
                                default:
                                    break;
                            }
                        }
                    })
                    .setPositiveButton(getString(R.string.submit), null)
                    .create()
                    .show();
        }
    }

    private void displayApps(){
        if (popupMenu == null && !installedApplicationsMenu.isEmpty()) {
            try {
                popupMenu = new PopupMenu(getContext(), titleSearch);
                for(Map.Entry<String, Drawable> application : installedApplicationsMenu.entrySet()){
                    popupMenu.getMenu().add(application.getKey()).setIcon(application.getValue());
                }

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        titleSearch.setText(item.getTitle());
                        titleSearch.dismissDropDown();
                        return false;
                    }
                });

                Field fMenuHelper = PopupMenu.class.getDeclaredField("mPopup");
                fMenuHelper.setAccessible(true);
                Object menuHelper = fMenuHelper.get(popupMenu);
                Class[] argTypes = new Class[]{boolean.class};
                menuHelper.getClass().getDeclaredMethod("setForceShowIcon", argTypes).invoke(menuHelper, true);
            } catch (Exception e) {
                Logger.log(Logger.FRAG, e);
            }
        }

        if (popupMenu != null) {
            titleSearch.dismissDropDown();
            popupMenu.show();
        }
    }

    private void setBrightness() {
        if (getDialog() != null && getDialog().isShowing()) {
            int progress = editMedia != null && editMedia.getEnabled() == StreamingApplication.DEVICE && enabledAuxiliary == AuxiliaryApplication.valueOf(editMedia) ?
                    Integer.parseInt(editMedia.getStreamingID()) : 0;

            new AlertDialog.Builder(getActivity())
                    .setTitle(enabledAuxiliary.getName())
                    .setView(dialogHelper.getBrightness().setBrightness(enabledAuxiliary, progress))
                    .setPositiveButton(getString(R.string.submit), null)
                    .create()
                    .show();
        }
    }

    private void setVolume(){
        if (getDialog() != null && getDialog().isShowing()) {
            int[] oldValues = new int[8];
            boolean[] oldChecks = new boolean[8];
            if ((editMedia != null && editMedia.getEnabled() == StreamingApplication.DEVICE && enabledAuxiliary == AuxiliaryApplication.valueOf(editMedia))
                    || (titleSearch.getText().toString().endsWith(enabledAuxiliary.getName() + "(s) Set"))){
                String[] oldData = customID.getText().toString().split("~!~", -1);
                for (int i = 0; i < 7; i++){
                    oldValues[i] = (int)(100 * Float.parseFloat(oldData[i]));
                    oldChecks[i] = oldValues[i] != -100;
                }
                oldValues[7] = (int)Float.parseFloat(oldData[7]);
                oldChecks[7] = oldValues[7] != -1;
            }

            new AlertDialog.Builder(getActivity())
                    .setTitle(enabledAuxiliary.getName())
                    .setView(dialogHelper.getVolume().setVolume(oldValues, oldChecks))
                    .setPositiveButton(getString(R.string.submit), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialogHelper.getVolume().getResults(enabledAuxiliary);
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .create()
                    .show();
        }
    }

    private void setTimer(){
        if (getDialog() != null && getDialog().isShowing()) {
            if (enabledAuxiliary == AuxiliaryApplication.TIMER || enabledAuxiliary == AuxiliaryApplication.ALARM) {
                int oldHours = 0, oldMinutes = 0;
                if (editMedia != null && editMedia.getEnabled() == StreamingApplication.CLOCK && !editMedia.getStreamingID().isEmpty()
                        && enabledAuxiliary.name().equalsIgnoreCase(editMedia.getType())) {
                    oldHours = (Integer.parseInt(editMedia.getStreamingID())) / 60 / 60;
                    oldMinutes = (Integer.parseInt(editMedia.getStreamingID()) - (oldHours * 60 * 60)) / 60;
                }
                dialogHelper.getEvent().setTimer(oldHours, oldMinutes, enabledAuxiliary);
            } else if (enabledAuxiliary == AuxiliaryApplication.SCAN_ALARM) {
                int oldHours = 0, oldMinutes = 0;
                if (editMedia != null && editMedia.getEnabled() == StreamingApplication.CLOCK && !editMedia.getStreamingID().isEmpty()
                        && enabledAuxiliary.name().equalsIgnoreCase(editMedia.getType())) {
                    Calendar calendar = Event.parseTime(handler, editMedia.getStreamingID());
                    oldHours = calendar.get(Calendar.HOUR_OF_DAY);
                    oldMinutes = calendar.get(Calendar.MINUTE);
                }
                dialogHelper.getEvent().setTimer(oldHours, oldMinutes, enabledAuxiliary);
            } else if (enabledAuxiliary == AuxiliaryApplication.VEVENT){
                new AlertDialog.Builder(getActivity())
                        .setView(dialogHelper.getEvent().setEvent(editMedia != null && editMedia.getEnabled() == StreamingApplication.CLOCK && !editMedia.getStreamingID().isEmpty()&& enabledAuxiliary.name().equalsIgnoreCase(editMedia.getType())
                                ? editMedia : null))
                        .setPositiveButton(getActivity().getString(R.string.submit), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                eventDisplay = dialogHelper.getEvent().getEventResults();
                            }
                        })
                        .setNegativeButton(getActivity().getString(R.string.cancel), null)
                        .create()
                        .show();
            } else if (enabledAuxiliary == AuxiliaryApplication.COUNTDOWN){
              dialogHelper.getEvent().setCountdown();
            }
        }
    }

    private void setContact(){
        if (getDialog() != null && getDialog().isShowing()) {
            final VCard vCard = new VCard(activity);
            if (editMedia != null && editMedia.getEnabled() == StreamingApplication.CONTACT && !editMedia.getStreamingID().isEmpty()
                    && enabledAuxiliary == AuxiliaryApplication.valueOf(editMedia)) {
                vCard.loadMedia(activity, editMedia);
            }

            new AlertDialog.Builder(getActivity())
                    .setView(vCard.getDialog())
                    .setPositiveButton(getActivity().getString(R.string.submit), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            titleSearch.setText(vCard.getTitle());
                            if (titleSearch.getText().toString().isEmpty()) {
                                titleSearch.setText(String.valueOf("New Contact"));
                            }
                            customID.setText(vCard.getData());
                        }
                    })
                    .setNegativeButton(getActivity().getString(R.string.cancel), null)
                    .create()
                    .show();
        }
    }

    private void setForm(){
        if (getDialog() != null && getDialog().isShowing()) {
            final Form form = new Form(true, activity, handler, titleSearch.getText().toString(), customID.getText().toString());
            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setView(form.getDialog())
                    .setPositiveButton(getActivity().getString(R.string.submit), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            titleSearch.setText(form.getTitle());
                            customID.setText(form.getData());
                        }
                    })
                    .setNegativeButton(getActivity().getString(R.string.cancel), null)
                    .create();
            dialog.show();
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE| WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }

    private boolean basicMatch(){
        if (editMedia != null && editMedia.getEnabled() == enabled && enabledAuxiliary != null) {
            return enabledAuxiliary.name().equals(editMedia.getType());
        } else {
            return editMedia != null && editMedia.getEnabled() == enabled && enabledAuxiliary == null;
        }
    }

    private MediaMetadata matchTitle() {
        String title, option, custom;

        if (enabled.isFolder()){
            matchAuxiliaryCustom();
            title = metadata.get(MediaMetadata.Type.INPUT_TITLE);
            option = metadata.get(MediaMetadata.Type.INPUT_OPTION);
            custom = metadata.get(MediaMetadata.Type.INPUT_CUSTOM);
        } else {
            title = titleSearch.getText().toString().trim();
            option = ((RadioButton)optionGroup.findViewById(optionGroup.getCheckedRadioButtonId())).getText().toString().toLowerCase();
            custom = customID.getText().toString().trim();
            metadata.set(MediaMetadata.Type.INPUT_TITLE, title)
                    .set(MediaMetadata.Type.INPUT_OPTION, option)
                    .set(MediaMetadata.Type.INPUT_CUSTOM, custom);
        }

        if (basicMatch()
                //title match
                && ((editMedia.getEnabled() == StreamingApplication.URI && editMedia.getStreamingID().equals(title))
                || (enabledAuxiliary == AuxiliaryApplication.COUNTER && editMedia.getAlternateID().equals(title))
                || (editMedia.getTitle().equalsIgnoreCase(title)))
                //custom match
                && (((editMedia.getEnabled() == StreamingApplication.LOCAL || editMedia.getEnabled() == StreamingApplication.MAPS || editMedia.getEnabled() == StreamingApplication.LAUNCH || editMedia.getEnabled() == StreamingApplication.DEVICE || enabledAuxiliary == AuxiliaryApplication.SMS || enabledAuxiliary == AuxiliaryApplication.EMAIL) && editMedia.getAlternateID().equals(custom))
                || (editMedia.getStreamingID().equals(custom)))
                //option match
                && (option.isEmpty() || editMedia.getType().equalsIgnoreCase(option)
                || (enabled == StreamingApplication.DEVICE && editMedia.getAlternateID().equalsIgnoreCase(option)))){
            return null;}

        switch(enabled){
            case PLEX:
            case KODI:
            case GOOGLE:
            case YOUTUBE:
                metadata.putAll(searchTitles.get(title));
                break;
            case TWITCH:
                if (!metadata.putAll(twitchChannels.get(title))){
                    metadata.putAll(searchTitles.get(title));}
                break;
            case LOCAL:
                metadata.putAll(searchTitles.get(title));
                if (!(editMedia == null || editMedia.getEnabled() != StreamingApplication.LOCAL)){
                    metadata.set(MediaMetadata.Type.MEDIA_ALTERNATE, editMedia.getAlternateID())
                            .set(MediaMetadata.Type.MEDIA_TYPE, editMedia.getType())
                            .set(MediaMetadata.Type.MEDIA_AUXILIARY, editMedia.getAuxiliaryString())
                            .set(MediaMetadata.Type.MEDIA_STREAMING, editMedia.getStreamingID());
                }
                break;
            case SPOTIFY:
                if (!metadata.putAll(spotifyTitles.get(title))) {
                    metadata.putAll(searchTitles.get(title));}
                break;
            case LAUNCH:
                metadata.putAll(installedApplications.get(title));
                break;
            case PANDORA:
                metadata.putAll(pandoraStations.get(title));
                break;
        }

        if (editMedia != null
                && AuxiliaryApplication.valueOf(editMedia) == AuxiliaryApplication.SCAN_ALARM
                && editMedia.getDetail().equals("Active")) {
            AuxiliaryApplication.SCAN_ALARM.scan(activity, handler, editMedia, null);
        }

        return metadata;
    }

    private void matchAuxiliaryCustom(){
        String title = titleSearch.getText().toString().trim();
        String option = ((RadioButton)optionGroup.findViewById(optionGroup.getCheckedRadioButtonId())).getText().toString().toLowerCase();
        String custom = customID.getText().toString().trim();

        switch (enabledAuxiliary){
            case WIFI_CONNECTION:
                if (option.equalsIgnoreCase(getString(R.string.network_1)) && searchTitles.get(titleSearch.getText().toString()) != null){
                    custom = searchTitles.get(titleSearch.getText().toString()).get(MediaMetadata.Type.INPUT_CUSTOM);
                }
                custom = title + "~!~" + custom;
                break;
            case DICE:
                dialogHelper.getDice().setResults();
                title = titleSearch.getText().toString().trim();
                custom = customID.getText().toString().trim();
                break;
            case VEVENT:
                metadata.set(MediaMetadata.Type.SUMMARY, eventDisplay);
                break;
            case LIST:
            case SIMPLE_NOTE:
                custom = custom.isEmpty() ? custom : editMedia.getStreamingID();
                break;
        }

        metadata.set(MediaMetadata.Type.TYPE, enabledAuxiliary.name());
        metadata.set(MediaMetadata.Type.INPUT_TITLE, title)
                .set(MediaMetadata.Type.INPUT_OPTION, option)
                .set(MediaMetadata.Type.INPUT_CUSTOM, custom);
    }

    private String getDefault(){
        return handler.getPreferences().getCollection(handler.getDefaultCollection());
    }

    private void resetCustom(){
        titleSearch.setVisibility(VISIBLE);
        titleSearch.setOnClickListener(null);
        titleSearch.setFocusableInTouchMode(true);
        titleSearch.setInputType(InputType.TYPE_CLASS_TEXT);

        setCustomID(customID);
        customID.setVisibility(GONE);
        customID.setFocusableInTouchMode(true);
        customID.setInputType(InputType.TYPE_CLASS_TEXT);
        customID.setText("");

        optionGroup.clearCheck();
        for(int i = 0; i < optionGroup.getChildCount(); i++){
            optionGroup.getChildAt(i).setVisibility(GONE);
            getOption(i).setText("");
        }

        searchTitles.clear();
        streamSource.clearAnimation();
        searchButton.setBackground(activity.getIcon(R.drawable.ic_action_search, false));

        existingMedia = null;
    }

    private void setCustomID(View view){
        customIDHolder.removeAllViews();
        customIDHolder.addView(view);
        view.requestLayout();
        customIDHolder.requestLayout();
    }

    private RadioButton getOption(int index){
        return ((RadioButton)optionGroup.getChildAt(index));
    }

    public static void updateKodiOptions(DBHandler handler){
        kodiOptions.clear();
        for(KodiServer server : handler.getKodiServers()){
            for(String library : server.getChosenLibraries()) {
                kodiOptions.add(KodiAPI.LibraryType.valueOf(library).getOptionName());
            }
        }
    }

    private static class SourceAdapter extends BaseAdapter {
        DatabaseDialog dialog;
        FragmentManager manager;
        BaseActivity activity;
        boolean initialAdd;
        ApplicationCategory[] basicOptions, streamOptions, deviceOptions, contactOptions, otherOptions, clockOptions, qrCreationOptions;
        ApplicationCategory[] options;

        SourceAdapter(BaseActivity activity, DatabaseDialog dialog, FragmentManager manager, boolean creation){
            this.activity = activity;
            this.dialog = dialog;
            this.manager = manager;
            this.basicOptions = basicApplications.toArray(new StreamingApplication[basicApplications.size()]);
            this.streamOptions = streamApplications.toArray(new StreamingApplication[streamApplications.size()]);
            this.deviceOptions = deviceApplications.toArray(new AuxiliaryApplication[deviceApplications.size()]);
            this.otherOptions = otherApplications.toArray(new AuxiliaryApplication[otherApplications.size()]);
            this.contactOptions = contactApplications.toArray(new AuxiliaryApplication[contactApplications.size()]);
            this.clockOptions = clockApplications.toArray(new AuxiliaryApplication[clockApplications.size()]);
            this.qrCreationOptions = qrCreation.keySet().toArray(new ApplicationCategory[qrCreation.size()]);
            this.options = creation ? qrCreationOptions : basicOptions;
            this.initialAdd = manager != null;
        }

        @Override
        public int getCount() {
            return options == qrCreationOptions ? options.length : options.length + 1;
        }

        @Override
        public Object getItem(int position) {return options == qrCreationOptions ? options[position] : options[position - 1];}

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null){
                SourceHolder holderTemp = new SourceHolder();
                convertView = View.inflate(activity, R.layout.media_popup_source, null);
                holderTemp.imageView = convertView.findViewById(R.id.sourceImage);
                holderTemp.textView = convertView.findViewById(R.id.sourceTitle);
                convertView.setTag(holderTemp);
            }

            final SourceHolder holder = (SourceHolder)convertView.getTag();
            holder.position = position;

            if (options == qrCreationOptions){
                final ApplicationCategory current = (ApplicationCategory) getItem(position);
                holder.imageView.setImageDrawable(current.getIcon());
                holder.textView.setText(current.getName());
                holder.imageView.clearColorFilter();

                if (enabled == current || enabledAuxiliary == current) {
                    holder.textView.setShadowLayer(12, 0, 0, activity.getResourceColor(R.color.highlight));
                } else {
                    holder.textView.setShadowLayer(0, 0, 0, 0);
                }

                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ApplicationCategory oldEnabled = enabled, oldEnabledAuxiiary = enabledAuxiliary;
                        if (current instanceof AuxiliaryApplication) {
                            enabled = (StreamingApplication) qrCreation.get(current);
                            enabledAuxiliary = (AuxiliaryApplication) current;
                        } else {
                            enabled = (StreamingApplication) current;
                            enabledAuxiliary = null;
                        }

                        if (!initialAdd) {
                            if (oldEnabled != enabled && oldEnabledAuxiiary != enabledAuxiliary) {
                                dialog.chooseSource();
                            }
                            dialog.sourceDialog.dismiss();
                        } else {
                            dialog.show(manager, "add_media");
                            addSourceDialog.dismiss();
                        }
                    }
                });
            } else {
                if (position == 0) {
                    holder.imageView.setImageDrawable(activity.getIcon(options == basicOptions ? R.drawable.ic_action_playlist : R.drawable.ic_action_back, true));
                    holder.textView.setText(options == basicOptions ? "Apps" : "Back");
                    holder.textView.setShadowLayer(0, 0, 0, 0);
                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            options = options == basicOptions ? streamOptions : basicOptions;
                            notifyDataSetChanged();
                        }
                    });
                } else if (options == basicOptions && ((StreamingApplication) getItem(position)).isFolder()) {
                    final ApplicationCategory current = (ApplicationCategory) getItem(position);
                    holder.imageView.setImageDrawable(current.getIcon());
                    holder.textView.setText(current.getName());

                    if (enabled == current) {
                        holder.textView.setShadowLayer(12, 0, 0, activity.getResourceColor(R.color.highlight));
                    } else {
                        holder.textView.setShadowLayer(0, 0, 0, 0);
                    }

                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int positionTemp = position;
                            positionTemp -= 1;
                            if (positionTemp == basicApplications.indexOf(StreamingApplication.DEVICE)) {
                                options = deviceOptions;
                            } else if (positionTemp == basicApplications.indexOf(StreamingApplication.CLOCK)) {
                                options = clockOptions;
                            } else if (positionTemp == basicApplications.indexOf(StreamingApplication.CONTACT)) {
                                options = contactOptions;
                            } else if (positionTemp == basicApplications.indexOf(StreamingApplication.OTHER)) {
                                options = otherOptions;
                            }
                            notifyDataSetChanged();
                        }
                    });
                } else {
                    final ApplicationCategory current = (ApplicationCategory) getItem(position);
                    holder.imageView.setImageDrawable(current.getIcon());
                    holder.textView.setText(current.getName());
                    holder.imageView.clearColorFilter();

                    if (enabled == current || enabledAuxiliary == current) {
                        holder.textView.setShadowLayer(12, 0, 0, activity.getResourceColor(R.color.highlight));
                    } else {
                        holder.textView.setShadowLayer(0, 0, 0, 0);
                    }

                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (options != basicOptions) {
                                if (options == deviceOptions) {
                                    enabled = StreamingApplication.DEVICE;
                                } else if (options == otherOptions) {
                                    enabled = StreamingApplication.OTHER;
                                } else if (options == contactOptions) {
                                    enabled = StreamingApplication.CONTACT;
                                } else if (options == clockOptions) {
                                    enabled = StreamingApplication.CLOCK;
                                }

                                if (!initialAdd) {
                                    if (current instanceof AuxiliaryApplication && enabledAuxiliary != current) {
                                        enabledAuxiliary = (AuxiliaryApplication) current;
                                        dialog.chooseAuxiliarySource();
                                    } else if (current instanceof StreamingApplication && enabled != current) {
                                        enabled = (StreamingApplication) current;
                                        enabledAuxiliary = null;
                                        dialog.chooseSource();
                                    }
                                    dialog.sourceDialog.dismiss();
                                } else {
                                    if (current instanceof AuxiliaryApplication) {
                                        enabledAuxiliary = (AuxiliaryApplication) current;
                                    } else {
                                        enabled = (StreamingApplication) current;
                                        enabledAuxiliary = null;
                                    }
                                    dialog.show(manager, "add_media");
                                    addSourceDialog.dismiss();
                                }
                            } else {
                                if (!initialAdd) {
                                    if (enabled != current) {
                                        enabled = (StreamingApplication) current;
                                        dialog.chooseSource();
                                    }
                                    dialog.sourceDialog.dismiss();
                                } else {
                                    enabled = (StreamingApplication) current;
                                    dialog.show(manager, "add_media");
                                    addSourceDialog.dismiss();
                                }
                            }
                        }
                    });
                }
            }

            return convertView;
        }
    }

    private static class SourceHolder {
        ImageView imageView;
        TextView textView;
        int position;
    }

    private static LinkedHashMap<ApplicationCategory, ApplicationCategory> qrCreation;

    private static final List<StreamingApplication>
            basicApplications = new LinkedList<>(),
            trueBasicApplications = Arrays.asList(
                    StreamingApplication.DEVICE,
                    StreamingApplication.LOCAL,
                    StreamingApplication.URI,
                    StreamingApplication.MAPS,
                    StreamingApplication.CLOCK,
                    StreamingApplication.CONTACT,
                    StreamingApplication.OTHER,
                    StreamingApplication.COPY),
            streamApplications = new LinkedList<>(),
            trueStreamApplications = Arrays.asList(
                    StreamingApplication.LAUNCH,
                    StreamingApplication.PLEX,
                    StreamingApplication.KODI,
                    StreamingApplication.GOOGLE,
                    StreamingApplication.YOUTUBE,
                    StreamingApplication.SPOTIFY,
                    StreamingApplication.PANDORA,
                    StreamingApplication.TWITCH);
    private static final List<AuxiliaryApplication>
            clockApplications = new LinkedList<>(),
            trueClockApplications = Arrays.asList(
                    AuxiliaryApplication.TIMER,
                    AuxiliaryApplication.STOPWATCH,
                    AuxiliaryApplication.ALARM,
                    AuxiliaryApplication.SCAN_ALARM,
                    AuxiliaryApplication.VEVENT,
                    AuxiliaryApplication.COUNTDOWN),
            contactApplications = new LinkedList<>(),
            trueContactApplications = Arrays.asList(
                    AuxiliaryApplication.VIEW_CONTACT,
                    AuxiliaryApplication.VCARD,
                    AuxiliaryApplication.CALL,
                    AuxiliaryApplication.SMS,
                    AuxiliaryApplication.EMAIL),
            otherApplications = new LinkedList<>(),
            trueOtherApplications = Arrays.asList(
                    AuxiliaryApplication.SIMPLE_NOTE,
                    AuxiliaryApplication.LIST,
                    AuxiliaryApplication.FORM,
                    AuxiliaryApplication.COUNTER,
                    AuxiliaryApplication.DICE),
            deviceApplications = new LinkedList<>(),
            trueDeviceApplications = Arrays.asList(
                    AuxiliaryApplication.WIFI_CONNECTION,
                    AuxiliaryApplication.WIFI,
                    AuxiliaryApplication.BLUETOOTH,
                    AuxiliaryApplication.VOLUME,
                    AuxiliaryApplication.ORIENTATION,
                    AuxiliaryApplication.BRIGHTNESS,
                    AuxiliaryApplication.FLASHLIGHT);
}