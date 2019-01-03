package com.drageniix.raspberrypop.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.drageniix.raspberrypop.R;

public class SplashScreenActivity extends BaseActivity {

    @Override
    public void onResume() {
        super.onResume();
        if (getIntent().getData() != null) {
            int result = handler.getFileHelper().readFile(getIntent().getData(), false);
            if (result > 0) {
                Toast.makeText(getApplicationContext(),
                        result + " Task" + (result == 1 ? "" : "s") + " Registered!",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.share_failed), Toast.LENGTH_LONG).show();
            }
            getIntent().setData(null);
        }

        Intent intent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}