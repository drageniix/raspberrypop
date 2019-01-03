package com.drageniix.raspberrypop.fragments;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.dialog.adapter.BaseCallback;
import com.drageniix.raspberrypop.dialog.adapter.media.MediaAdapter;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.utilities.Logger;

public class DatabaseFragment extends BaseFragment {
    protected static DatabaseFragment database;
    private String collection;

    public static synchronized DatabaseFragment getsInstance(String collectionName) {
        database = new DatabaseFragment();
        database.collection = collectionName;
        return database;}

    public static void filter(String searchText){
        if (database != null && database.dbAdapter != null){
            database.dbAdapter.filter(searchText);
        }
    }

    public static void switchScan(final boolean on){
        if (database != null && database.getBaseActivity() != null && database.getScan() != null) {
            database.getBaseActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (on && database.getScan().getVisibility() == View.GONE) {
                        database.getScan().setVisibility(View.VISIBLE);
                    } else if (!on && database.getScan().getVisibility() == View.VISIBLE) {
                        database.getScan().setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    public static void switchLoading(final boolean on){
        if (database != null && database.getBaseActivity() != null && database.getLoading() != null) {
            database.getBaseActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (on && database.getLoading().getVisibility() == View.GONE) {
                        database.getLoading().setVisibility(View.VISIBLE);
                    } else if (!on && database.getLoading().getVisibility() == View.VISIBLE) {
                        database.getLoading().setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    protected static boolean addOrUpdateMedia(final Media media){
        if (database != null && database.getBaseActivity() != null && database.dbAdapter != null){
            database.getBaseActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    database.dbAdapter.addOrUpdate(media);
                }
            });
            return true;
        }
        return false;
    }

    protected static void changeMedia(final Media media){
        if (database != null && database.getBaseActivity() != null && database.dbAdapter != null){
            database.getBaseActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    database.dbAdapter.updateCyclePosition(media);
                }
            });
        }
    }

    public static boolean refresh(){
        if (database != null && database.dbAdapter != null){
            database.dbAdapter.refresh();
            return true;
        }
        return false;
    }

    private ImageView scan;
    private ProgressBar loading;
    private MediaAdapter dbAdapter;
    public ImageView getScan() {return scan;}
    public ProgressBar getLoading() {return loading;}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    static int lastOrientation;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        database = this;
        setHandler();

        if (collection == null) {
            collection = handler.getDefaultCollection();}

        if (BaseActivity.isCameraOpen() && lastOrientation != (lastOrientation = getResources().getConfiguration().orientation)){
            getFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
            openScanner();
        } else {
            BaseActivity.setCameraOpen(false);
            lastOrientation = getResources().getConfiguration().orientation;
        }

        final View view = inflater.inflate(R.layout.media_fragment, container, false);
        scan = view.findViewById(R.id.scanPrompt);
        loading = view.findViewById(R.id.progress);

        RecyclerView databaseList = view.findViewById(R.id.databaseRecycler);
        databaseList.setHasFixedSize(false);

        RecyclerView.LayoutManager lm = null;
        MediaAdapter.ViewType type = handler.getPreferences().getView();
        if (type == MediaAdapter.ViewType.CARD) {
            lm = new StaggeredGridLayoutManager(getColumns(300), StaggeredGridLayoutManager.VERTICAL);
        } else if (type == MediaAdapter.ViewType.CELL) {
            lm = new StaggeredGridLayoutManager(getColumns(100), StaggeredGridLayoutManager.VERTICAL);
        } else if (type == MediaAdapter.ViewType.LIST) {
            lm = new StaggeredGridLayoutManager(getColumns(275), StaggeredGridLayoutManager.VERTICAL);
        }

        databaseList.setLayoutManager(lm);

        dbAdapter = new MediaAdapter(getBaseActivity(), handler, databaseList, collection);
        databaseList.setAdapter(dbAdapter);

        ItemTouchHelper.Callback callback = new BaseCallback(dbAdapter);
        ItemTouchHelper dragHelper = new ItemTouchHelper(callback);
        dragHelper.attachToRecyclerView(databaseList);

        loading.setVisibility(View.GONE);
        if (!dbAdapter.getDataset().isEmpty()) {
            scan.setVisibility(View.GONE);
        }

