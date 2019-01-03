package com.drageniix.raspberrypop.utilities.api;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.activities.ListActivity;
import com.drageniix.raspberrypop.activities.ScanActivity;
import com.drageniix.raspberrypop.dialog.Form;
import com.drageniix.raspberrypop.fragments.BaseFragment;
import com.drageniix.raspberrypop.dialog.DatabaseDialog;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.media.MediaMetadata;
import com.drageniix.raspberrypop.utilities.Logger;
import com.drageniix.raspberrypop.utilities.TimeManager;
import com.drageniix.raspberrypop.utilities.categories.AuxiliaryApplication;
import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;
import com.drageniix.raspberrypop.utilities.custom.CaseInsensitiveMap;
import com.google.android.gms.vision.barcode.Barcode;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.net.URI;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.drageniix.raspberrypop.utilities.categories.AuxiliaryApplication.WIFI_CONNECTION;

public class BarCodeAPI extends APIBase{
    private DecimalFormat df = new DecimalFormat("0.00");
    private static Pattern URIpattern = Pattern.compile("([^\\s]*)([A-Za-z0-9]*):([^$\\s]*)");
	
	private String getTitle(Barcode barcode){
        switch (barcode.valueFormat){
            case Barcode.GEO:
                return "Location";
            case Barcode.URL:
                return "Website URL";
            case Barcode.PHONE:
                return "Phone Number";
            case Barcode.EMAIL:
                return "Email";
            case Barcode.SMS:
                return "SMS";
            case Barcode.CALENDAR_EVENT:
                return "Event";
            case Barcode.WIFI:
                return  "Wifi Connection";
            case Barcode.CONTACT_INFO:
                return "Contact";
            case Barcode.DRIVER_LICENSE:
                return "License";
            case Barcode.ISBN:
                return "ISBN";
            case Barcode.PRODUCT:
                return "Product";
            case Barcode.TEXT:
            default:
                if (barcode.rawValue.startsWith("BEGIN:VFORM") && barcode.rawValue.endsWith("END:VFORM")) {
                    Pattern pattern = Pattern.compile("TEMPLATE:(.*)\n");
                    Matcher matcher = pattern.matcher(barcode.rawValue);
                    if (matcher.find()){
                       return matcher.group(1);
                    } else {
                        return AuxiliaryApplication.FORM.getName();
                    }
                } else if (barcode.rawValue.startsWith("BEGIN:VLIST") && barcode.rawValue.endsWith("END:VLIST")) {
                    Pattern pattern = Pattern.compile("TITLE:(.*)\n");
                    Matcher matcher = pattern.matcher(barcode.rawValue);
                    if (matcher.find()){
                        return matcher.group(1);
                    } else {
                        return AuxiliaryApplication.LIST.getName();
                    }
                } else {
                    return AuxiliaryApplication.SIMPLE_NOTE.getName();
                }
        }
    }

    private String getButton(Barcode barcode){
        switch (barcode.valueFormat){
            case Barcode.GEO:
                return "Navigate";
            case Barcode.URL:
                return "Open Link";
            case Barcode.PHONE:
                return "Dial Number";
            case Barcode.EMAIL:
                return "Send Email";
            case Barcode.SMS:
                return "Send SMS";
            case Barcode.CALENDAR_EVENT:
                return "Create Event";
            case Barcode.WIFI:
                return  "Connect";
            case Barcode.CONTACT_INFO:
                return "Add Contact";
            case Barcode.DRIVER_LICENSE:
                return "";
            case Barcode.ISBN:
            case Barcode.PRODUCT:
                return "Search Web";
            case Barcode.TEXT:
            default:
                return "";
        }
    }

