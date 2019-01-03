package com.drageniix.raspberrypop.dialog.adapter;


import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

import com.drageniix.raspberrypop.dialog.adapter.cycle.CycleAdapter;
import com.drageniix.raspberrypop.dialog.adapter.media.MediaAdapter;


public class BaseCallback extends ItemTouchHelper.Callback{
    private static final float ALPHA_FULL = 1.0f;
    private final BaseAdapter mAdapter;

    public BaseCallback(BaseAdapter adapter) {
        mAdapter = adapter;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return mAdapter instanceof CycleAdapter ||
                (mAdapter instanceof MediaAdapter && ((MediaAdapter) mAdapter).canRearrange());
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlags = 0, swipeFlags = 0;

        if (mAdapter instanceof MediaAdapter) {
            dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
        } else if (mAdapter instanceof CycleAdapter){
            dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        }

        if (mAdapter instanceof MediaAdapter) {
            swipeFlags = 0; //ItemTouchHelper.START | ItemTouchHelper.END;
        } else if (mAdapter instanceof CycleAdapter){
            swipeFlags = viewHolder.getAdapterPosition() == 0 ? 0 : ItemTouchHelper.START | ItemTouchHelper.END;
        }

        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
        //if (source.getItemViewType() != target.getItemViewType()) {return false;}
        mAdapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
        mAdapter.onItemSwiped(viewHolder.getAdapterPosition(), i);
    }


    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            // Fade out the view as it is swiped out of the parent's bounds
            final float alpha = ALPHA_FULL - Math.abs(dX) / (float) viewHolder.itemView.getWidth();
            viewHolder.itemView.setAlpha(alpha);
            viewHolder.itemView.setTranslationX(dX);
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            if (viewHolder instanceof BaseHolder) {
                BaseHolder itemViewHolder = (BaseHolder) viewHolder;
                itemViewHolder.onItemSelected();
            }
        }

        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        viewHolder.itemView.setAlpha(ALPHA_FULL);
        if (viewHolder instanceof BaseHolder) {
            BaseHolder itemViewHolder = (BaseHolder) viewHolder;
            itemViewHolder.onItemClear();
        }
    }
}