        FloatingActionButton fab = view.findViewById(R.id.fabQR);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openScanner();
            }
        });

        return view;
    }

    private void openScanner(){
        if (getBaseActivity().askCameraPermission(BaseActivity.BARCODE_REQUEST_CODE)) {
            try {
                getFragmentManager().beginTransaction()
                        .replace(R.id.activity_content, ScannerFragment.getsInstance(collection))
                        .addToBackStack(null)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
            } catch (Exception e) {
                Logger.log(Logger.FRAG, e);
            }
        }
    }

    private MenuItem selectAll, search, rearrange, select, currentSort, sort;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.database_menu, menu);

        setTitle(collection);

        select = menu.findItem(R.id.select);
        selectAll = menu.findItem(R.id.selectAll);
        search = menu.findItem(R.id.action_search);
        rearrange = menu.findItem(R.id.rearrange);
        sort = menu.findItem(R.id.action_sort);

        rearrange.setVisible(!collection.equals(handler.getDefaultCollection()));

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(search);
        dbAdapter.setSearchView(searchView);
        searchView.setQueryHint("Search tags...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return false;
            }
        });
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rearrange.setVisible(false);
                sort.setVisible(false);
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                rearrange.setVisible(!collection.equals(handler.getDefaultCollection()));
                sort.setVisible(true);
                ActivityCompat.invalidateOptionsMenu(getBaseActivity());
                return false;
            }
        });

        SubMenu sortOptions = sort.getSubMenu();
        for(int i = 0; i < sortOptions.size(); i++){
            sortOptions.getItem(i).setIcon(R.drawable.ic_action_sort_current);}

        menu.findItem(R.id.collectionSort).setVisible(
                collection.equals(handler.getDefaultCollection()) && handler.getBilling().hasPremium() && handler.getCollections().size() > 1);

        switch (handler.getPreferences().getLastOrder()){
            case OFTEN:
                currentSort = menu.findItem(R.id.often);
                break;
            case DATE:
                currentSort = menu.findItem(R.id.date);
                break;
            case TITLE:
                currentSort = menu.findItem(R.id.title);
                break;
            case NAME:
                currentSort = menu.findItem(R.id.name);
                break;
            case TYPE:
            case SOURCE:
                currentSort = menu.findItem(R.id.type);
                break;
            case LABEL:
                currentSort = menu.findItem(R.id.label);
                break;
            case COLLECTION:
                currentSort = menu.findItem(R.id.collectionSort);
                break;
        }

        if (currentSort != null) currentSort.setIcon(R.drawable.ic_action_sort_filled);
    }

    private void setCurrentSort(MenuItem newCurrent){
        if (currentSort != null){currentSort.setIcon(R.drawable.ic_action_sort_current);}
        currentSort = newCurrent;
        currentSort.setIcon(R.drawable.ic_action_sort_filled);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                getActivity().onBackPressed();
            case R.id.rearrange:
                dbAdapter.setRearrangeMode();
                if (dbAdapter.isRearrangeMode()){
                    item.setIcon(getBaseActivity().getIcon(R.drawable.ic_action_cancel, false));
                    recolorStatusBar(getBaseActivity().getAttributeColor(R.attr.colorAccent2));
                    search.setVisible(false);
                    select.setVisible(false);
                } else {
                    item.setIcon(getBaseActivity().getIcon(R.drawable.ic_action_rearrange, false));
                    recolorStatusBar();
                    search.setVisible(true);
                    select.setVisible(true);
                }
                break;
            case R.id.select:
                dbAdapter.setSelectionMode();
                if (dbAdapter.isSelectionMode()){
                    item.setIcon(getBaseActivity().getIcon(R.drawable.ic_action_cancel, false));
                    recolorStatusBar(getBaseActivity().getAttributeColor(R.attr.colorSelection));
                    selectAll.setVisible(true);
                    search.setVisible(false);
                    rearrange.setVisible(false);
                } else {
                    item.setIcon(getBaseActivity().getIcon(R.drawable.ic_action_select, false));
                    recolorStatusBar();
                    selectAll.setVisible(false);
                    search.setVisible(true);
                    rearrange.setVisible(!collection.equals(handler.getDefaultCollection()));
                }
                break;
            case R.id.selectAll:
                dbAdapter.selectAll();
                break;
            case R.id.name:
                setCurrentSort(item);
                dbAdapter.sort(MediaAdapter.Order.NAME);
                break;
            case R.id.title:
                setCurrentSort(item);
                dbAdapter.sort(MediaAdapter.Order.TITLE);
                break;
            case R.id.type:
                setCurrentSort(item);
                dbAdapter.sort(MediaAdapter.Order.TYPE);
                break;
            case R.id.date:
                setCurrentSort(item);
                dbAdapter.sort(MediaAdapter.Order.DATE);
                break;
            case R.id.often:
                setCurrentSort(item);
                dbAdapter.sort(MediaAdapter.Order.OFTEN);
                break;
            case R.id.label:
                setCurrentSort(item);
                dbAdapter.sort(MediaAdapter.Order.LABEL);
                break;
            case R.id.collectionSort:
                setCurrentSort(item);
                dbAdapter.sort(MediaAdapter.Order.COLLECTION);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void recolorStatusBar(){
        getBaseActivity().getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getBaseActivity().getAttributeColor(R.attr.colorPrimary)));
        getBaseActivity().getWindow().setStatusBarColor(getBaseActivity().getAttributeColor(R.attr.colorPrimaryDark));
    }
    private void recolorStatusBar(int color){
        getBaseActivity().getSupportActionBar().setBackgroundDrawable(new ColorDrawable(color));
        getBaseActivity().getWindow().setStatusBarColor(color);
    }
}