    private String getDisplayData(Barcode barcode){
        StringBuilder display = new StringBuilder();
        switch (barcode.valueFormat){
            case Barcode.CONTACT_INFO:
                Pattern pattern = Pattern.compile(
                        "([A-Z\\-]+)" +
                        "(?:[0-9A-Z,;=()\\-]+)*:" +
                        "(.*)" +
                        "(?=(?s:\n(?:[0-9A-Z,;=()\\-]+):))");

                Matcher matcher = pattern.matcher(barcode.rawValue);
                HashSet<String> details = new HashSet<>();
                while (matcher.find()){
                    if (!matcher.group(1).equals("BEGIN") && !matcher.group(1).equals("END")
                            && !matcher.group(1).equals("VERSION") && !matcher.group(1).equals("N")) {
                        details.add(matcher.group(2).replace(";", " ").trim());
                    }
                }
                for(String item : details){
                    display.append(item).append("\n");
                }
                break;
            case Barcode.DRIVER_LICENSE:
                display.append(barcode.driverLicense.documentType)
                        .append("\nName: ")
                        .append(barcode.driverLicense.firstName)
                        .append(" ")
                        .append(barcode.driverLicense.middleName)
                        .append(" ")
                        .append(barcode.driverLicense.lastName)
                        .append("\nDate of Birth: ")
                        .append(barcode.driverLicense.birthDate)
                        .append("\nGender: ")
                        .append(barcode.driverLicense.gender.equals("2") ? "F" : "M")
                        .append("\nAddress: ")
                        .append(barcode.driverLicense.addressStreet)
                        .append(", ")
                        .append(barcode.driverLicense.addressCity)
                        .append(", ")
                        .append(barcode.driverLicense.addressState)
                        .append(", ")
                        .append(barcode.driverLicense.addressZip)
                        .append("\nLicense Number: ")
                        .append(barcode.driverLicense.licenseNumber)
                        .append("\nIssued: ")
                        .append(barcode.driverLicense.issueDate)
                        .append(", ")
                        .append(barcode.driverLicense.issuingCountry)
                        .append("\nExpires: ")
                        .append(barcode.driverLicense.expiryDate);
                break;
            case Barcode.CALENDAR_EVENT:
                Calendar beginCal = Calendar.getInstance();
                beginCal.set(barcode.calendarEvent.start.year, barcode.calendarEvent.start.month, barcode.calendarEvent.start.day, barcode.calendarEvent.start.hours, barcode.calendarEvent.start.minutes);

                Calendar endCal = Calendar.getInstance();
                endCal.set(barcode.calendarEvent.end.year, barcode.calendarEvent.end.month, barcode.calendarEvent.end.day, barcode.calendarEvent.end.hours, barcode.calendarEvent.end.minutes);

                beginCal.add(Calendar.MONTH, 1);
                endCal.add(Calendar.MONTH, 1);

                display.append("Title: ")
                        .append(barcode.calendarEvent.summary)
                        .append("\nDescription: ")
                        .append(barcode.calendarEvent.description)
                        .append("\nLocation: ")
                        .append(barcode.calendarEvent.location)
                        .append("\nStart: ")
                        .append(handler.getTimeManager().format(TimeManager.FULL_TIME, beginCal.getTime()))
                        .append("\nEnd: ")
                        .append(handler.getTimeManager().format(TimeManager.FULL_TIME, endCal.getTime()));

                beginCal.add(Calendar.MONTH, -1);
                endCal.add(Calendar.MONTH, -1);

                display.append("~!~").append("BEGIN:VCALENDAR\n" + "BEGIN:VEVENT\n" + "SUMMARY:")
                        .append(barcode.calendarEvent.summary).append("\n")
                        .append("DESCRIPTION:")
                        .append(barcode.calendarEvent.description).append("\n")
                        .append("LOCATION:")
                        .append(barcode.calendarEvent.location).append("\n")
                        .append("DTSTART:")
                        .append(handler.getTimeManager().format(TimeManager.VEVENT, beginCal.getTime())).append("\n")
                        .append("DTEND:")
                        .append(handler.getTimeManager().format(TimeManager.VEVENT, endCal.getTime())).append("\n")
                        .append("END:VEVENT\n").append("END:VCALENDAR");

                break;
            case Barcode.URL:
                display.append(barcode.url.title.isEmpty() ? (barcode.url.url.isEmpty() ? barcode.rawValue : barcode.url.url) : barcode.url.title);
                break;
            case Barcode.PHONE:
                display.append("Call Number: ")
                        .append(barcode.phone.number);
                break;
            case Barcode.EMAIL:
                display.append("Email Address: ")
                        .append(barcode.email.address)
                        .append("\nSubject: ")
                        .append(barcode.email.subject)
                        .append("\nBody: ")
                        .append(barcode.email.body);
                break;
            case Barcode.SMS:
                display.append("SMS Number: ")
                        .append(barcode.sms.phoneNumber)
                        .append("\nMessage: ")
                        .append(barcode.sms.message);
                break;
            case Barcode.TEXT:
                display.append(parseTracking(barcode.rawValue));
                break;
            case Barcode.ISBN:
            case Barcode.PRODUCT:
            default:
                display.append(barcode.rawValue);
                break;
        }
        return display.toString().replaceAll("((\r\n)|(\n)){2,}", "\n").trim();
    }

