package com.drageniix.raspberrypop.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.fragments.BaseFragment;
import com.drageniix.raspberrypop.fragments.ScannerFragment;
import com.drageniix.raspberrypop.fragments.SettingsFragment;
import com.drageniix.raspberrypop.dialog.adapter.cycle.CycleManager;
import com.drageniix.raspberrypop.dialog.DatabaseDialog;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.media.MediaMetadata;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.Logger;
import com.drageniix.raspberrypop.utilities.api.ThumbnailAPI;
import com.drageniix.raspberrypop.utilities.categories.AuxiliaryApplication;
import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {
    final public static int IMAGE_REQUEST_CODE = 69;
    final public static int RESTORE_REQUEST_CODE = 419;
    final public static int BACKUP_REQUEST_CODE = 382;
    final public static int FILE_REQUEST_CODE = 12;
    final public static int BARCODE_REQUEST_CODE = 14;
    final public static int SCAN_IMAGE_REQUEST_CODE = 20;
    final public static int PLACE_REQUEST_CODE = 91;
    final public static int OVERLAY_REQUEST_CODE = 38;
    final public static int CONTACT_REQUEST_CODE = 1010;
    final public static int CONTACT_VCARD_REQUEST_CODE = 1011;
    final public static int CONTACT_NUMBER_REQUEST_CODE = 1012;
    final public static int CONTACT_EMAIL_REQUEST_CODE = 1013;
    final public static int LOAD_REQUEST_CODE = 72;
    final public static int SETTING_REQUEST_CODE = 87;
    final public static int VOLUME_REQUEST_CODE = 33;
    final public static int FLASHLIGHT_REQUEST_CODE = 943;
    final public static int SHARE_REQUEST_CODE = 462;
    final public static int SHARE_MULTIPLE_REQUEST_CODE = 422;
    final public static int CREATE_QR_REQUEST_CODE = 245;

    final public static String NOTIFICATION_CHANNEL_ID = "raspberrypop";

    public static boolean cameraOpen, alreadyHandledTarget;
    protected static Typeface font;
    protected static Media[] targetMultiple;
    protected static Media target;
    private GoogleApiClient mGoogleApiClient;
    private Uri temp;

    protected DBHandler handler;
    public DBHandler getHandler(){
        handler = DBHandler.getsInstance(this);
        DBHandler.initialize(this);
        return handler;
    }

    @Override
    public void onResume() {
        super.onResume();
        getHandler();
        if (target != null && !alreadyHandledTarget){
            DatabaseDialog.editMedia(getSupportFragmentManager(), handler, target);
            target = null;
        }
        alreadyHandledTarget = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getHandler();
    }

    public static boolean isCameraOpen() {return cameraOpen;}
    public static void setCameraOpen(boolean newCameraOpen) {cameraOpen = newCameraOpen;}

    public void advertisePremium(final SettingsFragment fragment){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            handler.getBilling().advertise(BaseActivity.this, fragment);
            }
        });
    }

    public void openIntent(final int code, Media...targetMedia){
        if (targetMedia == null){
            target = null;
            targetMultiple = null;
        } else if (targetMedia.length == 1){
            target = targetMedia[0];
            targetMultiple = null;
        } else {
            target = null;
            targetMultiple = targetMedia;
        }

        Set<Intent> intents = new LinkedHashSet<>();
        String title = null;
        switch (code) {
            case SHARE_REQUEST_CODE: //QR
                CycleManager.MediaCycle cycle = handler.getCycleManager().loadCycle(target);
                String qrText = target.getEnabled().getQR(this, target);
                boolean isSingular = cycle.size() == 1 || target.getCycleType() == 1  || target.getCycleType() == 2;
                if (isSingular && target.availableToStream() && qrText != null && !qrText.isEmpty()) {
                    intents.add(new Intent(this, QRActivity.class)
                            .putExtra(Intent.EXTRA_TEXT, qrText));
                }
            case SHARE_MULTIPLE_REQUEST_CODE: //CSV
                title = (targetMultiple == null ? target.getFigureName() :
                        (targetMultiple[0].getFigureName() + (targetMultiple.length == 1 ? "" :
                                (" and " + (targetMultiple.length-1)) + " other" +
                                        ((targetMultiple.length-1 >= 2) ? "s" : ""))));

                intents.add(new Intent(this, CSVActivity.class)
                        .putExtra(Intent.EXTRA_RETURN_RESULT, code)
                        .putExtra(Intent.EXTRA_TITLE, title));
            case BACKUP_REQUEST_CODE: //RPOP
                if (title == null) title = "Backup";

                Intent rpopIntent = new Intent(this, RPOPActivity.class)
                        .putExtra(Intent.EXTRA_RETURN_RESULT, code)
                        .putExtra(Intent.EXTRA_TITLE, title);

                Intent shareChooser = Intent.createChooser(rpopIntent, title);
                shareChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toArray(new Parcelable[intents.size()]));
                alreadyHandledTarget = true;
                startActivity(shareChooser);
                break;
            case FLASHLIGHT_REQUEST_CODE:
                if (askCameraPermission(code)){
                    AuxiliaryApplication.valueOf(target).scan(this, handler, target, null);
                    target = null;
                }
                break;
            case VOLUME_REQUEST_CODE:
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !notificationManager.isNotificationPolicyAccessGranted()) {
                    Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                    startActivityForResult(intent, code);
                } else {
                    AuxiliaryApplication.valueOf(target).scan(this, handler, target, null);
                    target = null;
                }
                break;
            case SETTING_REQUEST_CODE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(getApplicationContext())) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, code);
                } else {
                    AuxiliaryApplication.valueOf(target).scan(this, handler, target, null);
                    target = null;
                }
                break;
            case OVERLAY_REQUEST_CODE:
                boolean note = AuxiliaryApplication.valueOf(target) == AuxiliaryApplication.SIMPLE_NOTE;
                startActivity(new Intent(this, note ? NoteActivity.class : ListActivity.class)
                        .putExtra(ScanActivity.UID, target.getScanUID())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                target = null;
                break;
            case CONTACT_REQUEST_CODE:
                Intent contactIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                        .setType(ContactsContract.Contacts.CONTENT_TYPE);
                startActivityForResult(contactIntent, code);
                break;
            case CONTACT_VCARD_REQUEST_CODE:
                if (askContactPermission(code)) {
                    Intent contactVCardIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                            .setType(ContactsContract.Contacts.CONTENT_TYPE);
                    startActivityForResult(contactVCardIntent, code);
                }
                break;
            case CONTACT_NUMBER_REQUEST_CODE:
                Intent contactNumberIntent = new Intent(Intent.ACTION_PICK)
                        .setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
                startActivityForResult(contactNumberIntent, code);
                break;
            case CONTACT_EMAIL_REQUEST_CODE:
                Intent contactEmailIntent = new Intent(Intent.ACTION_PICK)
                        .setType(ContactsContract.CommonDataKinds.Email.CONTENT_TYPE);
                startActivityForResult(contactEmailIntent, code);
                break;
            case PLACE_REQUEST_CODE:
                if (askLocationPermission(code)) {
                    if (mGoogleApiClient == null){
                        mGoogleApiClient = new GoogleApiClient.Builder(this)
                            .addApi(Places.GEO_DATA_API)
                            .addApi(Places.PLACE_DETECTION_API)
                            .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                                @Override
                                public void onConnected(@Nullable Bundle bundle) {
                                    try {
                                        startActivityForResult(new PlacePicker.IntentBuilder().build(BaseActivity.this), code);
                                    } catch (Exception e){
                                        Logger.log(Logger.CAST, e);
                                    }
                                }

                                @Override
                                public void onConnectionSuspended(int i) {}
                            }).build();
                            mGoogleApiClient.connect();
                    } else {
                        try {
                            startActivityForResult(new PlacePicker.IntentBuilder().build(this), code);
                        } catch (Exception e){
                            Logger.log(Logger.CAST, e);
                        }
                    }
                }
                break;
            case IMAGE_REQUEST_CODE:
                if(askCameraPermission(code)) {
                    final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    final List<ResolveInfo> listCam = getPackageManager().queryIntentActivities(captureIntent, 0);
                    temp = handler.getFileHelper().createUri(target.getCameraPath());
                    for (ResolveInfo res : listCam) {
                        final String packageName = res.activityInfo.packageName;
                        final Intent newCaptureIntent = new Intent(captureIntent)
                                .setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name))
                                .setPackage(packageName)
                                .putExtra(MediaStore.EXTRA_OUTPUT, temp);
                        intents.add(newCaptureIntent);
                    }

                    final Intent imageIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    imageIntent.setType("image/*");

                    final Intent chooserIntent = Intent.createChooser(imageIntent, getString(R.string.image_request));
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toArray(new Parcelable[intents.size()]));
                    startActivityForResult(chooserIntent, code);
                }
                break;
            case FILE_REQUEST_CODE:
            case LOAD_REQUEST_CODE:
            case RESTORE_REQUEST_CODE:
                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
                chooseFile.setType("*/*");
                startActivityForResult(
                        Intent.createChooser(chooseFile, getString(R.string.file_request)),
                        code
                );
                break;
            case SCAN_IMAGE_REQUEST_CODE:
                final Intent imageIntent2 = new Intent(Intent.ACTION_GET_CONTENT);
                imageIntent2.setType("image/*");
                final Intent chooserIntent2 = Intent.createChooser(imageIntent2, getString(R.string.image_request));
                startActivityForResult(chooserIntent2, code);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        boolean preserveTarget = false;
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (
                (requestCode == SETTING_REQUEST_CODE && Settings.System.canWrite(this))))
            || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                (requestCode == VOLUME_REQUEST_CODE && ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).isNotificationPolicyAccessGranted()))) {
            openIntent(requestCode, target);
        } else if (resultCode == RESULT_OK && data == null) {
            switch (requestCode){
                case IMAGE_REQUEST_CODE:
                    BaseFragment.switchLoading(true);

                    File thumbnail = new File(target.getThumbnailPath());
                    if(thumbnail.exists()) thumbnail.delete();
                    File camera = new File(target.getCameraPath());
                    camera.renameTo(thumbnail);
                    temp = null;

                    target.setThumbnailString("camera-HD");
                    if(!BaseFragment.addOrUpdate(target)){
                        handler.addOrUpdateMedia(target);
                    }
                    break;
                default:
                    break;
            }
        } else if (resultCode == RESULT_OK  && data != null) {
            switch (requestCode) {
                case FILE_REQUEST_CODE:
                    if (target != null) {
                        Uri uri = data.getData();
                        if (uri != null) {
                            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null, null)) {
                                String name = cursor != null && cursor.moveToFirst() ?
                                        cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)) :
                                        uri.getLastPathSegment();

                                //POTENTIAL <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
                                String path = handler.getFileHelper().getPath(uri);
                                path = (path == null) ? name : path;

                                String type = handler.getFileHelper().getMimeTypeForFile(path);
                                type = (type == null) ? "file" : "file|" + type;

                                for (ThumbnailAPI.Type file : ThumbnailAPI.Type.values()) {
                                    file.remove(target);
                                }

                                if (target.isInferredTitle()) name = target.getTitle();

                                target.clearMetadata();
                                target.setTitle(name);
                                target.setType(type);
                                target.setAlternateID(path);
                                target.setStreamingID(uri.toString());
                                target.setAuxiliaryString(uri.toString());
                                target.setMetadata(null, true);
                                preserveTarget = true;
                            }
                        }
                    }
                    break;
                case IMAGE_REQUEST_CODE:
                    if (target != null) {
                        BaseFragment.switchLoading(true);
                        File thumbnailPath = new File(target.getCameraPath());
                        if(thumbnailPath.exists()) thumbnailPath.delete();

                        if (!data.hasExtra("data")) {
                            Uri selectedImage = data.getData();
                            if (selectedImage != null) {
                                target.setThumbnailString(selectedImage.toString());
                                handler.getParser().getThumbnailAPI().asyncThumbnailURI(selectedImage, target);
                            }
                        } else {
                            target.setThumbnailString("camera");
                            Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
                            handler.getParser().getThumbnailAPI().setThumbnailBitmap(thumbnail, target, ThumbnailAPI.Type.THUMBNAIL);
                        }
                    }
                    break;
                case PLACE_REQUEST_CODE:
                    Place place = PlacePicker.getPlace(this, data);
                    if (place != null && place.isDataValid()) {
                        LatLng latLng = place.getLatLng();
                        target.clearMetadata();
                        target.setTitle(String.valueOf(place.getName()));
                        target.setSummary(String.valueOf(place.getAddress()));
                        target.setAlternateID(String.valueOf(place.getAddress()));
                        target.setStreamingID(latLng.latitude + "," + latLng.longitude);
                        target.setExternalID("geo:" + latLng.latitude + "," + latLng.longitude);
                        target.setMetadata(null, true);
                    }
                    break;
                case CONTACT_REQUEST_CODE:
                    Uri contactUri = data.getData();
                    String[] projection = new String[]{
                            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
                            ContactsContract.Contacts.DISPLAY_NAME,
                    };

                    try(Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null)) {
                        if (cursor != null && cursor.moveToFirst()) {
                            target.setMetadata(new MediaMetadata()
                                    .set(MediaMetadata.Type.TITLE, cursor.getString(cursor.getColumnIndex(projection[1])))
                                    .set(MediaMetadata.Type.TYPE, AuxiliaryApplication.VIEW_CONTACT.name())
                                    .set(MediaMetadata.Type.THUMBNAIL, cursor.getString(cursor.getColumnIndex(projection[0])))
                                    .set(MediaMetadata.Type.STREAMING, contactUri.toString()), false);
                            preserveTarget = true;
                        }
                    } catch (Exception e){
                        Logger.log(Logger.DB, e);
                    }
                    break;
                case CONTACT_VCARD_REQUEST_CODE:
                    Uri contactVCardUri = data.getData();
                    String[] vCardProjection = new String[]{
                            ContactsContract.Contacts.LOOKUP_KEY,
                            ContactsContract.Contacts.DISPLAY_NAME,
                            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
                    };

                    try (Cursor cursor = getContentResolver().query(contactVCardUri, vCardProjection, null, null, null)) {
                        if (cursor != null && cursor.moveToFirst()) {
                            Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, cursor.getString(cursor.getColumnIndex(vCardProjection[0])));
                            AssetFileDescriptor fd = this.getContentResolver().openAssetFileDescriptor(uri, "r");
                            FileInputStream fis = fd.createInputStream();
                            byte[] buf = new byte[(int) fd.getDeclaredLength()];
                            if (fis.read(buf) != -1) {
                                String vCard = new String(buf).replaceAll("(?s:\nPHOTO(.*)\n\n)", "\n");
                                if (!vCard.equals(new String(buf))) {
                                    target.setMetadata(new MediaMetadata()
                                            .set(MediaMetadata.Type.INPUT_TITLE, cursor.getString(cursor.getColumnIndex(vCardProjection[1])))
                                            .set(MediaMetadata.Type.TYPE, AuxiliaryApplication.VCARD.name())
                                            .set(MediaMetadata.Type.THUMBNAIL, cursor.getString(cursor.getColumnIndex(vCardProjection[2])))
                                            .set(MediaMetadata.Type.INPUT_CUSTOM, vCard), false);
                                }
                            }
                        }
                    } catch (Exception e){
                        Logger.log(Logger.DB, e);
                    }
                    break;
                case CONTACT_NUMBER_REQUEST_CODE:
                case CONTACT_EMAIL_REQUEST_CODE:
                    Uri contactNumberUri = data.getData();
                    String[] numberProjection = new String[3];
                    numberProjection[1] = ContactsContract.Contacts.DISPLAY_NAME;
                    numberProjection[2] = ContactsContract.Contacts.PHOTO_THUMBNAIL_URI;

                    String prefix = "";
                    AuxiliaryApplication type = (AuxiliaryApplication) target.getTempDetails()[0];
                    switch (type){
                        case CALL:
                            numberProjection[0] = ContactsContract.CommonDataKinds.Phone.NUMBER;
                            prefix = "tel:";
                            break;
                        case SMS:
                            numberProjection[0] = ContactsContract.CommonDataKinds.Phone.NUMBER;
                            prefix = "sms:";
                            break;
                        case EMAIL:
                            numberProjection[0] = ContactsContract.CommonDataKinds.Email.ADDRESS;
                            prefix = "mailto:";
                            break;
                    }

                    try (Cursor cursor = getContentResolver().query(contactNumberUri, numberProjection, null, null, null)) {
                        if (cursor != null && cursor.moveToFirst()) {
                            target.setMetadata(new MediaMetadata()
                                    .set(MediaMetadata.Type.DETAIL, cursor.getString(cursor.getColumnIndex(numberProjection[1])))
                                    .set(MediaMetadata.Type.TYPE, type.name())
                                    .set(MediaMetadata.Type.THUMBNAIL, cursor.getString(cursor.getColumnIndex(numberProjection[2])))
                                    .set(MediaMetadata.Type.ALTERNATE, (String)target.getTempDetails()[1])
                                    .set(MediaMetadata.Type.STREAMING, prefix + cursor.getString(cursor.getColumnIndex(numberProjection[0]))), false);
                        }
                    } catch (Exception e){
                        Logger.log(Logger.DB, e);
                    }
                    break;
                case SCAN_IMAGE_REQUEST_CODE:
                    ScannerFragment.imageUri = data.getData();
                    break;
                case LOAD_REQUEST_CODE:
                case RESTORE_REQUEST_CODE:
                    if (data.getData() != null) {
                        int result = handler.getFileHelper().readFile(data.getData(), requestCode == RESTORE_REQUEST_CODE);
                        if (result > 0) {
                            Toast.makeText(getApplicationContext(),
                                    requestCode == RESTORE_REQUEST_CODE ?
                                            "Restore Successful!" :
                                            result + " Task" + (result == 1 ? "" : "s") + " Registered!",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(), getString(R.string.share_failed), Toast.LENGTH_LONG).show();
                        }
                    }
                    break;
                default:
                    super.onActivityResult(requestCode, resultCode, data);
            }
        }

        if (!preserveTarget){
            target = null;
            targetMultiple = null;
        }
    }


    public boolean askLocationPermission(int PERMISSION_REQUEST_CODE){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {

                String[] permissions = {
                        Manifest.permission.ACCESS_COARSE_LOCATION};

                if (!handler.getPreferences().askedLocation()
                        || shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)){
                    requestPermissions(permissions, PERMISSION_REQUEST_CODE);
                } else {
                    Toast.makeText(this, getString(R.string.location_failed), Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        }
        return true;
    }

    public boolean askContactPermission(int PERMISSION_REQUEST_CODE){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED) {

                String[] permissions = {
                        Manifest.permission.READ_CONTACTS};

                if (!handler.getPreferences().askedContact()
                        || shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)){
                    requestPermissions(permissions, PERMISSION_REQUEST_CODE);
                } else {
                    Toast.makeText(this, getString(R.string.contact_failed), Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        }
        return true;
    }

    public boolean askCameraPermission(int PERMISSION_REQUEST_CODE){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {

                String[] permissions = {
                        Manifest.permission.CAMERA};

                if (!handler.getPreferences().askedCamera()
                        || shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                    requestPermissions(permissions, PERMISSION_REQUEST_CODE);
                } else {
                    Toast.makeText(this, getString(R.string.camera_failed), Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            switch (requestCode) {
                case PLACE_REQUEST_CODE:
                case FLASHLIGHT_REQUEST_CODE:
                case CONTACT_EMAIL_REQUEST_CODE:
                case CONTACT_NUMBER_REQUEST_CODE:
                case CONTACT_REQUEST_CODE:
                case CONTACT_VCARD_REQUEST_CODE:
                case IMAGE_REQUEST_CODE:
                    alreadyHandledTarget = true;
                    openIntent(requestCode, target);
                    break;
                case BARCODE_REQUEST_CODE:
                    ScannerFragment fragment = ScannerFragment.getsInstance(handler.getDefaultCollection());
                    try {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.activity_content, fragment)
                                .addToBackStack(null)
                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                .commit();
                    } catch (Exception e) {
                        Logger.log(Logger.FRAG, e);
                    }
                    break;
                default:
                    break;
            }
        } else {
            target = null;
            if (requestCode == BARCODE_REQUEST_CODE || requestCode == FLASHLIGHT_REQUEST_CODE || requestCode == IMAGE_REQUEST_CODE){
                Toast.makeText(this, getString(R.string.camera_failed), Toast.LENGTH_SHORT).show();
            } else if (requestCode == PLACE_REQUEST_CODE){
                Toast.makeText(this, getString(R.string.location_failed), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.contact_failed), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static Typeface getFont(Context context) {
        if (font == null){ font = ResourcesCompat.getFont(context, R.font.default_font); }
        return font;
    }

    public Drawable getIcon(int resId, boolean overlay){
        Drawable drawable = getTheme().getDrawable(resId);
        if (overlay){DrawableCompat.setTint(drawable, getAttributeColor(R.attr.textColor));}
        return drawable;
    }

    public int getResourceColor(int resource){
        return ResourcesCompat.getColor(getResources(), resource, getTheme());
    }

    public int getAttributeColor(int attribResId) {
        TypedValue tv = new TypedValue();
        boolean found = getTheme().resolveAttribute(attribResId, tv, true);
        return found ? tv.data : Color.YELLOW;
    }

    public BitmapDrawable getFileIcon(String text){
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_file).copy(Bitmap.Config.ARGB_8888, true);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 6, getResources().getDisplayMetrics()));
        paint.setTypeface(getFont(this));
        paint.setTextAlign(Paint.Align.CENTER);

        Canvas canvas = new Canvas(bm);
        canvas.drawText(handler.getFileHelper().getFileExtension(text), bm.getWidth()/2, bm.getHeight()/2 - (paint.ascent() + paint.descent()), paint);

        return new BitmapDrawable(getResources(), bm);
    }

    /*public Uri getIconAsset(String item){
        return Uri.parse("file:///android_asset/" + handler.getFileHelper().getFileIconForExtension(item));
    }*/

    public static void getNotificationChannel(Context context){
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Scan Notifications", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("RaspberryPOP");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(ResourcesCompat.getColor(context.getResources(), R.color.light_colorPrimary, context.getTheme()));
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    public static NotificationCompat.Builder createNotification(Context context, Media media, int icon, String title, String content){
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), icon);
        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(ResourcesCompat.getColor(context.getResources(), R.color.light_textColor, context.getTheme()), PorterDuff.Mode.SRC_IN));
        Bitmap image = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawBitmap(bitmap, 0, 0, paint);

        getNotificationChannel(context);

        return new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setColor(ResourcesCompat.getColor(context.getResources(), R.color.light_colorPrimary, context.getTheme()))
                .setSmallIcon(R.drawable.ic_launcher_notification)
                .setLargeIcon(image)
                .setContentTitle(title + (media == null ? "" : " (" + media.getFigureName() + ")"))
                .setContentText(content)
                .setContentIntent(PendingIntent.getActivity(context.getApplicationContext(), 0, new Intent(context, SplashScreenActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0))
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setPriority(Notification.PRIORITY_HIGH);
    }

    @Override
    public void setTheme(@StyleRes int resid) {
        super.setTheme(resid);

        StreamingApplication.URI.setIcon(this, handler);
        StreamingApplication.OFF.setIcon(this, handler);
        StreamingApplication.LOCAL.setIcon(this, handler);
        StreamingApplication.COPY.setIcon(this, handler);
        StreamingApplication.MAPS.setIcon(this, handler);
        StreamingApplication.CLOCK.setIcon(this, handler);
        StreamingApplication.CONTACT.setIcon(this, handler);
        StreamingApplication.OTHER.setIcon(this, handler);
        StreamingApplication.LAUNCH.setIcon(this, handler);
        StreamingApplication.DEVICE.setIcon(this, handler);

        for (AuxiliaryApplication category : AuxiliaryApplication.values()){
            category.setIcon(this, handler);}

        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getAttributeColor(R.attr.colorPrimaryDark));
    }
}