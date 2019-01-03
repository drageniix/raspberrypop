package com.drageniix.raspberrypop.fragments;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.dialog.RecolorDialog;
import com.drageniix.raspberrypop.dialog.RenameDialog;
import com.drageniix.raspberrypop.dialog.adapter.BaseHolder;
import com.drageniix.raspberrypop.dialog.adapter.cycle.CycleManager;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.utilities.billing.Billing;
import com.drageniix.raspberrypop.utilities.categories.ApplicationCategory;
import com.drageniix.raspberrypop.utilities.categories.AuxiliaryApplication;
import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class StatisticsFragment extends BaseFragment {
    private DecimalFormat decimalFormat = new DecimalFormat("###,###,###,###.##");
    private int textColor;
    private TextView collectionText, barText, breakdownText;
    private TextView fileSize, fileText;
    private TextView log;
    private BarChart categoryChart;
    private HorizontalBarChart tagChart;
    private List<Media> menuMediaList;
    private TagAdapter adapter;
    private NestedScrollView view;
    private View listHeader;
    private int nColl, nLab, nTag, nScans, nUTag;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            setHasOptionsMenu(false);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHandler();
        setTitle("Tag Statistics");

        view = (NestedScrollView) inflater.inflate(R.layout.stat_fragment, container, false);

        listHeader = view.findViewById(R.id.p7);
        RecyclerView list = view.findViewById(R.id.stat_list);
        adapter = new TagAdapter();
        list.setAdapter(adapter);
        list.setLayoutManager(new LinearLayoutManager(getBaseActivity(), LinearLayoutManager.VERTICAL, false));
        list.setHasFixedSize(false);

        fileText = view.findViewById(R.id.fileText);
        fileSize = view.findViewById(R.id.fileSize);
        collectionText = view.findViewById(R.id.p1);
        barText = view.findViewById(R.id.p3);
        breakdownText = view.findViewById(R.id.p8);
        categoryChart = view.findViewById(R.id.category_chart);
        tagChart = view.findViewById(R.id.tag_chart);
        log = view.findViewById(R.id.p6);

        textColor = fileText.getCurrentTextColor();

        TextView debugHeader = view.findViewById(R.id.p5);
        if(Billing.isBeta()) {
            debugHeader.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    refreshCharts();
                    log.setText(handler.getPreferences().getLogger());
                }
            });
        } else {
            log.setVisibility(GONE);
            debugHeader.setVisibility(GONE);
        }

        setBreakdownOptions(view);
        initialize();
        refreshCharts();

        return view;
    }

    private void refreshCharts(){
        List<Media> mediaList = new LinkedList<>(handler.getMediaList(handler.getDefaultCollection()));
        menuMediaList = null;

        if (mediaList.isEmpty()){
            categoryChart.setVisibility(GONE);
            tagChart.setVisibility(GONE);
            collectionText.setVisibility(GONE);
            barText.setVisibility(GONE);
            listHeader.setVisibility(GONE);
            fileText.setVisibility(GONE);

            String empty = "No Tags Added Yet!";
            fileSize.setTextColor(textColor);
            fileSize.setText(empty);
        } else {
            listHeader.setVisibility(VISIBLE);
            fileText.setVisibility(VISIBLE);

            handler.getFileHelper().clearCreatedFiles();
            handler.getFileHelper().cleanFiles(handler.getFileHelper().getLocalPath());
            handler.getFileHelper().cleanFiles(handler.getFileHelper().getThumbnailPath());
            long folderSize = handler.getFileHelper().getFolderSize(new File(handler.getFileHelper().getLocalPath()));
            fileSize.setText(handler.getFileHelper().translateBytes(folderSize, true));
            fileSize.setTextColor(folderSize > 1048576 * 25 ? Color.RED : Color.GREEN);

            categoryChart();
            combined(mediaList);
            breakdown(mediaList);

            categoryChart.highlightValue(null);
            tagChart.highlightValue(null);

            setHeaders();
            view.scrollTo(0, 0);
        }
    }

    private String designatePlural(String source, int number){
        return " " + (number == 1 ? source : source + "s");
    }

    private void setHeaders(){
        String text;
        collectionText.setVisibility(VISIBLE);
        if(nColl != 0 && nLab != 0){
            text = nColl + designatePlural("Collection", nColl);
            text += ", " + nLab + designatePlural("Selected Label", nLab);
            collectionText.setText(text);
        } else if (nColl != 0){
            text = nColl + designatePlural("Collection", nColl);
            collectionText.setText(text);
        } else if (nLab != 0){
            text = nLab + designatePlural("Label", nLab);
            collectionText.setText(text);
        } else {
            collectionText.setVisibility(GONE);
        }

        barText.setVisibility(VISIBLE);
        if (nTag == 0){
            barText.setVisibility(GONE);
        } if (nScans != 0){
            text = nTag + designatePlural("Action", nTag) + " in " + nUTag + designatePlural("Tag", nUTag);
            text += ", Scanned " + nScans + designatePlural("Total Time", nScans);
            barText.setText(text);
        } else {
            text = nTag + designatePlural("Action", nTag) + " in " + nUTag + designatePlural("Tag", nUTag);
            barText.setText(text);
        }
    }

    private void initialize(){
        categoryChart.setScaleEnabled(true);
        categoryChart.getLegend().setEnabled(false);
        categoryChart.getAxisRight().setEnabled(false);
        categoryChart.getAxisLeft().setDrawLabels(false);
        categoryChart.getAxisLeft().setDrawAxisLine(true);
        categoryChart.getAxisLeft().setGranularity(1f);
        categoryChart.getXAxis().setCenterAxisLabels(false);
        categoryChart.getXAxis().setTextColor(textColor);
        categoryChart.getXAxis().setDrawGridLines(false);
        categoryChart.getXAxis().setDrawAxisLine(true);
        categoryChart.getXAxis().setDrawLabels(true);
        categoryChart.getXAxis().setGranularity(1f);
        categoryChart.getXAxis().setPosition(XAxis.XAxisPosition.TOP);
        categoryChart.setPinchZoom(true);
        categoryChart.setDrawBarShadow(false);
        categoryChart.setDrawGridBackground(false);
        categoryChart.getDescription().setEnabled(false);

        tagChart.setScaleEnabled(true);
        tagChart.getLegend().setEnabled(false);
        tagChart.getAxisRight().setEnabled(false);
        tagChart.getAxisLeft().setDrawLabels(false);
        tagChart.getAxisLeft().setDrawGridLines(false);
        tagChart.getAxisLeft().setDrawAxisLine(false);
        tagChart.getAxisLeft().setGranularity(1f);
        tagChart.getXAxis().setCenterAxisLabels(true);
        tagChart.getXAxis().setTextColor(textColor);
        tagChart.getXAxis().setDrawGridLines(false);
        tagChart.getXAxis().setDrawAxisLine(true);
        tagChart.getXAxis().setGranularity(1f);
        tagChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        tagChart.setPinchZoom(true);
        tagChart.setDrawBarShadow(false);
        tagChart.setDrawGridBackground(false);
        tagChart.setDrawValueAboveBar(true);
        tagChart.getDescription().setEnabled(false);
        tagChart.setFitBars(true);
    }

    private void categoryChart(){
        categoryChart.setVisibility(VISIBLE);

        final List<BarEntry> collEntries = new ArrayList<>();
        Set<String> collections = new LinkedHashSet<>();
        Set<String> labels = new LinkedHashSet<>();

        boolean includeDefault = false, includeDefaultLabel = false;

        for(Media media : handler.getMediaList(handler.getDefaultCollection())){
            if (media.getCyclePosition() == 0) {
                labels.add(media.getLabel());
                collections.add(media.getCollection());
                if (!includeDefault && media.getCollection().equals(handler.getDefaultCollection())){
                    includeDefault = true;
                }
                if(!includeDefaultLabel && media.getLabel().isEmpty()){
                    includeDefaultLabel = true;
                }
            }
        }

        if(!includeDefault) collections.remove(handler.getDefaultCollection());
        nColl = collections.size();
        nLab = labels.size();

        if ((includeDefault && nColl == 1 && includeDefaultLabel && nLab == 1) || (nColl == 0)){
           nColl = 0;
           nLab = 0;
           categoryChart.setVisibility(GONE);
           return;
        }

        final List<String>
                indexedLabels = new LinkedList<>(labels),
                indexedCollection = new LinkedList<>(collections);

        int index = 0;
        for (String title : collections) {
            if (!handler.getMediaList(title).isEmpty()) {
                if(includeDefault || !title.equals(handler.getDefaultCollection())) {
                    float[] labelCount = new float[labels.size()];
                    for (Media media : handler.getMediaList(title)) {
                        if (media.getCollection().equals(title) && media.getCyclePosition() == 0) {
                            labelCount[indexedLabels.indexOf(media.getLabel())]++;
                        }
                    }
                    collEntries.add(new BarEntry(index++, labelCount));
                }
            }
        }

        int[] colors = new int[labels.size()];
        String[] labelNames = new String[labels.size()];
        for(int i = 0; i < indexedLabels.size(); i++){
            String label = indexedLabels.get(i);
            if(label.equals("")){
                colors[i] = textColor;
                labelNames[i] = "Unlabeled";
            } else {
                colors[i] = Color.parseColor(label);
                labelNames[i] = handler.getPreferences().getColorString(label);
            }
        }

        BarDataSet categorySet = new BarDataSet(collEntries, "");
        categorySet.setColors(colors);
        categorySet.setStackLabels(labelNames);
        categorySet.setDrawIcons(false);

        BarData data = new BarData(categorySet);
        data.setBarWidth(1f);
        data.setValueTextColor(textColor);
        data.setValueTextSize(11f);
        data.setValueTypeface(BaseActivity.getFont(getContext()));
        data.setValueFormatter(new IValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                return value == 0 ? "" : decimalFormat.format(value);
            }
        });

        categoryChart.getXAxis().setAxisMaximum(data.getXMax() + 0.25f);
        categoryChart.getXAxis().setAxisMinimum(data.getXMin() - 0.25f);
        categoryChart.getXAxis().setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                int index = (int)Math.floor(value);
                return index >= indexedCollection.size() || index < 0 ? "" : indexedCollection.get(index);
            }
        });

        categoryChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                List<Media> mediaList = new LinkedList<>();
                int c = (int )h.getX();
                int l = h.getStackIndex();

                if (c != -1 && l != -1) {
                    for (Media m : handler.getMediaList(handler.getDefaultCollection())) {
                        if (m.getCollection().equals(indexedCollection.get(c))
                                && m.getLabel().equals(indexedLabels.get(l))) {
                            mediaList.add(m);
                        }
                    }

                    menuMediaList = mediaList;
                    combined(mediaList);
                    breakdown(mediaList);
                    setHeaders();
                }
            }

            @Override
            public void onNothingSelected() {
                List<Media> mediaList = new LinkedList<>(handler.getMediaList(handler.getDefaultCollection()));
                menuMediaList = mediaList;
                combined(mediaList);
                breakdown(mediaList);
                setHeaders();
            }
        });

        categoryChart.setData(data);
        categoryChart.invalidate();
        categoryChart.animateY(1400, Easing.EasingOption.EaseInSine);

    }


    private void combined(final List<Media> mediaList){
        tagChart.setVisibility(VISIBLE);
        menuMediaList = new LinkedList<>(mediaList);

        final List<BarEntry> tagEntries = new LinkedList<>(), scanEntries = new LinkedList<>();
        final List<Integer> trueColors = new LinkedList<>();
        final List<ApplicationCategory> titles = new LinkedList<>();
        final HashMap<ApplicationCategory, List<Media>> tagCount = new LinkedHashMap<>();

        nUTag = 0;
        nScans = 0;
        nTag = mediaList.size();
        for (Media media : mediaList) {
            if (media.getCyclePosition() == 0) nUTag++;
            if (!media.getEnabled().isFolder()){
                if(!tagCount.containsKey(media.getEnabled())){
                    tagCount.put(media.getEnabled(), new LinkedList<Media>());
                }
                tagCount.get(media.getEnabled()).add(media);
            } else {
                AuxiliaryApplication auxiliaryApplication = AuxiliaryApplication.valueOf(media);
                if(!tagCount.containsKey(auxiliaryApplication)){
                    tagCount.put(auxiliaryApplication, new LinkedList<Media>());
                }
                tagCount.get(auxiliaryApplication).add(media);
            }
        }

        int index = 0;
        for (Map.Entry<ApplicationCategory, List<Media>> entry : tagCount.entrySet()) {
            ApplicationCategory key = entry.getKey();
            List<Media> value = entry.getValue();

            titles.add(key);
            trueColors.add(getBaseActivity().getResourceColor(key.getColor()));
            tagEntries.add(new BarEntry(index, value.size()));

            int scans = 0;
            for(Media media : value){nScans += (scans += media.getScanHistory().length - 1); }
            scanEntries.add(new BarEntry(index, scans));
            index++;
        }

        BarDataSet tagCountSet = new BarDataSet(tagEntries, "Tags");
        tagCountSet.setColors(trueColors);
        tagCountSet.setValueTextColor(textColor);

        BarDataSet scanCountSet = new BarDataSet(scanEntries, "Scans");
        scanCountSet.setColor(textColor);
        scanCountSet.setValueTextColor(textColor);

        float groupSpace = 0.3f;
        float barSpace = 0.01f;
        float barWidth = 0.34f;

        BarData data = new BarData(tagCountSet, scanCountSet);
        data.setDrawValues(true);
        data.setBarWidth(barWidth);
        data.setValueTextSize(11f);
        data.setValueTypeface(BaseActivity.getFont(getContext()));
        data.setValueFormatter(new IValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                return decimalFormat.format(value);
            }
        });

        tagChart.getXAxis().setLabelCount(tagEntries.size());
        tagChart.getXAxis().setAxisMinimum(-data.getBarWidth() / 2);
        tagChart.getXAxis().setAxisMaximum(tagEntries.size()-data.getBarWidth() / 2);
        tagChart.getXAxis().setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                int index = (int)Math.floor(value);
                return index >= titles.size() ? "" : index < 0 ? "" : titles.get(index).getName();
            }
        });

        tagChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int index = tagEntries.indexOf(e);
                if (index == -1) index = scanEntries.indexOf(e);
                ApplicationCategory type = titles.get(index);
                breakdownText.setText(type.getName());
                nUTag = 0;
                nScans = 0;
                Set<Media> tagSet = new LinkedHashSet<>();
                for (Media media : tagCount.get(type)){
                    if (media.getCyclePosition() == 0) {
                        nUTag++;
                        nScans += media.getScanHistory().length - 1;
                    }
                    tagSet.addAll(handler.getCycleManager().loadCycle(media));
                }

                List<Media> newList = new LinkedList<>(tagSet);
                nTag = newList.size();
                menuMediaList = newList;
                breakdown(newList);
                setHeaders();
            }

            @Override
            public void onNothingSelected() {
                breakdownText.setText(getString(R.string.stat_breakdown));
                List<Media> newList = new LinkedList<>(mediaList);
                nUTag = 0;
                nTag = mediaList.size();
                for (Media media : newList){
                    if (media.getCyclePosition() == 0) nUTag++;
                }
                menuMediaList = newList;
                breakdown(newList);
                setHeaders();
            }
        });

        tagChart.setData(data);
        tagChart.groupBars(0f, groupSpace, barSpace);
        tagChart.invalidate();
        tagChart.animateY(1400, Easing.EasingOption.EaseInSine);
    }

    public void setBreakdownOptions(View view) {
        view.findViewById(R.id.breakdown_export).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (menuMediaList != null && !menuMediaList.isEmpty()) {
                    getBaseActivity().openIntent(BaseActivity.SHARE_MULTIPLE_REQUEST_CODE, menuMediaList.toArray(new Media[menuMediaList.size()]));
                }
            }
        });

        if (handler.getBilling().hasPremium()) {
            view.findViewById(R.id.breakdown_collection).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (menuMediaList != null && !menuMediaList.isEmpty()) {
                        RenameDialog.getInstance(handler, getBaseActivity(), new LinkedHashSet<>(menuMediaList), new RenameDialog.OnPositive() {
                            @Override
                            public void submit(String name, String collection) {
                                if (name != null) {
                                    for (Media media : menuMediaList) {
                                        if (!media.getFigureName().equals(name)) {
                                            media.setFigureName(name);
                                            handler.addOrUpdateMedia(media);
                                        }
                                    }
                                }
                                if (collection != null) {
                                    for (Media media : menuMediaList) {
                                        if (!media.getCollection().equals(collection)) {
                                            media.setOldCollection(media.getCollection());
                                            media.setCollection(collection);
                                            handler.addOrUpdateMedia(media);
                                        }
                                    }
                                    refreshCharts();
                                }
                            }
                        });
                    }
                }
            });
        } else {
            view.findViewById(R.id.breakdown_collection).setVisibility(GONE);
        }

        view.findViewById(R.id.breakdown_label).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (menuMediaList != null && !menuMediaList.isEmpty()) {
                    RecolorDialog.getInstance(handler, null, getBaseActivity(),
                            new RecolorDialog.OnChooseLabel() {
                                @Override
                                public void chooseLabel(String color) {
                                    for (Media media : menuMediaList) {
                                        if (!media.getLabel().equals(color)) {
                                            media.setLabel(color);
                                            handler.addOrUpdateMedia(media);
                                        }
                                    }
                                    refreshCharts();
                                }
                            },
                            new RecolorDialog.OnClearLabel() {
                                @Override
                                public void clearLabel() {
                                    String color = "";
                                    for (Media media : menuMediaList) {
                                        if (!media.getLabel().equals(color)) {
                                            media.setLabel(color);
                                            handler.addOrUpdateMedia(media);
                                        }
                                    }
                                    refreshCharts();
                                }
                            },
                            new RecolorDialog.OnDismiss() {
                                @Override
                                public void dismiss() {
                                    categoryChart();
                                }
                            });
                }
            }
        });

        view.findViewById(R.id.breakdown_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (menuMediaList != null && !menuMediaList.isEmpty()) {
                    new AlertDialog.Builder(getContext())
                            .setMessage(getString(R.string.confirm))
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    for (Media media : menuMediaList) {
                                        handler.deleteMedia(media);
                                    }
                                    refreshCharts();
                                }
                            })
                            .setNegativeButton(getString(R.string.cancel), null)
                            .create()
                            .show();
                }
            }
        });
    }

    private void breakdown(List<Media> mediaList){
        List<String> adapterList = new ArrayList<>();
        for(Media media : mediaList){
            if(media.getCyclePosition() == 0){
                adapterList.add(getListText(media));
            }
        }

        adapter.setData(adapterList);
    }

    private String getListText(Media listMedia){
        CycleManager.MediaCycle cycle = handler.getCycleManager().loadCycle(listMedia);
        StringBuilder summarySB = new StringBuilder();

        if(cycle.size() > 1) {
            String amount = cycle.size() + " Tasks";
            switch (listMedia.getCycleType()) {
                case 0:
                    amount += " (" + getString(R.string.cycle_0) + ")";
                    break;
                case 1:
                    amount += " (" + getString(R.string.cycle_1) + ")";
                    break;
                case 2:
                    amount += " (" + getString(R.string.cycle_2) + ")";
                    break;
                case 3:
                    amount += " (" + getString(R.string.cycle_3) + ")";
                    break;
            }

            summarySB.append("\n").append(amount).append("\n");
        }

        for(Media media : cycle){
            summarySB
                    .append(media.getCycleType() == 0 ? BaseHolder.offlineEmoji : media.availableToStream() ? "• " : BaseHolder.warningEmoji)
                    .append(media.getEnabled().getName())
                    .append(" - ")
                    .append(media.getTitle());
            if (media.getDetail() != null && !media.getDetail().isEmpty()) {
                summarySB
                        .append(" (")
                        .append(media.getDetail())
                        .append(")");
            }

            if ((media.getEnabled().isFolder() && media.getEnabled() != StreamingApplication.DEVICE) || AuxiliaryApplication.valueOf(media) == AuxiliaryApplication.WIFI_CONNECTION){
                AuxiliaryApplication device = AuxiliaryApplication.valueOf(media);
                summarySB.append(" [")
                        .append(device.getName())
                        .append("]");
            }

            summarySB
                    .append("\n");
        }

        return summarySB.toString().trim();
    }

    class TagAdapter extends RecyclerView.Adapter<TagAdapter.MyViewHolder>{
        private LayoutInflater inflater;
        private List<String> mediaList;

        TagAdapter() {
            inflater = LayoutInflater.from(getContext());
            mediaList = new LinkedList<>();
        }

        public void setData(List<String> list){
            this.mediaList = list;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.stat_list_item, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            holder.textView.setText(mediaList.get(position));
        }

        @Override
        public int getItemCount() {
            return mediaList.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            MyViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.text);
            }
        }
    }

}
