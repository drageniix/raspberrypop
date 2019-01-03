package com.drageniix.raspberrypop.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.utilities.DBHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RenameDialog extends DialogFragment{
    public interface OnPositive {
         void submit(String name, String collection);
    }

    private DBHandler handler;
    private Set<Media> media;
    private OnPositive onPositive;

    public static void getInstance(DBHandler handler, BaseActivity activity, Set<Media> media, OnPositive positive){
        RenameDialog fragment = new RenameDialog();
        fragment.handler = handler;
        fragment.media = media;
        fragment.onPositive = positive;
        fragment.show(activity.getSupportFragmentManager(), "rename_media");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (handler == null){
            dismiss();
            return new Dialog(getActivity());
        }

        final LinearLayout view = (LinearLayout) View.inflate(getContext(), R.layout.media_collection, null);
        final EditText input = view.findViewById(R.id.collectionName);
        final Spinner collectionSpinner = view.findViewById(R.id.collectionSpinner);

        View namePrompt = View.inflate(getContext(), R.layout.simple_edittext, null);
        final TextView text = namePrompt.findViewById(R.id.text);
        final EditText title = namePrompt.findViewById(R.id.editText);
        text.setText(String.valueOf("Tag Name: "));

        if (media.size() == 1) {
            view.addView(namePrompt, 0);
            title.setText(media.iterator().next().getFigureName());
        }

        if (!handler.getBilling().hasPremium()) {
            view.removeView(view.findViewById(R.id.collectionLine));
        } else {
            final List<String> collections = new ArrayList<>(handler.getCollections());
            final String newCollectionEmoji = ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && new Paint().hasGlyph(getContext().getString(R.string.new_collection))) ?
                    getContext().getString(R.string.new_collection) : getString(R.string.new_collection_backup)) + " New Collection";

            collections.add(0, newCollectionEmoji);

            ArrayAdapter<String> collectionAdapter = new ArrayAdapter<>(getContext(), R.layout.media_popup_dropdown, collections);
            collectionSpinner.setAdapter(collectionAdapter);
            collectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String choice = collections.get(position);
                    if (choice.equals(newCollectionEmoji)) {
                        input.setText("");
                        collectionSpinner.setVisibility(View.GONE);
                        input.setVisibility(View.VISIBLE);
                    } else {
                        input.setText(choice);
                        input.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            String collection = media.iterator().next().getCollection();
            for (Media media : media){
                if(!media.getCollection().equals(collection)){
                    collection = handler.getDefaultCollection();
                    break;
                }
            }

            collectionSpinner.setSelection(collections.indexOf(collection));
        }

        return new AlertDialog.Builder(getContext())
                .setView(view)
                .setPositiveButton(getContext().getString(R.string.submit), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newName = media.size() == 1 ? title.getText().toString().trim() : null;
                        if (newName != null && newName.isEmpty()) newName = "New Tag";

                        String collection = handler.getBilling().hasPremium() ?
                            (input.getText().toString().trim().isEmpty() ?
                                    handler.getPreferences().getCollection(handler.getDefaultCollection()) :
                                    input.getText().toString().trim()) :
                            null;

                        onPositive.submit(newName, collection);
                    }
                })
                .setNegativeButton(getContext().getString(R.string.cancel), null)
                .create();
    }
}
