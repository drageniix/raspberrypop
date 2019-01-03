package com.drageniix.raspberrypop.activities;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.fragments.BaseFragment;
import com.drageniix.raspberrypop.media.Media;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ListActivity extends BaseActivity {
    public static float x, y, listRadius;
    private Media media;
    private RecyclerView checkList;
    private EditText titleText;
    private CardView card;
    private Point offset;
    boolean fullscreen = true, checkboxes;
    private FloatingActionButton fab;
    private Toolbar toolbar;
    private ListAdapter adapter;
    private View base;
    private int listWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        media = handler.readMedia(getIntent().getStringExtra(ScanActivity.UID));
        if (media == null){ finish(); return; }

        super.setContentView(R.layout.floating_list);
        DisplayMetrics metrics = getResources().getDisplayMetrics();

        titleText = findViewById(R.id.miniNoteTitle);
        checkList = findViewById(R.id.miniListText);
        card = findViewById(R.id.miniNoteCard);
        listRadius  = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, metrics);
        listWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 250, metrics);
        toolbar = findViewById(R.id.noteToolbar);
        base = findViewById(R.id.base);

        checkboxes = handler.getPreferences().showChecklist();

        titleText.setText(!media.getTitle().isEmpty() ? media.getTitle() : media.getFigureName() + "'s List");

        adapter = new ListAdapter();
        RecyclerView.LayoutManager lm = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        ItemTouchHelper.Callback callback = new ListCallback();
        ItemTouchHelper drag = new ItemTouchHelper(callback);
        drag.attachToRecyclerView(checkList);
        checkList.setHasFixedSize(false);
        checkList.setLayoutManager(lm);
        checkList.setAdapter(adapter);

       fab = findViewById(R.id.fabList);
       fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.add(-1);
            }
        });

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setHomeButtonEnabled(false);
            getSupportActionBar().setTitle("");
        }

        base.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent event) {
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_EXITED:
                        if (!event.getResult()) {
                            card.setVisibility(View.VISIBLE);
                            checkList.requestFocus();
                        }
                        break;
                    case DragEvent.ACTION_DROP:
                        card.setVisibility(View.VISIBLE);
                        checkList.requestFocus();
                        if (!event.getResult()) {
                            card.setX(x = event.getX() - offset.x);
                            card.setY(y = event.getY() - offset.y);
                        }
                        return false;
                }
                return true;
            }
        });

        setLayout();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setLayout(){
        fullscreen = !fullscreen;
        ViewGroup.LayoutParams params = card.getLayoutParams();
        if (!fullscreen){
            base.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    exit();
                }
            });
            fab.setVisibility(View.GONE);
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.width = listWidth;
            card.setX(x);
            card.setY(y);
            card.setRadius(listRadius);
            toolbar.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        ClipData data = ClipData.newPlainText("", "");
                        offset = new Point((int) event.getX(), (int) event.getY());
                        View.DragShadowBuilder shadowBuilder = new ListDragShadow(card, offset);
                        card.startDrag(data, shadowBuilder, view, 0);
                        card.setVisibility(View.INVISIBLE);
                        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(card.getWindowToken(), 0);
                        return true;
                    } else return false;
                }
            });
        } else {
            base.setOnClickListener(null);
            fab.setVisibility(View.VISIBLE);
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            card.setX(0);
            card.setY(0);
            card.setRadius(0);
            toolbar.setOnTouchListener(null);
        }
        card.setLayoutParams(params);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.fullscreen:
                setLayout();
                break;
            case R.id.exit:
                exit();
                break;
            case R.id.checklist:
                checkboxes = !checkboxes;
                handler.getPreferences().setChecklist(checkboxes);
                adapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.list_menu, menu);
        return true;
    }

    private void exit(){
        if (!exited && media != null) {
            setTheme(handler.getPreferences().getTheme());

            adapter.saveItems();
            if (!BaseFragment.addOrUpdate(media)) {
                handler.addOrUpdateMedia(media);
            }

            if (!isFinishing()) finish();
            overridePendingTransition(0, 0);
            exited = true;
        }
    }

    @Override
    protected void onUserLeaveHint() {
        exit();
        super.onUserLeaveHint();
    }

    @Override
    public void onDetachedFromWindow() {
        exit();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onPause() {
        exit();
        super.onPause();
    }

    private boolean exited;

    class ListDragShadow extends View.DragShadowBuilder {
        private Point _offset;

        ListDragShadow(View view, Point offset) {
            super(view);
            _offset = offset;
        }

        @Override
        public void onProvideShadowMetrics (Point size, Point touch) {
            size.set(getView().getWidth(), getView().getHeight());
            touch.set(_offset.x, _offset.y);
        }
    }

    class Item {
        boolean selected;
        String item;

        Item(String item, boolean selected){
            this.item = item;
            this.selected = selected;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public String getItem() {
            return item;
        }

        public void setItem(String item) {
            this.item = item.trim();
        }
    }

    private static final String checked = "X ", unchecked = "O ";
    public static String getSummary(String raw){
        return raw
                .replaceFirst("\nTITLE:(.*)\n", "\n")
                .replace("BEGIN:VLIST", "")
                .replace("\nEND:VLIST", "")
                .replace("\n" + ListActivity.checked, "\n• ")
                .replace("\n" + ListActivity.unchecked, "\n• ")
                .trim();
    }

    class ListAdapter extends RecyclerView.Adapter<ViewHolder> {
        List<Item> items;

        ListAdapter(){
            items = new LinkedList<>();
            String[] data = media.getStreamingID().split("\n");
            for (String datum : data){
                String text = datum.substring(datum.indexOf(" ") + 1);
                if (datum.startsWith(checked)){
                    items.add(new Item(text, true));
                } else if (datum.startsWith(unchecked)){
                    items.add(new Item(text, false));
                }
            }

            if (items.size() == 0){
                items.add(new Item("", false));
            }
        }

        void saveItems(){
            StringBuilder itemString = new StringBuilder("BEGIN:VLIST");

            String title = titleText.getText().toString().trim();
            if (!title.equals(media.getFigureName() + "'s List")) {
                media.setTitle(title);
                itemString.append("\nTITLE:").append(title);
            }

            for (Item item : items){
                String toString = item.getItem();
                if (!toString.isEmpty()) {
                    itemString.append("\n").append(item.isSelected() ? checked : unchecked).append(toString);
                }
            }

            media.setStreamingID(itemString.toString() + "\nEND:VLIST");
            media.setSummary(getSummary(media.getStreamingID()));
        }

        void add(int index){
            if (index == -1) index = items.size();
            items.add(index, new Item("", false));
            notifyItemInserted(index);
        }

        void remove(int index){
            items.remove(index);
            notifyItemRemoved(index);
            if (items.isEmpty()) add(-1);
        }

        void onItemMove(int start, int end){
            Collections.swap(items, start, end);
            notifyItemMoved(start, end);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.floating_list_item, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
            final Item item = items.get(position);

            if (holder.watcher != null){holder.text.removeTextChangedListener(holder.watcher);}
            holder.text.setText(item.getItem());
            holder.text.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_NEXT
                            || actionId == EditorInfo.IME_ACTION_DONE
                            || (event != null && event.getAction() == KeyEvent.ACTION_DOWN && (
                                (actionId == EditorInfo.IME_NULL || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)))) {
                        add(holder.getAdapterPosition() + 1);
                        return true;
                    }
                    return false;
                }
            });
            holder.text.addTextChangedListener(holder.watcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String text = s.toString();
                    if (text.contains("\n")){
                        String[] lines = text.split("\n");
                        item.setItem(lines[0]);
                        int counter = 1;
                        for (int i = 1; i < lines.length; i++){
                            if (!lines[i].isEmpty()){
                                items.add(holder.getAdapterPosition() + counter, new Item(lines[i], false));
                                notifyItemInserted(holder.getAdapterPosition() + counter);
                                counter++;
                            }
                        }
                    } else {
                        item.setItem(text);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });

            if (!item.isSelected() || !checkboxes){
                holder.text.setPaintFlags(holder.text.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            } else if (item.isSelected()){
                holder.text.setPaintFlags(holder.text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }

            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(item.isSelected());
            if (checkboxes) {
                holder.bullet.setVisibility(View.GONE);
                holder.checkBox.setVisibility(View.VISIBLE);
                holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        item.setSelected(isChecked);
                        if (isChecked) {
                            holder.text.setPaintFlags(holder.text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                        } else {
                            holder.text.setPaintFlags(holder.text.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                        }
                    }
                });
            } else {
                holder.bullet.setVisibility(View.VISIBLE);
                holder.checkBox.setVisibility(View.GONE);
            }

            holder.subtract.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    remove(holder.getAdapterPosition());
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        EditText text;
        TextView bullet;
        ImageButton subtract;
        TextWatcher watcher;

        ViewHolder(View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.check);
            text = itemView.findViewById(R.id.text);
            subtract = itemView.findViewById(R.id.operation);
            bullet = itemView.findViewById(R.id.point);
            text.requestFocus();
        }
    }

    class ListCallback extends ItemTouchHelper.Callback {
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