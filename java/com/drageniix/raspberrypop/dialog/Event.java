package com.drageniix.raspberrypop.dialog;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.TimeManager;
import com.drageniix.raspberrypop.utilities.categories.AuxiliaryApplication;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class Event {
    private BaseActivity activity;
    private DBHandler handler;
    private EditText customID;
    private AutoCompleteTextView titleSearch;

    private EditText title, description, location, start, end;
    private Calendar startCal, endCal;
    
    Event(BaseActivity activity, DBHandler handler, EditText customID, AutoCompleteTextView titleSearch){
        this.activity = activity;
        this.customID = customID;
        this.titleSearch = titleSearch;
        this.handler = handler;
    }

    public static Calendar parseTime(DBHandler handler, String time){
        Calendar now = Calendar.getInstance();
        Calendar alarm = Calendar.getInstance();
        alarm.setTimeInMillis(handler.getTimeManager().parse(TimeManager.BASIC, time).getTime());
        alarm.set(Calendar.YEAR, now.get(Calendar.YEAR));
        alarm.set(Calendar.MONTH, now.get(Calendar.MONTH));
        alarm.set(Calendar.DAY_OF_YEAR, now.get(Calendar.DAY_OF_YEAR));
        if (alarm.before(now)){
            alarm.add(Calendar.DAY_OF_YEAR, 1);
        }
        return alarm;
    }

    void setCountdown(){
        this.startCal = Calendar.getInstance(TimeZone.getDefault());
        String[] details = customID.getText().toString().split("~!~");
        if (details.length > 1) {
            startCal.set(Integer.parseInt(details[0]), Integer.parseInt(details[1]), Integer.parseInt(details[2]), Integer.parseInt(details[3]), Integer.parseInt(details[4]), 0);
        }

        new DatePickerDialog(activity, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, final int year, final int month, final int dayOfMonth) {
                new TimePickerDialog(activity, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, final int hourOfDay, final int minute) {
                        startCal.set(year, month, dayOfMonth, hourOfDay, minute, 0);
                        titleSearch.setText(handler.getTimeManager().format(TimeManager.WDATE, startCal.getTime()));
                        customID.setText(year + "~!~" + month + "~!~" + dayOfMonth  + "~!~" + hourOfDay + "~!~" + minute);
                    }
                }, startCal.get(Calendar.HOUR_OF_DAY), startCal.get(Calendar.MINUTE), DateFormat.is24HourFormat(activity)).show();
            }
        }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
    }

    void setTimer(int oldHours, int oldMinutes, final AuxiliaryApplication enabledAuxiliary){
        new TimePickerDialog(activity, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hours, int minutes) {
                customID.setText(String.valueOf((hours * 60 * 60) + (minutes * 60)));
                GregorianCalendar calendar = new GregorianCalendar(Locale.getDefault());
                calendar.set(GregorianCalendar.HOUR_OF_DAY, hours);
                calendar.set(GregorianCalendar.MINUTE, minutes);
                if (enabledAuxiliary == AuxiliaryApplication.TIMER) {
                    titleSearch.setText(handler.getTimeManager().format(TimeManager.DURATION, calendar.getTime()));
                } else if (enabledAuxiliary == AuxiliaryApplication.ALARM) {
                    titleSearch.setText(handler.getTimeManager().format(TimeManager.BASIC, calendar.getTime()));
                } else if (enabledAuxiliary == AuxiliaryApplication.SCAN_ALARM) {
                    customID.setText(handler.getTimeManager().format(TimeManager.BASIC, calendar.getTime()));
                }
            }
        }, oldHours, oldMinutes, enabledAuxiliary == AuxiliaryApplication.TIMER || DateFormat.is24HourFormat(activity)).show();
    }

    String getEventResults(){
        String startString = start.getText().toString().trim();
        String endString = end.getText().toString().trim();

        String eventDisplay = "Title: " +
                title.getText().toString() +
                "\nDescription: " +
                description.getText().toString() +
                "\nLocation: " +
                location.getText().toString() +
                "\nStart: " +
                (startString.isEmpty() ? "" : handler.getTimeManager().format(TimeManager.FULL_TIME, startCal.getTime())) +
                "\nEnd: " +
                (endString.isEmpty() ? "" : handler.getTimeManager().format(TimeManager.FULL_TIME, endCal.getTime()));

        startCal.add(Calendar.MONTH, -1);
        endCal.add(Calendar.MONTH, -1);

        if (!startString.isEmpty()){
            startString = handler.getTimeManager().format(TimeManager.VEVENT, startCal.getTime());}
        if (!endString.isEmpty()){
            endString = handler.getTimeManager().format(TimeManager.VEVENT, endCal.getTime());}

        String display = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "SUMMARY:" + title.getText().toString() + "\n" +
                "DESCRIPTION:" + description.getText().toString() + "\n" +
                "LOCATION:" + location.getText().toString() + "\n" +
                "DTSTART:" + startString +"\n" +
                "DTEND:" + endString + "\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        titleSearch.setText(title.getText().toString());
        if (titleSearch.getText().toString().isEmpty()) {
            titleSearch.setText(String.valueOf("New Event"));
        }
        customID.setText(display);
        return eventDisplay;
    }
    
    View setEvent(Media editMedia){
        View dialog = View.inflate(activity, R.layout.media_event, null);
        this.startCal = Calendar.getInstance(TimeZone.getDefault());
        this.endCal = Calendar.getInstance(TimeZone.getDefault());
        this.title = dialog.findViewById(R.id.event_title);
        this.description = dialog.findViewById(R.id.event_description);
        this.location = dialog.findViewById(R.id.event_location);
        this.start = dialog.findViewById(R.id.event_start);
        this.end = dialog.findViewById(R.id.event_end);

        if (editMedia != null){
            String[] data = editMedia.getStreamingID().split("\n(.*?):", -1);
            title.setText(editMedia.getTitle());
            if (editMedia.getStreamingID().startsWith("BEGIN:VCALENDAR")) {
                description.setText(data[3]);
                location.setText(data[4]);
                if (!data[5].isEmpty()) {
                    startCal.set(
                            Integer.parseInt(data[5].substring(0, 4)),
                            Integer.parseInt(data[5].substring(4, 6)),
                            Integer.parseInt(data[5].substring(6, 8)),
                            Integer.parseInt(data[5].substring(9, 11)),
                            Integer.parseInt(data[5].substring(11, 13)),
                            Integer.parseInt(data[5].substring(13, 15)));

                    start.setText(handler.getTimeManager().format(TimeManager.FULL_TIME, startCal.getTime()));
                }

                if (!data[6].isEmpty()) {
                    endCal.set(
                            Integer.parseInt(data[6].substring(0, 4)),
                            Integer.parseInt(data[6].substring(4, 6)),
                            Integer.parseInt(data[6].substring(6, 8)),
                            Integer.parseInt(data[6].substring(9, 11)),
                            Integer.parseInt(data[6].substring(11, 13)),
                            Integer.parseInt(data[6].substring(13, 15)));

                    end.setText(handler.getTimeManager().format(TimeManager.FULL_TIME, endCal.getTime()));
                }
            }
        }

        start.setFocusableInTouchMode(false);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(activity, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        startCal.set(Calendar.YEAR, year);
                        startCal.set(Calendar.MONTH, month);
                        startCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        new TimePickerDialog(activity, new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hours, int minutes) {
                                startCal.set(Calendar.HOUR_OF_DAY, hours);
                                startCal.set(Calendar.MINUTE, minutes);
                                start.setText(handler.getTimeManager().format(TimeManager.FULL_TIME, startCal.getTime()));
                            }
                        }, startCal.get(Calendar.HOUR_OF_DAY), startCal.get(Calendar.MINUTE), DateFormat.is24HourFormat(activity)).show();
                    }
                }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        end.setFocusableInTouchMode(false);
        end.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(activity, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        endCal.set(Calendar.YEAR, year);
                        endCal.set(Calendar.MONTH, month);
                        endCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        new TimePickerDialog(activity, new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hours, int minutes) {
                                endCal.set(Calendar.HOUR_OF_DAY, hours);
                                endCal.set(Calendar.MINUTE, minutes);
                                end.setText(handler.getTimeManager().format(TimeManager.FULL_TIME, endCal.getTime()));
                            }
                        }, endCal.get(Calendar.HOUR_OF_DAY), endCal.get(Calendar.MINUTE), DateFormat.is24HourFormat(activity)).show();
                    }
                }, endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH), endCal.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        return dialog;
    }
}
