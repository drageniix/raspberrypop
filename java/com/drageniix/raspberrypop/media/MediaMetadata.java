package com.drageniix.raspberrypop.media;

import java.util.LinkedList;
import java.util.List;

public class MediaMetadata {

    public enum Type {
        INPUT_TITLE, INPUT_OPTION, INPUT_CUSTOM,
        TITLE, DETAIL, SUMMARY, IMDB, THUMBNAIL, AUXILIARY, STREAMING, ALTERNATE, TYPE,
        SERVER_ID, EXTERNAL_ID,
        MEDIA_UID, MEDIA_CYCLE, MEDIA_ALTERNATE, MEDIA_TYPE, MEDIA_AUXILIARY, MEDIA_STREAMING,
        PACKAGE_NAME, PACKAGE_LABEL, PACKAGE_CLASS, PACKAGE_INTENT,
        CSV_FIGURE_NAME, CSV_ORIGINAL, CSV_SCAN_HISTORY, CSV_LABEL, CSV_COMMENTS, CSV_SOURCE, CSV_TITLE, CSV_DETAIL, CSV_SUMMARY
    }

    private String[] row;
    private boolean success;

    public MediaMetadata() {
        row = new String[Type.values().length];
    }

    public static String[] getCSVTitles(){
        List<String> csvElements = new LinkedList<>();
        for(Type type : Type.values()){
            if (type.name().startsWith("CSV_")){
                csvElements.add(type.name().substring(4));
            }
        }
        csvElements.set(0, "");
        return csvElements.toArray(new String[csvElements.size()]);
    }

    public String[] toCSVArray(){
        List<String> csvElements = new LinkedList<>();
        for(Type type : Type.values()){
            if (type.name().startsWith("CSV_")){
                csvElements.add(get(type));
            }
        }
        return csvElements.toArray(new String[csvElements.size()]);
    }

    public boolean recentSuccess(){
        return success;
    }

    public boolean putAll(MediaMetadata other){
        if (other == null){
            success =  false;
        } else {
            success = true;
            System.arraycopy(
                    other.row,
                    Type.TITLE.ordinal(),
                    row,
                    Type.TITLE.ordinal(),
                    Type.values().length - Type.TITLE.ordinal());
        }
        return success;
    }

    public MediaMetadata set(Type location, String data) {
        row[location.ordinal()] = data == null ? null : data.replace("null", "").trim();
        return this;
    }

    public String get(Type location) {
        String data = row[location.ordinal()];
        return data == null ? "" : data;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("[");
        for(Type metadata : Type.values()){
            if (row[metadata.ordinal()] != null && !row[metadata.ordinal()].isEmpty()) {
                stringBuilder.append("$ ")
                        .append(metadata.name())
                        .append(": ")
                        .append(row[metadata.ordinal()].trim())
                        .append("\n");
            }
        }
        return stringBuilder.toString().trim() + "]\n";
    }
}
