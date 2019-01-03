package com.drageniix.raspberrypop.dialog;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.utilities.FileHelper;
import com.drageniix.raspberrypop.utilities.FormTemplates;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

public class FormTemplate {
    private BaseActivity activity;
    private FormTemplates savedTemplates;
    private View dialog;
    private List<String> hints, formTypes, dateOptions, checkOptions;
    private EditText name;
    private Spinner formDetail;
    private FormAdapter adapter;
    private RecyclerView.LayoutManager lm;

    public FormTemplate(BaseActivity activity, FormTemplates savedTemplates){
        this.activity = activity;
        this.savedTemplates = savedTemplates;
        this.dialog = View.inflate(activity, R.layout.media_form_template, null);
        this.name = dialog.findViewById(R.id.form_name);
        this.formDetail = dialog.findViewById(R.id.form_field);

        this.hints = Arrays.asList(activity.getResources().getStringArray(R.array.form_hints));
        this.formTypes = Arrays.asList(activity.getResources().getStringArray(R.array.form_params));
        this.dateOptions = Arrays.asList(activity.getResources().getStringArray(R.array.form_date_params));
        this.checkOptions = Arrays.asList(activity.getResources().getStringArray(R.array.form_check_params));

        RecyclerView list = dialog.findViewById(R.id.entries);
        lm = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
        adapter = new FormAdapter(new LinkedList<FormParam>());
        ItemTouchHelper.Callback callback = new FormCallback(adapter);
        ItemTouchHelper drag = new ItemTouchHelper(callback);
        drag.attachToRecyclerView(list);
        list.setHasFixedSize(false);
        list.setLayoutManager(lm);
        list.setAdapter(adapter);

        final ImageButton formAdd = dialog.findViewById(R.id.operation);
        formAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.add(new FormParam(formDetail.getSelectedItemPosition(), ""));
            }
        });
    }

    public View getDialog() {return dialog;}
    public String getData(){return adapter.saveTemplate();}
    public void deleteTemplate(){
        savedTemplates.removeTemplate(getTitle());
    }
    public String getTitle(){
        String title = name.getText().toString().trim();
        return title.isEmpty() ? "New Form" : title;
    }

    public void loadTemplate(String title, String data){
        adapter.clear();
        formDetail.setSelection(0);

        if (data.isEmpty()){
            name.setText("");
            adapter.add(new FormParam(formDetail.getSelectedItemPosition(), ""));
        } else {
            name.setText(title);
            Set<String> foundTexts = new HashSet<>();
            Matcher matcher = Form.getPattern().matcher(data);
            while (matcher.find()) {
                String param = matcher.group(1);
                String option = matcher.group(2);
                //String detail = matcher.group(3);
                if (foundTexts.add(param + option)) {
                    adapter.add(new FormParam(formTypes.indexOf(param), option));
                }
            }
        }
    }

    class FormParam {
        String type;
        String prompt;
        String[] option;
        int index, radioIndex;
        List<String> additional;

        FormParam(int index, String option){
            this.index = index;
            this.type = formTypes.get(index);
            this.additional = new LinkedList<>();
            this.option = option.replaceFirst(";", "").split(";");
            this.prompt = this.option[0];
            if (this.option.length > 1) {
                String pref = this.option[1];
                this.radioIndex = pref.isEmpty() || !FileHelper.isNumeric(pref) ? 0 : Integer.parseInt(pref);
            }
            if (this.option.length > 2){
                additional.addAll(Arrays.asList(this.option).subList(2, this.option.length));
            }
        }

        @Override
        public String toString() {
            String promptText = prompt + ";";
            if (prompt.isEmpty()) return "";

            String indexText = (radioIndex == -1) ? "" : (radioIndex + ";");

            String finalAdditionalText = "";
            if (!additional.isEmpty()){
                StringBuilder additionalText = new StringBuilder();
                for (String text : additional) {
                    String option = text.trim();
                    if (!option.isEmpty()) {
                        additionalText.append(option).append(";");
                    }
                }
                finalAdditionalText = additionalText.toString().trim();
                if (finalAdditionalText.isEmpty()) return "";
            } else if (this.index == 3){
                return "";
            }

            return promptText + indexText + finalAdditionalText;
        }
    }


    class FormAdapter extends RecyclerView.Adapter<FormAdapter.ViewHolder>{
        private List<FormParam> formParams;

        FormAdapter(List<FormParam> formParams){
            this.formParams = formParams;
        }

        void add(FormParam entry){
            formParams.add(entry);
            notifyItemInserted(formParams.size()-1);
            lm.scrollToPosition(formParams.size()-1);
        }

        void remove(FormParam entry){
            int index = formParams.indexOf(entry);
            formParams.remove(entry);
            notifyItemRemoved(index);
        }

        void clear(){
            formParams.clear();
            notifyDataSetChanged();
        }

        String saveTemplate(){
            StringBuilder formTemplate = new StringBuilder();
            for (int i = 0; i < formParams.size(); i++) {
                String text = formParams.get(i).toString();
                if (!text.isEmpty()) {
                    formTemplate
                            .append(formParams.get(i).type)
                            .append(";")
                            .append(text.replace(":", " "))
                            .append(":\n");
                }
            }

            String formData = formTemplate.toString().trim();
            if (!formData.isEmpty()) {
                String title = getTitle();
                String data = "BEGIN:VFORM" +
                        "\nVERSION:1.0" +
                        "\nTEMPLATE:" + title +
                        "\n" + formData +
                        "\nEND:VFORM";
                savedTemplates.addTemplate(title, data);
                return data;
            }
            return "";
        }

        void onItemMove(int start, int end){
            Collections.swap(formParams, start, end);
            notifyItemMoved(start, end);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.media_popup_operation_radio, parent, false);
            return new FormAdapter.ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(FormAdapter.ViewHolder holder, int position) {
            FormParam item = formParams.get(position);
            holder.setEntry(item);
        }

        @Override
        public int getItemCount() {
            return formParams.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            EditText formText;
            ImageButton formSubtract;
            RadioGroup options;
            Button addCheckBox;
            LinearLayout checkParamHolder;
            TextWatcher watcher;

            ViewHolder(View formLine) {
                super(formLine);
                formText = formLine.findViewById(R.id.param);
                formSubtract = formLine.findViewById(R.id.operation);
                options = formLine.findViewById(R.id.options);
                addCheckBox = formLine.findViewById(R.id.add_box);
                checkParamHolder = formLine.findViewById(R.id.checkboxes);
                formSubtract.setBackground(activity.getIcon(R.drawable.ic_action_minus, false));
            }

            private void setEntry(final FormParam form){
                if (watcher != null) formText.removeTextChangedListener(watcher);
                options.setVisibility(View.GONE);
                options.removeAllViews();
                addCheckBox.setVisibility(View.GONE);
                checkParamHolder.setVisibility(View.GONE);
                checkParamHolder.removeAllViews();

                formText.addTextChangedListener(watcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        form.prompt = String.valueOf(s);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });

                switch (form.index){
                    case 2:
                        addDate(form);
                        break;
                    case 3:
                        addCheckbox(form);
                        break;
                }

                formSubtract.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        adapter.remove(form);
                    }
                });

                formText.setText(form.prompt);
                formText.setHint(hints.get(form.index));
                itemView.startAnimation(AnimationUtils.loadAnimation(activity, android.R.anim.slide_in_left));
            }

            private void addDate(final FormParam item){
                for (String date : dateOptions){
                    RadioButton button = new RadioButton(activity);
                    button.setText(date);
                    options.addView(button);
                }

                options.setVisibility(View.VISIBLE);
                options.setOrientation(LinearLayout.VERTICAL);
                options.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        item.radioIndex = options.indexOfChild(options.findViewById(checkedId));
                    }
                });

                ((RadioButton)options.getChildAt(item.radioIndex)).setChecked(true);
            }

            private void addCheckbox(final FormParam item){
                checkParamHolder.setVisibility(View.VISIBLE);
                addCheckBox.setVisibility(View.VISIBLE);
                addCheckBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        item.additional.add("");
                        addCheckBox(checkParamHolder, item, item.additional.size()-1);
                    }
                });

                for (int i = 0; i < item.additional.size(); i++){
                    addCheckBox(checkParamHolder, item, i);
                }

                for (String check : checkOptions){
                    RadioButton button = new RadioButton(activity);
                    button.setText(check);
                    options.addView(button);
                }

                options.setVisibility(View.VISIBLE);
                options.setOrientation(LinearLayout.HORIZONTAL);
                options.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        item.radioIndex = options.indexOfChild(options.findViewById(checkedId));
                    }
                });

                ((RadioButton)options.getChildAt(item.radioIndex)).setChecked(true);
            }

            private void addCheckBox(final LinearLayout checkParamHolder, final FormParam item, final int index){
                final LinearLayout checkLine = (LinearLayout) View.inflate(activity, R.layout.media_popup_operation, null);
                final EditText checkText = checkLine.findViewById(R.id.param);
                final ImageButton checkSubtract = checkLine.findViewById(R.id.operation);

                checkParamHolder.addView(checkLine, checkParamHolder.getChildCount());
                checkText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        item.additional.set(checkParamHolder.indexOfChild(checkLine), String.valueOf(s));
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });

                checkText.setText(item.additional.get(index));
                checkText.setHint("Checkbox Option");
                checkLine.startAnimation(AnimationUtils.loadAnimation(activity, android.R.anim.slide_in_left));
                checkSubtract.setBackground(activity.getIcon(R.drawable.ic_action_minus, false));
                checkSubtract.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int index = checkParamHolder.indexOfChild(checkLine);
                        checkParamHolder.removeViewAt(index);
                        item.additional.remove(index - 1);
                    }
                });
            }
        }
    }

    class FormCallback extends ItemTouchHelper.Callback {
        private FormAdapter adapter;

        FormCallback(FormAdapter adapter){
            this.adapter = adapter;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        }

        @Override
        public boolean isLongPressDragEnabled() {return true;}

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
            adapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
            return false;
        }

        @Override public boolean isItemViewSwipeEnabled() {return false;}
        @Override public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {}
    }
}