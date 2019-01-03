package com.drageniix.raspberrypop.dialog.adapter.cycle;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.fragments.CycleFragment;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.dialog.adapter.BaseAdapter;
import com.drageniix.raspberrypop.utilities.DBHandler;


public class CycleAdapter extends BaseAdapter<CycleHolder>{
    private DBHandler handler;
    private CycleManager.MediaCycle mediaCycle;
    private RecyclerView.LayoutManager lm;
    private int lastPosition = -1;

    public CycleAdapter(BaseActivity context, DBHandler handler, RecyclerView.LayoutManager lm, CycleManager.MediaCycle media){
        this.context = context;
        this.handler = handler;
        this.mediaCycle = media;
        this.lm = lm;
        setColor();
    }

    @Override
    public void addOrUpdate(Media media){
        int size = mediaCycle.size();
        handler.addOrUpdateMedia(media);
        if (mediaCycle.size() > size) {
            notifyItemInserted(size);
        } else {
            notifyItemChanged(indexOf(media));
        }
        lm.scrollToPosition(indexOf(media));
        CycleFragment.switchLoading(false);
    }

    public void remove(Media medium){
        int position = indexOf(medium);
        mediaCycle.remove(medium);
        handler.deleteMedia(medium);
        notifyItemRemoved(position);
        lastPosition -= 1;
    }

    public int indexOf(Media media){
        return this.mediaCycle.indexOf(media);
    }

    @Override
    public CycleHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new CycleHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.cycle_card, parent, false), this, handler, mediaCycle);
    }

    @Override
    public void onBindViewHolder(final CycleHolder holder, int position) {
        holder.initialize(mediaCycle.get(position));
        holder.adjustView(false, false);
        position = holder.getAdapterPosition();
        if (position > lastPosition) {
            holder.startAnimation();
            lastPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return mediaCycle.size();
    }

    public void onItemMove(int fromPosition, int toPosition) {
        mediaCycle.update(fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onItemSwiped(int position, int direction) {
        remove(mediaCycle.get(position));
    }
}
