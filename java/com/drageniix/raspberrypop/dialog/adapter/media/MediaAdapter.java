package com.drageniix.raspberrypop.dialog.adapter.media;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.fragments.DatabaseFragment;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.dialog.adapter.BaseAdapter;
import com.drageniix.raspberrypop.dialog.adapter.cycle.CycleManager;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.FileHelper;
import com.drageniix.raspberrypop.utilities.categories.ApplicationCategory;
import com.drageniix.raspberrypop.utilities.categories.AuxiliaryApplication;
import com.drageniix.raspberrypop.utilities.custom.CaseInsensitiveMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class MediaAdapter extends BaseAdapter<MediaHolder> {
    CaseInsensitiveMap<Media, MediaHolder> selectedItems = new CaseInsensitiveMap<>();
    private List<Media> aMediaList;
    private boolean selectAll, isDefault;
    private MediaComparator comparator;
    private int lastPosition = -1;
    private RecyclerView rv;
    private LayoutManager lm;
    private Media recent;
    private String collection;
    private SearchView searchView;
    private boolean selectionMode;
    private boolean rearrangeMode;

    public boolean isRearrangeMode() {
        return rearrangeMode;
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public enum ViewType{CARD, CELL, LIST}
    public MediaAdapter(BaseActivity context, DBHandler handler, RecyclerView rv, String collection){
        this.context = context;
        this.handler = handler;
        this.collection = collection;
        this.aMediaList = new ArrayList<>(getDataset());
        this.rv = rv;
        this.lm = rv.getLayoutManager();
        this.comparator = new MediaComparator();
        this.isDefault = collection.equals(handler.getDefaultCollection());
        setColor();
        boolean customSorted = false;
        for(Media media : aMediaList){
            if (media.getPosition() != -1){
                customSorted = true;
                break;
            }
        }

        if (isDefault || !customSorted){
            sort(handler.getPreferences().getLastOrder(), handler.getPreferences().getReverseOrder());
        } else {
            sort(Order.CUSTOM, false);
        }
    }

    @Override
    public int getItemViewType(int position) {
        CycleManager.MediaCycle cycle = handler.getCycleManager().loadCycle(aMediaList.get(position));
        return cycle.size() == 1 ||
                aMediaList.get(position).getCycleType() == 1  ||
                aMediaList.get(position).getCycleType() == 2 ?
                0 : 1;
    }

    @Override
    public MediaHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MediaAdapter.ViewType type = handler.getPreferences().getView();
        switch (type){
            case LIST:
                return new MediaHolder_List(LayoutInflater.from(parent.getContext()).inflate(R.layout.media_list, parent, false), this, handler);
            case CELL:
                return viewType == 0 ?
                        new MediaHolder_Cell(LayoutInflater.from(parent.getContext()).inflate(R.layout.media_cell, parent, false), this, handler) :
                        new MediaHolder_Cell_Task(LayoutInflater.from(parent.getContext()).inflate(R.layout.media_cell_multiple, parent, false), this, handler);
            case CARD:
                default:
                return viewType == 0 ?
                        new MediaHolder_Card(LayoutInflater.from(parent.getContext()).inflate(R.layout.media_card, parent, false), this, handler) :
                        new MediaHolder_Card_Task(LayoutInflater.from(parent.getContext()).inflate(R.layout.media_card_multiple, parent, false), this, handler);
        }
    }

    @Override
    public void onBindViewHolder(final MediaHolder holder, int position) {
        holder.initialize(aMediaList.get(position));
        holder.adjustView(selectedItems.containsKey(holder.media), false);
        position = holder.getAdapterPosition();
        if (position > lastPosition) {
            holder.startAnimation();
            lastPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return aMediaList.size();
    }

    public void onItemSwiped(int position, int direction) {
        aMediaList.set(position,
                handler.getCycleManager().getNext(
                        aMediaList.get(position),
                        (direction == ItemTouchHelper.START ? -1 : 1)));
        notifyItemChanged(position);
    }

    public void onItemMove(int fromPosition, int toPosition) {
        Collections.swap(aMediaList, fromPosition, toPosition);
        comparator.setOrder(Order.CUSTOM, false);
        updatePositions();
        notifyItemMoved(fromPosition, toPosition);
    }

    public boolean canRearrange(){
        return rearrangeMode && !isDefault;
    }

    public void updateCyclePosition(Media media){
        int position = aMediaList.indexOf(media);
        if (position != -1) {
            aMediaList.set(position, handler.getCycleManager().loadCycle(media).get(0));
            notifyItemChanged(position);
        }
    }

    private void updatePositions(){
        if (!isDefault) {
            for (int i = 0; i < aMediaList.size(); i++) {
                if (comparator.order == Order.CUSTOM && aMediaList.get(i).getPosition() != i) {
                    handler.updateMediaPosition(aMediaList.get(i), i);
                } else if (comparator.order != Order.CUSTOM && aMediaList.get(i).getPosition() != -1) {
                    handler.updateMediaPosition(aMediaList.get(i), -1);
                }
            }
        }
    }

    public Set<Media> getDataset(){
        Set<Media> datatset = new HashSet<>(handler.getMediaList(collection));
        for(Iterator<Media> it = datatset.iterator(); it.hasNext();) {
            Media next = it.next();
            if (next.getCyclePosition() != 0) {
                it.remove();
            }
        }
        return datatset;
    }

    public void refresh(){
        handler.getAllMedia();
        lastPosition = -1;
        recent = null;
        selectedItems.clear();
        aMediaList = new ArrayList<>(getDataset());
        sort();
        DatabaseFragment.switchScan(aMediaList.isEmpty());
        DatabaseFragment.switchLoading(false);
    }

    public void addOrUpdate(Media media){
        handler.addOrUpdateMedia(media);
        DatabaseFragment.switchLoading(false);
        if (collection.equals(media.getCollection()) || isDefault) {
            if (!aMediaList.contains(media) && media.getCyclePosition() == 0) {
                aMediaList.add(media);
                DatabaseFragment.switchScan(aMediaList.isEmpty());
            }
            recent = media;
            sort();
        } else if (!isDefault){
            handler.updateMediaPosition(media, -1);
            int position = aMediaList.indexOf(media);
            if (selectedItems.containsKey(media)) selectedItems.remove(media);
            if (position > -1) {
                lastPosition -= 1;
                aMediaList.remove(position);
                notifyItemRemoved(position);
                DatabaseFragment.switchScan(aMediaList.isEmpty());
            }
            updatePositions();
        }
    }

    public void remove(Media medium){
        handler.getCycleManager().deleteCycle(handler, medium);
        int position = aMediaList.indexOf(medium);
        if (position != -1) {
            lastPosition -= 1;
            aMediaList.remove(position);
            if (selectedItems.containsKey(medium)) selectedItems.remove(medium);
            updatePositions();
            notifyItemRemoved(position);
            DatabaseFragment.switchScan(aMediaList.isEmpty());
        }
    }


    public void setRearrangeMode(){
        rearrangeMode = !rearrangeMode;
    }

    public void setSelectionMode(){
        selectionMode = !selectionMode;
        if (!selectionMode){
            resetSelection(false);
        }
    }

    void resetSelection(boolean delete) {
        List<Map.Entry<Media, MediaHolder>> selections = new ArrayList<>(selectedItems.entrySet());
        for (Map.Entry<Media, MediaHolder> selection : selections) {
            if (selection.getValue() != null && aMediaList.contains(selection.getKey())) {selection.getValue().adjustView(false, false);}
            if (delete) remove(selection.getKey());
        }
        selectedItems.clear();
        selectAll = false;
    }

    public void selectAll(){
        if (!selectAll){
            selectAll = true;
            for (int i = 0, size = rv.getChildCount(); i < size; i++) {
                MediaHolder holder = (MediaHolder)rv.getChildViewHolder(rv.getChildAt(i));
                if (holder != null) {holder.adjustView(true, true);}}
            for(Media media : aMediaList){
                if (!selectedItems.containsKey(media)){
                    selectedItems.put(media, null);
                }
            }
        } else {
            resetSelection(false);
        }
    }

    public void filter(String text) {
        aMediaList.clear();
        if (text.isEmpty()) {
            aMediaList.addAll(getDataset());
            sort();
        } else {
            for (Media base : getDataset()) {
                for (Media m : handler.getCycleManager().loadCycle(base)) {
                    if (FileHelper.containsIgnoreCase(m.searchMediaData(), true, text.split(" "))) {
                        aMediaList.add(m);
                        break;
                    }
                }
            }
            notifyDataSetChanged();
        }
    }

    public void setSearchView(SearchView searchView){
        this.searchView = searchView;}
    public void search(String term){
        searchView.onActionViewExpanded();
        searchView.setQuery(term.trim(), false);}

    public void sort(Order order){ //from menu click
        comparator.setOrder(order);
        sort();
    }

    private void sort(Order order, boolean reverse){ //from preferences
        comparator.setOrder(order, reverse);
        sort();
    }

    private void sort(){ //general
        Collections.sort(aMediaList, comparator);
        updatePositions();
        notifyDataSetChanged();
        if (lm != null && recent != null && aMediaList.contains(recent)){
            int position = aMediaList.indexOf(recent);
            lm.scrollToPosition(position);}
    }

    public enum Order{DATE, OFTEN, TITLE, NAME, TYPE, LABEL, SOURCE, COLLECTION, CUSTOM}
    private class MediaComparator implements Comparator<Media> {
        private boolean reverse;
        private Order order;

        @Override public int compare(Media m1, Media m2){
            if (reverse && order != Order.CUSTOM){
                Media temp = m1;
                m1 = m2;
                m2 = temp;
            }

            switch(order){
                case OFTEN:
                    return Integer.compare(m2.getScanHistory().length, m1.getScanHistory().length);
                case DATE:
                    return (m1.getScanHistory()[0].isEmpty() || m2.getScanHistory()[0].isEmpty()) ?
                            Long.compare(m1.getId(), m2.getId()) :
                            Long.compare(Long.parseLong(m1.getScanHistory()[0]), Long.parseLong(m2.getScanHistory()[0]));
                case TITLE:
                    return removeArticle(m1.getTitle()).compareToIgnoreCase(removeArticle(m2.getTitle()));
                case NAME:
                    return removeArticle(m1.getFigureName()).compareToIgnoreCase(removeArticle(m2.getFigureName()));
                case TYPE:
                case SOURCE:
                    ApplicationCategory
                            category1 = m1.getEnabled().isFolder() ? AuxiliaryApplication.valueOf(m1) : m1.getEnabled(),
                            category2 = m2.getEnabled().isFolder() ? AuxiliaryApplication.valueOf(m2) : m2.getEnabled();
                    if (category1 == null || category2 == null){return 1;}
                    else return category1.getName().compareTo(category2.getName());
                case COLLECTION:
                    return m1.getCollection().compareToIgnoreCase(m2.getCollection());
                case LABEL:
                    return m1.getLabel().compareTo(m2.getLabel());
                case CUSTOM:
                    int pos1 = m1.getPosition() == -1 ? aMediaList.size() - 1 : m1.getPosition();
                    int pos2 = m2.getPosition() == -1 ? aMediaList.size() - 1 : m2.getPosition();
                    return Integer.compare(pos1, pos2);
                default:
                    return -1;
            }
        }

        private String removeArticle(String title){
            if (title.toLowerCase().startsWith("a ")){
                return title.substring(2);
            } else if (title.toLowerCase().startsWith("an ")){
                return title.substring(3);
            } else if (title.toLowerCase().startsWith("the ")){
                return title.substring(4);
            } else {
                return title;
            }
        }

        private void setOrder(Order newOrder, boolean reverse){
            this.reverse = reverse;
            order = newOrder;
        }

        private void setOrder(Order newOrder) {
            reverse = order == newOrder && !reverse;
            order = newOrder;
            if (newOrder != Order.CUSTOM) handler.getPreferences().setLastOrder(newOrder.ordinal());
            handler.getPreferences().setReverseOrder(reverse);
        }
    }
}