package com.drageniix.raspberrypop.dialog.adapter;

import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.support.v7.widget.RecyclerView;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.utilities.DBHandler;

public abstract class BaseAdapter<T extends BaseHolder> extends RecyclerView.Adapter<T> {
    public BaseActivity context;
    protected DBHandler handler;
    public RadialGradient normalColor, selectedColor;

    protected void setColor(){
        this.normalColor = new RadialGradient(0, 24, 32, context.getAttributeColor(R.attr.colorAccent2), context.getAttributeColor(R.attr.colorAccent), Shader.TileMode.CLAMP);
        this.selectedColor = new RadialGradient(0, 0, 1, context.getAttributeColor(R.attr.colorAccent2), context.getAttributeColor(R.attr.colorAccent2), Shader.TileMode.CLAMP);
    }

    @Override
    public void onViewDetachedFromWindow(T holder) {
        holder.clearAnimation();
    }

    public abstract void addOrUpdate(Media media);

    public abstract void remove(Media media);

    public abstract void onItemMove(int fromPosition, int toPosition);

    public abstract void onItemSwiped(int position, int direction);

}
