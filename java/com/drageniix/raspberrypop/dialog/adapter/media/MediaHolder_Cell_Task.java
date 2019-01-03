package com.drageniix.raspberrypop.dialog.adapter.media;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.bumptech.glide.request.RequestOptions;
import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.fragments.CycleFragment;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.dialog.adapter.cycle.CycleManager;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.Logger;
import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;

class MediaHolder_Cell_Task extends MediaHolder {
    private RecyclerView thumbnail;
    private CardView self;
    private SourceAdapter sourceAdapter;
    private View label;

    MediaHolder_Cell_Task(View mediaCell, MediaAdapter adapter, DBHandler handler) {
        super(mediaCell, adapter, handler);
        self = (CardView) itemView;
        label = itemView.findViewById(R.id.label);

        RecyclerView.LayoutManager lm = new GridLayoutManager(adapter.context, 2);
        thumbnail = itemView.findViewById(R.id.thumbnail);
        thumbnail.setLayoutManager(lm);
        thumbnail.setHasFixedSize(false);
    }

    @Override
    public void clearAnimation() {
        self.clearAnimation();
    }

    @Override
    public void startAnimation() {
        self.startAnimation(AnimationUtils.loadAnimation(adapter.context, android.R.anim.fade_in));
    }

    @Override
    public void loadImages() {
        if (!media.getLabel().isEmpty()){
            label.setBackgroundColor(Color.parseColor(media.getLabel()));
        } else {
            label.setBackgroundColor(Color.TRANSPARENT);
        }

        CycleManager.MediaCycle cycle = handler.getCycleManager().loadCycle(media);
        sourceAdapter = new SourceAdapter(handler, this, adapter, cycle, iconOptions);
        thumbnail.setAdapter(sourceAdapter);
    }


    @Override
    public void loadText() {}

    @Override
    public void loadListeners() {
    }

    @Override
    public void adjustView(boolean selected, boolean safe) {
        sourceAdapter.setSelection(selected);
        if (!selected) {
            if (adapter.isSelectionMode() && safe) getSelectedItems().remove(media);
            self.setCardBackgroundColor(adapter.context.getAttributeColor(R.attr.backgroundColor));
        } else {
            if (adapter.isSelectionMode()) getSelectedItems().put(media, this);
            self.setCardBackgroundColor(adapter.context.getAttributeColor(R.attr.colorSelection));
        }
    }

    private View.OnClickListener onClickListener(final int position){
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!adapter.isRearrangeMode()) {
                    if (adapter.isSelectionMode()) {
                        adjustView(!getSelectedItems().containsKey(media), true);
                    } else {
                        try {
                            adapter.context.getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.activity_content, CycleFragment.getsInstance(media, position))
                                    .addToBackStack(null)
                                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                    .commit();
                        } catch (Exception e) {
                            Logger.log(Logger.FRAG, e);
                        }
                    }
                }
            }
        };
    }

    private static class SourceAdapter extends RecyclerView.Adapter<SourceHolder> {
        DBHandler handler;
        MediaAdapter adapter;
        MediaHolder_Cell_Task holderCellTask;
        CycleManager.MediaCycle cycle;
        RequestOptions iconOptions;
        boolean selectionMode;

        SourceAdapter(DBHandler handler, MediaHolder_Cell_Task holderCellTask, MediaAdapter adapter, CycleManager.MediaCycle cycle, RequestOptions options) {
            this.handler = handler;
            this.adapter = adapter;
            this.cycle = cycle;
            this.iconOptions = options;
            this.holderCellTask = holderCellTask;
        }

        void setSelection(boolean selection){
            selectionMode = selection;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public SourceHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new SourceHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.simple_imageview, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull SourceHolder holder, int position) {
            ImageView icon = holder.icon;
            View self = holder.self;

            self.setOnClickListener(holderCellTask.onClickListener(position));
            self.setOnCreateContextMenuListener(holderCellTask);

            Media media = cycle.get(position);
            handler.getParser().getThumbnailAPI().setIconImageView(adapter.context, iconOptions, icon, media);

            if (!selectionMode) {
                if (holderCellTask.isTintedThumbnail(media, icon, StreamingApplication.LOCAL.getIcon() == icon.getDrawable())) {
                    icon.setColorFilter(adapter.context.getAttributeColor(R.attr.textColor), PorterDuff.Mode.SRC_ATOP);
                } else {
                    icon.clearColorFilter();
                }
            } else {
                if (holderCellTask.isTintedThumbnail(media, icon, StreamingApplication.LOCAL.getIcon() == icon.getDrawable())) {
                    icon.setColorFilter(adapter.context.getAttributeColor(R.attr.colorAccent2), PorterDuff.Mode.SRC_ATOP);
                } else {
                    icon.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
                }
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return cycle.size();
        }
    }

    private static class SourceHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        View self;

        SourceHolder(View itemView) {
            super(itemView);
            self = itemView;
            icon = itemView.findViewById(R.id.sourceImage);
        }
    }
}
