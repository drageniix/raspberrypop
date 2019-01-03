package com.drageniix.raspberrypop.utilities.custom;


import java.util.LinkedHashMap;
import java.util.Map;

public class CaseInsensitiveMap<K, V> extends LinkedHashMap<K, V> {
    //put and contains not insensitive
    @Override
    public V get(Object initialKey){
        if (initialKey instanceof String) {
            String key = (String)initialKey;
            for (Map.Entry<K, V> entry : entrySet()) {
                if (entry.getKey() instanceof String) {
                    String entryKey = (String) entry.getKey();
                    if (entryKey.equalsIgnoreCase(key)) {
                        return entry.getValue();
                    }
                } else {
                    return super.get(key);
                }
            }
        } else {
            return super.get(initialKey);
        }
        return null;
    }
}
