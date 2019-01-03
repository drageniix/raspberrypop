package com.drageniix.raspberrypop.utilities;

import com.drageniix.raspberrypop.media.Media;


import com.drageniix.raspberrypop.utilities.custom.CaseInsensitiveMap;
import java.util.LinkedHashSet;

class MediaCollection extends CaseInsensitiveMap<String, MediaCollection.Collectible>{
    private Collectible defaultCollection;
    private DBHandler handler;

    MediaCollection(DBHandler handler, String defaultCollection){
        this.handler = handler;
        put(defaultCollection, this.defaultCollection = new Collectible());
    }

    @Override
    public Collectible get(Object key) {
        if (!containsKey(key) && key instanceof String){
            put((String)key, new Collectible());}
        return super.get(key);
    }

    class Collectible extends LinkedHashSet<Media> {
        @Override
        public boolean remove(Object o) {
            return super.remove(o);
        }

        @Override
        public boolean add(Media media) {
            if (this != defaultCollection && !defaultCollection.contains(media)) {
                return defaultCollection.add(media) && super.add(media);
            } else {
                return super.add(media);
            }
        }

        void moveTo(Media media, Collectible other){
            if (this != defaultCollection){remove(media);}
            other.add(media);
            media.setOldCollection(null);
        }
    }
}