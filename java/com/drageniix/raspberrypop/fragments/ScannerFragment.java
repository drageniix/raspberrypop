package com.drageniix.raspberrypop.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentTransaction;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.activities.ScanActivity;
import com.drageniix.raspberrypop.utilities.FileHelper;
import com.drageniix.raspberrypop.utilities.Logger;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class ScannerFragment extends BaseFragment {

    public static synchronized ScannerFragment getsInstance(String collectionName) {
        ScannerFragment qr = new ScannerFragment();
        qr.collection = collectionName;
        return qr;
    }

	private static Pattern ISBNpattern = Pattern.compile("^(978)?\\d{10}$");
    private List<String> scannedUIDs;
    private String collection;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private SurfaceView cameraView;
    boolean flashmode = false;
    public static Uri imageUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        setHandler();

        if (!getBaseActivity().askCameraPermission(BaseActivity.BARCODE_REQUEST_CODE)){
            transition();
        }

        BaseActivity.setCameraOpen(true);

        DatabaseFragment.lastOrientation = getResources().getConfiguration().orientation;

        if (collection == null){
            collection = handler.getDefaultCollection();
        }

        scannedUIDs = new LinkedList<>();

        final View view = inflater.inflate(R.layout.qr_fragment, container, false);
        cameraView = view.findViewById(R.id.camera_view);

        FloatingActionButton fab = view.findViewById(R.id.databaseFab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                transition();
            }
        });

        barcodeDetector = new BarcodeDetector
                .Builder(getContext())
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build();

        cameraSource = new CameraSource
                .Builder(getContext(), barcodeDetector)
                .setRequestedFps(15.0f)
                .setAutoFocusEnabled(true)
                .build();

        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @SuppressLint("MissingPermission")
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    cameraSource.start(cameraView.getHolder());
                } catch (Exception e) {
                    Logger.log(Logger.FRAG, e);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                scan(detections.getDetectedItems());
            }
        });

        return view;
    }

    private boolean scan(SparseArray<Barcode> barcodes){
        if (barcodes != null && barcodes.size() > 0) {
            for(int i = 0; i < barcodes.size(); i++) {
                Barcode barcode = barcodes.valueAt(i);
                String newUid = FileHelper.convertToMD5(barcode.rawValue);
                if (!scannedUIDs.contains(newUid)) {
                    scannedUIDs.add(newUid);
                    boolean[] scanResult = ScanActivity.scan(newUid, getBaseActivity(), handler);
                    if (!scanResult[1]) {
                        if (i == barcodes.size() - 1 && !handler.getPreferences().continuousBarcode()) {
                            transition();
                        }
                        if (!scanResult[0]) {
                            handler.getParser().getBarCodeAPI().createMedia(getBaseActivity(), barcode, newUid, collection);
                        }
                    } else {
                        break;
                    }
                }
            }
            return true;
        }
        return false;
    }

    private void searchQR(){
        View prompt = View.inflate(getBaseActivity(), R.layout.simple_edittext, null);
        final TextView text = prompt.findViewById(R.id.text);
        final EditText title = prompt.findViewById(R.id.editText);
        text.setText("Barcode: ");

        new AlertDialog.Builder(getBaseActivity())
                .setView(prompt)
                .setPositiveButton(getString(R.string.submit), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String input = title.getText().toString().trim();
                        String newUid = FileHelper.convertToMD5(input);
                        if (!scannedUIDs.contains(newUid)) {
                            scannedUIDs.add(newUid);
                            boolean[] scanResult = ScanActivity.scan(newUid, getBaseActivity(), handler);
                            if (!scanResult[1]) {
                                if (!handler.getPreferences().continuousBarcode()) {
                                    transition();
                                }
                                if (!scanResult[0]) {
                                    handler.getParser().getBarCodeAPI().createMedia(getBaseActivity(), input, ISBNpattern.matcher(input).find(), newUid, collection);
                                }
                            }
                        }
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .create()
                .show();
    }

    private void scanImage(){
        if (imageUri != null) {
            Glide.with(this)
                    .asBitmap()
                    .load(imageUri)
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            super.onLoadFailed(errorDrawable);
                            imageUri = null;
                            Toast.makeText(ScannerFragment.this.getContext(), "Couldn't scan image.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                            try {
                                if (!scan(barcodeDetector.detect(frame))) {
                                    Toast.makeText(ScannerFragment.this.getContext(), "No codes detected.", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Logger.log(Logger.API, e);
                                Toast.makeText(ScannerFragment.this.getContext(), "Couldn't scan image.", Toast.LENGTH_SHORT).show();
                            }
                            imageUri = null;
                        }
                    });
        }
    }

    private void toggleFlash(boolean mode) {
        Camera camera = getCamera(cameraSource);
        if (camera != null) {
            try {
                Camera.Parameters param = camera.getParameters();
                if (!mode){
                    param.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                } else {
                    param.setFlashMode(!flashmode ? Camera.Parameters.FLASH_MODE_TORCH :Camera.Parameters.FLASH_MODE_OFF);
                    flashmode = !flashmode;
                }
                camera.setParameters(param);
            } catch (Exception e) {
                Logger.log(Logger.FRAG, e);
            }

        }
    }

    private static Camera getCamera(@NonNull CameraSource cameraSource) {
        Field[] declaredFields = CameraSource.class.getDeclaredFields();

        for (Field field : declaredFields) {
            if (field.getType() == Camera.class) {
                field.setAccessible(true);
                try {
                    Camera camera = (Camera) field.get(cameraSource);
                    if (camera != null) {
                        return camera;
                    }
                    return null;
                } catch (Exception e) {
                    Logger.log(Logger.FRAG, e);
                }
                break;
            }
        }
        return null;
    }

    public void transition(){
        BaseActivity.setCameraOpen(false);
        toggleFlash(false);
        getFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
        try {
            getFragmentManager().beginTransaction()
                    .replace(R.id.activity_content, DatabaseFragment.getsInstance(collection))
                    .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        } catch (Exception e) {
            Logger.log(Logger.FRAG, e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        scanImage();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.qr_menu, menu);
        setTitle("Barcode Scanner");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                getActivity().onBackPressed();
                break;
            case R.id.action_flash:
                toggleFlash(true);
                break;
            case R.id.action_searchQR:
                searchQR();
                break;
            case R.id.action_imageQR:
                getBaseActivity().openIntent(BaseActivity.SCAN_IMAGE_REQUEST_CODE);
                break;
            default:
                return super.onOptionsItemSelected(item);}
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) cameraSource.release();
        if (barcodeDetector != null) barcodeDetector.release();
    }
}