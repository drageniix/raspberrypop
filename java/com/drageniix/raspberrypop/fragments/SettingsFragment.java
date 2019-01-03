package com.drageniix.raspberrypop.fragments;

import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.activities.MainActivity;
import com.drageniix.raspberrypop.dialog.DatabaseDialog;
import com.drageniix.raspberrypop.dialog.FormTemplate;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.Logger;
import com.drageniix.raspberrypop.utilities.api.PlexAPI;
import com.drageniix.raspberrypop.utilities.api.RokuAPI;
import com.drageniix.raspberrypop.utilities.billing.Billing;
import com.drageniix.raspberrypop.utilities.categories.ScanApplication;
import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public class SettingsFragment extends PreferenceFragmentCompat {
    private DBHandler handler;
    private MainActivity activity;
    private Preference theme, layout, scanner, continuous, prices,
            sync, rpop, thumbnail, template,
            spotify, plex, roku, twitch, pandora;
    private PreferenceCategory accounts, debug;
    private GradientDrawable shape;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.settings_menu, menu);
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle("Settings");
            activity.getSupportActionBar().setBackgroundDrawable(new ColorDrawable(activity.getAttributeColor(R.attr.colorPrimary)));
            activity.getWindow().setStatusBarColor(activity.getAttributeColor(R.attr.colorPrimaryDark));
        for(StreamingApplication icon : StreamingApplication.values()){
                if (icon.isInstalled()) icon.getIcon().clearColorFilter();}
            for(ScanApplication icon : ScanApplication.values()){
                if (icon.isInstalled()) icon.getIcon().clearColorFilter();}

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                getActivity().onBackPressed();
            case R.id.pie_chart:
                try {
                    if (!activity.updateRecreate()) {
                        activity.getSupportFragmentManager().beginTransaction()
                                .replace(R.id.activity_content, new StatisticsFragment())
                                .addToBackStack(null)
                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                .commit();
                    }
                } catch (Exception e) {
                    Logger.log(Logger.FRAG, e);
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        activity = ((MainActivity) getActivity());
        handler = activity.getHandler();

        setPreferencesFromResource(R.xml.preferences, rootKey);

        debug = (PreferenceCategory) findPreference("debug");
        accounts = (PreferenceCategory) findPreference("accounts");
        theme = findPreference("THEME_STRING");
        layout = findPreference("VIEW");
        scanner = findPreference("BARCODE");
        spotify = findPreference("spotify");
        thumbnail = findPreference("THUMBNAIL");
        prices = findPreference("PRICES");
        plex = findPreference("plex");
        roku = findPreference("roku");
        twitch = findPreference("twitch");
        pandora = findPreference("pandora");
        sync = findPreference("sync");
        continuous = findPreference("CONTINUOUS");
        rpop = findPreference("LOAD");
        template = findPreference("TEMPLATE");

        initialize();
    }

    public void updateValues(){
        shape.setStroke(1, activity.getAttributeColor(R.attr.colorAccent));
        shape.setColors(new int[]{
                activity.getAttributeColor(R.attr.colorPrimary),
                activity.getAttributeColor(R.attr.backgroundColor),
        });

        if (handler.getPreferences().hasRoku()){
            roku.setSummary(handler.getPreferences().getRokuName());}
        if (handler.getPreferences().hasPlex()){
            plex.setSummary(handler.getPreferences().getPlexAccount());}
        if (handler.getPreferences().hasSpotify()){
            spotify.setSummary(handler.getPreferences().getSpotifyAccount());}
        if (handler.getPreferences().hasTwitch()){
            twitch.setSummary(handler.getPreferences().getTwitchDisplay());}
        if (handler.getPreferences().hasPandora()){
            pandora.setSummary(handler.getPreferences().getPandoraAccount());}

        handler.getBilling().getPremiumDetails(this);
    }

    private void initialize(){
        if (Billing.isBeta()){
            findPreference("createTag").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    DatabaseDialog.addMedia((BaseActivity)getActivity(), getFragmentManager(), handler, false, UUID.randomUUID().toString(), "", "Debug Testing");
                    return false;
                }
            });
            findPreference("currentTest").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Snackbar.make(getView(), "Unit Test Initiated.", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Display Log", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                new AlertDialog.Builder(getActivity())
                                        .setMessage(handler.getPreferences().getLogger())
                                        .show();
                            }
                        }).show();
                    return false;
                }
            });
        } else {
            getPreferenceScreen().removePreference(debug);
        }

        shape = new GradientDrawable(GradientDrawable.Orientation.TL_BR, null);
        shape.setShape(GradientDrawable.OVAL);
        shape.setSize(24, 24);
        theme.setIcon(shape);

        theme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int value = Integer.valueOf(newValue.toString());
                if (value != 0 && value != 1 && !handler.getBilling().hasPremium()){
                    ((BaseActivity)getActivity()).advertisePremium(SettingsFragment.this);
                    return false;
                }
                setTheme(value);
                return true;
            }
        });

        scanner.setIcon(((BaseActivity)getActivity()).getIcon(R.drawable.ic_action_barcode, true));
        continuous.setIcon(((BaseActivity)getActivity()).getIcon(R.drawable.ic_action_continuous, true));
        layout.setIcon(((BaseActivity)getActivity()).getIcon(R.drawable.ic_action_layout, true));
        thumbnail.setIcon(((BaseActivity)getActivity()).getIcon(R.drawable.ic_action_gallery, true));
        prices.setIcon(((BaseActivity)getActivity()).getIcon(R.drawable.ic_action_prices, true));

        template.setIcon(((BaseActivity)getActivity()).getIcon(R.drawable.ic_action_form, true));
        template.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final FormTemplate form = new FormTemplate(activity, handler.getTemplates());
                LinkedHashMap<String, String> templates = new LinkedHashMap<>();
                templates.put("➕ Add New Form", "");
                templates.putAll(handler.getTemplates().getTemplates());

                final String[] titles = templates.keySet().toArray(new String[templates.size()]);
                final String[] data = templates.values().toArray(new String[templates.size()]);

                AlertDialog dialog = new AlertDialog.Builder(activity)
                        .setTitle("Saved Form Templates")
                        .setItems(titles, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, final int selectedPosition) {
                                form.loadTemplate(titles[selectedPosition], data[selectedPosition]);
                                new AlertDialog.Builder(activity)
                                        .setView(form.getDialog())
                                        .setPositiveButton(activity.getString(R.string.submit), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                form.getData();
                                                if (selectedPosition == 0){
                                                    dialog.dismiss();}
                                            }
                                        })
                                        .setNegativeButton(activity.getString(R.string.cancel), null)
                                        .setNeutralButton(R.string.delete_form, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                form.deleteTemplate();
                                            }
                                        }).create()
                                        .show();
                            }
                        })
                        .setPositiveButton(R.string.submit, null)
                        .create();

                dialog.show();
                dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE| WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

                return false;
            }
        });

        rpop.setIcon(((BaseActivity)getActivity()).getIcon(R.drawable.ic_action_import, true));
        rpop.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ((BaseActivity)getActivity()).openIntent(BaseActivity.LOAD_REQUEST_CODE);
                return false;
            }
        });

        sync.setIcon(((BaseActivity)getActivity()).getIcon(R.drawable.ic_action_sync, true));
        sync.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
          @Override
          public boolean onPreferenceClick(Preference preference) {
              new AlertDialog.Builder(getActivity())
                      .setMessage("Restoring can't be undone!")
                      .setPositiveButton("Backup", new DialogInterface.OnClickListener() {
                          @Override
                          public void onClick(DialogInterface dialog, int which) {
                              ((BaseActivity)getActivity()).openIntent(BaseActivity.BACKUP_REQUEST_CODE);
                          }
                      }).setNegativeButton("Restore", new DialogInterface.OnClickListener() {
                          @Override
                          public void onClick(DialogInterface dialog, int which) {
                              ((BaseActivity)getActivity()).openIntent(BaseActivity.RESTORE_REQUEST_CODE);
                          }
                        }).setNeutralButton(getString(R.string.cancel), null).
                      create().show();
              return false;
          }
        });


        roku.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final List<String[]> rokus = RokuAPI.scanForAllRokus();
                if (rokus != null && !rokus.isEmpty()) {
                    String[] clientNames = new String[rokus.size() + 1];
                    clientNames[rokus.size()] = "None";
                    for(int i = 0; i < rokus.size(); i++){
                        clientNames[i] = rokus.get(i)[1];
                    }

                    AlertDialog.Builder rokuDialog = new AlertDialog.Builder(getActivity());
                    rokuDialog.setSingleChoiceItems(clientNames, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int checked) {
                            if (checked == rokus.size()){
                                handler.getPreferences().clearRoku();
                                preference.setSummary("");
                            } else {
                                handler.getPreferences().setRokuIp(rokus.get(checked)[0]);
                                handler.getPreferences().setRokuName(rokus.get(checked)[1]);
                                preference.setSummary(rokus.get(checked)[1]);
                            }
                        }
                    });

                    rokuDialog.setTitle("Choose a Device");
                    rokuDialog.setPositiveButton(getString(R.string.submit), null);
                    rokuDialog.create().show();
                } else {
                    Toast.makeText(getContext(), "No Rokus currently available.", Toast.LENGTH_SHORT).show();
                    handler.getPreferences().clearRoku();
                    preference.setSummary("");
                }
                return false;
            }
        });

        if (!StreamingApplication.PLEX.isInstalled()) {
            accounts.removePreference(plex);
            handler.getPreferences().clearPlex();
        } else {
            plex.setIcon(StreamingApplication.PLEX.getIcon());
            plex.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    View dialogView = View.inflate(getContext(), R.layout.preference_login, null);
                    final EditText username = dialogView.findViewById(R.id.login);
                    final EditText password = dialogView.findViewById(R.id.password);
                    final TextView uPrompt = dialogView.findViewById(R.id.login_prompt);
                    final TextView pPrompt = dialogView.findViewById(R.id.password_prompt);

                    username.setHint(getString(R.string.email_hint));
                    password.setHint(getString(R.string.password_hint));
                    uPrompt.setText(getString(R.string.plex_email));
                    pPrompt.setText(getString(R.string.plex_password));

                    AlertDialog.Builder login = new AlertDialog.Builder(getActivity());
                    login.setView(dialogView);

                    if (handler.getPreferences().hasPlex()){
                        username.setText(handler.getPreferences().getPlexAccount());
                        login.setNegativeButton("Logout", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                handler.getPreferences().clearPlex();
                                preference.setSummary("");
                            }
                        });
                    } else { login.setNegativeButton(getString(R.string.cancel), null); }
                    login.setPositiveButton("Login", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            PlexAPI.getTVToken(getActivity(), username.getText().toString().trim(), password.getText().toString().trim(), true);
                            preference.setSummary(handler.getPreferences().getPlexAccount());
                        }
                    });
                    login.create().show();
                    return false;
                }
            });
        }

        if (!StreamingApplication.PANDORA.isInstalled()) {
            accounts.removePreference(pandora);
            handler.getPreferences().clearPandora();
        } else {
            pandora.setIcon(StreamingApplication.PANDORA.getIcon());
            pandora.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    View dialogView = View.inflate(getContext(), R.layout.preference_login, null);
                    final EditText username = dialogView.findViewById(R.id.login);
                    final EditText password = dialogView.findViewById(R.id.password);
                    final TextView uPrompt = dialogView.findViewById(R.id.login_prompt);
                    final TextView pPrompt = dialogView.findViewById(R.id.password_prompt);

                    username.setHint(getString(R.string.email_hint));
                    password.setHint(getString(R.string.password_hint));
                    uPrompt.setText(getString(R.string.pandora_email));
                    pPrompt.setText(getString(R.string.pandora_password));

                    AlertDialog.Builder login = new AlertDialog.Builder(getActivity());
                    login.setView(dialogView);

                    if (handler.getPreferences().hasPandora()){
                        username.setText(handler.getPreferences().getPandoraAccount());
                        login.setNegativeButton("Logout", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                handler.getPreferences().clearPandora();
                                preference.setSummary("");
                            }
                        });
                    } else { login.setNegativeButton(getString(R.string.cancel), null); }
                    login.setPositiveButton("Login", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            handler.getPreferences().setPandoraAccount(username.getText().toString().trim());
                            handler.getPreferences().setPandoraPassword(password.getText().toString().trim());
                            DatabaseDialog.pandoraStations = handler.getParser().getPandoraAPI().getStations();
                            preference.setSummary(handler.getPreferences().getPandoraAccount());
                        }
                    });
                    login.create().show();
                    return false;
                }
            });
        }

        if (!StreamingApplication.SPOTIFY.isInstalled()){
            accounts.removePreference(spotify);
            handler.getParser().getSpotifyAPI().clear();
        } else {
            spotify.setIcon(StreamingApplication.SPOTIFY.getIcon());
            spotify.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    preference.setSummary("");
                    handler.getParser().getSpotifyAPI().clear();
                    handler.getParser().getSpotifyAPI().authenticateSpotify(SettingsFragment.this);
                    return false;
                }
            });
        }

        if (!StreamingApplication.TWITCH.isInstalled()){
            accounts.removePreference(twitch);
            handler.getParser().getTwitchAPI().clear();
        } else {
            twitch.setIcon(StreamingApplication.TWITCH.getIcon());
            twitch.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    preference.setSummary("");
                    handler.getParser().getTwitchAPI().clear();
                    handler.getParser().getTwitchAPI().authenticateTwitch(SettingsFragment.this);
                    return false;
                }
            });
        }

        updateValues();
    }

    private void setTheme(int theme){
        switch (theme) {
            case 0:
                handler.getPreferences().setTheme(R.style.LightTheme);
                shape.setStroke(1, activity.getResourceColor(R.color.light_colorAccent));
                shape.setColors(new int[]{
                        activity.getResourceColor(R.color.light_colorPrimary),
                        activity.getResourceColor(R.color.light_background)
                });
                break;
            case 1:
                handler.getPreferences().setTheme(R.style.DarkTheme);
                shape.setStroke(1, activity.getResourceColor(R.color.dark_colorAccent));
                shape.setColors(new int[]{
                        activity.getResourceColor(R.color.dark_colorPrimary),
                        activity.getResourceColor(R.color.dark_background)
                });
                break;
            case 2:
                handler.getPreferences().setTheme(R.style.AmoledDarkTheme);
                shape.setStroke(1, activity.getResourceColor(R.color.amoled_colorAccent));
                shape.setColors(new int[]{
                        activity.getResourceColor(R.color.amoled_colorPrimary),
                        activity.getResourceColor(R.color.amoled_background)
                });
                break;
            case 3:
                handler.getPreferences().setTheme(R.style.SeasonalTheme);
                shape.setStroke(1, activity.getResourceColor(R.color.seasonal_colorAccent));
                shape.setColors(new int[]{
                        activity.getResourceColor(R.color.seasonal_colorPrimary),
                        activity.getResourceColor(R.color.seasonal_background)
                });
                break;
            case 4:
                handler.getPreferences().setTheme(R.style.SeasonalTheme2);
                shape.setStroke(1, activity.getResourceColor(R.color.seasonal2_colorAccent));
                shape.setColors(new int[]{
                        activity.getResourceColor(R.color.seasonal2_colorPrimary),
                        activity.getResourceColor(R.color.seasonal2_background)
                });
                break;
            case 5:
                handler.getPreferences().setTheme(R.style.SeasonalTheme3);
                shape.setStroke(1, activity.getResourceColor(R.color.seasonal3_colorAccent));
                shape.setColors(new int[]{
                        activity.getResourceColor(R.color.seasonal3_colorPrimary),
                        activity.getResourceColor(R.color.seasonal3_background)
                });
                break;

            case 6:
                handler.getPreferences().setTheme(R.style.SeasonalTheme4);
                shape.setStroke(1, activity.getResourceColor(R.color.seasonal4_colorAccent));
                shape.setColors(new int[]{
                        activity.getResourceColor(R.color.seasonal4_colorPrimary),
                        activity.getResourceColor(R.color.seasonal4_background)
                });
                break;
        }
    }
}
