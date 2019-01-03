package com.drageniix.raspberrypop.dialog;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.CardView;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.utilities.DBHandler;

import java.util.Arrays;
import java.util.List;

public class ColorAdapter extends android.widget.BaseAdapter {
    private boolean editMode;
    private DBHandler handler;
    private BaseActivity context;
    private List<String> colors;
    private Media media;
    private EditText focusedEditText;
    
    ColorAdapter(BaseActivity activity, DBHandler handler, Media media){
        this.context = activity;
        this.handler = handler;
        this.media = media;
        this.colors = Arrays.asList(activity.getResources().getStringArray(R.array.colors));
    }

    public boolean toggleEditMode(){
        editMode = !editMode;
        notifyDataSetChanged();
        return editMode;
    }

    public void hideKeyboard(){
        if (focusedEditText != null){
            handler.getPreferences().setColorString(colors.get((int)focusedEditText.getTag()), focusedEditText.getText().toString());
            if(focusedEditText.hasFocus()) {
                InputMethodManager in = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                in.hideSoftInputFromWindow(focusedEditText.getApplicationWindowToken(), 0);
            }
        }
    }

    @Override
    public int getCount() {
        return colors.size();
    }

    @Override
    public Object getItem(int position) {return null;}

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final CardView labelColor = editMode ?
                (CardView) View.inflate(context, R.layout.label_color_edit, null) :
                (CardView) View.inflate(context, R.layout.label_color, null);

        labelColor.setBackgroundColor(Color.parseColor(colors.get(position)));

        if (editMode){
            final EditText labelText = labelColor.findViewById(R.id.color_text);
            if (media != null && media.getLabel().equals(colors.get(position))){
                labelText.setShadowLayer(24,0, 0, context.getResourceColor(R.color.highlight));
            }
            labelText.setTag(position);
            labelText.setText(handler.getPreferences().getColorString(colors.get(position)));
            labelText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus){
                        focusedEditText = labelText;
                    } else {
                        hideKeyboard();
                    }
                }
            });
            labelText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))){
                        hideKeyboard(); return true;} else return false;}
            });
        } else {
            final TextView labelText = labelColor.findViewById(R.id.color_text);
            if (media != null && media.getLabel().equals(colors.get(position))){
                labelText.setShadowLayer(12, 0, 0, context.getResourceColor(R.color.highlight));
            }
            labelText.setText(handler.getPreferences().getColorString(colors.get(position)));
        }

        return labelColor;
    }
}
