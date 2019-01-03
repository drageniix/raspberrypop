package com.drageniix.raspberrypop.dialog.adapter.media;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.activities.ScanActivity;
import com.drageniix.raspberrypop.dialog.DatabaseDialog;
import com.drageniix.raspberrypop.dialog.RecolorDialog;
import com.drageniix.raspberrypop.dialog.RenameDialog;
import com.drageniix.raspberrypop.fragments.CycleFragment;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.dialog.adapter.BaseHolder;
import com.drageniix.raspberrypop.utilities.DBHandler;

import com.drageniix.raspberrypop.utilities.Logger;
import com.drageniix.raspberrypop.utilities.categories.AuxiliaryApplication;
import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;
import com.drageniix.raspberrypop.utilities.custom.CaseInsensitiveMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

abstract class MediaHolder extends BaseHolder implements View.OnCreateContextMenuListener{
    protected MediaAdapter adapter;
    private String newCollectionEmojiBackup = "\uD83D\uDCC1";
    private String newCollectionEmojiOriginal = "\uD83D\uDDC2";

    MediaHolder(View mediaCard, MediaAdapter adapter, DBHandler handler) {
        super(mediaCard, handler);
        this.adapter = adapter;
    }

    boolean isTinted(Media media, ImageView icon){
        return (media.getEnabled().isFolder()
                    && icon.getDrawable() == AuxiliaryApplication.valueOf(media).getIcon()) ||
                ((media.getEnabled() == StreamingApplication.LAUNCH || media.getEnabled() == StreamingApplication.URI || media.getEnabled() == StreamingApplication.MAPS)
                    && icon.getDrawable() == media.getEnabled().getIcon()) ||
                media.getEnabled() == StreamingApplication.LOCAL;
    }

    boolean isTintedThumbnail(Media media, ImageView icon, boolean localThumb){
        return (media.getEnabled().isFolder()
                && icon.getDrawable() == AuxiliaryApplication.valueOf(media).getIcon()) ||
                ((media.getEnabled() == StreamingApplication.LAUNCH || media.getEnabled() == StreamingApplication.URI || media.getEnabled() == StreamingApplication.MAPS)
                        && icon.getDrawable() == media.getEnabled().getIcon()) ||
                (media.getEnabled() == StreamingApplication.LOCAL
                        && !localThumb);
    }

    String getLabelString(){
        String color = handler.getPreferences().getColorString(media.getLabel());
        return media.getLabel().isEmpty() || color.isEmpty() ? "" :
                ("\n\n\uD83C\uDFF7️ㅤ" + color + "ㅤ");
    }

