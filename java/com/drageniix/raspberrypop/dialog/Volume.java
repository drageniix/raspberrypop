package com.drageniix.raspberrypop.dialog;

import android.os.Build;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.utilities.categories.AuxiliaryApplication;

class Volume {
    private BaseActivity activity;
    private EditText customID;
    private AutoCompleteTextView titleSearch;

    Volume(BaseActivity activity, EditText customID, AutoCompleteTextView titleSearch){
        this.activity = activity;
        this.customID = customID;
        this.titleSearch = titleSearch;
    }

    private CheckBox[] boxes;
    private SeekBar[] amounts;
    private RadioGroup dndGroup;

    View setVolume(int[] oldValues, boolean[] oldChecks){
        ScrollView scrollView = new ScrollView(activity);
        LinearLayout layout = new LinearLayout(activity);
        scrollView.addView(layout);
        layout.setOrientation(LinearLayout.VERTICAL);
        boxes = new CheckBox[8];
        amounts = new SeekBar[7];

        final String[] titles = new String[]{
                "Ringer",
                "Notification",
                "Media",
                "System",
                "Alarm",
                "Voice Call",
                "DTMF"
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //string resource
            final View dndView = View.inflate(activity, R.layout.media_disturb, null);
            boxes[7] = dndView.findViewById(R.id.checkbox);
            dndGroup = dndView.findViewById(R.id.radioGroup);
            dndGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    boxes[7].setChecked(true);
                    dndGroup.setTag(group.indexOfChild(dndView.findViewById(checkedId)));
                }
            });
            dndGroup.check(dndGroup.getChildAt(oldValues[7] < 0 ? 0 : oldValues[7]).getId());
            boxes[7].setChecked(oldChecks[7]);
            layout.addView(dndView);
        } else {
            dndGroup = null;
        }

        for(int i = 0; i < 7; i++) {
            final int index = i;
            final View view = View.inflate(activity, R.layout.media_seekbar, null);
            final TextView text = view.findViewById(R.id.text);
            amounts[i] = view.findViewById(R.id.seekbar);
            boxes[i] = view.findViewById(R.id.checkbox);

            text.setText(titles[i]);
            boxes[i].setChecked(oldChecks[i]);
            amounts[i].setProgress(oldValues[i]);
            amounts[i].setTag(i == 0 & oldValues[i] < 0 && oldValues[i] != -100);
            amounts[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    boxes[index].setChecked(true);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (index == 0 && seekBar.getProgress() == 0 && (seekBar.getTag() == null || !(Boolean)seekBar.getTag())){
                        seekBar.setTag(true);
                        text.setText(String.valueOf(titles[index] + " (Vibrate)"));
                    } else if (index == 0 && seekBar.getProgress() == 0){
                        seekBar.setTag(false);
                        text.setText(String.valueOf(titles[index] + " (Mute)"));
                    } else {
                        seekBar.setTag(false);
                        if (index == 0) text.setText(titles[index]);
                    }
                }
            });

            layout.addView(view);
        }
        return scrollView;
    }

    void getResults(AuxiliaryApplication enabledDevice){
        int finalAmount = 0;
        for(CheckBox box : boxes){
            if (box.isChecked()) finalAmount++;
        }

        titleSearch.setText(finalAmount == 0 ? "" :
                finalAmount + " " + enabledDevice.getName() + "(s) Set");

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < 7; i++){
            sb.append(boxes[i].isChecked() ?
                    (Boolean)amounts[i].getTag() ?
                            -99.0 : ((float)amounts[i].getProgress()/100) : "-1.0")
                    .append("~!~");
        }

        sb.append(dndGroup != null && boxes[7].isChecked() ? dndGroup.getTag() : "-1.0");
        customID.setText(sb.toString().trim());
    }
}
