package com.drageniix.raspberrypop.dialog.adapter.cycle;

import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.utilities.DBHandler;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TimeZone;

public class CycleManager{
    private DBHandler handler;
    private Map<String, MediaCycle> cycles;
    private MediaCycleSorter sorter;

    public CycleManager(DBHandler handler){
        this.handler = handler;
        cycles = new HashMap<>();
        sorter = new MediaCycleSorter();
    }

    private static class MediaCycleSorter implements Comparator<Media> {
        @Override
        public int compare(Media a, Media b) {
            return Integer.compare(a.getCyclePosition(), b.getCyclePosition());
        }
    }

    public void update(Media media){
        loadCycle(media).update(media);
    }

    public Media getNext(Media media, int direction){
        MediaCycle cycle = loadCycle(media);
        if (cycle != null) {
            return cycle.setNext(media, -direction);
        } else {
            return media;
        }
    }

    public Media loadNext(Media media){
        MediaCycle cycle = loadCycle(media);
        if (cycle != null) {
            Media next = cycle.get(0);
            cycle.setNext(media, -1);
            return next;
        } else {
            return media;
        }
    }

    public void deleteCycle(DBHandler handler, Media medium) {
        MediaCycle cycle = loadCycle(medium);
        if (cycle != null) {
            for (Media media : cycle) {
                if (!media.getCollection().equals(handler.getDefaultCollection())) {
                    handler.getMediaList(handler.getDefaultCollection()).remove(media);
                }
                handler.deleteMedia(media);
            }
            cycles.remove(cycle);
            cycle.clear();
        }
    }

    void move(Media start, String dest){
        MediaCycle original = loadCycle(start);
        start.setCyclePosition(Integer.MAX_VALUE);
        start.setCycle(dest);
        original.remove(original.indexOf(start));
        loadCycle(start);
        cycles.get(dest).update(cycles.get(dest).get(0));
    }

    public MediaCycle loadCycle(Media media){
        if (media.getCycleString() == null){
            media.setCycle(media.getScanUID());
        }

        MediaCycle cycle = cycles.get(media.getCycleString());
        if (cycle == null) {
            cycles.put(media.getCycleString(), cycle = new MediaCycle());
            cycle.add(media);
            if (media.getCyclePosition() == Integer.MAX_VALUE){
                media.setCyclePosition(0);
                media.setCycleType(3);
                media.setScanHistory(new String[]{String.valueOf(Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis())});
                media.setComments("");
                media.setLabel("");
            }
        } else if (!cycle.contains(media)){
            cycle.add(media);
            if (media.getCyclePosition() == Integer.MAX_VALUE){
                media.setCyclePosition(cycle.indexOf(media));
                media.setCycleType(cycle.get(0).getCycleType());
                media.setScanHistory(cycle.get(0).getScanHistory());
                media.setComments(cycle.get(0).getComments());
                media.setLabel(cycle.get(0).getLabel());
            }
            cycle.sort();
        }
        return cycle;
    }

    public class MediaCycle extends LinkedList<Media> {
        private void sort(){
            Collections.sort(this, sorter);
        }

        public void update(int from, int to){
            Collections.swap(this, from, to);

            Media fromMedia = get(from);
            fromMedia.setCyclePosition(to);

            Media toMedia = get(to);
            toMedia.setCyclePosition(from);

            handler.addOrUpdateMedia(toMedia);
        }

        //synchronize
        private void update(Media media){
            for(Media m : this){
                boolean
                        fig = !m.getFigureName().equals(media.getFigureName()),
                        col = !m.getCollection().equals(media.getCollection()),
                        pos = m.getPosition() != media.getPosition(),
                        cyc = m.getCyclePosition() != indexOf(m),
                        seq = m.getCycleType() != media.getCycleType(),
                        his = !Arrays.equals(m.getScanHistory(), media.getScanHistory()),
                        com = !m.getComments().equals(media.getComments()),
                        lab = !m.getLabel().equals(media.getLabel());

                if (col){
                    m.setOldCollection(m.getCollection());
                    m.setCollection(media.getCollection());
                }

                if (fig) m.setFigureName(media.getFigureName());
                if (pos) m.setPosition(media.getPosition());
                if (cyc) m.setCyclePosition(indexOf(m));
                if (seq) m.setCycleType(media.getCycleType());
                if (his) m.setScanHistory(media.getScanHistory());
                if (com) m.setComments(media.getComments());
                if (lab) m.setLabel(media.getLabel());

                if (col || fig || pos || cyc || seq || his || com || lab){
                    handler.addOrUpdateMedia(m);
                    break;
                }
            }
        }

        private Media setNext(Media base, int direction){
            Collections.rotate(this, direction);
            update(base);
            return get(0);
        }

        @Override
        public Media remove(int index) {
            Media removed = super.remove(index);
            if (size() > index){
                handler.addOrUpdateMedia(get(index));
            }
            return removed;
        }

        @Override
        public String toString() {
            if (!isEmpty()) {
                StringBuilder stringBuilder = new StringBuilder()
                    .append(get(0).getCycleString())
                    .append(" --- [");
                for (Media m : this) {
                    stringBuilder.append(m.getScanUID()).append(", ");
                }
                return stringBuilder.substring(0, stringBuilder.lastIndexOf(",")) + "]";
            }
            return super.toString();
        }
    }
}

