package com.drageniix.raspberrypop.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.webkit.URLUtil;
import android.widget.Toast;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.fragments.BaseFragment;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.media.MediaMetadata;
import com.drageniix.raspberrypop.utilities.FileHelper;
import com.drageniix.raspberrypop.utilities.Logger;
import com.drageniix.raspberrypop.utilities.api.ThumbnailAPI;
import com.drageniix.raspberrypop.utilities.categories.AuxiliaryApplication;
import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class CreationActivity extends BaseActivity {
    private String text, title, uid, type;
    private Media media;

    @Override
    public void onResume() {
        alreadyHandledTarget = true;
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        alreadyHandledTarget = true;
        if ((type = intent.getType()) != null) {
            title = null;
            if (type.equals("text/vcard") || type.equals("text/x-vcard")) {
                try(InputStream in = getContentResolver().openInputStream((Uri)intent.getExtras().get(Intent.EXTRA_STREAM))){
                    if (in != null) {
                        try(BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
                            StringBuilder total = new StringBuilder(in.available());
                            String line;
                            while ((line = r.readLine()) != null) {
                                total.append(line).append("\n");
                            }

                            text = total.toString().trim().replaceAll("(?s:\nPHOTO(.*)\n\n)", "\n");
                        }
                    }
                } catch (Exception e){
                    Logger.log(Logger.FILE, e);
                }

                title = intent.getStringExtra(Intent.EXTRA_TITLE);
                if (title == null) title = intent.getStringExtra(Intent.EXTRA_SUBJECT);
                if (title == null) title = "Shared Contact";
            } else if (type.equals("text/plain")) {
                text = intent.getStringExtra("url");
                if (text == null) text = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (text == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) text = getIntent().getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT).toString();

                title = intent.getStringExtra(Intent.EXTRA_SUBJECT);
                if (title == null) title = intent.getStringExtra(Intent.EXTRA_TITLE);
                if (title == null) title = "Shared Text";
            }

            if (text != null && !text.isEmpty()) {
                String generated = FileHelper.convertToMD5(text);
                media = handler.readMedia(intent.getStringExtra(ScanActivity.UID) == null ?
                        generated : intent.getStringExtra(ScanActivity.UID));

                uid = generated;
                if (media == null && !(handler.getBilling().canAddMedia())) {
                    Toast.makeText(this, R.string.premium_required, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    new generateQRcode(this, media, title, null).execute(uid, text, type);
                }
            } else finish();
        } else finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK  && data != null && data.getData() != null) {
            switch (requestCode) {
                case CREATE_QR_REQUEST_CODE:
				new generateQRcode(this, media, title, data.getData()).execute(uid, text, type);
				break;
            }
        } else {
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        }

        uid = text = type = title = null;
        target = null;
        targetMultiple = null;
    }

    private static class generateQRcode extends AsyncTask<String, Void, Bitmap> {
        private final static int WIDTH = 400, HEIGHT = 400;
        BaseActivity activity;
        Media media;
        String title;
		Uri uri;

        generateQRcode(BaseActivity activity, Media media, String title, Uri uri) {
            this.activity = activity;
            this.media = media;
            this.title = title;
			this.uri = uri;
        }

        private void createMedia(String...urls){
            String uid = urls[0];
            String data = urls[1];
            if (media == null){
                String type = urls[2];
                if (uri != null) {
                    title = activity.handler.getFileHelper().getPath(uri);
                    title = title.substring(title.lastIndexOf("/") + 1, title.lastIndexOf("."));
                    title.replace("_", " ").replaceAll("\\s{2,}", " ").trim();
                }

                if (type.equals("text/vcard") || type.equals("text/x-vcard")) {
                    Scanner vCardScanner = new Scanner(data);
                    String name = "Contact";
                    while (vCardScanner.hasNextLine()){
                        name = vCardScanner.nextLine();
                        if (name.startsWith("FN")){
                            name = name.substring(name.indexOf(":") + 1);
                            vCardScanner.close();
                            break;
                        }
                    }

                    media = new Media(activity.handler, StreamingApplication.CONTACT, uid, title, activity.handler.getDefaultCollection(), uid,
                            false, "", "", new String[]{""},
                            new MediaMetadata()
                                    .set(MediaMetadata.Type.INPUT_TITLE, name)
                                    .set(MediaMetadata.Type.TYPE, AuxiliaryApplication.VCARD.name())
                                    .set(MediaMetadata.Type.INPUT_CUSTOM, data)
                                    .set(MediaMetadata.Type.SUMMARY, ""), false);
                }  else if (URLUtil.isValidUrl(data)) {
                    media = new Media(activity.handler, StreamingApplication.URI, uid, title, activity.handler.getDefaultCollection(), uid,
                            false, "", "", new String[]{""}, null, false);
                    media.getEnabled().setMetadata(media, new MediaMetadata()
                                    .set(MediaMetadata.Type.INPUT_TITLE, data)
                                    .set(MediaMetadata.Type.INPUT_OPTION, URLUtil.isNetworkUrl(data) ? activity.getString(R.string.uri_1) : activity.getString(R.string.uri_2)),
                            activity.handler.getParser());

                    activity.handler.getParser().getThumbnailAPI().setThumbnailURL(media, ThumbnailAPI.Type.THUMBNAIL);
                } else {
                    media = new Media(activity.handler, StreamingApplication.OTHER, uid, title, activity.handler.getDefaultCollection(), uid,
                            false, "", "", new String[]{""}, null, false);

                    media.setTitle(title);
                    media.setSummary(data);
                    media.setType(AuxiliaryApplication.SIMPLE_NOTE.name());
                    media.setStreamingID(data);
                    activity.handler.getParser().getThumbnailAPI().setThumbnailText(media);
                }
            } else if (!media.getScanUID().contains(uid)){
                media.appendScanUID(uid);
            }

            media.setOriginal(data);
            if (!BaseFragment.addOrUpdate(media)){
                activity.handler.addOrUpdateMedia(media);
            }
        }

        protected Bitmap doInBackground(String... urls) {
            createMedia(urls);

            String Value = urls[1];
            com.google.zxing.Writer writer = new QRCodeWriter();
            Bitmap bitmap = null;
            BitMatrix bitMatrix;
            try {
                Map<EncodeHintType, Integer> map = new HashMap<>();
                map.put(EncodeHintType.MARGIN, 1);

                bitMatrix = writer.encode(Value, com.google.zxing.BarcodeFormat.QR_CODE, WIDTH, HEIGHT, map);
                bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
                for (int i = 0; i < HEIGHT; i++) {
                    for (int j = 0; j < WIDTH; j++) {
                        bitmap.setPixel(i, j, bitMatrix.get(i, j) ? Color.BLACK
                                : Color.WHITE);
                    }
                }
            } catch (Exception e) {
                Logger.log(Logger.API, e);
            }
            return bitmap;
        }

        protected void onPostExecute(Bitmap result) {
			if (uri == null){
				Set<Intent> intents = new LinkedHashSet<>();
				Intent createQRIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .setType("image/png")
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .putExtra(Intent.EXTRA_TITLE, title)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intents.add(createQRIntent);

				Uri uri = activity.handler.getFileHelper().createUri(activity.handler.getFileHelper().getExternalCachePath() + File.separator + title + ".png");
				activity.handler.getFileHelper().saveQR(activity, result, uri);

				Intent sendFileIntent = new Intent(Intent.ACTION_SEND)
                        .setType("image/png")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .putExtra(Intent.EXTRA_STREAM, uri)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

				Intent shareChooser = Intent.createChooser(sendFileIntent, title)
                        .putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toArray(new Parcelable[intents.size()]));
				activity.startActivityForResult(shareChooser, BaseActivity.CREATE_QR_REQUEST_CODE);
			} else {
				activity.handler.getFileHelper().saveQR(activity, result, uri);
                activity.startActivity(new Intent(activity, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                activity.finish();
			}
        }
    }
}