    private String parseProduct(String upc, boolean isbn, Media media, int attempt){
        media.setType(AuxiliaryApplication.SIMPLE_NOTE.name());
        media.setSummary((isbn ? "ISBN: " : "Barcode: ") + upc);
        media.setStreamingID("n/a");

        MediaMetadata book = null;
        if (isbn){
            CaseInsensitiveMap<String,MediaMetadata> books = handler.getParser().getGoogleAPI().getBook("isbn:" + upc);
            if (books.size() != 1 || !books.containsKey("")){
                book = books.entrySet().iterator().next().getValue();
                media.setTitle(book.get(MediaMetadata.Type.TITLE));
                media.setDetail(book.get(MediaMetadata.Type.DETAIL));
                media.setSummary(media.getSummary() + "\n" + book.get(MediaMetadata.Type.SUMMARY));
                media.setThumbnailString(book.get(MediaMetadata.Type.THUMBNAIL));
                if (!preferences.showPrices()){
                    handler.getParser().getThumbnailAPI().asyncThumbnailURL(media);
                    return ("Title: " + media.getTitle() +
                            "\nAuthor: " + media.getDetail() +
                            "\n\n" + media.getSummary()).trim();
                }
            }
        }

        try {
            String url = "https://api.upcitemdb.com/prod/trial/lookup";
            String[] data = new String[]{
                    "upc", upc
            };

            JSONObject response = getJSON(getRequest(url, data, null));
            if ((response == null || !response.has("items")) && book != null){
                handler.getParser().getThumbnailAPI().asyncThumbnailURL(media);
                return ("Title: " + media.getTitle() +
                        "\nAuthor: " + media.getDetail() +
                        "\n\n" + media.getSummary()).trim();
            } else if (response != null && response.has("items")) {
                JSONArray responseArray = response.getJSONArray("items");
                if (responseArray != null && responseArray.length() > 0) {
                    JSONObject product = responseArray.getJSONObject(0);
                    if (book == null && product.has("title")){
                        media.setTitle(Jsoup.parse(product.getString("title")).text());}
                    if (book == null && product.has("description")){
                        media.setSummary(media.getSummary() + "\n" + Jsoup.parse(product.getString("description")).text());}

                    if (preferences.showPrices()) {
                        if (!(product.has("offers") && product.getJSONArray("offers").length() > 0) && product.has("asin")) {
                            String price = handler.getParser().getURLAPI().getAmazonPrice(product.getString("asin"));
                            if (!price.isEmpty()) {
                                media.setSummary(media.getSummary() + "\n\nAmazon: " + price);
                            }
                        } else if (product.has("offers") && product.getJSONArray("offers").length() > 0) {
                            JSONArray offers = product.getJSONArray("offers");
                            TreeSet<String> prices = new TreeSet<>(), foreign = new TreeSet<>();
                            double lowest = Integer.MAX_VALUE, highest = Integer.MIN_VALUE, average = 0;

                            if (product.has("asin")) {
                                String rawPrice = handler.getParser().getURLAPI().getAmazonPrice(product.getString("asin"));
                                if (!rawPrice.isEmpty()) {
                                    int index = 0;
                                    for (int i = 0; i < rawPrice.length(); i++) {
                                        if (Character.isDigit(rawPrice.charAt(i))) {
                                            index = i;
                                            break;
                                        }
                                    }

                                    double price = Double.parseDouble(rawPrice.substring(index));

                                    if (price != 0) {
                                        if (rawPrice.contains("$")) {
                                            lowest = price;
                                            highest = price;
                                            average = price;
                                            prices.add("Amazon: " + rawPrice);
                                        } else {
                                            foreign.add("Amazon: " + rawPrice);
                                        }
                                    }
                                }
                            }

                            for (int i = 0; i < offers.length(); i++) {
                                JSONObject offer = offers.getJSONObject(i);
                                double price = offer.getDouble("price");
                                if (price != 0 && offer.getString("currency").isEmpty()) {
                                    if (price < lowest) lowest = price;
                                    if (price > highest) highest = price;
                                    average += price;
                                    prices.add(offer.getString("merchant").replaceFirst("\\.com$", "") + ": $" + df.format(price));
                                } else if (price != 0) {
                                    foreign.add(offer.getString("merchant").replaceFirst("\\.com$", "") + ": " + df.format(price) + " " + offer.getString("currency"));
                                }
                            }

                            if (prices.size() > 0) {
                                String summary = prices.size() > 1 ?
                                        "\n\nPrices: $" + df.format(lowest) + " - $" + df.format(highest) + "\nAverage: $" + df.format(average / prices.size()) + "\n\n" : "\n\n";
                                media.setSummary(media.getSummary() + summary +
                                        prices.toString()
                                                .replace("[", "")
                                                .replace("]", "")
                                                .replace(", ", "\n")
                                                .replace("$" + df.format(lowest), "$" + df.format(lowest) + " \uD83D\uDECD️"));
                            }

                            if (foreign.size() > 0) {
                                media.setSummary(media.getSummary() + "\n\nGlobal:\n" +
                                        foreign.toString()
                                                .replace("[", "")
                                                .replace("]", "")
                                                .replace(", ", "\n"));
                            }
                        }
                    }

                    media.setStreamingID(media.getSummary());
                    if (product.has("images") && product.getJSONArray("images").length() > 0) {
                        String ico = product.getJSONArray("images").getString(0);
                        if (ico.startsWith("/") && !ico.startsWith("//")){
                            URI uri = new URI(url);
                            ico = url.substring(0, url.indexOf("://")) + "://" + uri.getAuthority() + ico;
                        } else if (ico.startsWith("//")){
                            ico = url.substring(0, url.indexOf("://")) + ":" + ico;
                        }
                        media.setThumbnailString(ico);
                        handler.getParser().getThumbnailAPI().asyncThumbnailURL(media);
                    } else if (book != null){
                        handler.getParser().getThumbnailAPI().asyncThumbnailURL(media);
                    } else {
                        handler.getParser().getThumbnailAPI().setThumbnailText(media);
                        if (!BaseFragment.addOrUpdate(media)){
                            handler.addOrUpdateMedia(media);
                        }
                    }

                    return ("Title: " + media.getTitle() +
                            (media.getDetail().isEmpty() ? "" : "\nAuthor: " + media.getDetail()) +
                            "\n\n" + media.getSummary()).trim();
                }
            } else {
                if (attempt == -1){ //0
                    return parseProduct(upc, isbn, media, 1);
                } else {
                    media.setTitle("Unknown " +((isbn ? "ISBN: " : "Barcode: ") + upc));
                    media.setSummary("");
                    handler.getParser().getThumbnailAPI().setThumbnailText(media);
                    if (!BaseFragment.addOrUpdate(media)) {
                        handler.addOrUpdateMedia(media);
                    }
                }
            }
        } catch (Exception e) {
            Logger.log(Logger.API, e);
        }

        return upc;
    }


