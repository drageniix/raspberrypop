package com.drageniix.raspberrypop.dialog.adapter.media;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.fragments.CycleFragment;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.dialog.adapter.cycle.CycleManager;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.Logger;
import com.drageniix.raspberrypop.utilities.categories.AuxiliaryApplication;
import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;

public class MediaHolder_Card_Task extends MediaHolder {
    private View self, label;
    private TextView title, subtitle;
    protected ImageView icon;
    private boolean summarized;
    private String summary, amount;

    MediaHolder_Card_Task(View mediaCard, final MediaAdapter adapterMain, final DBHandler handler) {
        super(mediaCard, adapterMain, handler);
        self = itemView;
        label = itemView.findViewById(R.id.label);
        title = itemView.findViewById(R.id.title);
        subtitle = itemView.findViewById(R.id.description);
        icon = itemView.findViewById(R.id.icon);
        icon.setImageDrawable(adapter.context.getIcon(R.drawable.ic_action_tasks, true));
    }

    public void clearAnimation() {
        self.clearAnimation();
    }

    public void startAnimation() {
        self.startAnimation(AnimationUtils.loadAnimation(adapter.context, android.R.anim.slide_in_left));
    }

    public void loadImages() {
        icon.clearColorFilter();
        if (!media.getLabel().isEmpty()){
            label.setBackgroundColor(Color.parseColor(media.getLabel()));
        } else {
            label.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    public void loadText() {
        CycleManager.MediaCycle cycle = handler.getCycleManager().loadCycle(media);

        title.setText(media.getFigureName());
        amount = cycle.size() + " Tasks";

        switch (media.getCycleType()){
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

        subtitle.setText(amount);

        StringBuilder summarySB = new StringBuilder(cycle.get(0).getDateString()).append("\n\n");
        for(Media media : cycle){
            summarySB
                    .append(media.getCycleType() == 0 ? offlineEmoji : media.availableToStream() ? "\uD83D\uDD16 " : warningEmoji)
                    .append(media.getEnabled().getName())
                    .append(" - ")
                    .append(media.getTitle());
            if (media.getDetail() != null && !media.getDetail().isEmpty()) {
                summarySB
                    .append(" (")
                    .append(media.getDetail())
                    .append(")");
            }

            if ((media.getEnabled().isFolder() && media.getEnabled() != StreamingApplication.DEVICE) || AuxiliaryApplication.valueOf(media) == AuxiliaryApplication.WIFI_CONNECTION){
                AuxiliaryApplication device = AuxiliaryApplication.valueOf(media);
                summarySB.append(" [")
                        .append(device.getName())
                        .append("]");
            }

            summarySB
                    .append("\n");
        }

        String comments = media.getComments().isEmpty() ? "" : "\n\n" + media.getComments();
        summary = (summarySB.toString().trim() + comments + getLabelString()).trim();
    }

    public void loadListeners() {
        label.setOnClickListener(labelClickListener());
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
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
                    } else if (summarized) {
                        summarized = false;
                        subtitle.setText(amount);
                    } else {
                        summarized = true;
                        loadText();
                        setSummary(summary, subtitle);
                    }
                }
            }});

        self.setOnCreateContextMenuListener(this);
    }

    public void adjustView(boolean selected, boolean safe) {
        if (!selected) {
            if (safe && adapter.isSelectionMode()) getSelectedItems().remove(media);
            self.setBackgroundColor(adapter.context.getAttributeColor(R.attr.backgroundColor));
            icon.clearColorFilter();
            title.getPaint().setShader(adapter.normalColor);
            title.invalidate();
        } else {
            if (adapter.isSelectionMode()) getSelectedItems().put(media, this);
            self.setBackgroundColor(adapter.context.getAttributeColor(R.attr.colorSelection));
            icon.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
            title.getPaint().setShader(adapter.selectedColor);
            title.invalidate();
        }
    }
}