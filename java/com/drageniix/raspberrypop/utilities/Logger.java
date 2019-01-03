package com.drageniix.raspberrypop.utilities;
// e d i v w wtf

import android.os.Bundle;
import android.util.Log;

import com.drageniix.raspberrypop.utilities.billing.Billing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.util.Calendar;
import java.util.regex.Matcher;

public class Logger {
    public static String TEST = "TEST", BILL = "BILL", FRAG = "FRAG", FILE = "FILE", DB = "DB", CAST = "CAST", API = "API";
    private static DBHandler handler;
    private static String logComplete = "";

    public static void setHandler(DBHandler dbhandler){
        handler = dbhandler;
    }

    public static void test(Object...object){
        log(TEST, object);
    }

    public static void log(String tag, Object...object) {
        String TAG = "RPDebug";
        tag = tag == null ? TEST : tag.toUpperCase();
        StringBuilder stringBuilder = new StringBuilder();
        if (object != null) {
            for (Object o : object) {
                toString(stringBuilder, o, true);
            }
            String result = stringBuilder.toString().trim();//.replace("\n","\n\\n");
            logComplete += tag + " - " +
                    handler.getTimeManager().format(TimeManager.FULL_TIME, Calendar.getInstance().getTime()).replace("-", "/") + "\n" +
                    result + "\n---------------\n";

            if(Billing.isBeta()) {
                if (tag.equalsIgnoreCase(TEST)) {
                    Log.d(TAG + "-" + tag, result);
                } else {
                    Log.e(TAG + "-" + tag, result);
                }
            }
        } else {
            logComplete +=  tag + " - " +
                    handler.getTimeManager().format(TimeManager.FULL_TIME, Calendar.getInstance().getTime()).replace("-", "/") + "\n" +
                    "NULL OBJECT\n---------------\n";

            if(Billing.isBeta()) {
                if (tag.equalsIgnoreCase(TEST)) {
                    Log.d(TAG + "-" + tag, "NULL OBJECT");
                } else {
                    Log.e(TAG + "-" + tag, "NULL OBJECT");
                }
            }
        }
        handler.getPreferences().setLogger(logComplete);
    }

    private static void toString(StringBuilder stringBuilder, Object o, boolean newLine){
        if (o == null) o = "null";
        if (o.getClass().isArray()) {
            stringBuilder.append(o.getClass().getSimpleName()).append(" [");
            int length = Array.getLength(o);
            for (int i = 0; i < length; i++) {
                toString(stringBuilder, Array.get(o, i), false);
                if (i + 1 < length) stringBuilder.append(", ");
            }
            stringBuilder.append("]");
        } else if (o instanceof Bundle) {
            for (String key : ((Bundle) o).keySet()) {
                Object value = ((Bundle) o).get(key);
                stringBuilder
                        .append(String.format("%s %s (%s)\n", key, value.toString(), value.getClass().getName()));
            }
        } else if (o instanceof Matcher){
            Matcher matcher = (Matcher)o;
            matcher.reset();
            while(matcher.find()) {
                for (int i = 0; i <= matcher.groupCount(); i++) {
                    stringBuilder
                            .append(String.format("%s: %s\n", i, matcher.group(i).trim()));
                }
            }
            matcher.reset();
		} else if (o instanceof Throwable){
			Throwable e = (Throwable)o;
			stringBuilder
				.append(e.toString())
				.append("\n\u0009");
            
			StackTraceElement[] stackTrace = e.getStackTrace();
            for(StackTraceElement line : stackTrace){
                if (line.getClassName().contains("raspberrypop")){
                    stringBuilder.append(line).append("\n\u0009");
                }
            }
        } else {
            stringBuilder.append(o.toString());
        }
        if (newLine)stringBuilder.append("\n");
    }
}