    void setSummary(final String text, TextView summary) {
        Spannable textSpannable = new SpannableString(text);
        final String color = getLabelString();
        if (!color.isEmpty() && text.contains(color)){
            final int start = text.indexOf(color);
            final int end = start + color.length();
            textSpannable.setSpan(new BackgroundColorSpan(Color.parseColor(media.getLabel())), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textSpannable.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    adapter.search(handler.getPreferences().getColorString(media.getLabel()));
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    ds.setUnderlineText(false);
                    ds.setTypeface(Typeface.DEFAULT_BOLD);
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (!media.getComments().isEmpty()) {
            int lastIndex = text.indexOf(media.getComments());
            while (lastIndex != -1 && lastIndex < text.length()) {
                lastIndex = text.indexOf("#", lastIndex);
                if (lastIndex != -1) {
                    int sEnd = text.indexOf(" ", lastIndex),
                            nEnd = text.indexOf("\n", lastIndex),
                            eEnd = text.length();
                    final int end = (sEnd < nEnd || nEnd == -1) && sEnd != -1 ? sEnd :
                            nEnd < eEnd && nEnd != -1 ? nEnd : eEnd;
                    if (end - lastIndex > 1) {
                        final int start = lastIndex;
                        textSpannable.setSpan(new BackgroundColorSpan(adapter.context.getAttributeColor(R.attr.colorSelection)), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        textSpannable.setSpan(new ClickableSpan() {
                            @Override
                            public void onClick(View widget) {
                                adapter.search(text.substring(start, end));
                            }

                            @Override
                            public void updateDrawState(TextPaint ds) {
                                ds.setUnderlineText(false);
                                ds.setColor(adapter.context.getAttributeColor(R.attr.colorAccent2));
                                ds.setTypeface(Typeface.DEFAULT_BOLD);
                            }
                        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    lastIndex = end;
                }
            }
        }

        summary.setMovementMethod(LinkMovementMethod.getInstance());
        summary.setText(textSpannable, TextView.BufferType.SPANNABLE);
    }


    CaseInsensitiveMap<Media, MediaHolder> getSelectedItems(){return adapter.selectedItems;}

    @Override
    public void onItemSelected() {
        adjustView(true, false);
    }

    @Override
    public void onItemClear() {
        adjustView(false, false);
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        if (!adapter.isRearrangeMode()) {
            if (!adapter.isSelectionMode()) {
                menu.setHeaderTitle(media.getFigureName() + "'s Settings");
                menu.add("View Details")
                        .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            try {
                                adapter.context.getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.activity_content, CycleFragment.getsInstance(media, -1))
                                        .addToBackStack(null)
                                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                        .commit();
                                if (!hasMultiple){
                                    DatabaseDialog.editMedia(adapter.context.getSupportFragmentManager(), handler, media);
                                }
                            } catch (Exception e) {
                                Logger.log(Logger.FRAG, e);
                            }
                            return false;
                        }
                    });
                menu.add("Rename Tag")
                        .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                RenameDialog.getInstance(handler, adapter.context, Collections.singleton(media), new RenameDialog.OnPositive() {
                                    @Override
                                    public void submit(String name, String collection) {
                                        boolean changeName = name != null && !media.getFigureName().equals(name);
                                        boolean changeCollection = collection != null && !media.getCollection().equals(collection);

                                        if(changeName) {
                                            media.setFigureName(name); }
                                        if (changeCollection) {
                                            media.setOldCollection(media.getCollection());
                                            media.setCollection(collection); }
                                        if(changeName || changeCollection) {
                                            adapter.addOrUpdate(media); }
                                    }
                                });
                                return false;
                            }
                        });
                menu.add("Share Tag")
                        .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem) {
                                adapter.context.openIntent(BaseActivity.SHARE_REQUEST_CODE, media);
                                return false;
                            }
                        });
                menu.add("Simulate Scan")
                        .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem) {
                                ScanActivity.scan(media.getScanUID(), adapter.context, handler);
                                return false;
                            }
                        });
                menu.add("Delete Tag")
                        .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem) {
                                AlertDialog.Builder confirmationDialog = new AlertDialog.Builder(adapter.context);
                                confirmationDialog.setMessage(adapter.context.getString(R.string.confirm));
                                confirmationDialog.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        adapter.remove(media);
                                    }
                                });
                                confirmationDialog.setNegativeButton(adapter.context.getString(R.string.cancel), null);
                                confirmationDialog.create().show();
                                return false;
                            }
                        });
            } else {
                if (getSelectedItems().size() > 0) {
                    menu.setHeaderTitle(getSelectedItems().size() + " Item(s) Selected");
                    menu.add("Share Selection")
                            .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem menuItem) {
                                    adapter.context.openIntent(BaseActivity.SHARE_MULTIPLE_REQUEST_CODE, getSelectedItems().keySet().toArray(new Media[getSelectedItems().size()]));
                                    return false;
                                }
                            });
                    menu.add("Label Selection")
                            .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    RecolorDialog.getInstance(handler, null, adapter.context,
                                            new RecolorDialog.OnChooseLabel(){
                                                @Override
                                                public void chooseLabel(String color) {
                                                    List<Media> selectedIems = new ArrayList<>(getSelectedItems().keySet());
                                                    for (Media media : selectedIems) {
                                                        if (!media.getLabel().equals(color)) {
                                                            media.setLabel(color);
                                                            adapter.addOrUpdate(media);
                                                        }
                                                    }
                                                    adapter.resetSelection(false);
                                                }
                                            },
                                            new RecolorDialog.OnClearLabel(){
                                                @Override
                                                public void clearLabel() {
                                                    String color = "";
                                                    List<Media> selectedIems = new ArrayList<>(getSelectedItems().keySet());
                                                    for (Media media : selectedIems) {
                                                        if (!media.getLabel().equals(color)) {
                                                            media.setLabel(color);
                                                            adapter.addOrUpdate(media);
                                                        }
                                                    }
                                                    adapter.resetSelection(false);
                                                }
                                            }, null);
                                    return false;
                                }
                            });
                    if (handler.getBilling().hasPremium()) {
                        menu.add("Move Selection")
                                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        AlertDialog.Builder dialog = new AlertDialog.Builder(adapter.context);
                                        final LinearLayout view = (LinearLayout) View.inflate(adapter.context, R.layout.media_collection, null);

                                        final EditText input = view.findViewById(R.id.collectionName);
                                        final Spinner collectionSpinner = view.findViewById(R.id.collectionSpinner);
                                        final List<String> collections = new ArrayList<>(handler.getCollections());
                                        final String newCollectionEmoji = ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && new Paint().hasGlyph(newCollectionEmojiOriginal)) ?
                                                newCollectionEmojiOriginal : newCollectionEmojiBackup) + " New Collection";

                                        collections.add(0, newCollectionEmoji);

                                        ArrayAdapter<String> collectionAdapter = new ArrayAdapter<>(adapter.context, android.R.layout.simple_dropdown_item_1line, collections);
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
                                        collectionSpinner.setSelection(1);

                                        input.setVisibility(View.GONE);
                                        dialog.setView(view);
                                        dialog.setTitle("Move Selection?");
                                        dialog.setPositiveButton(adapter.context.getString(R.string.submit), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                String collection = input.getText().toString().trim().isEmpty() ?
                                                        handler.getPreferences().getCollection(handler.getDefaultCollection()) :
                                                        input.getText().toString().trim();

                                                List<Media> selectedIems = new ArrayList<>(getSelectedItems().keySet());
                                                for (Media media : selectedIems) {
                                                    if (!media.getCollection().equals(collection)) {
                                                        media.setOldCollection(media.getCollection());
                                                        media.setCollection(collection);
                                                        adapter.addOrUpdate(media);
                                                    }
                                                }

                                                adapter.resetSelection(false);
                                            }
                                        });
                                        dialog.setNegativeButton(adapter.context.getString(R.string.cancel), null);
                                        dialog.create().show();
                                        return false;
                                    }
                                });
                    }
                    menu.add("Delete Selection")
                            .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem menuItem) {
                                    adapter.resetSelection(true);
                                    return true;
                                }
                            });
                    /*menu.add("Turn Selection Off")
                            .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem menuItem) {
                                    for (Media media : getSelectedItems().keySet()) {
                                        media.turnOff(); //cycle instead
                                    }
                                    adapter.resetSelection(false);
                                    return true;
                                }
                            });*/
                }
            }
        }
    }

    View.OnClickListener labelClickListener(){
        return new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                RecolorDialog.getInstance(handler, null, adapter.context,
                        new RecolorDialog.OnChooseLabel(){
                            @Override
                            public void chooseLabel(String color) {
                                if (!media.getLabel().equals(color)) {
                                    media.setLabel(color);
                                    adapter.addOrUpdate(media);
                                }
                            }
                        },
                        new RecolorDialog.OnClearLabel(){
                            @Override
                            public void clearLabel() {
                                String color = "";
                                if (!media.getLabel().equals(color)) {
                                    media.setLabel(color);
                                    adapter.addOrUpdate(media);
                                }
                            }
                        }, null);
            }
        };
    }

}