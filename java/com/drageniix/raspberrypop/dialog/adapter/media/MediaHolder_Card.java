package com.drageniix.raspberrypop.dialog.adapter.media;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.text.util.Linkify;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.request.RequestOptions;
import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.dialog.DatabaseDialog;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.categories.ScanApplication;
import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;

class MediaHolder_Card extends MediaHolder {
    CardView self;
    private View label;
    private TextView summary;
    private ImageView taskerIcon;
    private TextView title;
    private TextView subtitle;
    private ImageView thumbnail, icon;
    private boolean summarized;

    MediaHolder_Card(View mediaCard, final MediaAdapter adapterMain, final DBHandler handler) {
        super(mediaCard, adapterMain, handler);
        self = (CardView) itemView;

        label = itemView.findViewById(R.id.label);

        icon = itemView.findViewById(R.id.icon);
        taskerIcon = itemView.findViewById(R.id.taskericon);

        thumbnail = itemView.findViewById(R.id.thumb);

        title = itemView.findViewById(R.id.title);
        title.setTextSize(28);

        subtitle = itemView.findViewById(R.id.subtitle);

        summary = itemView.findViewById(R.id.summary);
        summary.setAutoLinkMask(Linkify.ALL);
        summary.setLinksClickable(true);
        summary.setTextSize(14);
    }

    private void setSummaryText(){
        String sum = media.getSummary().isEmpty() ? "" : "\n\n" + media.getSummary();
        String comments = media.getComments().isEmpty() ? "" : "\n\n" + media.getComments();
        setSummary(handler.getPreferences().debugDatabase() ? media.toString() :
                (media.getDateString() + sum + comments + getLabelString()).trim(), summary);
    }

    void loadPopup(final Media media, final AlertDialog dialog){
        this.media = media;

        this.iconOptions = new RequestOptions()
                .error(media.getEnabled().getIcon())
                .fitCenter();

        this.thumbOptions = new RequestOptions()
                .error(R.drawable.placeholder)
                .fitCenter();

        loadImages();
        loadText();

        setSummaryText();

        adjustView(false, false);
        self.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        label.setOnClickListener(labelClickListener());
        thumbnailListener(adapter, dialog, thumbnail);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseDialog.editMedia(adapter.context.getSupportFragmentManager(), handler, media);
                dialog.dismiss();
            }
        });
    }

    public void clearAnimation() {
        self.clearAnimation();}
    public void startAnimation() {
        self.startAnimation(AnimationUtils.loadAnimation(adapter.context, android.R.anim.slide_in_left));}

    public void loadImages() {
        if (!media.getLabel().isEmpty()){
            label.setBackgroundColor(Color.parseColor(media.getLabel()));
        } else {
            label.setBackgroundColor(Color.TRANSPARENT);
        }

        taskerIcon.setImageDrawable(
                media.useTasker() && media.getEnabled() != StreamingApplication.OFF ? ScanApplication.TASKER.getIcon() : null);

        handler.getParser().getThumbnailAPI().setIconImageView(adapter.context, iconOptions, icon, media);
        handler.getParser().getThumbnailAPI().setThumbnailImageView(adapter.context, thumbOptions, thumbnail, media);
    }

    public void loadText() {
        title.setText(media.getFigureName());

        String title = "";
        if (!media.availableToStream()) {
            title += media.getEnabled() == StreamingApplication.OFF || media.getCycleType() == 0 ? (!media.getTitle().isEmpty() ? offlineEmoji : "") : warningEmoji;
        }
        if (!media.getTitle().isEmpty()) {
            title += media.getTitle();
            if (media.getDetail() != null && !media.getDetail().isEmpty() && !media.getTitle().isEmpty()) {
                title += " (" + media.getDetail() + ")";
            }
        }

        subtitle.setText(title);
        subtitle.setVisibility(title.isEmpty() ? View.GONE : View.VISIBLE);

        summarized = false;
        summary.setText(handler.getPreferences().debugDatabase() ? media.toString() : "");
    }

    public void loadListeners() {
        label.setOnClickListener(labelClickListener());
        thumbnailListener(adapter,null, thumbnail);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseDialog.editMedia(adapter.context.getSupportFragmentManager(), handler, media);
            }
        });
        self.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!adapter.isRearrangeMode()) {
                    if (adapter.isSelectionMode()) {
                        adjustView(!getSelectedItems().containsKey(media), true);
                    } else {
                        if (summarized) {
                            summarized = false;
                            summary.setText("");
                        } else {
                            summarized = true;
                            setSummaryText();
                        }
                    }
                }
            }
        });

        self.setOnCreateContextMenuListener(this);
    }

    public void adjustView(boolean selected, boolean safe) {
        if (!selected) {
            if (safe && adapter.isSelectionMode()) getSelectedItems().remove(media);
            self.setCardBackgroundColor(adapter.context.getAttributeColor(R.attr.backgroundColor));
            thumbnail.clearColorFilter();
            if (isTinted(media, icon)) {
                icon.setColorFilter(adapter.context.getAttributeColor(R.attr.textColor), PorterDuff.Mode.SRC_ATOP);
            } else {
                icon.clearColorFilter();
            }
            title.getPaint().setShader(adapter.normalColor);
            title.invalidate();
        } else {
            if (adapter.isSelectionMode()) getSelectedItems().put(media, this);
            self.setCardBackgroundColor(adapter.context.getAttributeColor(R.attr.colorSelection));
            thumbnail.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
            if (isTinted(media, icon)) {
                icon.setColorFilter(adapter.context.getAttributeColor(R.attr.colorAccent2), PorterDuff.Mode.SRC_ATOP);
            } else {
                icon.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
            }
            title.getPaint().setShader(adapter.selectedColor);
            title.invalidate();
        }
    }
}