    //Tracking patterns
    private static Pattern UPSpattern1 = Pattern.compile("\\b(1Z ?[0-9A-Z]{3} ?[0-9A-Z]{3} ?[0-9A-Z]{2} ?[0-9A-Z]{4} ?[0-9A-Z]{3} ?[0-9A-Z]|[\\dT]\\d\\d\\d ?\\d\\d\\d\\d ?\\d\\d\\d)\\b");
    private static Pattern FedEXpattern1 = Pattern.compile("(\\b96\\d{20}\\b)|(\\b\\d{15}\\b)|(\\b\\d{12}\\b)");
    private static Pattern FedEXpattern2 = Pattern.compile("\\b((98\\d\\d\\d\\d\\d?\\d\\d\\d\\d|98\\d\\d) ?\\d\\d\\d\\d ?\\d\\d\\d\\d( ?\\d\\d\\d)?)\\b");
    private static Pattern FedEXpattern3 = Pattern.compile("^[0-9]{15}$");
    private static Pattern USPSpattern1 = Pattern.compile("(\\b\\d{30}\\b)|(\\b91\\d+\\b)|(\\b\\d{20}\\b)");
    private static Pattern USPSpattern2 = Pattern.compile("^E\\D{1}\\d{9}\\D{2}$|^9\\d{15,21}$");
    private static Pattern USPSpattern3 = Pattern.compile("^91[0-9]+$");
    private static Pattern USPSpattern4 = Pattern.compile("^[A-Za-z]{2}[0-9]+US$");

