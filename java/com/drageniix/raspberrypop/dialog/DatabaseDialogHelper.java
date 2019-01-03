package com.drageniix.raspberrypop.dialog;


import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.utilities.DBHandler;

public class DatabaseDialogHelper {
    private static Calculator calculator = new Calculator(" ");
    private Brightness brightness;
    private Tasker tasker;
    private Event event;
    private Volume volume;
    private Dice.DieDialog dice;

    DatabaseDialogHelper(BaseActivity activity, DBHandler handler, Media editMedia, View dialogView, EditText customID){
        AutoCompleteTextView titleSearch = dialogView.findViewById(R.id.title);

        tasker = new Tasker(activity, editMedia,
                (LinearLayout)dialogView.findViewById(R.id.taskerParams),
                (Switch)dialogView.findViewById(R.id.advanced),
                (AutoCompleteTextView)dialogView.findViewById(R.id.taskerSearchA),
                (AutoCompleteTextView)dialogView.findViewById(R.id.taskerSearchB));

        event = new Event(activity, handler, customID, titleSearch);

        brightness = new Brightness(activity, customID, titleSearch);

        volume = new Volume(activity, customID, titleSearch);

        dice = new Dice.DieDialog(activity, customID, titleSearch);
    }

    public static Calculator getCalculator() {
        return calculator;
    }

    Event getEvent() {
        return event;
    }

    Tasker getTasker() {
        return tasker;
    }

    Brightness getBrightness() {
        return brightness;
    }

    Volume getVolume() {
        return volume;
    }

    Dice.DieDialog getDice() {return dice;}
}
