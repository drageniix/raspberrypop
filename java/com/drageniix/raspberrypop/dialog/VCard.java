package com.drageniix.raspberrypop.dialog;

import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.media.Media;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class VCard {
    private List<String> hints, vCardParam;
    private View dialog;
    private LinearLayout contactParamHolder;
    private List<EditText> contactParams;
    private List<String> contactDetails;
    private EditText name;

    VCard(BaseActivity activity){
        this.dialog = View.inflate(activity, R.layout.media_contact, null);
        this.name = dialog.findViewById(R.id.contact_name);
        this.contactParamHolder = dialog.findViewById(R.id.contactParams);
        this.hints = Arrays.asList(activity.getResources().getStringArray(R.array.contact_hints));
        this.vCardParam = Arrays.asList(activity.getResources().getStringArray(R.array.contact_params));
        setUp(activity);
    }

    View getDialog() {return dialog;}
    String getTitle(){return name.getText().toString().trim();}

    String getData(){
        StringBuilder vCard = new StringBuilder("BEGIN:VCARD\nVERSION:3.0\n")
                .append("FN:").append(name.getText().toString().trim()).append("\n");
        for(int i = 0; i < contactParams.size(); i++) {
            String text = contactParams.get(i).getText().toString().trim();
            if (!text.isEmpty()) {
                vCard
                        .append(contactDetails.get(i))
                        .append(":")
                        .append(text)
                        .append("\n");
            }
        }
        vCard.append("END:VCARD");
        return vCard.toString();
    }

    void setUp(final BaseActivity activity){
        final Spinner contactDetail = dialog.findViewById(R.id.contact_field);
        final ImageButton contactAdd = dialog.findViewById(R.id.operation);

        final LinearLayout contactLine = (LinearLayout) View.inflate(activity, R.layout.media_popup_operation, null);
        final EditText contactText = contactLine.findViewById(R.id.param);
        final ImageButton contactSubtract = contactLine.findViewById(R.id.operation);

        contactParams = new LinkedList<>();
        contactDetails = new LinkedList<>();

        contactParamHolder.addView(contactLine);
        contactParams.add(contactText);
        contactDetails.add(vCardParam.get(contactDetail.getSelectedItemPosition()));

        contactText.setHint(hints.get(contactDetail.getSelectedItemPosition()));

        contactSubtract.setBackground(activity.getIcon(R.drawable.ic_action_minus, false));
        contactSubtract.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = contactParamHolder.indexOfChild(contactLine);
                contactParamHolder.removeViewAt(index);
                contactParams.remove(index);
                contactDetails.remove(index);
            }
        });

        contactAdd.setBackground(activity.getIcon(R.drawable.ic_action_add_light, false));
        contactAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final LinearLayout contactLine = (LinearLayout) View.inflate(activity, R.layout.media_popup_operation, null);
                final EditText contactText = contactLine.findViewById(R.id.param);
                final ImageButton contactSubtract = contactLine.findViewById(R.id.operation);
                contactParamHolder.addView(contactLine);
                contactParams.add(contactText);
                contactDetails.add(vCardParam.get(contactDetail.getSelectedItemPosition()));

                contactText.setHint(hints.get(contactDetail.getSelectedItemPosition()));
                contactLine.startAnimation(AnimationUtils.loadAnimation(activity, android.R.anim.slide_in_left));
                contactSubtract.setBackground(activity.getIcon(R.drawable.ic_action_minus, false));
                contactSubtract.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int index = contactParamHolder.indexOfChild(contactLine);
                        contactParamHolder.removeViewAt(index);
                        contactParams.remove(index);
                        contactDetails.remove(index);
                    }
                });
            }
        });
    }

    void loadMedia(BaseActivity activity, Media media){
        contactParamHolder.removeViewAt(0);
        contactParams.remove(0);
        contactDetails.remove(0);

        name.setText(media.getTitle());

        StringBuilder paramPatternSB = new StringBuilder();
        for(int i = 0; i < vCardParam.size(); i++){
            paramPatternSB.append(vCardParam.get(i)).append("|");
        }
        String paramPattern = paramPatternSB.toString();
        paramPattern = "(" + paramPattern.substring(0, paramPattern.length()-1) + ")";
        Pattern pattern = Pattern.compile(paramPattern +
                "(?:[0-9A-Z,;=()\\-]+)*:(.*)" + //actual data
                "(?=(?s:\n(?:[0-9A-Z,;=()\\-]+):))"); //until next data

        Set<String> foundTexts = new HashSet<>();
        Matcher matcher = pattern.matcher(media.getAlternateID());
        while (matcher.find()) {
            String param = matcher.group(1);
            String detail = matcher.group(2);
            if (foundTexts.add(param + detail)) {
                int index = vCardParam.indexOf(param);
                final LinearLayout contactLine = (LinearLayout) View.inflate(activity, R.layout.media_popup_operation, null);
                final EditText contactText = contactLine.findViewById(R.id.param);
                final ImageButton contactSubtract = contactLine.findViewById(R.id.operation);
                contactParamHolder.addView(contactLine);
                contactParams.add(contactText);
                contactDetails.add(vCardParam.get(index));

                contactText.setText(detail);
                contactText.setHint(hints.get(index));
                contactLine.startAnimation(AnimationUtils.loadAnimation(activity, android.R.anim.slide_in_left));
                contactSubtract.setBackground(activity.getIcon(R.drawable.ic_action_minus, false));
                contactSubtract.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int index = contactParamHolder.indexOfChild(contactLine);
                        contactParamHolder.removeViewAt(index);
                        contactParams.remove(index);
                        contactDetails.remove(index);
                    }
                });
            }
        }
    }
}