    private String parseTracking(String trackingNumber){
        if (trackingNumber.length() < 25 && trackingNumber.matches("^[a-zA-Z0-9 ]*$")) {
            //UPS
            Matcher ups1 = UPSpattern1.matcher(trackingNumber);
            if (ups1.find()) {return "https://wwwapps.ups.com/WebTracking/track?track=yes&trackNums=" + ups1.group();}

            //USPS
            Matcher usps1 = USPSpattern1.matcher(trackingNumber);
            if (usps1.find()){return "https://tools.usps.com/go/TrackConfirmAction?tLabels=" + usps1.group();}
            Matcher usps2 = USPSpattern2.matcher(trackingNumber);
            if (usps2.find()){return "https://tools.usps.com/go/TrackConfirmAction?tLabels=" + usps2.group();}
            Matcher usps3 = USPSpattern3.matcher(trackingNumber);
            if (usps3.find()){return "https://tools.usps.com/go/TrackConfirmAction?tLabels=" + usps3.group();}
            Matcher usps4 = USPSpattern4.matcher(trackingNumber);
            if (usps4.find()){return "https://tools.usps.com/go/TrackConfirmAction?tLabels=" + usps4.group();}

            //FEDEX
            Matcher fedex1 = FedEXpattern1.matcher(trackingNumber);
            if (fedex1.find()){return "https://www.fedex.com/apps/fedextrack/?action=track&trackingnumber=" + fedex1.group();}
            Matcher fedex2 = FedEXpattern2.matcher(trackingNumber);
            if (fedex2.find()){return "https://www.fedex.com/apps/fedextrack/?action=track&trackingnumber=" + fedex2.group();}
            Matcher fedex3 = FedEXpattern3.matcher(trackingNumber);
            if (fedex3.find()){return "https://www.fedex.com/apps/fedextrack/?action=track&trackingnumber=" + fedex3.group();}

        }

        return trackingNumber;
    }

    private String parseLocation(String location, Media media){
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";
        String[] data = new String[]{
                "location", location,
                "radius", "15",
                "key", handler.getParser().getGoogleAPI().getAPIKey()
        };

        try {
            JSONObject response = getJSON(getRequest(url, data, null));
            if (response != null && response.has("results")) {
                JSONArray responseArray = response.getJSONArray("results");
                if (responseArray.length() > 0) {
                    int index = response.length() == 1 ? 0 : 1;
                    String place = responseArray.getJSONObject(index).getString("place_id");
                    url = "https://maps.googleapis.com/maps/api/place/details/json";
                    data = new String[]{
                            "placeid", place,
                            "key", handler.getParser().getGoogleAPI().getAPIKey()
                    };

                    response = getJSON(getRequest(url, data, null));
                    if (response != null && response.has("result")) {
                        response = response.getJSONObject("result");
                        String address = response.getString("formatted_address");
                        String name = response.getString("name");

                        media.setTitle(name);
                        media.setSummary(address);
                        media.setAlternateID(address);
                    }
                }
            }
        } catch (Exception e){
            Logger.log(Logger.API, e);
        }

        media.setStreamingID(location);
        media.setThumbnailString("https://maps.googleapis.com/maps/api/staticmap?"
                + "markers=" + media.getStreamingID()
                + "&size=200x250&scale=2&key=" + handler.getParser().getGoogleAPI().getAPIKey());

        if (media.getTitle().isEmpty()){
            media.setTitle(location);
            media.setSummary("Unknown Address");
            media.setAlternateID(location);
        }

        media.setMetadata(null, true);
        return media.getTitle().equals(media.getAlternateID()) ? media.getAlternateID() :
                "Place: " + media.getTitle() + "\n\n" + media.getAlternateID();
    }

    public void createMedia(final BaseActivity activity, final String barcode, final boolean isbn, final String uid, final String collection) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (handler.getBilling().canAddMedia()) {
                    final Media media;
                    final String title;
                    final StringBuilder display = new StringBuilder();

                    String parsed = parseTracking(barcode);
                    if (!parsed.equalsIgnoreCase(barcode)){ //tracking number
                        title = "Tracking Number";
                        media = new Media(handler, StreamingApplication.URI, uid, "New Tag", collection, uid,
                                false, "", "", new String[]{""},
                                new MediaMetadata()
                                        .set(MediaMetadata.Type.INPUT_TITLE,  parsed)
                                        .set(MediaMetadata.Type.INPUT_OPTION, activity.getString(R.string.uri_1))
                                        .set(MediaMetadata.Type.SUMMARY, parsed)
                                        .set(MediaMetadata.Type.TITLE, barcode), false);
                        display.append(barcode).append("\n\n").append(parsed);
                    } else {
                        title = "Product";
                        media = new Media(handler, StreamingApplication.OTHER, uid, "New Tag", collection, uid,
                                false, "", "", new String[]{""}, null, false);
                        display.append(parseProduct(barcode, isbn, media, event));
                    }

                    media.setOriginal(barcode);
                    handler.addOrUpdateMedia(media);

                    final View view = View.inflate(activity, R.layout.scan_display, null);
                    final TextView textView = view.findViewById(R.id.code_summary);
                    textView.setText(display.toString().trim());

                    if (!media.getThumbnailString().equals("text") && !media.getThumbnailString().isEmpty()){
                        Glide.with(activity).load(media.getThumbnailString()).into(new SimpleTarget<Drawable>() {
                            @Override
                            public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                                ImageView thumbnail = view.findViewById(R.id.thumb);
                                thumbnail.setImageDrawable(resource);
                                thumbnail.setVisibility(View.VISIBLE);
                            }
                        });
                    }

