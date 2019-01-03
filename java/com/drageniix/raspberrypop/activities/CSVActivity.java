package com.drageniix.raspberrypop.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;

import com.drageniix.raspberrypop.media.Media;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

public class CSVActivity extends BaseActivity {
    @Override
    public void onResume() {
        alreadyHandledTarget = true;
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String title = intent.getStringExtra(Intent.EXTRA_TITLE);
        int code = intent.getIntExtra(Intent.EXTRA_RETURN_RESULT, 0);

        Set<Intent> intents = new LinkedHashSet<>();
        Intent createCSVIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .setType("text/csv")
                .addCategory(Intent.CATEGORY_OPENABLE)
                .putExtra(Intent.EXTRA_TITLE, title + ".csv");
        intents.add(createCSVIntent);

        Uri uri = handler.getFileHelper().createCSV(this,
                handler.getFileHelper().createUri(handler.getFileHelper().getExternalCachePath() + File.separator + title + ".csv"),
                code == SHARE_MULTIPLE_REQUEST_CODE ? targetMultiple : new Media[]{target});

        Intent sendFileIntent = new Intent(Intent.ACTION_SEND)
                .setType("text/csv")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .putExtra(Intent.EXTRA_STREAM, uri);

        Intent shareChooser = Intent.createChooser(sendFileIntent, title);
        shareChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toArray(new Parcelable[intents.size()]));
        startActivityForResult(shareChooser, code);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null && data.getData() != null){
            handler.getFileHelper().createCSV(this, data.getData(),
                    requestCode == SHARE_MULTIPLE_REQUEST_CODE ? targetMultiple : new Media[]{target});
        }
        target = null;
        targetMultiple = null;
        finish();
    }
}