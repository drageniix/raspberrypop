package com.drageniix.raspberrypop.activities;

import android.content.Intent;
import android.os.Bundle;

import com.drageniix.raspberrypop.media.Media;

public class QRActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);

        startActivity(new Intent(this, CreationActivity.class)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, text)
                .putExtra(Intent.EXTRA_TITLE, target.getFigureName())
                .putExtra(ScanActivity.UID, target.getScanUID())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

        target = null;
        targetMultiple = null;
        finish();
    }
}