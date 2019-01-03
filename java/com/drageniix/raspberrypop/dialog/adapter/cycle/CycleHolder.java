package com.drageniix.raspberrypop.dialog.adapter.cycle;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.text.util.Linkify;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.dialog.DatabaseDialog;
import com.drageniix.raspberrypop.media.MediaMetadata;
import com.drageniix.raspberrypop.dialog.adapter.BaseHolder;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.categories.ScanApplication;
import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;
import com.drageniix.raspberrypop.utilities.custom.CaseInsensitiveMap;

class CycleHolder extends BaseHolder {
    private CardView self;
    private TextView summary;
    private ImageView taskerIcon;
    private TextView title;
    private TextView subtitle;
    private ImageView thumbnail, icon;
    private CycleAdapter adapter;
    private CycleManager.MediaCycle cycle;

    CycleHolder(View mediaCard, final CycleAdapter adapter, final DBHandler handler, CycleManager.MediaCycle cycle) {
        super(mediaCard, handler);
        this.adapter = adapter;
        this.cycle = cycle;

        self = (CardView) itemView;

        icon = itemView.findViewById(R.id.icon);
        taskerIcon = itemView.findViewById(R.id.taskericon);

        thumbnail = itemView.findViewById(R.id.thumb);

        title = itemView.findViewById(R.id.title);
        title.setTextSize(24);

        subtitle = itemView.findViewById(R.id.subtitle);

        summary = itemView.findViewById(R.id.summary);
        summary.setAutoLinkMask(Linkify.ALL);
        summary.setLinksClickable(true);
        summary.setTextSize(14);
    }

    public void clearAnimation() {
        self.clearAnimation();
    }

    public void startAnimation() {
        self.startAnimation(AnimationUtils.loadAnimation(adapter.context, android.R.anim.slide_in_left));
    }

    public void loadImages() {
        taskerIcon.setImageDrawable(
                media.useTasker() && media.getEnabled() != StreamingApplication.OFF ? ScanApplication.TASKER.getIcon() : null);

        handler.getParser().getThumbnailAPI().setIconImageView(adapter.context, iconOptions, icon, media);
        handler.getParser().getThumbnailAPI().setThumbnailImageView(adapter.context, thumbOptions, thumbnail, media);
    }

    public void loadText() {
        String title = "";
        if (!media.availableToStream() && media.getCycleType() != 0) {
            title += media.getEnabled() == StreamingApplication.OFF  ? (!media.getTitle().isEmpty() ? offlineEmoji : "") : warningEmoji;
        }
        title += media.getTitle();
        this.title.setText(title);

        subtitle.setText(media.getDetail());
        subtitle.setVisibility(media.getDetail().isEmpty() ? View.GONE : View.VISIBLE);

        summary.setText(handler.getPreferences().debugDatabase() ? media.toString() : media.getSummary());
    }

    public void loadListeners() {
        thumbnailListener(adapter, null, thumbnail);

        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseDialog.editMedia(adapter.context.getSupportFragmentManager(), handler, media);
            }
        });

        final CaseInsensitiveMap<String, MediaMetadata> searchTitles = new CaseInsensitiveMap<>();
        StreamingApplication.COPY.search(handler, media, searchTitles, null, "");
        final String[] tags = searchTitles.keySet().toArray(new String[searchTitles.size()]);

        final String[] items = tags.length > 0 && hasMultiple ?
                new String[]{"Edit Task", "Move Task", "Delete Task"} :
                new String[]{"Edit Task", "Delete Task"};
        self.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(adapter.context);
                builder.setTitle("Task " + (adapter.indexOf(media) + 1) + "'s Settings");
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                DatabaseDialog.editMedia(adapter.context.getSupportFragmentManager(), handler, media);
                                break;
                            case 1:
                                if (tags.length > 0) {
                                    AlertDialog.Builder moveDialog = new AlertDialog.Builder(adapter.context);
                                    final ArrayAdapter<String> moveAdapter = new ArrayAdapter<>(adapter.context, android.R.layout.simple_dropdown_item_1line, tags);
                                    moveDialog.setAdapter(moveAdapter, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (cycle.size() != 1) {
                                                int index = adapter.indexOf(media);
                                                if (index == 0) {
                                                    cycle.update(0, 1);
                                                    adapter.notifyItemMoved(0, 1);
                                                }
                                                handler.getCycleManager().move(media, searchTitles.get(tags[which]).get(MediaMetadata.Type.MEDIA_CYCLE));
                                                adapter.notifyItemRemoved(index);
                                            } else {
                                                Toast.makeText(adapter.context, "Can't move last task!", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });
                                    moveDialog.create().show();
                                    break;
                                }
                            case 2:
                                AlertDialog.Builder confirmationDialog = new AlertDialog.Builder(adapter.context);
                                confirmationDialog.setMessage(adapter.context.getString(R.string.confirm));
                                confirmationDialog.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (cycle.size() != 1) {
                                            int index = adapter.indexOf(media);
                                            if (index == 0) {
                                                cycle.update(0, 1);
                                                adapter.notifyItemMoved(0, 1);
                                            }
                                            adapter.remove(media);
                                        } else {
                                            Toast.makeText(adapter.context, "Can't delete last task!", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                                confirmationDialog.setNegativeButton(adapter.context.getString(R.string.cancel), null);
                                confirmationDialog.create().show();
                                break;
                        }
                    }
                });
                builder.create().show();
            }
        });
    }

    public void adjustView(boolean selected, boolean safe) {
        if (!selected) {
            self.setBackgroundColor(adapter.context.getAttributeColor(R.attr.backgroundColor));
            thumbnail.clearColorFilter();
            if (media.getEnabled() == StreamingApplication.LOCAL && StreamingApplication.LOCAL.getIcon() != icon.getDrawable()) {
                icon.setColorFilter(adapter.context.getAttributeColor(R.attr.textColor), PorterDuff.Mode.SRC_ATOP);
            } else {
                icon.clearColorFilter();
            }
            title.getPaint().setShader(adapter.normalColor);
            title.invalidate();
        } else {
            self.setBackgroundColor(adapter.context.getAttributeColor(R.attr.colorSelection));
            thumbnail.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
            if (media.getEnabled() == StreamingApplication.LOCAL && StreamingApplication.LOCAL.getIcon() != icon.getDrawable()) {
                icon.setColorFilter(adapter.context.getAttributeColor(R.attr.colorAccent2), PorterDuff.Mode.SRC_ATOP);
            } else {
                icon.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
            }
            title.getPaint().setShader(adapter.selectedColor);
            title.invalidate();
        }
    }

    @Override
    public void onItemSelected() {
        adjustView(true, false);
    }

    @Override
    public void onItemClear() {
        adjustView(false, false);
    }

}
