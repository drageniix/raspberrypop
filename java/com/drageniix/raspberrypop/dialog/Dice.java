package com.drageniix.raspberrypop.dialog;

import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Dice implements Comparable<Dice>{
    private static Random random = new Random();
    private static int[] standardSizes = new int[]{4, 6, 8, 10, 12, 20};
    private String display;
    private int roll, die;

    public Dice(int die, boolean rollDie){
        this.die = die != -1 ? die : standardSizes[random.nextInt(standardSizes.length)];
        if (rollDie) {
            roll();
        }
    }

    public void roll(){
        this.roll = random.nextInt(this.die) + 1;
        if (this.die == 2){
            this.display = this.roll == 1 ? "Heads" : "Tails";
        } else {
            this.display = this.roll + "/" + this.die;
        }
    }

    public String getDisplay() {
        return display;
    }
    public int getRoll() {return roll;}
    private int getDie() {return die;}

    @Override
    public String toString() {
        return display;
    }

    @Override
    public int compareTo(@NonNull Dice o) {
        return Integer.compare(this.roll, o.roll);
    }

    static class DieDialog {
        BaseActivity activity;
        Button options;
        Dice.DiceAdapter adapter;
        EditText custom;
        AutoCompleteTextView title;

        DieDialog(BaseActivity activity, EditText custom, AutoCompleteTextView title){
            this.activity = activity;
            this.custom = custom;
            this.title = title;
        }

        void setResults(){
            String results = adapter.summarize();
            custom.setText(String.valueOf(results + options.getTag()));
            title.setText(String.valueOf("Roll Dice: " + results));
        }

        View createDialog(String currentValue){
            final View diceView = View.inflate(activity, R.layout.media_dice, null);
            adapter = new Dice.DiceAdapter(currentValue, activity);
            final GridView view = diceView.findViewById(R.id.grid);
            view.setAdapter(adapter);

            options = diceView.findViewById(R.id.dice_options);
            final int[] values = new int[4];
            Pattern pattern = Pattern.compile(
                        "H([0-9]+)" +
                                "L([0-9]+)" +
                                "R([0-9]+)" +
                                "M([-+]?[0-9]+)");

            Matcher matcher = pattern.matcher(currentValue);
            if (matcher.find()) {
                values[0] = Integer.parseInt(matcher.group(1));
                values[1] = Integer.parseInt(matcher.group(2));
                values[2] = Integer.parseInt(matcher.group(3));
                values[3] = Integer.parseInt(matcher.group(4));
            }

            options.setTag("H" + values[0] + "L" + values[1] + "R" + values[2] + "M" + values[3]);

            options.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    View optionsView = View.inflate(activity, R.layout.media_dice_options, null);
                    final EditText highest = optionsView.findViewById(R.id.highest);
                    final EditText lowest = optionsView.findViewById(R.id.lowest);
                    final EditText reRoll = optionsView.findViewById(R.id.reroll);
                    final EditText modifier = optionsView.findViewById(R.id.modifier);

                    highest.setText(String.valueOf(values[0]));
                    lowest.setText(String.valueOf(values[1]));
                    reRoll.setText(String.valueOf(values[2]));
                    modifier.setText(String.valueOf(values[3]));

                    new AlertDialog.Builder(activity)
                            .setView(optionsView)
                            .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    values[0] = highest.getText().toString().isEmpty() ? 0 : Integer.parseInt(highest.getText().toString());
                                    values[1] = lowest.getText().toString().isEmpty() ? 0 : Integer.parseInt(lowest.getText().toString());
                                    values[2] = reRoll.getText().toString().isEmpty() ? 0 : Integer.parseInt(reRoll.getText().toString());
                                    values[3] = modifier.getText().toString().isEmpty() ? 0 : Integer.parseInt(modifier.getText().toString());
                                    options.setTag("H" + values[0] + "L" + values[1] + "R" + values[2] + "M" + values[3]);
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .create()
                            .show();
                }
            });

            final Button add = diceView.findViewById(R.id.add_die);
            add.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final NumberPicker numberPicker = new NumberPicker(activity);
                    numberPicker.setMinValue(2);
                    numberPicker.setMaxValue(20);
                    numberPicker.setValue(6);
                    numberPicker.setWrapSelectorWheel(true);

                    final FrameLayout layout = new FrameLayout(activity);
                    layout.addView(numberPicker, new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            Gravity.CENTER));

                    new AlertDialog.Builder(activity)
                            .setView(layout)
                            .setTitle("Number of Sides")
                            .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    adapter.add(new Dice(numberPicker.getValue(), false));
                                    view.requestLayout();
                                    view.invalidate();
                                    diceView.requestLayout();
                                    diceView.invalidate();
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .create()
                            .show();
                }
            });

            return diceView;
        }
    }

    static class DiceAdapter extends BaseAdapter {
        List<Dice> diceList;
        BaseActivity activity;
        SparseIntArray count;
        RequestOptions options;

        DiceAdapter(String currentValue, BaseActivity activity){
            this.diceList = new LinkedList<>();
            this.count = new SparseIntArray();
            this.activity = activity;
            this.options = new RequestOptions().fitCenter();

            Pattern patternDie = Pattern.compile("([0-9]+)d([0-9]+)");
            Matcher matcherDie = patternDie.matcher(currentValue);
            while(matcherDie.find()) {
                int amount = Integer.parseInt(matcherDie.group(1));
                int die = Integer.parseInt(matcherDie.group(2));
                for (int i = 0; i < amount; i++) {
                    int index = count.indexOfKey(die);
                    int value = index >= 0 ? count.valueAt(index) + 1 : 1;
                    count.put(die, value);

                    Dice rolledDie = new Dice(die, false);
                    diceList.add(rolledDie);
                }
            }
        }

        void add(Dice dice){
            int index = count.indexOfKey(dice.getDie());
            int value = index >= 0 ? count.valueAt(index) + 1 : 1;
            count.put(dice.getDie(), value);
            diceList.add(dice);
            notifyDataSetChanged();
        }

        void remove(Dice dice){
            if (diceList.remove(dice)){
                int value = count.get(dice.getDie()) - 1;
                if (value > 0) {
                    count.put(dice.getDie(), value);
                } else {
                    count.delete(dice.getDie());
                }
            }
            notifyDataSetChanged();
        }

        String summarize(){
            StringBuilder result = new StringBuilder();
            for(int i = 0; i < count.size(); i++) {
                int key = count.keyAt(i);
                int value = count.valueAt(i);
                result.append(value).append("d").append(key).append(" ");
            }

            return result.toString();
        }

        @Override
        public int getCount() {
            return diceList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final View view = View.inflate(activity, R.layout.media_die,null);
            final Dice die = diceList.get(position);
            Glide.with(activity)
                .load(Uri.parse("file:///android_asset/d" + die.getDie()) + ".png")
                .apply(options)
                .into(new SimpleTarget<Drawable>() {
                    @Override
                    public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                        ImageView imageView = view.findViewById(R.id.die_asset);
                        imageView.setVisibility(View.VISIBLE);
                        imageView.setColorFilter(activity.getAttributeColor(R.attr.colorAccent));
                        imageView.setImageDrawable(resource);
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        TextView textView = view.findViewById(R.id.die_text);
                        textView.setVisibility(View.VISIBLE);
                        textView.setText(String.valueOf(die.getDie()));
                    }
                });

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    remove(diceList.get(position));
                }
            });

            return view;
        }
    }
}
