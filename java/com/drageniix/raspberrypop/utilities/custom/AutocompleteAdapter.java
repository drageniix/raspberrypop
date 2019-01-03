package com.drageniix.raspberrypop.utilities.custom;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.drageniix.raspberrypop.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AutocompleteAdapter<T> extends ArrayAdapter<T> implements Filterable {
    private List<T> listObjects;
    private List<T> suggestions = new ArrayList<>();
    private int resource;

    public AutocompleteAdapter(Context context, List<T> listObjects) {
        super(context, R.layout.media_popup_dropdown, new ArrayList<T>());
        this.listObjects = listObjects;
        this.resource = R.layout.media_popup_dropdown;
    }

    public void resetList(){
        clear();
        listObjects.clear();
        suggestions.clear();
    }

    @Override
    public void addAll(@NonNull Collection<? extends T> collection) {
        listObjects.addAll(collection);
        super.addAll(collection);
    }

    private Filter mFilter = new Filter(){
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults filterResults = new FilterResults();

            if (constraint != null) {
                suggestions.clear();
                for(T object : listObjects){
                    if (object.toString().toLowerCase().contains(constraint.toString().toLowerCase())){
                        suggestions.add(object);
                    }
                }

                filterResults.values = suggestions;
                filterResults.count = suggestions.size();
            }

            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (results == null){
                return;
            }

            List<T> filteredList = (List<T>) results.values;
            if (results.count > 0) {
                clear();
                for (T filteredObject : filteredList) {
                    add(filteredObject);
                }
                notifyDataSetChanged();
            }
        }
    };

    @NonNull
    @Override
    public Filter getFilter() {
        return mFilter;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Object listObject = getItem(position);

        viewHolder holder;
        if (convertView != null) {
            holder = (viewHolder) convertView.getTag();
        } else {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(resource, parent, false);
            holder = new viewHolder();
            holder.name = convertView.findViewById(R.id.name);
            convertView.setTag(holder);
        }

        holder.name.setText(listObject.toString());

        return convertView;
    }


    private static class viewHolder {
        TextView name;
    }
}
