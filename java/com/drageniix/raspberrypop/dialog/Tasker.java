package com.drageniix.raspberrypop.dialog;

import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.utilities.categories.ScanApplication;
import com.drageniix.raspberrypop.utilities.custom.AutocompleteAdapter;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

class Tasker {
    private static List<String> taskerTasks = new ArrayList<>();
    private List<EditText> taskerParams;
    private LinearLayout taskerParamHolder;
    private Switch taskerSwitch;
    private AutoCompleteTextView taskerSearchA, taskerSearchB;
    private String hint;
    private BaseActivity activity;
    private Media editMedia;

    Tasker(BaseActivity activity, Media editMedia, LinearLayout taskerParamHolder, Switch taskerSwitch, AutoCompleteTextView taskerSearchA, AutoCompleteTextView taskerSearchB){
        hint = activity.getString(R.string.tasker_parameters);
        this.taskerParamHolder = taskerParamHolder;
        this.taskerSwitch = taskerSwitch;
        this.taskerSearchA = taskerSearchA;
        this.taskerSearchB = taskerSearchB;
        this.activity = activity;
        this.editMedia = editMedia;
        setUpTasker();
    }
    
    String[] getTaskerParams(){
        List<String> params = new ArrayList<>();
        if (ScanApplication.TASKER.isInstalled()){
            for(int i = 0; i < taskerParams.size(); i++) {
                String param = taskerParams.get(i).getText().toString().trim();
                if (!param.isEmpty())params.add(param);
            }
        }
        if (params.isEmpty()) params.add("");
        return params.toArray(new String[params.size()]);
    }

    private void setUpTasker(){
        if (!ScanApplication.TASKER.isInstalled()){
            taskerSwitch.setVisibility(GONE);
            taskerSwitch.setChecked(false);
        } else {
            final ArrayAdapter<String> taskerAdapter = new AutocompleteAdapter<>(activity, taskerTasks);
            taskerSearchA.setAdapter(taskerAdapter);
            taskerSearchB.setAdapter(taskerAdapter);

            taskerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean enabled) {
                    if (enabled) {
                        if (taskerTasks.isEmpty()) {
                            taskerAdapter.clear();
                            ScanApplication.TASKER.search(activity, taskerTasks);
                            taskerAdapter.addAll(taskerTasks);
                        }

                        taskerParamHolder.setVisibility(VISIBLE);
                        taskerSearchA.setVisibility(VISIBLE);
                        taskerSearchB.setVisibility(VISIBLE);
                    } else {
                        taskerParamHolder.setVisibility(GONE);
                        taskerSearchA.setVisibility(GONE);
                        taskerSearchB.setVisibility(GONE);
                    }
                }
            });

            taskerParams = new ArrayList<>();
            final LinearLayout taskerLine = (LinearLayout) View.inflate(activity, R.layout.media_popup_operation, null);
            final EditText taskerText = taskerLine.findViewById(R.id.param);
            final ImageButton taskerAdd = taskerLine.findViewById(R.id.operation);
            taskerText.setHint(hint);
            taskerAdd.setBackground(activity.getIcon(R.drawable.ic_action_add_light, false));
            taskerAdd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final LinearLayout taskerLine = (LinearLayout) View.inflate(activity, R.layout.media_popup_operation, null);
                    final EditText taskerText = taskerLine.findViewById(R.id.param);
                    final ImageButton taskerSubtract = taskerLine.findViewById(R.id.operation);
                    taskerText.setHint(hint);
                    taskerLine.startAnimation(AnimationUtils.loadAnimation(activity, android.R.anim.slide_in_left));
                    taskerParamHolder.addView(taskerLine);
                    taskerParams.add(taskerText);

                    taskerSubtract.setBackground(activity.getIcon(R.drawable.ic_action_minus, false));
                    taskerSubtract.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            taskerParamHolder.removeView(taskerLine);
                            taskerParams.remove(taskerText);
                        }
                    });
                }
            });

            taskerParamHolder.addView(taskerLine);
            taskerParams.add(taskerText);
            loadTaskerFromMedia(editMedia);
        }
    }

    void loadTaskerFromMedia(Media editMedia){
        if (ScanApplication.TASKER.isInstalled()) {
            while (taskerParamHolder.getChildCount() > 1) {
                taskerParamHolder.removeView(taskerParamHolder.getChildAt(taskerParamHolder.getChildCount() - 1));
                taskerParams.remove(taskerParams.size() - 1);
            }

            taskerParams.get(0).setText("");

            if (editMedia != null) {
                taskerSearchA.setText(editMedia.getTaskerTaskA());
                taskerSearchB.setText(editMedia.getTaskerTaskB());
                taskerSwitch.setChecked(editMedia.useTasker());

                taskerParams.get(0).setText(editMedia.getTaskerParams()[0]);
                for (int i = 1; i < editMedia.getTaskerParams().length; i++) {
                    final LinearLayout taskerLineFilled = (LinearLayout) View.inflate(activity, R.layout.media_popup_operation, null);
                    final EditText taskerTextFilled = taskerLineFilled.findViewById(R.id.param);
                    final ImageButton taskerSubtractFilled = taskerLineFilled.findViewById(R.id.operation);
                    taskerTextFilled.setHint(hint);
                    taskerTextFilled.setText(editMedia.getTaskerParams()[i]);
                    taskerParamHolder.addView(taskerLineFilled);
                    taskerParams.add(taskerTextFilled);

                    taskerSubtractFilled.setBackground(activity.getIcon(R.drawable.ic_action_minus, false));
                    taskerSubtractFilled.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            taskerParamHolder.removeView(taskerLineFilled);
                            taskerParams.remove(taskerTextFilled);
                        }
                    });
                }
            }
        }
    }
}
