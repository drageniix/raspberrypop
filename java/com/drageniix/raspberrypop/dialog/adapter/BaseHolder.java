package com.drageniix.raspberrypop.dialog.adapter;

import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;

import java.io.File;

public abstract class BaseHolder extends RecyclerView.ViewHolder {
    public Media media;
    protected boolean isSingular, hasMultiple;
    protected RequestOptions iconOptions, thumbOptions;
    public static String offlineEmoji = "\uD83D\uDCF4 ";
    public static String warningEmoji = "\u26A0 ";
    public DBHandler handler;

    public BaseHolder(View view, DBHandler handler){
        super(view);
        this.handler = handler;
    }

    public void initialize(Media media) {
        this.media = media;
        this.hasMultiple = handler.getCycleManager().loadCycle(media).size() != 1;
        this.isSingular = !hasMultiple || media.getCycleType() == 1 || media.getCycleType() == 2;
        this.iconOptions = new RequestOptions()
                .error(media.getEnabled().getIcon())
                .fitCenter();

        this.thumbOptions = new RequestOptions()
                .error(R.drawable.placeholder)
                .fitCenter();

        loadImages();
        loadText();
        loadListeners();
    }

    protected void thumbnailListener(final BaseAdapter adapter, AlertDialog dialog, ImageView thumbnail){
        if (dialog != null) dialog.dismiss();

        final File thumbnailFile;
        if (!media.getThumbnailString().isEmpty()) {thumbnailFile= new File(media.getThumbnailPath());}
        else if (media.getEnabled() == StreamingApplication.LOCAL && !media.getAuxiliaryString().isEmpty()) {thumbnailFile = new File(media.getAuixiliaryPath());}
        else thumbnailFile = null;

        if ((thumbnailFile != null && thumbnailFile.exists())){
            thumbnail.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    adapter.context.openIntent(BaseActivity.IMAGE_REQUEST_CODE, media);
                    return false;
                }
            });
            thumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    View prompt = View.inflate(adapter.context, R.layout.media_thumbnail, null);
                    final ImageView image = prompt.findViewById(R.id.image);

                    final AlertDialog popup = new AlertDialog.Builder(adapter.context)
                            .setView(prompt)
                            .create();

                    Glide.with(adapter.context)
                            .load(thumbnailFile)
                            .apply(thumbOptions)
                            .into(image);

                    image.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            adapter.context.openIntent(BaseActivity.IMAGE_REQUEST_CODE, media);
                            popup.dismiss();
                        }
                    });

                    popup.show();
                }
            });
        } else {
            thumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    adapter.context.openIntent(BaseActivity.IMAGE_REQUEST_CODE, media);
                }
            });
        }
    }

    public abstract void onItemSelected();
    public abstract void onItemClear();
    public abstract void clearAnimation();
    public abstract void startAnimation();
    public abstract void adjustView(boolean selected, boolean safe);
    public abstract void loadImages();
    public abstract void loadText();
    public abstract void loadListeners();
}
