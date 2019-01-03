package com.drageniix.raspberrypop.dialog;

import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.SeekBar;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.utilities.categories.AuxiliaryApplication;

import static android.view.View.GONE;

class Brightness {
    private BaseActivity activity;
    private EditText customID;
    private AutoCompleteTextView titleSearch;

    Brightness(BaseActivity activity, EditText customID, AutoCompleteTextView titleSearch){
        this.activity = activity;
        this.customID = customID;
        this.titleSearch = titleSearch;
    }

    View setBrightness(final AuxiliaryApplication enabledDevice, int progress){
        View view = View.inflate(activity, R.layout.media_seekbar, null);
        SeekBar amount = view.findViewById(R.id.seekbar);
        view.findViewById(R.id.checkbox).setVisibility(GONE);
        amount.setMax(255);
        amount.setProgress(progress);

        amount.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                customID.setText(String.valueOf(seekBar.getProgress()));
                String percentage = enabledDevice.getName() + " (" + String.valueOf((int) (100 * ((double) seekBar.getProgress() / (double) seekBar.getMax()))) + "%)";
                titleSearch.setText(percentage);
            }
        });

        return view;
    }
}
