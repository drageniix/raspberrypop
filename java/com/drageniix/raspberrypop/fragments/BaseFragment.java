package com.drageniix.raspberrypop.fragments;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.TypefaceSpan;
import android.util.DisplayMetrics;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.categories.AuxiliaryApplication;
import com.drageniix.raspberrypop.utilities.categories.ScanApplication;
import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;

public class BaseFragment extends Fragment{
    protected DBHandler handler;

    public static boolean addOrUpdate(Media media){
        return (CycleFragment.addOrUpdateMedia(media) | DatabaseFragment.addOrUpdateMedia(media));
    }

    public static void switchLoading(boolean on){
        CycleFragment.switchLoading(on);
        DatabaseFragment.switchLoading(on);
    }

    public static void updateDataset(Media media){
        CycleFragment.changeDataset();
        DatabaseFragment.changeMedia(media);
    }

    protected void setHandler(){
        handler = getBaseActivity().getHandler();
    }

    protected int getColumns(int size){
        DisplayMetrics realMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getRealMetrics(realMetrics);
        double dpi = realMetrics.densityDpi;
        double width = realMetrics.widthPixels;
        int result = (int)((width - (32*(dpi/160)))/(size*(dpi/160)));
        return result > 0 ? result : 1;
    }

    protected BaseActivity getBaseActivity(){
        return (BaseActivity)getActivity();
    }

    protected void setTitle(String title){
        BaseActivity activity = getBaseActivity();
        if (activity.getSupportActionBar() != null) {
            SpannableString text = new SpannableString(title);
            text.setSpan(new CustomTypefaceSpan(ResourcesCompat.getFont(getContext(), R.font.default_font)), 0, text.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            activity.getSupportActionBar().setTitle(text);
            activity.getSupportActionBar().setBackgroundDrawable(new ColorDrawable(activity.getAttributeColor(R.attr.colorPrimary)));
            activity.getWindow().setStatusBarColor(activity.getAttributeColor(R.attr.colorPrimaryDark));
            for(StreamingApplication icon : StreamingApplication.values()){
                if (icon.isInstalled()) icon.getIcon().clearColorFilter();}
            for(AuxiliaryApplication icon : AuxiliaryApplication.values()){
                if (icon.isInstalled()) icon.getIcon().clearColorFilter();}
            for(ScanApplication icon : ScanApplication.values()){
                if (icon.isInstalled()) icon.getIcon().clearColorFilter();}
        }
    }

    public static class CustomTypefaceSpan extends TypefaceSpan {

        private final Typeface newType;

        CustomTypefaceSpan(Typeface type) {
            super("");
            newType = type;
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            applyCustomTypeFace(ds, newType);
        }

        @Override
        public void updateMeasureState(TextPaint paint) {
            applyCustomTypeFace(paint, newType);
        }

        private static void applyCustomTypeFace(Paint paint, Typeface tf) {
            int oldStyle;
            Typeface old = paint.getTypeface();
            if (old == null) {
                oldStyle = 0;
            } else {
                oldStyle = old.getStyle();
            }

            int fake = oldStyle & ~tf.getStyle();
            if ((fake & Typeface.BOLD) != 0) {
                paint.setFakeBoldText(true);
            }

            if ((fake & Typeface.ITALIC) != 0) {
                paint.setTextSkewX(-0.25f);
            }

            paint.setTypeface(tf);
        }
    }
}