                    new AlertDialog.Builder(activity)
                            .setView(view)
                            .setTitle(title)
                            .setPositiveButton(activity.getString(R.string.submit), null)
                            .setNegativeButton(activity.getString(R.string.reassign), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    DatabaseDialog.editMedia(
                                            activity.getSupportFragmentManager(),
                                            handler, media);
                                }
                            })
                            .setNeutralButton("Search Web", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(Intent.ACTION_WEB_SEARCH );
                                    intent.putExtra(SearchManager.QUERY, barcode);
                                    activity.startActivity(intent);
                                }
                            }).show();
                } else {
                    activity.advertisePremium(null);
                }
            }
        });
    }

    public void createMedia(final BaseActivity activity, final Barcode barcode, final String uid, final String collection){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (handler.getBilling().canAddMedia()) {
                    final Media media;
                    final StringBuilder display = new StringBuilder();
                    final String title = getTitle(barcode);
                    final String lookupSummary;
                    Form form = null;

                    switch (barcode.valueFormat){
                        case Barcode.GEO:
                            media = new Media(handler, StreamingApplication.MAPS, uid, "New Tag", collection, uid,
                                false, "", "", new String[]{""},
                                null, false);
                            media.setExternalID(barcode.rawValue);
                            display.append(parseLocation(barcode.geoPoint.lat + "," + barcode.geoPoint.lng, media));
                            break;
                        case Barcode.PHONE:
                        case Barcode.EMAIL:
                        case Barcode.SMS:
                            lookupSummary = getDisplayData(barcode);
                            AuxiliaryApplication type = null;
                            switch (barcode.valueFormat){
                                case Barcode.PHONE:
                                    type = AuxiliaryApplication.CALL;
                                    break;
                                case Barcode.SMS:
                                    type = AuxiliaryApplication.SMS;
                                    break;
                                case Barcode.EMAIL:
                                    type = AuxiliaryApplication.EMAIL;
                                    break;
                            }

                            media = new Media(handler, StreamingApplication.CONTACT, uid, "New Tag", collection, uid,
                                    false, "", "", new String[]{""},
                                    new MediaMetadata()
                                            .set(MediaMetadata.Type.DETAIL,  barcode.rawValue.toLowerCase())
                                            .set(MediaMetadata.Type.TYPE, type.name())
                                            .set(MediaMetadata.Type.SUMMARY, lookupSummary)
                                            .set(MediaMetadata.Type.STREAMING, barcode.rawValue.toLowerCase()), false);
                            display.append(lookupSummary);
                            break;
                        case Barcode.URL:
                            lookupSummary = getDisplayData(barcode);
                            media = new Media(handler, StreamingApplication.URI, uid, "New Tag", collection, uid,
                                    false, "", "", new String[]{""},
                                    new MediaMetadata()
                                            .set(MediaMetadata.Type.INPUT_TITLE,  barcode.rawValue.toLowerCase())
                                            .set(MediaMetadata.Type.INPUT_OPTION, activity.getString(R.string.uri_1))
                                            .set(MediaMetadata.Type.SUMMARY, lookupSummary)
                                            .set(MediaMetadata.Type.TITLE, barcode.rawValue.toLowerCase()), false);
                            display.append(lookupSummary);
                            break;
                        case Barcode.CALENDAR_EVENT:
                            String[] summary = getDisplayData(barcode).split("~!~");
                            media = new Media(handler, StreamingApplication.CLOCK, uid, "New Tag", collection, uid,
                                    false, "", "", new String[]{""},
                                    new MediaMetadata()
                                        .set(MediaMetadata.Type.INPUT_TITLE, barcode.calendarEvent.summary)
                                        .set(MediaMetadata.Type.TYPE, AuxiliaryApplication.VEVENT.name())
                                        .set(MediaMetadata.Type.INPUT_CUSTOM, summary[1])
                                        .set(MediaMetadata.Type.SUMMARY, summary[0]), false);
                            display.append(summary[0]);
                            break;
                        case Barcode.WIFI:
                            String encryption = "";
                            switch (barcode.wifi.encryptionType){
                                case 1: encryption = activity.getString(R.string.network_1); break;
                                case 2: encryption = activity.getString(R.string.network_2); break;
                                case 3: encryption = activity.getString(R.string.network_3); break;
                            }

                            media = new Media(handler, StreamingApplication.DEVICE, uid, "New Tag", collection, uid,
                                    false, "", "", new String[]{""},
                                    new MediaMetadata()
                                    .set(MediaMetadata.Type.INPUT_TITLE, barcode.wifi.ssid)
                                    .set(MediaMetadata.Type.INPUT_OPTION, encryption)
                                    .set(MediaMetadata.Type.INPUT_CUSTOM, barcode.wifi.ssid  + "~!~" + barcode.wifi.password)
                                    .set(MediaMetadata.Type.TYPE, WIFI_CONNECTION.name()), false);

                            display.append("SSID: ")
                                    .append(barcode.wifi.ssid)
                                    .append("\nEncryption: ")
                                    .append(encryption)
                                    .append("\nPassword: ")
                                    .append(barcode.wifi.password);
                            break;
                        case Barcode.CONTACT_INFO:
                            lookupSummary = getDisplayData(barcode);
                            media = new Media(handler, StreamingApplication.CONTACT, uid, "New Tag", collection, uid,
                                    false, "", "", new String[]{""},
                                    new MediaMetadata()
                                        .set(MediaMetadata.Type.INPUT_TITLE, barcode.displayValue)
                                        .set(MediaMetadata.Type.TYPE, AuxiliaryApplication.VCARD.name())
                                        .set(MediaMetadata.Type.INPUT_CUSTOM, barcode.rawValue)
                                        .set(MediaMetadata.Type.SUMMARY, lookupSummary), false);
                            display.append(lookupSummary);
                            break;
                        case Barcode.ISBN:
                        case Barcode.PRODUCT:
                            media = new Media(handler, StreamingApplication.OTHER, uid, "New Tag", collection, uid,
                                    false, "", "", new String[]{""}, null, false);
                            display.append(parseProduct(barcode.rawValue, barcode.valueFormat == Barcode.ISBN, media, event));
                            break;
                        case Barcode.DRIVER_LICENSE:
							lookupSummary = getDisplayData(barcode);
							media = new Media(handler, StreamingApplication.OTHER, uid, "New Tag", collection, uid,
								false, "", "", new String[]{""},
								new MediaMetadata()
										.set(MediaMetadata.Type.INPUT_TITLE, title)
										.set(MediaMetadata.Type.TYPE, AuxiliaryApplication.SIMPLE_NOTE.name())
										.set(MediaMetadata.Type.SUMMARY, lookupSummary), false);		
							display.append(lookupSummary);
							break;
                        case Barcode.TEXT:
                        default:
                            if (barcode.rawValue.startsWith("BEGIN:VFORM") && barcode.rawValue.endsWith("END:VFORM")){
                                form = new Form(false, activity, handler, title, barcode.rawValue);
                                media = new Media(handler, StreamingApplication.OTHER, uid, "New Tag", collection, uid,
                                        false, "", "", new String[]{""},
                                        new MediaMetadata()
                                                .set(MediaMetadata.Type.TYPE, AuxiliaryApplication.FORM.name())
                                                .set(MediaMetadata.Type.INPUT_TITLE, title)
                                                .set(MediaMetadata.Type.INPUT_CUSTOM, barcode.rawValue), false);
                            } else if (barcode.rawValue.startsWith("BEGIN:VLIST") && barcode.rawValue.endsWith("END:VLIST")){
                                media = new Media(handler, StreamingApplication.OTHER, uid, "New Tag", collection, uid,
                                        false, "", "", new String[]{""},
                                        new MediaMetadata()
                                                .set(MediaMetadata.Type.TYPE, AuxiliaryApplication.LIST.name())
                                                .set(MediaMetadata.Type.INPUT_TITLE, title)
                                                .set(MediaMetadata.Type.INPUT_CUSTOM, barcode.rawValue), false);
                                display.append(ListActivity.getSummary(barcode.rawValue));
                            } else {
                                lookupSummary = getDisplayData(barcode);
                                if (!lookupSummary.equalsIgnoreCase(barcode.rawValue)){ //tracking number
                                    media = new Media(handler, StreamingApplication.URI, uid, "New Tag", collection, uid,
                                            false, "", "", new String[]{""},
                                            new MediaMetadata()
                                                    .set(MediaMetadata.Type.INPUT_TITLE,  lookupSummary)
                                                    .set(MediaMetadata.Type.INPUT_OPTION, activity.getString(R.string.uri_1))
                                                    .set(MediaMetadata.Type.SUMMARY, lookupSummary)
                                                    .set(MediaMetadata.Type.TITLE, barcode.rawValue), false);
                                    display.append(barcode.rawValue).append("\n\n").append(lookupSummary);
                                } else {
                                    Matcher matcher = URIpattern.matcher(barcode.rawValue);
                                    if (matcher.find() && new Intent(Intent.ACTION_VIEW)
                                            .setData(Uri.parse(barcode.rawValue))
                                            .resolveActivity(activity.getPackageManager()) != null) {
                                        media = new Media(handler, StreamingApplication.URI, uid, "New Tag", collection, uid,
                                                false, "", "", new String[]{""},
                                                new MediaMetadata()
                                                        .set(MediaMetadata.Type.INPUT_TITLE, barcode.rawValue.toLowerCase())
                                                        .set(MediaMetadata.Type.INPUT_OPTION, activity.getString(R.string.uri_2))
                                                        .set(MediaMetadata.Type.SUMMARY, lookupSummary)
                                                        .set(MediaMetadata.Type.TITLE, barcode.rawValue.toLowerCase()), false);
                                    } else {
                                        media = new Media(handler, StreamingApplication.OTHER, uid, "New Tag", collection, uid,
                                                false, "", "", new String[]{""},
                                                new MediaMetadata()
                                                        .set(MediaMetadata.Type.INPUT_TITLE, barcode.displayValue.equals(lookupSummary) ? title : barcode.displayValue)
                                                        .set(MediaMetadata.Type.TYPE, AuxiliaryApplication.SIMPLE_NOTE.name())
                                                        .set(MediaMetadata.Type.SUMMARY, lookupSummary), false);
                                    }
                                    if (!barcode.displayValue.equals(lookupSummary)) {
                                        display.append(barcode.displayValue)
                                                .append("\n\n");
                                    }
                                    display.append(lookupSummary);
                                }
                            }
                            break;
                    }

                    media.setOriginal(barcode.rawValue);
                    handler.addOrUpdateMedia(media);

                    final View view = form == null ? View.inflate(activity, R.layout.scan_display, null) : form.getDialog();
                    final TextView textView = form == null ? (TextView) view.findViewById(R.id.code_summary) : null;
                    if (textView != null) {
                        textView.setText(display.toString().trim());
                    }

                    if (!media.getThumbnailString().equals("text") && !media.getThumbnailString().isEmpty()){
                        Glide.with(activity).load(media.getThumbnailString()).into(new SimpleTarget<Drawable>() {
                            @Override
                            public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                                ImageView thumbnail = view.findViewById(R.id.thumb);
                                thumbnail.setImageDrawable(resource);
                                thumbnail.setVisibility(View.VISIBLE);
                            }
                        });
                    }

                    new AlertDialog.Builder(activity)
                            .setView(view)
                            .setTitle(title)
                            .setPositiveButton(activity.getString(R.string.submit), null)
                            .setNegativeButton(activity.getString(R.string.reassign), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    DatabaseDialog.editMedia(
                                            activity.getSupportFragmentManager(),
                                            handler, media);
                                }
                            })
                            .setNeutralButton(getButton(barcode), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (barcode.valueFormat == Barcode.ISBN || barcode.valueFormat == Barcode.PRODUCT){
                                        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH );
                                        intent.putExtra(SearchManager.QUERY, barcode.rawValue);
                                        activity.startActivity(intent);
                                    } else {
                                        ScanActivity.scan(uid, activity, handler);
                                    }
                                }
                        }).show();
                } else {
                    activity.advertisePremium(null);
                }
            }
        });
    }
}
