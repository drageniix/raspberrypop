package com.drageniix.raspberrypop.utilities;

import android.content.Context;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeManager {
    private SimpleDateFormat basicTime, dateTime, dateTimeNoYear, writtenDate, durationTime, fileTime, veventTime, stopwatchTime;
    public static final int FULL_TIME = 0, BASIC = 1, DURATION = 2, DATE = 3, DATE_NO_YEAR = 4,VEVENT = 5, STOPWATCH = 6,  WDATE = 7;

    TimeManager(Context context){
        basicTime = new SimpleDateFormat(DateFormat.is24HourFormat(context) ? "H:mm" : "h:mm a", Locale.getDefault());
        durationTime = new SimpleDateFormat("H'h', m'm'", Locale.getDefault());
        fileTime = new SimpleDateFormat((DateFormat.is24HourFormat(context) ? "yy-MM-dd (H:mm)" : "MM-dd-yy (h:mm a)"), Locale.getDefault());
        dateTime = new SimpleDateFormat((DateFormat.is24HourFormat(context) ? "yy-MM-dd" : "MM-dd-yy"), Locale.getDefault());
        dateTimeNoYear = new SimpleDateFormat((DateFormat.is24HourFormat(context) ? "dd-MM" : "MM-dd"), Locale.getDefault());
        veventTime = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault());
        stopwatchTime = new SimpleDateFormat("H:mm:ss.SSS", Locale.getDefault());
        writtenDate = new SimpleDateFormat("EEEE, MMMM dd, yyyy (" + (DateFormat.is24HourFormat(context) ? "H:mm" : "h:mm a") + ")", Locale.getDefault());
    }

    public Date parse(int source, String time){
        try {
            switch (source){
                case FULL_TIME:
                    return fileTime.parse(time);
                case BASIC:
                    return basicTime.parse(time);
                case DURATION:
                    return durationTime.parse(time.startsWith("0h, ") ? time : "0h, " + time);
                case STOPWATCH:
                    return stopwatchTime.parse(time);
                case DATE:
                    return dateTime.parse(time);
                case DATE_NO_YEAR:
                    return dateTimeNoYear.parse(time);
                case VEVENT:
                    return veventTime.parse(time.replace("Z", "").trim());
                case WDATE:
                    return writtenDate.parse(time);
            }
        } catch (Exception e){
            Logger.log(Logger.FRAG, e);
        }
        return null;
    }

    public String format(int source, Date date){
        switch (source){
            case FULL_TIME:
                return fileTime.format(date);
            case BASIC:
                return basicTime.format(date);
            case DURATION:
                String duration = durationTime.format(date);
                return duration.startsWith("0h, ") ? duration.substring(4) : duration;
            case STOPWATCH:
                return stopwatchTime.format(date);
            case DATE:
                return dateTime.format(date);
            case DATE_NO_YEAR:
                return dateTimeNoYear.format(date);
            case VEVENT:
                return veventTime.format(date) + "Z";
            case WDATE:
                return writtenDate.format(date);
        }
        return null;
    }
}
