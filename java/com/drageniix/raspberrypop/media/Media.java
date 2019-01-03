package com.drageniix.raspberrypop.media;

import android.os.AsyncTask;
import android.text.TextUtils;

import com.drageniix.raspberrypop.fragments.BaseFragment;
import com.drageniix.raspberrypop.fragments.CycleFragment;
import com.drageniix.raspberrypop.fragments.DatabaseFragment;
import com.drageniix.raspberrypop.servers.ServerBase;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.Logger;
import com.drageniix.raspberrypop.utilities.TimeManager;
import com.drageniix.raspberrypop.utilities.api.ThumbnailAPI;
import com.drageniix.raspberrypop.utilities.categories.AuxiliaryApplication;
import com.drageniix.raspberrypop.utilities.categories.ScanApplication;
import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class Media {
    private DBHandler handler;
    private String
            newID, scanUID, original, comments, figureName, collection, oldCollection, label,
            type, title, externalID, detail, summary, thumbnailString, auxiliaryString,
            taskerTaskA, taskerTaskB,
            alternateID, streamingID, cycle;
    private String[] taskerParams, scanHistory;
    private Object[] tempDetails;
    private boolean useTasker, inferredTitle, refactoredEnabled, createQR;
    private long id;
    private int position, cyclePosition, cycleType;
    private StreamingApplication enabled;

    //Initial Addition
    public Media(DBHandler handler, StreamingApplication enabled, String scanUID, String figureName, String collection, String cycle,
                 boolean useTasker, String taskerTaskA, String taskerTaskB, String[] taskerParams,
                 MediaMetadata metadata, boolean createQR) {
        this.handler = handler;
        this.enabled = enabled;
        this.scanUID = scanUID;
        this.original = scanUID;
        this.comments = "";
        this.figureName = figureName;
        this.position = -1;
        this.collection = collection;
        this.label = "";
        this.cycle = cycle;
        this.cycleType = 3;
        this.cyclePosition = Integer.MAX_VALUE;
        this.scanHistory = new String[]{String.valueOf(Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis())};
        this.useTasker = useTasker;
        this.taskerTaskA = taskerTaskA;
        this.taskerTaskB = taskerTaskB;
        this.taskerParams = taskerParams;
        this.createQR = createQR;
        clearMetadata();
        setMetadata(metadata, this.enabled == StreamingApplication.COPY);
    }

    //Scan Only
    public Media(DBHandler handler, String scanUID, String cycleData, int cycleType, String[] streaming, String[] tasker) {
        this.handler = handler;
        this.scanUID = scanUID;

        if (tasker != null) {
            useTasker = Boolean.valueOf(tasker[0]);
            taskerTaskA = tasker[1];
            taskerTaskB = tasker[2];
            taskerParams = tasker[3].split("~!!~");
        } else {
            useTasker = false;
            taskerTaskA = "";
            taskerTaskB = "";
            taskerParams = new String[]{""};
        }

        this.cycleType = cycleType;
        if (cycleData.contains("#")) {
            String[] cycleArray = cycleData.split("#", -1);
            cycle = cycleArray[0];
            cyclePosition = Integer.parseInt(cycleArray[1]);
            if (cycleArray.length > 2) scanHistory = cycleArray[2].split(",");
            else scanHistory = new String[]{String.valueOf(Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis())};
        } else {
            cycle = getScanUID();
            cyclePosition = 0;
            scanHistory = new String[]{String.valueOf(Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis())};
        }

        try {
            enabled = StreamingApplication.valueOf(streaming[0]);
            streamingID = streaming[1];
            alternateID = streaming[2];
        } catch (Exception e){
            adjustEnabled(streaming);
        }
    }

    //From Database
    public Media(DBHandler handler, long identifier, String nfc, String origin, String comment, String labeling, String figure, String cycle, int cycleType, int sortPosition, String collectionName, String[] streaming, String[] metadata, String[] tasker) {
        this(handler, nfc, cycle, cycleType, streaming, tasker);
        original = origin;
        comments = comment;
        label = labeling;
        id = identifier;
        figureName = figure;
        position = sortPosition;
        collection = collectionName;

        if (!refactoredEnabled) {
            if (type == null || type.isEmpty()){
                if (enabled == StreamingApplication.CONTACT){
                    if (metadata[0].equalsIgnoreCase("Insert or Edit")){
                        type = AuxiliaryApplication.VCARD.name();
                    } else if (metadata[0].equalsIgnoreCase("View")){
                        type = AuxiliaryApplication.VIEW_CONTACT.name();
                    }
                } else if (enabled == StreamingApplication.CLOCK){
                    if (metadata[0].equalsIgnoreCase("Timer")){
                        type = AuxiliaryApplication.TIMER.name();
                    } else if (metadata[0].equalsIgnoreCase("Alarm")){
                        type = AuxiliaryApplication.ALARM.name();
                    } else if (metadata[0].equalsIgnoreCase("Event")){
                        type = AuxiliaryApplication.VEVENT.name();
                    }
                }
            } else if (enabled == StreamingApplication.OTHER){
                if (metadata[0].equalsIgnoreCase("counter")){
                    type = AuxiliaryApplication.COUNTER.name();
                } else {
                    type = AuxiliaryApplication.SIMPLE_NOTE.name();
                }
            }

            if (type == null || type.isEmpty()){type = metadata[0];}

            title = metadata[1];
            detail = metadata[2];
            summary = metadata[3];
            externalID = metadata[4];
            thumbnailString = metadata[5];
            auxiliaryString = metadata[6];
        }
    }

    //From File
    public Media(DBHandler handler, String nfc, String origin, String figure, String collection, String cycle, int cycleType, String[] streaming, String[] metadata) {
        this(handler, 0, nfc, origin, "", "", figure, cycle, cycleType, -1, collection, streaming, metadata, null);
        if (!BaseFragment.addOrUpdate(this)){
            handler.addOrUpdateMedia(this);
        }
    }

    public void update(StreamingApplication enabled, String figureName, String collection,
                boolean useTasker, String taskerTaskA, String taskerTaskB, String[] taskerParams,
                MediaMetadata metadata) {

        this.enabled = enabled;
        this.figureName = figureName;
        this.oldCollection = this.collection;
        this.collection = collection;
        this.useTasker = useTasker;
        this.taskerTaskA = taskerTaskA;
        this.taskerTaskB = taskerTaskB;
        this.taskerParams = taskerParams;
        setMetadata(metadata, this.enabled == StreamingApplication.COPY);
    }

    public String searchMediaData(){
        return toString().replace("|", " ").replaceAll("\\{(.*?)\\}","").replaceAll("\\s+"," ").trim();
    }

    public boolean availableToStream() {
        if (cycleType != 0 && getEnabled() == StreamingApplication.PLEX  || getEnabled() == StreamingApplication.KODI) {
            ServerBase server = handler.getServer(alternateID);
            return (server != null && server.isOnline());
        } else {
            return cycleType != 0 && !(getEnabled().equals(StreamingApplication.OFF) || getStreamingID().isEmpty());
        }
    }

    public void turnOff(){
        enabled = StreamingApplication.OFF;
        clearMetadata();
        for(ThumbnailAPI.Type file : ThumbnailAPI.Type.values()) file.remove(this);
        if (!BaseFragment.addOrUpdate(this)){
            handler.addOrUpdateMedia(this);
        }
    }

    public void clearMetadata(){
        alternateID = "";
        streamingID = "";
        type = "";
        title = "";
        externalID = "";
        detail = "";
        summary = "";
        thumbnailString = "";
        auxiliaryString = "";
    }

    public void setMetadata(MediaMetadata metadata, boolean forced){
        BaseFragment.switchLoading(true);
        if (forced || metadata != null) {
            new SetMetadata(handler, this).execute(metadata);
        } else {
            if (!BaseFragment.addOrUpdate(this)){
                handler.addOrUpdateMedia(this);
            }
        }
    }

    public void incrementScan(){
        String[] temp = new String[scanHistory.length + 1];
        System.arraycopy(scanHistory, 0, temp, 0, scanHistory.length);
        temp[scanHistory.length] = String.valueOf(Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis());
        scanHistory = temp;
    }

    public void appendScanUID(String scanUID) {this.newID = this.scanUID + "_" + scanUID;}

    private void adjustEnabled(String[] data){
        clearMetadata();
        String enabled = data[0], streaming = data[1], alternate = data[2];
        if (enabled.equalsIgnoreCase("NOTE") || enabled.equalsIgnoreCase("COUNTER")){
            refactoredEnabled = false;
            this.enabled = StreamingApplication.OTHER;
            this.type = "REFACTOR";
            streamingID = streaming;
            alternateID = alternate;
        } else if (!streaming.isEmpty() && (enabled.equalsIgnoreCase("Netflix") ||
                enabled.equalsIgnoreCase("Hulu") ||
                enabled.equalsIgnoreCase("Amazon") ||
                enabled.equalsIgnoreCase("Custom"))){

            refactoredEnabled = true;
            String url = enabled.equalsIgnoreCase("Netflix") ?
                    "http://www.netflix.com/watch/" : enabled.equalsIgnoreCase("Hulu") ?
                    "http://www.hulu.com/watch/" : enabled.equalsIgnoreCase("Amazon") ?
                    "http://www.amazon.com/dp/" : "";

            this.enabled = StreamingApplication.URI;
            setMetadata(new MediaMetadata()
                            .set(MediaMetadata.Type.INPUT_TITLE, url + streaming)
                            .set(MediaMetadata.Type.INPUT_OPTION, "Website Link")
                            .set(MediaMetadata.Type.INPUT_CUSTOM, ""),
                    false);
        } else {
            refactoredEnabled = true;
            this.enabled = StreamingApplication.OFF;
            for(ThumbnailAPI.Type file : ThumbnailAPI.Type.values()) file.remove(this);
            clearMetadata();
        }
    }

    public String getLabel() {return label;}
    public String getComments() {return comments;}
    public String getOriginal() {return original;}
    public StreamingApplication getEnabled() {return enabled;}
    public DBHandler getHandler() {return handler;}
    public boolean useTasker() {return useTasker && !(taskerTaskA.isEmpty() && taskerTaskB.isEmpty());}
    public boolean isInferredTitle() {return inferredTitle;}
    public int getCycleType() {return cycleType;}
    public String[] getScanHistory() {return scanHistory;}
    public long getId() {return id;}
    public String getScanUID() {return scanUID;}
    public String getNewScanUID() {return newID == null ? scanUID : newID;}
    public String getFigureName(){ return figureName;}
    public int getPosition() {return position;}
    public Object[] getTempDetails() {return tempDetails;}
    public boolean isCreateQR() {return createQR;}
    public int getCyclePosition() {return cyclePosition;}
    public String getCycleString() {return cycle;}
    public String getCollection() {return collection;}
    public String getOldCollection() {return oldCollection;}
    public String getAlternateID(){return alternateID;}
    public String getStreamingID(){return streamingID;}
    public String getType(){return type;}
    public String getTitle(){return title;}
    public String getExternalID(){return externalID;}
    public String getDetail(){return detail;}
    public String getSummary(){return summary;}
    public String getThumbnailString(){return thumbnailString;}
    public String getThumbnailPath(){return handler.getFileHelper().getThumbnailPath() + File.separator +  getScanUID() + ".png";}
    public String getCameraPath(){return handler.getFileHelper().getThumbnailPath() + File.separator +  getScanUID() + "-01.png";}
    public String getAuxiliaryString(){return auxiliaryString;}
    public String getAuixiliaryPath(){return handler.getFileHelper().getThumbnailPath() + File.separator +  getScanUID() + "-0.png";}
    public String getFilePath(){return handler.getFileHelper().getLocalPath() + File.separator + getScanUID() + "-FILE." + handler.getFileHelper().getFileExtension(alternateID);}
    public String getTaskerTaskA() {return taskerTaskA;}
    public String getTaskerTaskB() {return taskerTaskB;}
    public String[] getTaskerParams(){return taskerParams;}
    public String getTaskerParamString(){
        StringBuilder sb = new StringBuilder();
        for(String s : taskerParams){
            sb.append(s);
            sb.append("~!!~");
        }
        return sb.substring(0, sb.length()-4);}
    public String getDateString(){
        return "Added: " +
                handler.getTimeManager().format(TimeManager.FULL_TIME, new Date(Long.parseLong(scanHistory[0]))).replace("-","/") +
                "\nTotal Scans: " +
                (scanHistory.length - 1);}
    
    public String getScanHistoryString(){
        if (scanHistory == null) return "";
        return TextUtils.join(",", scanHistory);}
    public void setScanUID(String scanUID) {
        this.newID = null;
        if (!scanUID.equals(this.scanUID)) {
            File thumbnail = new File(getThumbnailPath());
            File auxiliary = new File(getThumbnailPath());
            File file = new File(getThumbnailPath());

            this.scanUID = scanUID;

            if (thumbnail.exists() && !thumbnail.renameTo(new File(getThumbnailPath()))) {
                Logger.log(Logger.FILE, new IOException("Cannot delete file."));
            }

            if (auxiliary.exists() && !auxiliary.renameTo(new File(getAuixiliaryPath()))) {
                Logger.log(Logger.FILE, new IOException("Cannot delete file."));
            }

            if (file.exists() && !file.renameTo(new File(getFilePath()))) {
                Logger.log(Logger.FILE, new IOException("Cannot delete file."));
            }
        }
    }

    public void setLabel(String label) {this.label = label;}
    public void setComments(String comments) {this.comments = comments;}
    public void setOriginal(String original) {this.original = original;}
    public void setCreateQR() {this.createQR = false;}
    public void setScanHistory(String[] scanHistory) {this.scanHistory = scanHistory;}
    public void setCycleType(int cycleType) {this.cycleType = cycleType;}
    public void setCyclePosition(int cycle) {this.cyclePosition = cycle;}
    public void setCycle(String cycle) {this.cycle = cycle;}
    public void setPosition(int position) {this.position = position;}
    public void setCollection(String collection) {this.collection = collection;}
    public void setOldCollection(String oldCollection) {this.oldCollection = oldCollection;}
    public void setTitle(String title) {this.title = title;}
    public void setExternalID(String id) {this.externalID = id;}
    public void setDetail(String detail) {this.detail = detail;}
    public void setSummary(String summary) {this.summary = summary;}
    public void setThumbnailString(String thumbnailString) {this.thumbnailString = thumbnailString;}
    public void setAuxiliaryString(String auxiliaryString) {this.auxiliaryString = auxiliaryString;}
    public void setStreamingID(String streamingID) {this.streamingID = streamingID;}
    public void setAlternateID(String alternateID) {this.alternateID = alternateID;}
    public void setType(String type) {this.type = type;}
    public void setId(long id) {this.id = id;}
    public void setTempDetails(Object[] tempDetails) {this.tempDetails = tempDetails;}
    public void setEnabled(StreamingApplication enabled) {this.enabled = enabled;}
    public void setInferredTitle(boolean inferredTitle) {this.inferredTitle = inferredTitle;}
    public void setFigureName(String figureName) {this.figureName = figureName;}

    @Override
    public boolean equals(Object object){
        if (!(object instanceof Media)) return false;
        if (object == this) return true;
        Media media = (Media) object;
        return getScanUID().equals(media.getScanUID());
    }

    @Override
    public String toString(){
        StringBuilder tagData = new StringBuilder()
            .append("\n\n{Entry #").append(getId()).append(":\t\t").append(getScanUID()).append("}")
            .append("\n{Original:\t\t").append(getOriginal()).append("}")
            .append("\n{Name}\t\t").append(getFigureName())
            .append("\n{").append(getDateString()).append("}")
            .append("\n{Position:\t\t").append(getPosition()).append("}")
            .append("\n{Cycle:\t\t").append(getCycleString()).append("#").append(cyclePosition).append("-").append(cycleType).append("}")
            .append("\n{Collection}\t").append(getCollection())
            .append("\n{Label:\t\t").append(getLabel()).append("}\t").append(handler.getPreferences().getColorString(getLabel()))
            .append("\n{Enabled}\t").append(getEnabled().name())
            .append("\n{Type}\t\t").append(getType())
            .append("\n{Comments}\t\t").append(getComments())
            .append("\n{Title}\t\t").append(getTitle())
            .append("\n{Detail}\t\t").append(getDetail())
            .append("\n{Summary}\t").append(getSummary())
            .append("\n{External ID}\t\t").append(getExternalID())
            .append("\n{Thumbnails}\t").append(getThumbnailString()).append(" | ").append(getAuxiliaryString())
            .append("\n{Streaming}\t").append(getStreamingID()).append(" | ").append(getAlternateID());
        if (!getTaskerTaskB().isEmpty() || !getTaskerTaskA().isEmpty()){tagData
                .append("\n").append(ScanApplication.TASKER.name())
                .append(" |\t\t{Before Task}\t").append(getTaskerTaskB())
                .append("\t{After Task}\t").append(getTaskerTaskA())
                .append("\n{Tasker Params}\t ").append(Arrays.toString(getTaskerParams()));
        }
        return tagData.toString().trim();
    }

    private static class SetMetadata extends AsyncTask<MediaMetadata, Void, Void> {
        DBHandler handler;
        Media media;
        
        SetMetadata(DBHandler handler, Media media){
            this.handler = handler;
            this.media = media;
        }

        @Override
        protected Void doInBackground(MediaMetadata...metadataArray) {
            MediaMetadata metadata = metadataArray == null ? null : metadataArray[0];
            if (media.getEnabled() == StreamingApplication.LOCAL && metadata == null){
                handler.getFileHelper().saveMediaContentFile(media, media.getStreamingID(), media.getFilePath());
                handler.getParser().getThumbnailAPI().setThumbnailURI(media, ThumbnailAPI.Type.AUXILIARY);
            } else if (media.getEnabled() == StreamingApplication.MAPS
                    || media.getEnabled() == StreamingApplication.COPY) {
                if (!(media.getEnabled() == StreamingApplication.MAPS && metadata != null)){
                    for (ThumbnailAPI.Type file : ThumbnailAPI.Type.values()){file.remove(media);}}
                if (metadata != null && (!metadata.get(MediaMetadata.Type.INPUT_TITLE).isEmpty() || metadata.get(MediaMetadata.Type.TITLE).isEmpty())){
                    media.setTitle(metadata.get(MediaMetadata.Type.INPUT_TITLE));}
                media.getEnabled().setMetadata(media, metadata, handler.getParser());
            } else {
                if (media.getEnabled() == StreamingApplication.LOCAL){
                    if (metadata.get(MediaMetadata.Type.TITLE) != null) ThumbnailAPI.Type.THUMBNAIL.remove(media);
                } else for (ThumbnailAPI.Type file : ThumbnailAPI.Type.values()){file.remove(media);}
                media.clearMetadata();
                if (!metadata.get(MediaMetadata.Type.INPUT_TITLE).isEmpty() || metadata.get(MediaMetadata.Type.TITLE).isEmpty()){
                    media.setTitle(metadata.get(MediaMetadata.Type.INPUT_TITLE));}
                media.getEnabled().setMetadata(media, metadata, handler.getParser());
                handler.getParser().getThumbnailAPI().setThumbnailURL(media, ThumbnailAPI.Type.THUMBNAIL);
            }
            media.setTempDetails(null);
            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            if (!BaseFragment.addOrUpdate(media)){
                handler.addOrUpdateMedia(media);
            }
        }
    }
}
