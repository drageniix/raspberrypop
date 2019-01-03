package com.drageniix.raspberrypop.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;

import com.drageniix.raspberrypop.media.Media;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

public class RPOPActivity extends BaseActivity {
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

        Intent createFileIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .setType("application/octet-stream")
                .addCategory(Intent.CATEGORY_OPENABLE)
                .putExtra(Intent.EXTRA_TITLE, title + ".rpop");
        intents.add(createFileIntent);

        Uri uri = handler.getFileHelper().createFile(this,
                handler.getFileHelper().createUri(handler.getFileHelper().getExternalCachePath() + File.separator + title + ".rpop"),
                code == BACKUP_REQUEST_CODE,
                code == SHARE_MULTIPLE_REQUEST_CODE ? targetMultiple : new Media[]{target});

        Intent sendFileIntent = new Intent(Intent.ACTION_SEND)
                .setType("application/octet-stream")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .putExtra(Intent.EXTRA_STREAM, uri);

        Intent shareChooser = Intent.createChooser(sendFileIntent, title);
        shareChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toArray(new Parcelable[intents.size()]));
        startActivityForResult(shareChooser, code);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null && data.getData() != null){
            handler.getFileHelper().createFile(this, data.getData(),
                    requestCode == BACKUP_REQUEST_CODE,
                    requestCode == SHARE_MULTIPLE_REQUEST_CODE ? targetMultiple : new Media[]{target});
        }

        target = null;
        targetMultiple = null;
        finish();
    }
}