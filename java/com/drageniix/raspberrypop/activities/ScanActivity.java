package com.drageniix.raspberrypop.activities;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.widget.Toast;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.fragments.BaseFragment;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.dialog.adapter.cycle.CycleManager;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.Logger;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.drageniix.raspberrypop.utilities.categories.ScanApplication.TASKER;

public class ScanActivity extends BaseActivity {
    public static final String UID ="UID", CONTENTS = "CONTENTS";

    @Override
    public void onResume() {
        super.onResume();
        String[] nfc = scanTag(getIntent());
        if (nfc[0] != null) {
            boolean[] scanResult = scan(nfc[0], this, handler);
            if (!scanResult[1]) {
                if (handler.getBilling().canAddMedia()) {
                    Intent openApp = new Intent(ScanActivity.this, MainActivity.class)
                            .putExtra(UID, nfc[0])
                            .putExtra(CONTENTS, nfc[1])
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(openApp);
                } else {
                    Toast.makeText(this, R.string.premium_required, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    public static boolean[] scan(String uid, final BaseActivity activity, DBHandler handler) {
        final Media media = handler.readMedia(uid);
        if (media != null && media.availableToStream()) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, ("Scanned " + media.getScanHistory().length + " time(s).\n" + media.getComments()).trim(), Toast.LENGTH_SHORT).show();
                }
            });
            new mediaPlayback(media, handler).execute(activity);
            return new boolean[]{true, true};
        } else if (media != null){
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, activity.getString(R.string.stream_failed), Toast.LENGTH_SHORT).show();
                }
            });
            return new boolean[]{true, false};
        } else {
            return new boolean[]{false, false};
        }
    }

    private static void readRecord(StringBuilder display, NdefRecord[] records){
        if (records == null) return;
        for (NdefRecord ndefRecord : records) {
            try {
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                    byte[] payload = ndefRecord.getPayload();
                    String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
                    int languageCodeLength = payload[0] & 0063;
                    display.append(new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding))
                            .append("\n");
                } else if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_URI)) {
                    byte[] payload = ndefRecord.getPayload();

                    int prefixCode = payload[0] & 0x0FF;
                    if (prefixCode >= URI_PREFIX.length) prefixCode = 0;

                    String reducedUri = new String(payload, 1, payload.length - 1, Charset.forName("UTF-8"));

                    String uri = URI_PREFIX[prefixCode] + reducedUri;
                    display.append(uri)
                            .append("\n");
                } else if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_SMART_POSTER)) {
                    readRecord(display, new NdefMessage(ndefRecord.getPayload()).getRecords());
                } else if (ndefRecord.getTnf() == NdefRecord.TNF_ABSOLUTE_URI) {
                    display.append(ndefRecord.toUri())
                            .append("\n");
                }
            } catch (Exception e) {
                Logger.log(Logger.API, e);
            }
        }
    }

    public static String[] scanTag(Intent intent){
        String[] tagDetails = new String[2];
        if (intent != null && intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) != null) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            StringBuilder sb = new StringBuilder();
            for (byte b : tag.getId()) {
                sb.append(String.format("%02X ", b));
            }

            String uid = sb.toString().trim().replace(" ", "");

            StringBuilder display = new StringBuilder()
                    .append("NFC Tag Serial Number:\n")
                    .append(uid)
                    .append("\n\nTechnologies:\n");

            for (String tech : tag.getTechList()) {
                display.append(tech).append("\n");
            }

            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                NdefMessage ndefMessage = ndef.getCachedNdefMessage();
                if (ndefMessage != null) {
                    display.append("\nContents:\n");
                    NdefRecord[] records = ndefMessage.getRecords();
                    readRecord(display, records);
                }
            }

            tagDetails[0] = uid;
            tagDetails[1] = display.toString().trim();
        }
        return tagDetails;
    }

    public static class mediaPlayback extends AsyncTask<BaseActivity, Void, Void> {
        private List<Media> mediaCycle;
        private CycleManager.MediaCycle fullCycle;
        private DBHandler handler;
        private Media initial;
        private boolean taskerWait;

        mediaPlayback(final Media media, final DBHandler handler) {
            this.handler = handler;
            this.initial = media;
            if (!DBHandler.isInitialized()){
                handler.getAllMedia();
            }

            fullCycle = handler.getCycleManager().loadCycle(media);
            fullCycle.get(0).incrementScan();
            handler.addOrUpdateMedia(fullCycle.get(0));

            switch (media.getCycleType()){
                case 1:
                    this.mediaCycle = Collections.singletonList(handler.getCycleManager().loadNext(media));
                    break;
                case 2:
                    this.mediaCycle = Collections.singletonList(fullCycle.get(0));
                    break;
                case 3:
                    this.mediaCycle = new LinkedList<>(fullCycle);
                    break;
            }

            if (media.getCycleType() == 2){ //random
                Collections.shuffle(fullCycle);
                handler.getCycleManager().update(fullCycle.get(0));
            }
        }

        private ScanActivity.mediaPlayback setTaskerWait(boolean taskerWait) {
            this.taskerWait = taskerWait;
            return this;
        }

        public boolean isTaskerWait() {return taskerWait;}

        public void updateProgress(BaseActivity activity, Media media){
            if (media == mediaCycle.get(mediaCycle.size()-1) && activity instanceof ScanActivity){
                activity.finish();
            }
        }

        @Override
        protected Void doInBackground(BaseActivity...contexts) {
            BaseActivity activity = contexts[0];
            for(Media media : mediaCycle) {
                if (media.availableToStream()) {
                    if (!TASKER.scan(activity, handler, media, this.setTaskerWait(true))){
                        media.getEnabled().scan(activity, handler, media, this);
                        TASKER.scan(activity, handler, media, this.setTaskerWait(false));
                        updateProgress(activity, media);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            BaseFragment.updateDataset(initial);
        }
    }

    final static String[] URI_PREFIX = new String[] {
    /* 0x00 */ "",
    /* 0x01 */ "http://www.",
    /* 0x02 */ "https://www.",
    /* 0x03 */ "http://",
    /* 0x04 */ "https://",
    /* 0x05 */ "tel:",
    /* 0x06 */ "mailto:",
    /* 0x07 */ "ftp://anonymous:anonymous@",
    /* 0x08 */ "ftp://ftp.",
    /* 0x09 */ "ftps://",
    /* 0x0A */ "sftp://",
    /* 0x0B */ "smb://",
    /* 0x0C */ "nfs://",
    /* 0x0D */ "ftp://",
    /* 0x0E */ "dav://",
    /* 0x0F */ "news:",
    /* 0x10 */ "telnet://",
    /* 0x11 */ "imap:",
    /* 0x12 */ "rtsp://",
    /* 0x13 */ "urn:",
    /* 0x14 */ "pop:",
    /* 0x15 */ "sip:",
    /* 0x16 */ "sips:",
    /* 0x17 */ "tftp:",
    /* 0x18 */ "btspp://",
    /* 0x19 */ "btl2cap://",
    /* 0x1A */ "btgoep://",
    /* 0x1B */ "tcpobex://",
    /* 0x1C */ "irdaobex://",
    /* 0x1D */ "file://",
    /* 0x1E */ "urn:epc:id:",
    /* 0x1F */ "urn:epc:tag:",
    /* 0x20 */ "urn:epc:pat:",
    /* 0x21 */ "urn:epc:raw:",
    /* 0x22 */ "urn:epc:",
    /* 0x23 */ "urn:nfc:"
    };
}
