package com.drageniix.raspberrypop.dialog.adapter.media;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.categories.AuxiliaryApplication;
import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;

import java.io.File;

class MediaHolder_Cell extends MediaHolder {
    private ImageView thumbnail;
    private View label;
    private CardView self;
    private boolean localThumb;

    MediaHolder_Cell(View mediaCell, MediaAdapter adapter, DBHandler handler) {
        super(mediaCell, adapter, handler);
        self = (CardView) itemView;
        thumbnail = itemView.findViewById(R.id.thumbnail);
        label = itemView.findViewById(R.id.label);
    }

    @Override public void clearAnimation() {
        self.clearAnimation();}
    @Override public void startAnimation() {
        self.startAnimation(AnimationUtils.loadAnimation(adapter.context, android.R.anim.fade_in));}

    @Override
    public void loadImages() {
        if (!media.getLabel().isEmpty()){
            label.setBackgroundColor(Color.parseColor(media.getLabel()));
        } else {
            label.setBackgroundColor(Color.TRANSPARENT);
        }

        localThumb = true;
        thumbnail.layout(0,0,0,0);
        thumbnail.clearColorFilter();
        iconOptions = iconOptions.dontAnimate();
        if (media.getEnabled() == StreamingApplication.LOCAL
                && media.getThumbnailString().isEmpty() && media.getAuxiliaryString().isEmpty()) {
            localThumb = false;
            Glide.with(adapter.context)
                    .load(adapter.context.getFileIcon(media.getAlternateID()))
                    .apply(iconOptions)
                    .into(thumbnail);
        } else if ((media.getEnabled() == StreamingApplication.URI || media.getEnabled() == StreamingApplication.LOCAL || media.getEnabled() == StreamingApplication.LAUNCH || media.getEnabled() == StreamingApplication.MAPS  || media.getEnabled() == StreamingApplication.CONTACT)
                && media.getThumbnailString().isEmpty() && !media.getAuxiliaryString().isEmpty()){
            File icon = new File(media.getAuixiliaryPath());
            Glide.with(adapter.context)
                    .load(icon)
                    .apply(iconOptions.signature(new ObjectKey(icon.lastModified())))
                    .into(thumbnail);
        } else if (!media.getThumbnailString().isEmpty()) {
            File thumbnail = new File(media.getThumbnailPath());
            Glide.with(adapter.context)
                    .load(thumbnail)
                    .apply(iconOptions.signature(new ObjectKey(thumbnail.lastModified())))
                    .into(this.thumbnail);
        } else if (media.getEnabled().isFolder()) {
            thumbnail.setImageDrawable(
                    AuxiliaryApplication.valueOf(media).getIcon());
        } else {
            thumbnail.setImageDrawable(media.getEnabled().isInstalled() ?
                    media.getEnabled().getIcon() :
                    adapter.context.getIcon(R.drawable.ic_action_warning, true));
        }
    }

    @Override
    public void loadText() {}

    @Override
    public void loadListeners() {
        label.setOnClickListener(labelClickListener());
        self.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!adapter.isRearrangeMode()) {
                    if (adapter.isSelectionMode()) {
                        adjustView(!getSelectedItems().containsKey(media), true);
                    } else {
                        MediaHolder_Card card = new MediaHolder_Card(View.inflate(adapter.context, R.layout.media_card, null), adapter, handler);
                        AlertDialog.Builder mediaDialog = new AlertDialog.Builder(adapter.context).setView(card.self);
                        final AlertDialog dialog = mediaDialog.create();
                        if (dialog.getWindow() != null)
                            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                        card.loadPopup(media, dialog);
                        dialog.show();
                    }
                }
            }
        });

        self.setOnCreateContextMenuListener(this);
    }

    @Override
    public void adjustView(boolean selected, boolean safe) {
        if (!selected) {
            if (adapter.isSelectionMode() && safe) getSelectedItems().remove(media);
            self.setCardBackgroundColor(adapter.context.getAttributeColor(R.attr.backgroundColor));
            if (isTintedThumbnail(media, thumbnail, localThumb)) {
                thumbnail.setColorFilter(adapter.context.getAttributeColor(R.attr.textColor), PorterDuff.Mode.SRC_ATOP);
            } else {
                thumbnail.clearColorFilter();
            }
        } else {
            if (adapter.isSelectionMode()) getSelectedItems().put(media, this);
            self.setCardBackgroundColor(adapter.context.getAttributeColor(R.attr.colorSelection));
            if (isTintedThumbnail(media, thumbnail, localThumb)) {
                thumbnail.setColorFilter(adapter.context.getAttributeColor(R.attr.colorAccent2), PorterDuff.Mode.SRC_ATOP);
            } else {
                thumbnail.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
            }
        }
    }
}
