package com.drageniix.raspberrypop.utilities.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.signature.ObjectKey;
import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.fragments.BaseFragment;
import com.drageniix.raspberrypop.fragments.CycleFragment;
import com.drageniix.raspberrypop.fragments.DatabaseFragment;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.utilities.Logger;
import com.drageniix.raspberrypop.utilities.categories.AuxiliaryApplication;
import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ThumbnailAPI extends APIBase {
    private Context context;
    private RequestOptions options;
    private DisplayMetrics metrics;

    ThumbnailAPI(Context context){
        this.context = context;
        this.metrics = context.getResources().getDisplayMetrics();
        this.options = new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true);
    }

    private float dipToPixels(float dipValue) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }

    public void setThumbnailImageView(BaseActivity context, RequestOptions thumbOptions, ImageView thumbnail, Media media){
        File thumbnailFile = new File("");
        thumbnail.clearColorFilter();
        if (!media.getThumbnailString().isEmpty()) {thumbnailFile= new File(media.getThumbnailPath());}
        else if (media.getEnabled() == StreamingApplication.LOCAL && !media.getAuxiliaryString().isEmpty()) {thumbnailFile = new File(media.getAuixiliaryPath());}
        Glide.with(context)
                .load(thumbnailFile)
                .apply(thumbOptions.signature(new ObjectKey(thumbnailFile.lastModified())))
                .into(thumbnail);
    }

    public void setIconImageView(BaseActivity context, RequestOptions iconOptions, ImageView icon, Media media){
        icon.clearColorFilter();
        if (media.getEnabled() == StreamingApplication.OFF) {
            icon.setImageDrawable(null);
        } else if ((media.getEnabled() == StreamingApplication.URI || media.getEnabled() == StreamingApplication.LAUNCH || media.getEnabled() == StreamingApplication.MAPS || media.getEnabled() == StreamingApplication.CONTACT)
                && !media.getAuxiliaryString().isEmpty()){
            File iconImage = new File(media.getAuixiliaryPath());
            Glide.with(context)
                    .load(iconImage)
                    .apply(iconOptions.signature(new ObjectKey(iconImage.lastModified())))
                    .into(icon);
        } else if (media.getEnabled() == StreamingApplication.LOCAL) {
            Glide.with(context)
                    .load(context.getFileIcon(media.getAlternateID()))
                    .apply(iconOptions)
                    .into(icon);
        } else if (media.getEnabled().isFolder()) {
            AuxiliaryApplication application = AuxiliaryApplication.valueOf(media);
            icon.setImageDrawable(application == null ? media.getEnabled().getIcon() : application.getIcon());
        } else {
            icon.setImageDrawable(media.getEnabled().isInstalled() ?
                    media.getEnabled().getIcon() :
                    context.getIcon(R.drawable.ic_action_warning, true));
        }
    }

    public void setThumbnailBitmap(Bitmap image, Media media, Type type){
        type.save(image, media);
    }

    public void setThumbnailURL(Media media, Type type){
        if (type.getString(media).equals("text")) return;

        if (handler.getPreferences().hasThumbnail() &&
                type.getString(media) != null && !type.getString(media).isEmpty()){
            try {
                if (type == Type.THUMBNAIL
                        && (media.getEnabled() == StreamingApplication.KODI)
                        && media.getTempDetails() != null){

                    GlideUrl url = new GlideUrl(type.getString(media), new LazyHeaders.Builder()
                            .addHeader((String)media.getTempDetails()[0], (String)media.getTempDetails()[1])
                            .build());

                    type.save(
                        Glide.with(context)
                                .asBitmap()
                                .apply(options)
                                .load(url)
                                .submit()
                                .get(),
                        media);
                } else {
                    type.save(
                        Glide.with(context)
                                .asBitmap()
                                .apply(options)
                                .load(type.getString(media))
                                .submit()
                                .get(),
                        media);
                }
            } catch (Exception e){
                Logger.log(Logger.FILE, e);
            }
        } else {
            type.remove(media);
        }
    }

    public void setThumbnailURI(Media media, Type type){
        if (type.getString(media) != null && !type.getString(media).isEmpty()){
            Bitmap image = null;
            Uri uri = Uri.parse(type.getString(media));
            if (media.getType().contains("pdf")){
                image = getPDFThumbnail(uri);
            } else if (media.getType().contains("epub") || media.getType().contains("mobi")){
                image = getEbookThumbnail(media, uri);
            } else if (media.getEnabled() == StreamingApplication.CONTACT || media.getType().contains("image") || media.getType().contains("video")) {
                try {
                    image = Glide.with(context)
                            .asBitmap()
                            .load(uri)
                            .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                            .get();
                } catch (Exception e) {
                    Logger.log(Logger.FILE, e);
                }
            }
            type.save(image, media);
        } else {
            type.remove(media);
        }
    }

    private Bitmap getEbookThumbnail(Media media, Uri path) {
        Bitmap image = null;
        try (ZipInputStream zipInputStream = new ZipInputStream(context.getContentResolver().openInputStream(path))){
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().contains("cover")) {
                    image = BitmapFactory.decodeStream(zipInputStream);
                } else if (entry.getName().contains("content.opf")){
                    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
                    parser.setInput(zipInputStream, null);
                    StringBuilder authors = new StringBuilder();
                    while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                        if (event == XmlPullParser.START_TAG
                                && parser.getName().equals("creator")
                                && parser.getPrefix().equals("dc")) {
                            authors.append(parser.nextText()).append(" ");
                        } else if (event == XmlPullParser.START_TAG
                                    && parser.getName().equals("title")
                                    && parser.getPrefix().equals("dc")) {
                                media.setTitle(((authors.length() == 0) ? "" : authors + "- ") + parser.nextText());
                                break;
                        }
                    }
                    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                }
            }
        } catch (Exception e) {
            Logger.log(Logger.FILE, e);
        }
        return image;
    }

    private Bitmap getPDFThumbnail(Uri path){
        Bitmap image = null;
        try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(path, "r")) {
            if (pfd != null) try (PdfRenderer renderer = new PdfRenderer(pfd)) {
                PdfRenderer.Page page = renderer.openPage(0);
                image = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                page.render(image, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();
            }
        } catch (Exception e) {
            image = null;
            Logger.log(Logger.FILE, e);
        }
        return image;
    }


    public void setThumbnailText(Media media) {
        if (media.getThumbnailString().isEmpty() || media.getThumbnailString().equals("text")) {
            String text = media.getSummary();
            if (text.isEmpty()){
                Type.THUMBNAIL.remove(media);
            } else {
                Bitmap image = Bitmap.createBitmap((int) dipToPixels(170), (int) dipToPixels(220), Bitmap.Config.ARGB_8888);

                Canvas canvas = new Canvas(image);
                canvas.drawColor(context.getResources().getColor(R.color.note));
                canvas.save();

                int textSize = text.length() < 150  && !media.getType().equals("nfc")? 24 : 16;
                TextPaint mTextPaint = new TextPaint();
                mTextPaint.setTypeface(Typeface.SANS_SERIF);
                mTextPaint.setTextSize(dipToPixels(textSize));
                StaticLayout mTextLayout = new StaticLayout(text, mTextPaint, (int) (canvas.getWidth() - dipToPixels(16)), Layout.Alignment.ALIGN_NORMAL, 1.1f, 0.01f, true);
                canvas.translate(dipToPixels(8), dipToPixels(6));
                mTextLayout.draw(canvas);
                canvas.restore();

                media.setThumbnailString("text");
                setThumbnailBitmap(image, media, Type.THUMBNAIL);
            }
        }
    }

    void asyncThumbnailURL(final Media target){
        if (handler.getPreferences().hasThumbnail()) {
            Glide.with(context)
                    .asBitmap()
                    .load(target.getThumbnailString())
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap bitmap, Transition<? super Bitmap> transition) {
                            setThumbnailBitmap(bitmap, target, Type.THUMBNAIL);
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            super.onLoadFailed(errorDrawable);
                            Type.THUMBNAIL.remove(target);
                            if (!BaseFragment.addOrUpdate(target)) {
                                handler.addOrUpdateMedia(target);
                            }
                        }
                    });
        } else {
            BaseFragment.switchLoading(false);
        }
    }

    public void asyncThumbnailURI(Uri uri, final Media target){
        Glide.with(context)
                .asBitmap()
                .load(uri)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap bitmap, Transition<? super Bitmap> transition) {
                        setThumbnailBitmap(bitmap, target, Type.THUMBNAIL);
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                        Type.THUMBNAIL.remove(target);
                        if (!BaseFragment.addOrUpdate(target)) {
                            handler.addOrUpdateMedia(target);
                        }
                    }
                });
    }

    public enum Type {
        THUMBNAIL {
            @Override
            public void save(Bitmap image, Media media) {
                saveImage(image, media, media.getThumbnailPath());
            }

            @Override
            public void remove(Media media) {
                media.setThumbnailString("");
                remove(media.getThumbnailPath());
            }

            @Override
            public String getString(Media media) {
                return media.getThumbnailString();
            }
        }, AUXILIARY {
            @Override
            public void save(Bitmap image, Media media) {
                saveImage(image, media, media.getAuixiliaryPath());
            }

            @Override
            public void remove(Media media) {
                media.setAuxiliaryString("");
                remove(media.getAuixiliaryPath());

            }
            @Override
            public String getString(Media media) {
                return media.getAuxiliaryString();
            }
        }, FILE {
            @Override
            public void save(Bitmap image, Media media) {

            }
            @Override
            public void remove(Media media) {
                remove(media.getFilePath());
            }
            @Override
            public String getString(Media media) {
                return media.getFilePath();
            }
        };

        void remove(String path){
            File file = new File(path);
            if (file.exists() && !file.delete()){
                Logger.log(Logger.FILE, new IOException("Cannot delete file."));
            }
        }

        void saveImage(Bitmap image, Media media, String path){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? Looper.getMainLooper().isCurrentThread() : Thread.currentThread() == Looper.getMainLooper().getThread()){
                new SaveMedia(path, image, media).execute();
            } else {
                boolean success = false;
                if (image != null && !image.isRecycled()) {
                    try (FileOutputStream out = new FileOutputStream(path)) {
                        image.compress(Bitmap.CompressFormat.PNG, 0, out);
                        out.flush();
                        success = true;
                    } catch (Exception e) {
                        image.recycle();
                        Logger.log(Logger.FILE, e);
                    }
                }

                if (!success) {
                    remove(path);
                }
            }
        }

        public abstract void save(Bitmap image, Media media);
        public abstract void remove(Media media);
        public abstract String getString(Media media);
    }

    public static class SaveMedia extends AsyncTask<Void, Void, Boolean> {
        String path;
        Bitmap image;
        Media target;

        SaveMedia(String path, Bitmap image, Media target){
            this.path = path;
            this.image = image;
            this.target = target;
        }

        @Override
        protected Boolean doInBackground(Void...voids) {
            boolean success = false;
            if (image != null && !image.isRecycled()) {
                try (FileOutputStream out = new FileOutputStream(path)) {
                    image.compress(Bitmap.CompressFormat.PNG, 0, out);
                    out.flush();
                    success = true;
                } catch (Exception e) {
                    image.recycle();
                    Logger.log(Logger.FILE, e);
                }
            }

            if(!success){
                File file = new File(path);
                if (file.exists() && !file.delete()){
                    Logger.log(Logger.FILE, new IOException("Cannot delete file."));
                }
            }

            return success;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (!BaseFragment.addOrUpdate(target)) {
                handler.addOrUpdateMedia(target);
            }
        }
    }
}
