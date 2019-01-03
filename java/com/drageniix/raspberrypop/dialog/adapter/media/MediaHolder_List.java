package com.drageniix.raspberrypop.dialog.adapter.media;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.fragments.CycleFragment;
import com.drageniix.raspberrypop.dialog.DatabaseDialog;
import com.drageniix.raspberrypop.dialog.adapter.cycle.CycleManager;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.Logger;
import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;


public class MediaHolder_List extends MediaHolder {
    private View self, label;
    private TextView title, subtitle;
    protected ImageView icon;

    MediaHolder_List(View mediaCard, final MediaAdapter adapterMain, final DBHandler handler) {
        super(mediaCard, adapterMain, handler);
        self = itemView;
        label = itemView.findViewById(R.id.label);
        title = itemView.findViewById(R.id.title);
        subtitle = itemView.findViewById(R.id.description);
        icon = itemView.findViewById(R.id.icon);
        icon.setColorFilter(null);
    }

    public void clearAnimation() {
        self.clearAnimation();
    }

    public void startAnimation() {
        self.startAnimation(AnimationUtils.loadAnimation(adapter.context, android.R.anim.slide_in_left));
    }

    public void loadImages() {
        if (isSingular){
            handler.getParser().getThumbnailAPI().setIconImageView(adapter.context, iconOptions, icon, media);
        } else {
            icon.setImageDrawable(adapter.context.getIcon(R.drawable.ic_action_tasks, true));
        }
        if (!media.getLabel().isEmpty()){
            label.setBackgroundColor(Color.parseColor(media.getLabel()));
        } else {
            label.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    public void loadText() {
        if (media.getFigureName().equalsIgnoreCase("New Tag")) {
            title.setVisibility(View.GONE);
            subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        } else {
            title.setVisibility(View.VISIBLE);
            title.setText(media.getFigureName());
            subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        }

        if (isSingular){
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
        } else {
            CycleManager.MediaCycle cycle = handler.getCycleManager().loadCycle(media);
            String amount = cycle.size() + " Tasks";
            switch (media.getCycleType()) {
                case 0:
                    amount += " (" + adapter.context.getString(R.string.cycle_0) + ")";
                    break;
                case 1:
                    amount += " (" + adapter.context.getString(R.string.cycle_1) + ")";
                    break;
                case 2:
                    amount += " (" + adapter.context.getString(R.string.cycle_2) + ")";
                    break;
                case 3:
                    amount += " (" + adapter.context.getString(R.string.cycle_3) + ")";
                    break;
            }

            amount += "\n" + cycle.get(0).getDateString();
            subtitle.setText(amount);
        }
    }

    public void loadListeners() {
        label.setOnClickListener(labelClickListener());
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               if (isSingular){
                   DatabaseDialog.editMedia(adapter.context.getSupportFragmentManager(), handler, media);
               } else try {
                    adapter.context.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.activity_content, CycleFragment.getsInstance(media, -1))
                            .addToBackStack(null)
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .commit();
                } catch (Exception e) {
                    Logger.log(Logger.FRAG, e);
                }
            }
        });

        self.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!adapter.isRearrangeMode()) {
                    if (adapter.isSelectionMode()) {
                        adjustView(!getSelectedItems().containsKey(media), true);
                    } else if (isSingular){
                        MediaHolder_Card card = new MediaHolder_Card(View.inflate(adapter.context, R.layout.media_card, null), adapter, handler);
                        AlertDialog.Builder mediaDialog = new AlertDialog.Builder(adapter.context).setView(card.self);
                        final AlertDialog dialog = mediaDialog.create();
                        if (dialog.getWindow() != null)
                            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                        card.loadPopup(media, dialog);
                        dialog.show();
                    } else try {
                        adapter.context.getSupportFragmentManager().beginTransaction()
                                .replace(R.id.activity_content, CycleFragment.getsInstance(media, -1))
                                .addToBackStack(null)
                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                .commit();
                    } catch (Exception e) {
                        Logger.log(Logger.FRAG, e);
                    }
                }
            }
        });

        self.setOnCreateContextMenuListener(this);
    }

    public void adjustView(boolean selected, boolean safe) {
        if (!selected) {
            if (safe && adapter.isSelectionMode()) getSelectedItems().remove(media);
            self.setBackgroundColor(adapter.context.getAttributeColor(R.attr.backgroundColor));
            if (isSingular && isTinted(media, icon)) {
                icon.setColorFilter(adapter.context.getAttributeColor(R.attr.textColor), PorterDuff.Mode.SRC_ATOP);
            } else {
                icon.clearColorFilter();
            }
            title.getPaint().setShader(adapter.normalColor);
            title.invalidate();
        } else {
            if (adapter.isSelectionMode()) getSelectedItems().put(media, this);
            self.setBackgroundColor(adapter.context.getAttributeColor(R.attr.colorSelection));
            if (isSingular && isTinted(media, icon)) {
                icon.setColorFilter(adapter.context.getAttributeColor(R.attr.colorAccent2), PorterDuff.Mode.SRC_ATOP);
            } else {
                icon.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
            }
            title.getPaint().setShader(adapter.selectedColor);
            title.invalidate();
        }
    }
}
