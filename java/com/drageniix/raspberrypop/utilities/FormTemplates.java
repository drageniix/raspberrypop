package com.drageniix.raspberrypop.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.LinearLayout;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class FormTemplates {
    private SharedPreferences templates;

    FormTemplates(Context context) {
        this.templates = context.getSharedPreferences("FORM_TEMPLATES", Context.MODE_PRIVATE);
    }

    public LinkedHashMap<String, String> getTemplates(){
        LinkedHashMap<String, String> templatesLoaded = new LinkedHashMap<>();
        for(Map.Entry<String, ?> entry : templates.getAll().entrySet()){
            if (entry.getValue() instanceof String){
                templatesLoaded.put(entry.getKey(), (String)entry.getValue());
            }
        }
        return templatesLoaded;
    }

    public void addTemplate(String name, String data){
        templates.edit().putString(name, data).apply();
    }

    public void removeTemplate(String name){
        templates.edit().remove(name).apply();
    }

    void readAll(Object input){
        Map<String, ?> entries = (Map<String, ?>)input;
        SharedPreferences.Editor editor = templates.edit();
        editor.clear();

        for (Map.Entry<String, ?> entry : entries.entrySet()) {
            Object v = entry.getValue();
            String key = entry.getKey();
            if (v instanceof Boolean)
                editor.putBoolean(key, (Boolean) v);
            else if (v instanceof Float)
                editor.putFloat(key, (Float) v);
            else if (v instanceof Integer)
                editor.putInt(key, (Integer) v);
            else if (v instanceof Long)
                editor.putLong(key, (Long) v);
            else if (v instanceof String)
                editor.putString(key, ((String) v));
        }

        editor.apply();
    }

    Map<String, ?> getAllTemplates(){
        return templates.getAll();
    }

}
