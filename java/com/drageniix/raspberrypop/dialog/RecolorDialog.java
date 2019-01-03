package com.drageniix.raspberrypop.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.utilities.DBHandler;

import java.util.Arrays;
import java.util.List;

public class RecolorDialog extends DialogFragment {

    public interface OnChooseLabel {
        void chooseLabel(String color);
    }

    public interface OnClearLabel {
        void clearLabel();
    }

    public interface OnDismiss{
        void dismiss();
    }

    private DBHandler handler;
    private Media media;
    private BaseActivity activity;
    private List<String> baseColors;
    private OnChooseLabel chooseLabel;
    private OnClearLabel clearLabel;
    private OnDismiss dismiss;
    private ColorAdapter colorAdapter;

    public static void getInstance(DBHandler handler, Media media, BaseActivity activity,
                                   OnChooseLabel chooseLabel, OnClearLabel clearLabel, OnDismiss dismiss){
        RecolorDialog fragment = new RecolorDialog();
        fragment.handler = handler;
        fragment.media = media;
        fragment.activity = activity;
        fragment.chooseLabel = chooseLabel;
        fragment.clearLabel = clearLabel;
        fragment.dismiss = dismiss;
        fragment.baseColors = Arrays.asList(activity.getResources().getStringArray(R.array.colors));
        fragment.show(activity.getSupportFragmentManager(), "recolor_media");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (handler == null) {
            dismiss();
            return new Dialog(getActivity());
        }

        final View view = View.inflate(getContext(), R.layout.label_grid, null);
        GridView grid = view.findViewById(R.id.grid);
        final TextView title = view.findViewById(R.id.title);
        final ImageView edit = view.findViewById(R.id.edit);
        final ImageView clear = view.findViewById(R.id.clear);

        final AlertDialog dialog = new AlertDialog.Builder(getContext()).setView(view).create();

        colorAdapter = new ColorAdapter(activity, handler, media);
        grid.setAdapter(colorAdapter);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                chooseLabel.chooseLabel(baseColors.get(position));
                dialog.dismiss();
            }
        });

        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                colorAdapter.hideKeyboard();
                boolean editMode = colorAdapter.toggleEditMode();
                if (editMode) {
                    title.setText(getString(R.string.edit_label));
                    edit.setImageResource(R.drawable.ic_action_back);
                } else {
                    title.setText(getString(R.string.no_label));
                    colorAdapter.hideKeyboard();
                    edit.setImageResource(R.drawable.ic_action_edit_pen);
                }
            }
        });

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearLabel.clearLabel();
                colorAdapter.hideKeyboard();
                dialog.dismiss();
            }
        });

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface)
            {
                dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE| WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            }
        });

        return dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        colorAdapter.hideKeyboard();
        if (dismiss != null) dismiss.dismiss();
    }
}
