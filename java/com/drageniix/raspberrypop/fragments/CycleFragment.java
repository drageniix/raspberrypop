package com.drageniix.raspberrypop.fragments;


import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.dialog.DatabaseDialog;
import com.drageniix.raspberrypop.dialog.RecolorDialog;
import com.drageniix.raspberrypop.dialog.RenameDialog;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.dialog.adapter.BaseCallback;
import com.drageniix.raspberrypop.dialog.adapter.cycle.CycleAdapter;
import com.drageniix.raspberrypop.dialog.adapter.cycle.CycleManager;
import com.drageniix.raspberrypop.utilities.Logger;
import com.drageniix.raspberrypop.utilities.TimeManager;
import com.drageniix.raspberrypop.utilities.custom.LinedEditText;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CycleFragment extends BaseFragment{
    private Media media;
    private ProgressBar loading;
    private CycleAdapter adapter;
    private MenuItem currentCycleType;
    private int scrollTo;
    private float rotation;
    protected static CycleFragment cycleFragment;

    public static synchronized CycleFragment getsInstance(Media media, int scrollTo) {
        cycleFragment = new CycleFragment();
        cycleFragment.media = media;
        cycleFragment.scrollTo = scrollTo;
        return cycleFragment;}


    public static void switchLoading(final boolean on){
        if (cycleFragment != null && cycleFragment.getBaseActivity() != null && cycleFragment.getLoading() != null) {
            cycleFragment.getBaseActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (on && cycleFragment.getLoading().getVisibility() == View.GONE) {
                        cycleFragment.getLoading().setVisibility(View.VISIBLE);
                    } else if (!on && cycleFragment.getLoading().getVisibility() == View.VISIBLE) {
                        cycleFragment.getLoading().setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    protected static boolean addOrUpdateMedia(final Media media){
        if (cycleFragment != null && cycleFragment.getBaseActivity() != null && cycleFragment.adapter != null && media.getCycleString().equals(cycleFragment.media.getCycleString())){
            cycleFragment.getBaseActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cycleFragment.adapter.addOrUpdate(media);
                }
            });
            return true;
        }
        return false;
    }

    protected static boolean changeDataset(){
        if (cycleFragment != null && cycleFragment.getBaseActivity() != null && cycleFragment.adapter != null){
            cycleFragment.getBaseActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cycleFragment.adapter.notifyDataSetChanged();
                }
            });
            return true;
        }
        return false;
    }

    public ProgressBar getLoading() {return loading;}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        cycleFragment = this;
        setHandler();

        final View view = inflater.inflate(R.layout.cycle_fragment, container, false);
        if (media == null){
            try {
                getFragmentManager().beginTransaction()
                        .replace(R.id.activity_content, DatabaseFragment.getsInstance(handler.getDefaultCollection()))
                        .addToBackStack(null)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
            } catch (Exception e) {
                Logger.log(Logger.FRAG, e);
            }
            return view;
        }

        loading = view.findViewById(R.id.cycleProgress);
        RecyclerView list = view.findViewById(R.id.cycle_list);
        RecyclerView.LayoutManager lm = new LinearLayoutManager(getBaseActivity(), LinearLayoutManager.VERTICAL, false);

        final CycleManager.MediaCycle cycle = handler.getCycleManager().loadCycle(media);
        adapter = new CycleAdapter(getBaseActivity(), handler, lm, cycle);
        ItemTouchHelper.Callback callback = new BaseCallback(adapter);
        ItemTouchHelper drag = new ItemTouchHelper(callback);
        drag.attachToRecyclerView(list);

        list.setHasFixedSize(false);
        list.setLayoutManager(lm);
        list.setAdapter(adapter);
        if (scrollTo != -1) list.scrollToPosition(scrollTo);

        final TextView labelText = view.findViewById(R.id.color_text);
        if (!media.getLabel().isEmpty()){
            LayerDrawable shape = (LayerDrawable) ContextCompat.getDrawable(getBaseActivity(), R.drawable.label_border);
            GradientDrawable gradientDrawable = (GradientDrawable) shape.findDrawableByLayerId(R.id.label);
            gradientDrawable.setColor(Color.parseColor(media.getLabel()));
            labelText.setBackground(shape);
            labelText.setText(handler.getPreferences().getColorString(media.getLabel()));
        }
        labelText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RecolorDialog.getInstance(handler, media, getBaseActivity(),
                        new RecolorDialog.OnChooseLabel() {
                            @Override
                            public void chooseLabel(String color) {
                                media.setLabel(color);
                                handler.addOrUpdateMedia(media);
                                LayerDrawable shape = (LayerDrawable) ContextCompat.getDrawable(getBaseActivity(), R.drawable.label_border);
                                GradientDrawable gradientDrawable = (GradientDrawable) shape.findDrawableByLayerId(R.id.label);
                                gradientDrawable.setColor(Color.parseColor(color));
                                labelText.setBackground(shape);
                                labelText.setText(handler.getPreferences().getColorString(color));
                            }
                        },
                        new RecolorDialog.OnClearLabel() {
                            @Override
                            public void clearLabel() {
                                media.setLabel("");
                                handler.addOrUpdateMedia(media);
                                LayerDrawable shape = (LayerDrawable) ContextCompat.getDrawable(getBaseActivity(), R.drawable.label_border);
                                GradientDrawable gradientDrawable = (GradientDrawable) shape.findDrawableByLayerId(R.id.label);
                                gradientDrawable.setColor(Color.GRAY);
                                labelText.setBackground(shape);
                                labelText.setText(getString(R.string.no_label));
                            }
                        },
                        new RecolorDialog.OnDismiss() {
                            @Override
                            public void dismiss() {
                                if (!media.getLabel().isEmpty()){
                                    labelText.setText(handler.getPreferences().getColorString(media.getLabel()));
                                }
                            }
                        });
            }
        });

        final LinedEditText comments = view.findViewById(R.id.comments);
        view.findViewById(R.id.commentToggle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (comments.getVisibility() == View.GONE){
                    comments.setVisibility(View.VISIBLE);
                } else{
                    comments.setVisibility(View.GONE);
                }
            }
        });

        final int[] index = new int[]{-1};
        comments.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))){
                    comments.setVisibility(View.GONE); return true;} else return false;}
        });
        comments.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (comments.getSelectionStart() != -1) index[0] = comments.getSelectionStart();
                highlightTags(index[0], comments, comments.getText().toString());
            }
        });
        highlightTags(index[0], comments, media.getComments());

        final FloatingActionButton fab = view.findViewById(R.id.fabCycle);
        final OvershootInterpolator interpolator = new OvershootInterpolator();
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewCompat.animate(fab)
                        .rotation(rotation = rotation == 0 ? 180f : 0f)
                        .withLayer()
                        .setDuration(200)
                        .setInterpolator(interpolator)
                        .start();

                DatabaseDialog.addMedia(getBaseActivity(), getFragmentManager(), handler, false, UUID.randomUUID().toString(), "", media.getCollection(), media.getCycleString(), media.getFigureName());
            }
        });

        loading.setVisibility(View.GONE);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.cycle_menu, menu);

        setTitle(media.getFigureName() + "'s Tasks");

        SubMenu sortOptions = menu.findItem(R.id.action_cycle_type).getSubMenu();
        for(int i = 0; i < sortOptions.size(); i++){
            sortOptions.getItem(i).setIcon(R.drawable.ic_action_sort_current);}

        switch (media.getCycleType()){
            case 0:
                currentCycleType = menu.findItem(R.id.off);
                break;
            case 1:
                currentCycleType = menu.findItem(R.id.cycle);
                break;
            case 2:
                currentCycleType = menu.findItem(R.id.shuffle);
                break;
            case 3:
                currentCycleType = menu.findItem(R.id.all);
                break;
        }

        if (currentCycleType != null) currentCycleType.setIcon(R.drawable.ic_action_sort_filled);
    }

    private void setCurrentCycleType(MenuItem newCurrent){
        if (currentCycleType != null){
            currentCycleType.setIcon(R.drawable.ic_action_sort_current);}
        currentCycleType = newCurrent;
        currentCycleType.setIcon(R.drawable.ic_action_sort_filled);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                getActivity().onBackPressed();
            case R.id.edit:
                RenameDialog.getInstance(handler, adapter.context, Collections.singleton(media), new RenameDialog.OnPositive() {
                    @Override
                    public void submit(String name, String collection) {
                        boolean changeName = name != null && !media.getFigureName().equals(name);
                        boolean changeCollection = collection != null && !media.getCollection().equals(collection);

                        if (changeName) {
                            media.setFigureName(name); }
                        if (changeCollection) {
                            media.setOldCollection(media.getCollection());
                            media.setCollection(collection); }
                        if (changeName || changeCollection) {
                            adapter.addOrUpdate(media); }
                    }
                });
                break;
            case R.id.off:
                setCurrentCycleType(item);
                media.setCycleType(0);
                handler.addOrUpdateMedia(media);
                break;
            case R.id.cycle:
                setCurrentCycleType(item);
                media.setCycleType(1);
                handler.addOrUpdateMedia(media);
                break;
            case R.id.shuffle:
                setCurrentCycleType(item);
                media.setCycleType(2);
                handler.addOrUpdateMedia(media);
                break;
            case R.id.all:
                setCurrentCycleType(item);
                media.setCycleType(3);
                handler.addOrUpdateMedia(media);
                break;
            case R.id.chart:
                new android.support.v7.app.AlertDialog.Builder(getActivity())
                        .setView(scanHistory())
                        .setPositiveButton("OK", null)
                        .setNeutralButton("Reset History", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                media.setScanHistory(new String[]{media.getScanHistory()[0]});
                                handler.addOrUpdateMedia(media);
                            }
                        })
                        .create()
                        .show();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void highlightTags(int index, LinedEditText comments, String text){
        if ((comments.getText().toString().isEmpty() && !text.isEmpty()) ||
                (!media.getComments().equalsIgnoreCase(text))) {
            if (!media.getComments().equals(text)) {
                media.setComments(text);
                handler.addOrUpdateMedia(media); }
            if (text.contains("#")) {
                Spannable textSpannable = new SpannableString(text);
                int lastIndex = 0;
                while (lastIndex != -1 && lastIndex < text.length()) {
                    lastIndex = text.indexOf("#", lastIndex);
                    if (lastIndex != -1) {
                        int sEnd = text.indexOf(" ", lastIndex),
                                nEnd = text.indexOf("\n", lastIndex),
                                eEnd = text.length();
                        final int end = (sEnd < nEnd || nEnd == -1) && sEnd != -1 ? sEnd :
                                nEnd < eEnd && nEnd != -1 ? nEnd : eEnd;
                        if (end - lastIndex > 1) {
                            textSpannable.setSpan(new BackgroundColorSpan(adapter.context.getAttributeColor(R.attr.colorSelection)), lastIndex, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        lastIndex = end;
                    }
                }
                comments.setMovementMethod(LinkMovementMethod.getInstance());
                comments.setText(textSpannable, TextView.BufferType.SPANNABLE);
            } else if (!comments.getText().toString().equals(text)) {
                comments.setText(text);
            }
            if (!comments.getText().toString().isEmpty() && index != -1) {
                comments.setSelection(index);
            }
        }
    }

    private View scanHistory(){
        List<Entry> values = new ArrayList<>();
        Map<Calendar, Integer> raw = new HashMap<>();
        final List<String> scanDates = new LinkedList<>();

        Calendar base = null, end = Calendar.getInstance();
        end.set(Calendar.HOUR_OF_DAY, 0);
        end.set(Calendar.MINUTE, 0);
        end.set(Calendar.SECOND, 0);
        end.set(Calendar.MILLISECOND, 0);

        for (int i = 0; i < media.getScanHistory().length; i++) {
            String date = media.getScanHistory()[i];
            long milli = Long.parseLong(date);

            Calendar entry = Calendar.getInstance();
            entry.setTimeInMillis(milli);
            scanDates.add(handler.getTimeManager().format(TimeManager.FULL_TIME, entry.getTime()).replace("-", "/"));

            entry.set(Calendar.HOUR_OF_DAY, 0);
            entry.set(Calendar.MINUTE, 0);
            entry.set(Calendar.SECOND, 0);
            entry.set(Calendar.MILLISECOND, 0);

            raw.put(entry, raw.containsKey(entry) ?
                    raw.get(entry) + 1 : i == 0 ? 0 : 1);

            if (i == 0) base = (Calendar) entry.clone();
        }

        boolean includeYear = base.get(Calendar.YEAR) != end.get(Calendar.YEAR);
        final List<String> displayDates = new LinkedList<>();
        for(int i = 0; base.compareTo(end) <= 0; base.add(Calendar.DATE, 1), i++){
            values.add(new Entry(i, raw.containsKey(base) ? raw.get(base) : 0));
            displayDates.add(handler.getTimeManager().format(includeYear ?
                    TimeManager.DATE : TimeManager.DATE_NO_YEAR, base.getTime()).replace("-", "/"));
        }

        end.add(Calendar.DATE, 1);
        displayDates.add(handler.getTimeManager().format(includeYear ?
                TimeManager.DATE : TimeManager.DATE_NO_YEAR, end.getTime()).replace("-", "/"));

        final String scanStart = "Added: " + scanDates.get(0) +
                "\nTotal Scans: " + (scanDates.size() - 1) +
                "\n\n";

        //Design Graph
        View view = View.inflate(getContext(), R.layout.cycle_chart, null);
        LineChart mChart = view.findViewById(R.id.chart);
        final TextView history = view.findViewById(R.id.history);

        history.setText(scanStart.trim());

        int textColor = getBaseActivity().getAttributeColor(R.attr.textColor),
            accentColor = getBaseActivity().getAttributeColor(R.attr.colorAccent),
            accentColor2 = getBaseActivity().getAttributeColor(R.attr.colorAccent2);

        mChart.getDescription().setEnabled(false);

        // enable scaling and dragging
        mChart.setTouchEnabled(true);
        mChart.setPinchZoom(true);
        mChart.setDragEnabled(true);
        mChart.setDragDecelerationFrictionCoef(0.9f);
        mChart.setHighlightPerDragEnabled(true);
        mChart.setViewPortOffsets(60f, 0f, 0f, 60f);

        // create a dataset and give it a type
        LineDataSet set1 = new LineDataSet(values, media.getFigureName());
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setColor(accentColor2);
        set1.setValueTextColor(textColor);
        set1.setLineWidth(4.0f);
        set1.setCircleColor(accentColor);
        set1.setDrawCircles(true);
        set1.setDrawValues(false);
        set1.setHighLightColor(accentColor);
        set1.setDrawCircleHole(false);
        set1.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);

        // create a data object with the datasets
        LineData data = new LineData(set1);
        data.setValueTextColor(textColor);
        data.setValueTextSize(9f);

        // set data
        mChart.setData(data);
        mChart.invalidate();

        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();
        l.setEnabled(false);

        XAxis xAxis = mChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(textColor);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(true);
        xAxis.setCenterAxisLabels(false);
        xAxis.setAxisMinimum(0);
        xAxis.setAxisMaximum(displayDates.size()-1);
        xAxis.setGranularity(1f); // one day
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return displayDates.get((int)value);
            }
        });

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setTextColor(textColor);
        leftAxis.setDrawAxisLine(true);
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0);
        leftAxis.setTextSize(12f);
        leftAxis.setGranularity(1f);

        mChart.getAxisRight().setEnabled(false);
        mChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                StringBuilder scans = new StringBuilder(scanStart);
                for (int i = 1; i < scanDates.size(); i++){
                    if (scanDates.get(i).startsWith(displayDates.get((int)e.getX()))){
                        scans.append(scanDates.get(i)).append("\n");
                    }
                }

                history.setText(scans.toString().trim());
            }

            @Override
            public void onNothingSelected() {
                history.setText(scanStart.trim());
            }
        });

        mChart.animateX(500);
        return view;
    }
}
