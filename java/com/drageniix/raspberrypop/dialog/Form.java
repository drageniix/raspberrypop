package com.drageniix.raspberrypop.dialog;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.FormTemplates;
import com.drageniix.raspberrypop.utilities.TimeManager;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Form {
    private BaseActivity activity;
    private DBHandler handler;
    private View dialog;
    private FormAdapter adapter;
    private List<String> formType;
    private Spinner formDetail;
    private static Pattern pattern;
    private boolean editable;

    public static Pattern getPattern(){return pattern;}
    public static void setPattern(BaseActivity activity){
        String[] formType = activity.getResources().getStringArray(R.array.form_params);
        StringBuilder paramPatternSB = new StringBuilder();
        for (String aFormType : formType) {
            paramPatternSB.append(aFormType).append("|");
        }

        String paramPattern = paramPatternSB.toString();
        paramPattern = "(" + paramPattern.substring(0, paramPattern.length() - 1) + ")";
        pattern = Pattern.compile(paramPattern + "([^:]*):(.*)\n");
    }

    public Form (boolean editable, final BaseActivity activity, DBHandler handler, String title, String custom) {
        this.activity = activity;
        this.handler = handler;
        this.editable = editable;
        this.dialog = View.inflate(activity, R.layout.media_form, null);
        this.formType = Arrays.asList(activity.getResources().getStringArray(R.array.form_params));

        RecyclerView list = dialog.findViewById(R.id.entries);
        RecyclerView.LayoutManager lm = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
        adapter = new FormAdapter(new LinkedList<FormParam>());
        list.setHasFixedSize(false);
        list.setLayoutManager(lm);
        list.setAdapter(adapter);

        formDetail = dialog.findViewById(R.id.form_field);
        if (!editable) formDetail.setVisibility(View.GONE);

        final FormTemplates savedTemplates = handler.getTemplates();
        LinkedHashMap<String, String> templates = savedTemplates.getTemplates();

        int titleIndex = -1;
        if (!custom.isEmpty()) {
            String cleaned = loadTemplate(custom);
            if (!adapter.items.isEmpty()) {
                if (!templates.keySet().contains(title)) {
                    templates.put(title, cleaned);
                    savedTemplates.addTemplate(title, cleaned);
                }
                titleIndex = new LinkedList<>(templates.keySet()).indexOf(title);
            }
        }

        final List<String> titles = new LinkedList<>(templates.keySet());
        final ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_dropdown_item_1line, titles);
        formDetail.setAdapter(spinnerAdapter);

        if (titleIndex == -1){
            titleIndex = 0;
            if (titles.size() > 0){
                loadTemplate(new LinkedList<>(templates.values()).get(0));
            } else {
                spinnerAdapter.add("Please Add a Template");
                spinnerAdapter.notifyDataSetChanged();
            }
        }

        formDetail.setSelection(titleIndex, false);
        formDetail.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LinkedHashMap<String, String> templates = savedTemplates.getTemplates();
                String[] data = templates.values().toArray(new String[templates.size()]);
                if (data.length > position) {
                    loadTemplate(data[position]);
                }
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        ImageButton edit = dialog.findViewById(R.id.operation);
        if (!editable) edit.setVisibility(View.GONE);
        edit.setBackground(activity.getIcon(R.drawable.ic_action_edit_pen, true));
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editTemplate(spinnerAdapter);
            }
        });
    }

    public View getDialog() {return dialog;}
    String getTitle(){return formDetail.getSelectedItem() != null ? formDetail.getSelectedItem().toString() : "";}
    String getData(){return adapter.getResult();}

    private void editTemplate(final ArrayAdapter<String> spinnerAdapter){
        FormTemplates savedTemplates = handler.getTemplates();
        final FormTemplate form = new FormTemplate(activity, savedTemplates);
        LinkedHashMap<String, String> templates = new LinkedHashMap<>();
        templates.put("➕ Add New Form", "");
        templates.putAll(savedTemplates.getTemplates());

        final String[] titles = templates.keySet().toArray(new String[templates.size()]);
        final String[] data = templates.values().toArray(new String[templates.size()]);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Saved Form Templates")
                .setItems(titles, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int selectedPosition) {
                        form.loadTemplate(titles[selectedPosition], data[selectedPosition]);
                        new AlertDialog.Builder(activity)
                                .setView(form.getDialog())
                                .setPositiveButton(activity.getString(R.string.submit), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String result = form.getData();
                                        if (selectedPosition == 0){
                                            spinnerAdapter.add(form.getTitle());
                                            spinnerAdapter.notifyDataSetChanged();
                                            formDetail.setSelection(spinnerAdapter.getCount()-1);
                                            loadTemplate(result);
                                            spinnerAdapter.remove("Please Add a Template");
                                            spinnerAdapter.notifyDataSetChanged();
                                        } else {
                                            if (formDetail.getSelectedItemPosition() == selectedPosition -1){
                                                loadTemplate(result);
                                            } else {
                                                formDetail.setSelection(selectedPosition - 1);
                                            }
                                        }
                                    }
                                })
                                .setNegativeButton(activity.getString(R.string.cancel), null)
                                .setNeutralButton(R.string.delete_form, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        form.deleteTemplate();
                                        spinnerAdapter.remove(form.getTitle());
                                        if (spinnerAdapter.isEmpty()){
                                            spinnerAdapter.add("Please Add a Template");
                                            loadTemplate("");
                                        }
                                        spinnerAdapter.notifyDataSetChanged();
                                    }
                                }).create()
                                .show();
                    }
                })
                .setPositiveButton(R.string.submit, null)
                .create();
        dialog.show();
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE| WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    private String loadTemplate(String data){
        adapter.clear();
        Set<String> foundTexts = new HashSet<>();
        Matcher matcher = pattern.matcher(data);
        while (matcher.find()) {
            String param = matcher.group(1);
            String option = matcher.group(2);
            String detail = matcher.group(3);
            if (foundTexts.add(param + option + detail)) {
                int index = formType.indexOf(param);
                adapter.add(new FormParam(index, option, detail));
            }
        }
        matcher.reset();
        return matcher.find() ? matcher.replaceAll("$1$2:\n") : data;
    }

    class FormParam {
        int index;
        String[] details;
        String loadedData;
        String template;
        String textInput;
        Boolean[] checkedOptions;

        FormParam(int index, String options, String loadedData){
            this.index = index;
            this.loadedData = loadedData;
            this.details = options.replaceFirst(";", "").split(";");
            if (details.length > 2){
                checkedOptions = new Boolean[details.length - 2];
                for (int i = 0; i < checkedOptions.length; i++){
                    checkedOptions[i] = false;
                }
                String[] savedChecks = loadedData.split(",");
                for (int i = 0; i < savedChecks.length; i++){
                    checkedOptions[i] = savedChecks[i].equalsIgnoreCase("true");
                }
            } else {
                textInput = loadedData;
                checkedOptions = new Boolean[0];
            }
            this.template = formType.get(index) + options + ":";
        }

        @Override
        public String toString() {
            return template + (checkedOptions.length == 0 ? textInput : TextUtils.join(",", checkedOptions));
        }
    }

    class FormAdapter extends RecyclerView.Adapter<FormAdapter.ViewHolder> {
        private List<FormParam> items;

        FormAdapter(List<FormParam> items) {
            this.items = items;
        }

        void add(FormParam entry){
            items.add(entry);
            notifyItemInserted(items.size()-1);
        }

        void clear(){
            items.clear();
            notifyDataSetChanged();
        }

        String getResult(){
            String title = getTitle();
            if (title.isEmpty()) return "";

            StringBuilder data = new StringBuilder("BEGIN:VFORM" +
                    "\nVERSION:1.0" +
                    "\nTEMPLATE:" + title +
                    "\n");

            for(FormParam param : items){
                data.append(param.toString())
                    .append("\n");
            }

            data.append("END:VFORM");
            return data.toString();
        }

        @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.media_form_line, parent, false);
            return new ViewHolder(v);
        }

        @Override public void onBindViewHolder(ViewHolder holder, int position) {
            FormParam item = items.get(position);
            holder.setEntry(item);
        }

        @Override public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView formField;
            EditText formText;
            LinearLayout formChecks;
            ImageButton now;
            TextWatcher watcher;

            ViewHolder(View formLine) {
                super(formLine);
                formField = formLine.findViewById(R.id.field_name);
                formText = formLine.findViewById(R.id.field_data);
                formChecks = formLine.findViewById(R.id.additional);
                now = formLine.findViewById(R.id.operation);
            }

            void setEntry(final FormParam item){
                if (watcher != null) formText.removeTextChangedListener(watcher);
                if (!editable) formText.setEnabled(false);
                formText.setFocusableInTouchMode(true);
                formText.setLongClickable(false);
                formText.setOnClickListener(null);
                formText.setInputType(InputType.TYPE_CLASS_TEXT);
                formText.setVisibility(View.VISIBLE);
                now.setVisibility(View.GONE);
                formChecks.removeAllViews();

                formText.addTextChangedListener(watcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        item.textInput = String.valueOf(s);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });

                switch (item.index){
                    case 0:
                    case 1:
                        addText(item);
                        break;
                    case 2:
                        addDate(item);
                        break;
                    case 3:
                        addCheckboxes(item);
                        break;
                }
            }

            private void addText(FormParam entry){
                formField.setText(entry.details[0]);
                formText.setText(entry.textInput);
                formText.setLongClickable(true);
                if (entry.index == 1){
                    formText.setInputType(
                            InputType.TYPE_CLASS_NUMBER |
                            InputType.TYPE_NUMBER_FLAG_SIGNED |
                            InputType.TYPE_NUMBER_FLAG_DECIMAL);
                }
            }

            private void addDate(FormParam entry){
                final Calendar cal = Calendar.getInstance();
                final String type = entry.details[1];

                if (!type.equals("3") && editable){
                    now.setVisibility(View.VISIBLE);
                    now.setBackground(activity.getIcon(R.drawable.ic_action_time, true));
                    now.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            cal.setTime(Calendar.getInstance().getTime());
                            switch (type) {
                                case "0":
                                    formText.setText(handler.getTimeManager().format(TimeManager.FULL_TIME, cal.getTime()));
                                    break;
                                case "1":
                                    formText.setText(handler.getTimeManager().format(TimeManager.DATE, cal.getTime()));
                                    break;
                                case "2":
                                    formText.setText(handler.getTimeManager().format(TimeManager.BASIC, cal.getTime()));
                                    break;
                            }
                        }
                    });
                }

                formField.setText(entry.details[0]);

                formText.setText(entry.textInput);
                formText.setFocusableInTouchMode(false);
                formText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String current = formText.getText().toString();
                        switch (type){
                            case "0":
                                if (!current.isEmpty()) cal.setTime(handler.getTimeManager().parse(TimeManager.FULL_TIME, current));
                                new DatePickerDialog(activity, new DatePickerDialog.OnDateSetListener() {
                                    @Override
                                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                                        cal.set(Calendar.YEAR, year);
                                        cal.set(Calendar.MONTH, month);
                                        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                                        new TimePickerDialog(activity, new TimePickerDialog.OnTimeSetListener() {
                                            @Override
                                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                                cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                                cal.set(Calendar.MINUTE, minute);
                                                formText.setText(handler.getTimeManager().format(TimeManager.FULL_TIME, cal.getTime()));
                                            }
                                        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), DateFormat.is24HourFormat(activity)).show();
                                    }
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
                                break;
                            case "1":
                                if (!current.isEmpty()) cal.setTime(handler.getTimeManager().parse(TimeManager.DATE, current));
                                new DatePickerDialog(activity, new DatePickerDialog.OnDateSetListener() {
                                    @Override
                                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                                        cal.set(Calendar.YEAR, year);
                                        cal.set(Calendar.MONTH, month);
                                        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                                        formText.setText(handler.getTimeManager().format(TimeManager.DATE, cal.getTime()));
                                    }
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
                                break;
                            case "2":
                            case "3":
                                final int timeType = type.equalsIgnoreCase("2") ? TimeManager.BASIC : TimeManager.DURATION;
                                if (!current.isEmpty()){
                                    cal.setTime(handler.getTimeManager().parse(timeType, current));
                                } else if (timeType == TimeManager.DURATION){
                                    cal.set(Calendar.HOUR_OF_DAY, 0);
                                    cal.set(Calendar.MINUTE, 0);
                                }
                                new TimePickerDialog(activity, new TimePickerDialog.OnTimeSetListener() {
                                    @Override
                                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                        cal.set(Calendar.MINUTE, minute);
                                        formText.setText(handler.getTimeManager().format(timeType, cal.getTime()));
                                    }
                                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), timeType == TimeManager.DURATION || DateFormat.is24HourFormat(activity)).show();
                                break;
                        }
                    }
                });
            }

            private void addCheckboxes(final FormParam entry){
                final List<CheckBox> checkParams = new LinkedList<>();

                if (editable) {
                    formField.setText(entry.details[1].equals("0") ? "Choose One" : "Choose Multiple");
                    formText.setText(entry.details[0]);
                    formText.setFocusableInTouchMode(false);
                } else {
                    View margin = new View(activity);
                    margin.setLayoutParams(new LinearLayout.LayoutParams(0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, activity.getResources().getDisplayMetrics())));
                    formChecks.addView(margin);
                    formField.setText(entry.details[0]);
                    formText.setVisibility(View.GONE);
                }

                for (int i = 2, j = 0; i < entry.details.length; i++, j++){
                    final CheckBox checkBox = new CheckBox(activity);
                    if (!editable) checkBox.setEnabled(false);

                    final int index = j;
                    checkBox.setText(entry.details[i]);
                    checkBox.setChecked(entry.checkedOptions[index]);

                    checkParams.add(checkBox);
                    formChecks.addView(checkBox);

                    checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            //uncheck others for single check
                            entry.checkedOptions[index] = isChecked;
                            if (entry.details[1].equals("0") && isChecked) {
                                for (CheckBox check : checkParams) {
                                    if (check != checkBox && check.isChecked()) {
                                        check.setChecked(false);
                                    }
                                }
                            }
                        }
                    });
                }
            }
        }
    }
}
