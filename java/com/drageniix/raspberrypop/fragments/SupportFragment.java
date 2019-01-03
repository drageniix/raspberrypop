package com.drageniix.raspberrypop.fragments;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.drageniix.raspberrypop.R;

import java.util.ArrayList;
import java.util.List;

import com.drageniix.raspberrypop.utilities.custom.CaseInsensitiveMap;

public class SupportFragment extends BaseFragment {
    private ExpandableListAdapter expandableListAdapter;
    private ExpandableListView expandableListView;
    private boolean expanded;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup group, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.support_fragment, group, false);

        CaseInsensitiveMap<String, List<String>> expandableListDetail = getData(getContext());
        List<String> expandableListTitle = new ArrayList<>(expandableListDetail.keySet());
        expandableListView = view.findViewById(R.id.FAQ);
        expandableListAdapter = new CustomExpandableListAdapter(expandableListTitle, expandableListDetail);
        expandableListView.setAdapter(expandableListAdapter);

        return view;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.support_menu, menu);
        setTitle("Support");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                getActivity().onBackPressed();
            case R.id.action_expand:
                expanded = !expanded;
                for(int i = 0; i < expandableListAdapter.getGroupCount(); i++){
                    if (expanded && !expandableListView.isGroupExpanded(i)) {
                        expandableListView.expandGroup(i);
                    } else if (!expanded && expandableListView.isGroupExpanded(i)){
                        expandableListView.collapseGroup(i);
                    }
                }
                item.setTitle(expanded ? getString(R.string.support_expand) : getString(R.string.support_collapse));
                item.setIcon(expanded ? R.drawable.ic_action_collapse : R.drawable.ic_action_expand);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    static CaseInsensitiveMap<String, List<String>> getData(Context context) {
        CaseInsensitiveMap<String, List<String>> expandableListDetail = new CaseInsensitiveMap<>();

        List<String> general = new ArrayList<>();
        expandableListDetail.put("General", general);
        general.add("Q: What can I scan?" +
                "\nA: You can scan NFC tags and the following:\n" +
                "\n1d barcodes: EAN-13, EAN-8, UPC-A, UPC-E, Code-39, Code-93, Code-128, ITF, Codabar" +
                "\n2D barcodes: QR Code, Data Matrix, PDF-417, AZTEC");
        general.add("Q: I already bought the app!" +
                "\nA: If you upgraded the app, you'll receive all premium features automatically. If you are reinstalling the app, email me at raspberrypop@drageniix.com with a screenshot of your order " +
                "(Google Play > Account > Order History) or forward me your order receipt to receive a free unlock code. Good jokes and recent charity receipts work as well!");
        general.add("Q: What are NFC tags?" +
                "\nA: NFC (Near Field Communication) tags are small chips which are only powered up by your device's signals. They can hold small amounts of information, like a transit pass or bank number," +
                "but for this application, only the unique ID of the tag is read, so you can keep using it with other apps and devices without any interference!\n" +
                "\nA2: If you already have a tag to scan, Make sure it's close enough to your phone and NFC is turn on. If it opens to another application, it may be marked for use there only. " +
                "In that case you'll need to erase the tag first!");
        general.add("Q: How can I change the thumbnail image?" +
                "\nClick the image and it will ask you to select a new one. You can also edit other parts of your tags by long touching it to bring up its menu.");

        List<String> roku = new ArrayList<>();
        expandableListDetail.put("Roku", roku);
        roku.add("Q: How can I play to my Roku?" +
                "\nA: Roku playing currently supports Plex, YouTube, and Netflix links, currently. It's not a perfect system (yet!)," +
                " but will work reliably if you choose the roku in settings before scanning your tag.");

        List<String> tasker = new ArrayList<>();
        expandableListDetail.put("Tasker", tasker);
        tasker.add("Q: Where are my tasks?"
                + "\nA: They will appear automatically once you begin typing. However, you must enable \"external access\" in tasker first.");
        tasker.add("Q: My tasks aren't working!"
                + "\nA: Check to make sure external access AND tasker overall are both enabled.");
        tasker.add("Q: How can I access my parameters?"
                + "\nA: They will be available as %par1 %par2 etc.\n Parameters %uid, %name, and %title will always be available.");
        tasker.add("Q: Can I use tasker without streaming?"
                + "\nA: Currently no, but if this is a feature you'd like, shoot me an email.");

        List<String> plex = new ArrayList<>();
        expandableListDetail.put("Plex", plex);
        plex.add("Q: Will this work if I'm away from my server?" +
                "\nA: If you have remote access enabled, you'll still need to manually add your device's IP to your server's whitelist. ");
        plex.add("Q: How do I play my media? Nothing is happening." +
                "\nA: Unfortunately, Plex requires its app to already be open to show up as an available \"player,\" " +
                "so you'll need to do that first. Roku playing is implemented and will open its application automatically.");

        List<String> kodi = new ArrayList<>();
        expandableListDetail.put("Kodi", kodi);
        kodi.add("Q: What can I play with Kodi?" +
                "\nA: Currently, movies, shows, music videos, songs, image slide shows, and PVR recordings are supported.");
        kodi.add("Q: I can't find my server?" +
                "\nA: Make sure \"Allow remote control via HTTP\" is enabled in your Kodi settings, and ensure your username and password are correct if you have one.");

        List<String> google = new ArrayList<>();
        expandableListDetail.put("Google Play Movies & Shows", google);
        google.add("Q: How do I know what's available on Google Play?" +
                "\nA: The app is pretty smart! If you enter or search for a movie or show that's available in your region, it'll take care of the rest.");

        List<String> youtube = new ArrayList<>();
        expandableListDetail.put("Youtube", youtube);
        youtube.add("Q: How do I find a youtube video?" +
                "\nA: Just touch the search icon if you'd like to find the video but don't have a link. " +
                "The app supports playlist and video links, as well as searching for either. " +
                "You can also choose a channel and the tag will play its newest uploads. Roku playing is implemented for videos only.");

        List<String> spotify = new ArrayList<>();
        expandableListDetail.put("Spotify", spotify);
        spotify.add("Q: How do I find a song/album?" +
                "\nA: Just enter the title, and optionally the artist, and the app will do its best to match it. You can also use the " +
                "search icon to attempt to find it that way.");
        spotify.add("Q: Can I add my own playlists?" +
                "\nA: Yes! If you select \"" + context.getString(R.string.spotify_button) + "\" in settings, you can add your own playlists, or paste a link. Otherwise you can " +
                "search for any public playlist.");

        List<String> pandora = new ArrayList<>();
        expandableListDetail.put("Pandora", pandora);
        pandora.add("Q: How do I start a statiom?" +
                "\nA: Just enter the title, artist, or genre and a station will be created for you.");
        pandora.add("Q: Can I add my own stations?" +
                "\nA: Yes! If you select \"" + context.getString(R.string.pandora_button) + "\" in settings, you can add your own stations.");

        List<String> twitch = new ArrayList<>();
        expandableListDetail.put("Twitch", twitch);
        twitch.add("Q: How can I add channels?" +
                "\nA: You can search for a channel normally, paste a link, or log in to have your followed channels available. " +
                "They will play a live stream if available, or open up to the channel feed it not.");

        List<String> launch = new ArrayList<>();
        expandableListDetail.put("Search/Launch", launch);
        launch.add("Q: How can I search the web" +
                "\nA: Don't choose an app name from the suggested list and a search will be performed automatically.");

        List<String> navigation = new ArrayList<>();
        expandableListDetail.put("Navigation", navigation);
        navigation.add("Q: Do locations only work with Google Maps?" +
                "\nA: Nope! You can choose any map application you prefer, the intuitive location picker just comes from Google Maps.");

        List<String> custom = new ArrayList<>();
        expandableListDetail.put("Website Links/URIs", custom);
        custom.add("Q: What websites can I link?" +
                "\nA: Any you could type into the browser! Open Graph support means rich information will usually be available.");
        custom.add("Q: What URIs can I input?" +
                "\nA: Any that you know! There are some URIs auto-suggested, but otherwise you'll need to know what you want to do before attempting to use a URI.");

        List<String> local = new ArrayList<>();
        expandableListDetail.put("Local Files", local);
        local.add("Q: What file types can I add?" +
                "\nA: Any! If you want a specific type added, simply email me and it'll be " +
                "available in the next update.");
        local.add("Q: Can I look up metadata for my files too?" +
                "\nA: Absolutely! Once you've chosen a file, you can press the search icon, and a match will be found based on the type of file. " +
                "This currently supports video, audio, and game files.");

        List<String> location = new ArrayList<>();
        expandableListDetail.put("Location", location);
        location.add("Q: What will a location do?" +
                "\nA: If you use google maps, the app will start navigation to the location, otherwise your preferred app will open and provide options.");

        List<String> timer = new ArrayList<>();
        expandableListDetail.put("Time", timer);
        timer.add("Q: Can I add seconds to the timer?" +
                "\nA: Currently you can add up to 24 hours, in hour and minute increments.");
        timer.add("Q: Will I end up with a lot of duplicate alarms?" +
                "\nA: Nope! If you already have an alarm set for your chosen time, it will just turn it on.");
        timer.add("Q: Ah, I can't turn off my scannable alarm!" +
                "\nA: Just select \"Simulate Scan\" on the tag to force to stop.");

        return expandableListDetail;
    }

    private class CustomExpandableListAdapter extends BaseExpandableListAdapter {

        private List<String> expandableListTitle;
        private CaseInsensitiveMap<String, List<String>> expandableListDetail;

        CustomExpandableListAdapter(List<String> expandableListTitle, CaseInsensitiveMap<String, List<String>> expandableListDetail) {
            this.expandableListTitle = expandableListTitle;
            this.expandableListDetail = expandableListDetail;
        }

        @Override
        public Object getChild(int listPosition, int expandedListPosition) {
            return this.expandableListDetail.get(this.expandableListTitle.get(listPosition)).get(expandedListPosition);
        }

        @Override
        public long getChildId(int listPosition, int expandedListPosition) {
            return expandedListPosition;
        }

        @Override
        public View getChildView(int listPosition, final int expandedListPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            final String expandedListText = (String) getChild(listPosition, expandedListPosition);
            if (convertView == null) {
                convertView = View.inflate(getContext(), R.layout.support_item, null);
            }
            TextView expandedListTextView = convertView.findViewById(R.id.expandedListItem);
            expandedListTextView.setText(expandedListText);
            return convertView;
        }

        @Override
        public int getChildrenCount(int listPosition) {
            return this.expandableListDetail.get(this.expandableListTitle.get(listPosition)).size();
        }

        @Override
        public Object getGroup(int listPosition) {
            return this.expandableListTitle.get(listPosition);
        }

        @Override
        public int getGroupCount() {
            return this.expandableListTitle.size();
        }

        @Override
        public long getGroupId(int listPosition) {
            return listPosition;
        }

        @Override
        public View getGroupView(int listPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            String listTitle = (String) getGroup(listPosition);
            if (convertView == null) {
                convertView = View.inflate(getContext(), R.layout.support_group, null);
            }
            TextView listTitleTextView = convertView.findViewById(R.id.listTitle);
            listTitleTextView.setTypeface(null, Typeface.BOLD);
            listTitleTextView.setText(listTitle);
            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int listPosition, int expandedListPosition) {
            return false;
        }
    }
}