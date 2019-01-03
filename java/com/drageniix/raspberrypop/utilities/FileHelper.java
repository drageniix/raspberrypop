package com.drageniix.raspberrypop.utilities;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.fragments.DatabaseFragment;
import com.drageniix.raspberrypop.fragments.ServerFragment;
import com.drageniix.raspberrypop.media.Media;

import com.drageniix.raspberrypop.dialog.adapter.cycle.CycleManager;
import com.drageniix.raspberrypop.media.MediaMetadata;
import com.drageniix.raspberrypop.utilities.categories.ApplicationCategory;
import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;
import com.drageniix.raspberrypop.utilities.custom.CaseInsensitiveMap;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileHelper {
    private final int BUFFER = 2048;
    private DBHandler handler;
    private Context context;

    FileHelper(Context context, DBHandler handler){
        this.context = context;
        this.handler = handler;
    }

    public String getThumbnailPath(){
        File file = new File(context.getFilesDir(), "media_thumbnails");
        if (file.exists() || file.mkdirs()){
            return file.getAbsolutePath();
        } else return null;
    }

    public String getLocalPath() {
        File file = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (file != null && (file.exists() || file.mkdirs())) {
            return file.getAbsolutePath();
        } else return null;
    }

    public String getExternalCachePath() {
        File file = context.getExternalCacheDir();
        if (file != null && (file.exists() || file.mkdirs())) {
            return file.getAbsolutePath();
        } else return null;
    }

    private String getLocalCachePath() {
        File file = context.getCacheDir();
        if (file != null && (file.exists() || file.mkdirs())) {
            return file.getAbsolutePath();
        } else return null;
    }

    public void clearCreatedFiles(){
        deleteDir(new File(getLocalCachePath()));
        deleteDir(new File(getExternalCachePath()));
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else return dir != null && dir.isFile() && dir.delete();
    }

    public void saveQR(BaseActivity activity, Bitmap image, Uri uri){
        if (image != null && !image.isRecycled()) {
            try (FileOutputStream out = (FileOutputStream) activity.getContentResolver().openOutputStream(uri)) {
                image.compress(Bitmap.CompressFormat.PNG, 0, out);
                out.flush();
            } catch (Exception e) {
                Logger.log(Logger.FILE, e);
            }
            image.recycle();
        }
    }


    public Uri createUri(String name){
        try {
            File file = new File(name);
            if (file.exists() && !file.delete()){
                throw new FileNotFoundException("Cannot Delete File.");
            }
            if (!file.createNewFile()){
                throw new IOException("Cannot Create File.");
            }
            return getFileUri(file);
        } catch (Exception e){
            Logger.log(Logger.FILE, e);
            return null;
        }
    }

    public void saveMediaFile(Media media, String source, String destination, boolean setStreamingID){
        File destFile = new File(destination);
        File sourceFile = new File(source);
        if (sourceFile.exists()) {
            try (FileInputStream in = new FileInputStream(sourceFile)) {
                if (in != null) {
                    try (FileOutputStream fout = new FileOutputStream(destFile, false)) {
                        int count;
                        byte[] buffer = new byte[2048];
                        while ((count = in.read(buffer)) != -1) {
                            fout.write(buffer, 0, count);
                        }
                    }
                    if (setStreamingID) {
                        Uri uri = getFileUri(destFile);
                        media.setStreamingID(uri.toString());
                    }
                }
            } catch (Exception e) {
                Logger.log(Logger.FILE, e);
            }
        }
    }

    public void saveMediaContentFile(Media media, String source, String destination){
        File destFile = new File(destination);
        try(InputStream in = context.getContentResolver().openInputStream(Uri.parse(source))){
            if (in != null) {
                try (FileOutputStream fout = new FileOutputStream(destFile, false)) {
                    int count;
                    byte[] buffer = new byte[2048];
                    while ((count = in.read(buffer)) != -1) {
                        fout.write(buffer, 0, count);
                    }
                }

                Uri uri = getFileUri(destFile);
                media.setStreamingID(uri.toString());
            }
        } catch (Exception e){
            Logger.log(Logger.FILE, e);
        }
    }

    public void saveMediaDataFile(Media media, String data){
        File vcfFile = new File(media.getFilePath());
        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(vcfFile))){
            osw.write(data);
            Uri uri = getFileUri(vcfFile);
            media.setStreamingID(uri.toString());
            media.setAlternateID(data);
        } catch (Exception e) {
            Logger.log(Logger.FILE, e);
        }
    }

    public Uri createCSV(BaseActivity activity, Uri uri, Media...media){
        try {
            Set<Media> mediaSet = new LinkedHashSet<>();
            for (Media medium : media) {
                CycleManager.MediaCycle cycle = handler.getCycleManager().loadCycle(medium);
                if (cycle != null) mediaSet.addAll(cycle);
                else mediaSet.add(medium);
            }

            Media[] medium = mediaSet.toArray(new Media[mediaSet.size()]);

            CSVWriter writer = new CSVWriter(new FileWriter(activity.getContentResolver().openFileDescriptor(uri, "rwt").getFileDescriptor()));
            writer.writeNext(MediaMetadata.getCSVTitles());
            for (Media aMedium : medium) {
                writer.writeNext(aMedium.getEnabled().getCSV(aMedium).toCSVArray());
            }

            writer.close();
        } catch (Exception e){
            Logger.log(Logger.FILE, e);
            return null;
        }

        return uri;
    }

    public Uri createFile(BaseActivity activity, Uri uri, boolean backup, Media...originalMedium) {
        try {
            String[] files;
            if (backup){
                List<String> fileNames = new ArrayList<>();
                fileNames.add(handler.getDatabasePath());

                String preferencePath = handler.getFileHelper().getExternalCachePath() + "/preferences";
                try(ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(preferencePath, false))) {
                    output.writeObject(handler.getPreferences().getAllAccounts());
                    fileNames.add(preferencePath);
                } catch (Exception e){
                    Logger.log(Logger.FILE, e);
                }

                String templatePath = handler.getFileHelper().getExternalCachePath() + "/templates";
                try(ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(templatePath, false))) {
                    output.writeObject(handler.getTemplates().getAllTemplates());
                    fileNames.add(templatePath);
                } catch (Exception e){
                    Logger.log(Logger.FILE, e);
                }

                for(File thumbnail : new File(handler.getFileHelper().getThumbnailPath()).listFiles()){
                    fileNames.add(thumbnail.getAbsolutePath());}
                for(File local : new File(handler.getFileHelper().getLocalPath()).listFiles()){
                    fileNames.add(local.getAbsolutePath());}

                files = fileNames.toArray(new String[fileNames.size()]);
            } else {
                Set<Media> mediaSet = new LinkedHashSet<>();
                for (Media media : originalMedium) {
                    CycleManager.MediaCycle cycle = handler.getCycleManager().loadCycle(media);
                    if (cycle != null) mediaSet.addAll(cycle);
                    else mediaSet.add(media);
                }

                Media[] medium = mediaSet.toArray(new Media[mediaSet.size()]);

                clearCreatedFiles();
                files = new String[(medium.length * 3) + 1];

                File database = new File(getExternalCachePath() + "/rpop_database.csv");
                if (!database.exists() & !database.createNewFile())
                    throw new IOException("Cannot Create File.");
                CSVWriter writer = new CSVWriter(new FileWriter(database));
                files[0] = database.getAbsolutePath();
                String[][] datum = new String[medium.length][16];
                for (int i = 0; i < medium.length; i++) {
                    files[i + 1] = medium[i].getThumbnailString().isEmpty() ? null : medium[i].getThumbnailPath();
                    files[i + 1 + (medium.length)] = medium[i].getAuxiliaryString().isEmpty() ? null : medium[i].getAuixiliaryPath();
                    files[i + 1 + (medium.length * 2)] = medium[i].getEnabled() != StreamingApplication.LOCAL ? null : medium[i].getFilePath();
                    datum[i][0] = medium[i].getScanUID();
                    datum[i][1] = medium[i].getFigureName();
                    datum[i][2] = medium[i].getOriginal();
                    datum[i][3] = "Imported Tags"; //medium[i].getCollection();
                    datum[i][4] = medium[i].getCycleString() + "#" + medium[i].getCyclePosition();
                    datum[i][5] = String.valueOf(medium[i].getCycleType());
                    datum[i][6] = medium[i].getEnabled().name();
                    datum[i][7] = medium[i].getStreamingID();
                    datum[i][8] = medium[i].getAlternateID();
                    datum[i][9] = medium[i].getType();
                    datum[i][10] = medium[i].getTitle();
                    datum[i][11] = medium[i].getDetail();
                    datum[i][12] = medium[i].getSummary();
                    datum[i][13] = medium[i].getExternalID();
                    datum[i][14] = medium[i].getThumbnailString();
                    datum[i][15] = medium[i].getAuxiliaryString();
                    writer.writeNext(datum[i]);
                }
                writer.close();
            }

            BufferedInputStream origin;
            ZipOutputStream out = new ZipOutputStream(activity.getContentResolver().openOutputStream(uri));

            byte[] buffer = new byte[BUFFER];
            for (String file : files) {
                if (file != null) {
                    FileInputStream fi = new FileInputStream(file);
                    origin = new BufferedInputStream(fi, BUFFER);
                    ZipEntry entry = new ZipEntry(file.substring(file.lastIndexOf(File.separator) + 1));
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(buffer, 0, BUFFER)) != -1) {
                        out.write(buffer, 0, count);
                    }
                    origin.close();
                }
            }
            out.close();
        } catch (Exception e){
            Logger.log(Logger.FILE, e);
            return null;
        }

        return uri;
    }

    public int readFile(Uri uri, boolean restore){
        int filesRead = 0;
        clearCreatedFiles();
        try (ZipInputStream stream = new ZipInputStream(context.getContentResolver().openInputStream(uri))){
            File thumbnails = new File(getThumbnailPath());
            File local = new File(getLocalPath());

            ZipEntry entry = stream.getNextEntry();
            if (entry != null && !restore && entry.getName().equals("rpop_database.csv")) {
                CSVReader reader = new CSVReader(new BufferedReader(new InputStreamReader(stream)));
                String[] media;
                while ((media = reader.readNext()) != null) {
                    int version = media.length == 16 ? 1 : 0;
                    new Media(handler,
                            media[0],
                            media[1],
                            media[version + 1],
                            (handler.getBilling().hasPremium() ? media[version + 2] : handler.getDefaultCollection()),
                            media[version + 3],
                            Integer.parseInt(media[version + 4]),
                            new String[]{media[version + 5], media[version + 6], media[version + 7]},
                            new String[]{media[version + 8], media[version + 9], media[version + 10], media[version + 11], media[version + 12], media[version + 13], media[version + 14]});
                    filesRead++;
                }
            } else if (entry != null && restore && entry.getName().equals(handler.getDatabaseName())) {
                File file = new File(handler.getDatabasePath());

                for (File files : thumbnails.listFiles()) {if (!files.delete()) return 0;}
                for (File files : local.listFiles()) {if (!files.delete()) return 0;}

                byte[] buffer = new byte[BUFFER];
                try (FileOutputStream fout = new FileOutputStream(file, false)) {
                    int count;
                    while ((count = stream.read(buffer)) != -1)
                        fout.write(buffer, 0, count);
                }
            } else {
                return 0;
            }

            while ((entry = stream.getNextEntry()) != null) {
                File file = null;
                if (entry.getName().contains("-FILE")){
                    file = new File(local, entry.getName());
                } else if (entry.getName().endsWith(".png")){
                    file = new File(thumbnails, entry.getName());
                } else if (entry.getName().equals("preferences")){
                    handler.getPreferences().readAll(new ObjectInputStream(stream).readObject());
                    filesRead++;
                    continue;
                } else if (entry.getName().equals("templates")){
                    handler.getTemplates().readAll(new ObjectInputStream(stream).readObject());
                    filesRead++;
                    continue;
                }

                if (file != null) {
                    byte[] buffer = new byte[BUFFER];
                    File dir = entry.isDirectory() ? file : file.getParentFile();
                    if (!dir.isDirectory() && !dir.mkdirs())
                        throw new FileNotFoundException("Failed to ensure directory: " + dir.getAbsolutePath());
                    if (entry.isDirectory()) continue;
                    try (FileOutputStream fout = new FileOutputStream(file, false)) {
                        int count;
                        while ((count = stream.read(buffer)) != -1)
                            fout.write(buffer, 0, count);
                    }
                }
            }

            if (restore){
                handler.getBilling().updatePremium();
                if (!ServerFragment.refresh(true)) handler.getAllServers();
                filesRead += handler.getServers().size();
                if (!DatabaseFragment.refresh()) handler.getAllMedia();
                filesRead += handler.getMediaList(handler.getDefaultCollection()).size();
            }

        } catch (Exception e){
            Logger.log(Logger.FILE, e);
        }

        return filesRead;
    }

    private MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
    private CaseInsensitiveMap<String, String> sMimeTypeMap = createMap();
    private CaseInsensitiveMap<String, String> createMap(){
        CaseInsensitiveMap<String, String> sMimeTypeMap = new CaseInsensitiveMap<>();
        sMimeTypeMap.put("wpl", "application/vnd.ms-wpl");
        sMimeTypeMap.put("m3u8", "audio/x-mpegurl");
        sMimeTypeMap.put("js", "text/javascript");
        sMimeTypeMap.put("aax", "audio/vnd.audible.aax");
        sMimeTypeMap.put("epub", "application/epub+zip");
        sMimeTypeMap.put("mobi", "application/x-mobipocket-ebook");
        sMimeTypeMap.put("rpop", "application/rpop");

        sMimeTypeMap.put("gb","application/x-gameboy-rom");
        sMimeTypeMap.put("gba","application/x-gba-rom");
        sMimeTypeMap.put("32x","application/x-genesis-rom");
        sMimeTypeMap.put("sms","application/x-sms-rom");
        sMimeTypeMap.put("nes","application/x-nes-rom");
        sMimeTypeMap.put("nds","application/x-nintendo-ds-rom");
        sMimeTypeMap.put("n64","application/x-n64-rom");
        sMimeTypeMap.put("iso", "application/octetstream");
        return sMimeTypeMap;}

    private final List<String> gameExtensions = new ArrayList<>(Arrays.asList(new String[]{
            "GB", "GBA", "GBC", "32X", "SMS", "NES", "NDS", "N64",
            "ISO", "IMG", "CDI", "BIN",
            "ADF", "CPC", "DSK", "A26", "A52", "A78", "J64", "LNX",
            "D64", "DAPHNE", "GDI", "FDS", "MGW", }));

    private final List<String> bookExtensions = new ArrayList<>(Arrays.asList(new String[]{
            "EPUB", "MOBI", "TXT", "PDF", "PBD", "LIT", "AZW", "AZW3", "OPF",
            "DOC", "DOCX", "HTML", "PRC", "DJVU", "FB2", "HTM", "IBOOKS",
            "CBR", "CBZ", "CB7", "CBT", "CBA", "RTF"}));

    /**
     *
     * Checks if a file path matches a game mimeType
     *
     * @param path the path of the file
     * @return if the file is likely to be a game
     */
    public boolean isGame(String path){
        return gameExtensions.contains(getFileExtension(path).toUpperCase());
    }

    /**
     *
     * Checks if a file path matches a book mimeType
     *
     * @param path the path of the file
     * @return if the file is likely to be a game
     */
    public boolean isBook(String path){
        return bookExtensions.contains(getFileExtension(path).toUpperCase());
    }
    /**
     *
     * Attempts to get an extension from any file looking string.
     *
     * @param path the file path
     * @return the extension of the file, or an empty String
     */
    public String getFileExtension(String path){
        int i = path.lastIndexOf('.');
        return i < 0 ? "" : path.substring(i+1);
    }

    /**
     * Attempts to find the appropriate mimetype for a file
     *
     * @param path the path of the file
     * @return the mime type of the file
     */
    public String getMimeTypeForFile(String path) {
        String extension = getFileExtension(path).toLowerCase();
        if (mimeTypeMap.hasExtension(extension)){
            return mimeTypeMap.getMimeTypeFromExtension(extension);
        } else if (sMimeTypeMap.containsKey(extension)){
            return sMimeTypeMap.get(extension);
        } else {
            return null;
        }
    }

    /**
     *
     * Removes generic download information and fluff from a file name
     *
     * @param path the path of the file
     * @return a standard renaming of the file
     */
    public String normalizeTitle(String path){
        int lastDot = -1; if (path != null) lastDot = path.lastIndexOf('.');
        String name = (lastDot <= 0) ?  path : path.substring(0, lastDot);
        return Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "") //accents
                .replaceAll("(\\(.*\\))", "")    //parentheses
                .replaceAll("(\\[.*\\])","")     //brackets
                .replace("_", " ")               //underscores
                .replaceAll("[ ]{2,}", " ")      //empty spaces
                .trim();
    }

    public static boolean containsIgnoreCase(String baseString, boolean all, String...searchElements)     {
        if (baseString == null || searchElements == null) return false;

        boolean foundAll = false;
        for(String search : searchElements) {
            if (search != null) {
                final int length = search.length();
                if (length == 0) {
                    if (!all){
                        return true;
                    } else {
                        foundAll = true;
                        continue;
                    }
                }

                for (int i = baseString.length() - length; i >= 0; i--) {
                    foundAll = baseString.regionMatches(true, i, search, 0, length);
                    if (foundAll && !all)
                        return true;
                    else if (foundAll){
                        break;
                    }
                }

                if (!foundAll) return false;
            }
        }

        return foundAll;
    }

    /**
     * Attempts to locate the conent uri from a file.
     *
     * @param file file to find the uri of
     * @return the context uri from the file
     */
    public Uri getFileUri(final File file){
        return FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file);}

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders. Doesn't handle non-primary volumes.
     *
     * @param uri The Uri to query.
     */
    @Nullable
    public String getPath(final Uri uri) {
        try {
            // DocumentProvider
            if (DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if ("com.android.externalstorage.documents".equals(uri.getAuthority())) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + File.separator + split[1];
                    }

                }
                // DownloadsProvider
                else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                    final String id = DocumentsContract.getDocumentId(uri);
                    if (id.startsWith("raw:")){
                        return id.substring(4);}
                    else {
                        final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                        return getDataColumn(contentUri, null, null);
                    }
                }
                // MediaProvider
                else if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }

                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{
                            split[1]
                    };

                    return getDataColumn(contentUri, selection, selectionArgs);
                }
            }
            // MediaStore (and general)
            else if ("content".equalsIgnoreCase(uri.getScheme())) {
                return getDataColumn(uri, null, null);
            }
            // File
            else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        } catch (Exception e) {
            Logger.log(Logger.FILE, e);
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private String getDataColumn(Uri uri, String selection, String[] selectionArgs) {
        final String column = "_data";
        final String[] projection = {column};

        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null)){
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } catch (Exception e) {
            Logger.log(Logger.DB, e);
        }

        return null;
    }

    public void cleanFiles(String path){
        List<File> localFiles = new LinkedList<>(Arrays.asList(new File(path).listFiles()));
        if (!localFiles.isEmpty()) {
            List<String> localFileNames = new LinkedList<>();
            for (int i = 0; i < localFiles.size(); i++) {
                String name = localFiles.get(i).getName();
                int index = name.contains("-FILE.") ? name.lastIndexOf("-FILE.") :
                        (name.contains("-0.png") ?  name.lastIndexOf("-0.png") :
                            name.lastIndexOf("."));
                localFileNames.add(name.substring(0, index));
            }

            for (Media media : handler.getMediaList(handler.getDefaultCollection())) {
                for (int i = 0; i < localFileNames.size(); i++){
                    if (localFileNames.get(i).equals(media.getScanUID())){
                        localFileNames.set(i, "");
                        localFiles.set(i, null);
                    }
                }
            }

            for (File file : localFiles) {
                if (file != null) {
                    Logger.log(Logger.FILE, file + " was removed.");
                    file.delete();
                }
            }
        }
    }

    public long getFolderSize(File f) {
        long size = 0;
        if (f.isDirectory()) {
            for (File file : f.listFiles()) {
                size += getFolderSize(file);
            }
        } else {
            size=f.length();
        }
        return size;
    }

    public String translateBytes(long bytes, boolean metric){
        int unit = metric ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (metric ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (metric ? "" : "i");
        return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static String convertToMD5(String s) {
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                hexString.append(Integer.toHexString(0xFF & aMessageDigest));
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static boolean isNumeric(String s){
        return s != null && s.matches("[-+]?\\d*\\.?\\d+");
    }
}