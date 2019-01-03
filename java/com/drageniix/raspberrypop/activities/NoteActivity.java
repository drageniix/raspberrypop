package com.drageniix.raspberrypop.activities;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.fragments.BaseFragment;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.utilities.custom.LinedEditText;

public class NoteActivity extends BaseActivity {
    public static float x, y, noteRadius;
    private Media media;
    private LinedEditText noteText;
    private EditText titleText;
    private CardView card;
    private Point offset;
    boolean fullscreen = true;
    private Toolbar toolbar;
    private int noteWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        media = handler.readMedia(getIntent().getStringExtra(ScanActivity.UID));
        if (media == null){ finish(); return; }

        super.setContentView(R.layout.floating_note);
        DisplayMetrics metrics = getResources().getDisplayMetrics();

        titleText = findViewById(R.id.miniNoteTitle);
        noteText = findViewById(R.id.miniNoteText);
        card = findViewById(R.id.miniNoteCard);
        noteRadius  = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, metrics);
        noteWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 250, metrics);
        toolbar = findViewById(R.id.noteToolbar);
        View base = findViewById(R.id.base);

        titleText.setText(!media.getTitle().isEmpty() ? media.getTitle() : media.getFigureName() + "'s Note");

        if (!media.getSummary().isEmpty()){
            noteText.setText(media.getSummary());
            noteText.setSelection(media.getSummary().length());
        }
        noteText.requestFocus();

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setHomeButtonEnabled(false);
            getSupportActionBar().setTitle("");
        }

        base.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exit();
            }
        });
        base.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent event) {
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_EXITED:
                        if (!event.getResult()) {
                            card.setVisibility(View.VISIBLE);
                            noteText.requestFocus();
                        }
                        break;
                    case DragEvent.ACTION_DROP:
                        card.setVisibility(View.VISIBLE);
                        noteText.requestFocus();
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
            noteText.setMinLines(3);
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.width = noteWidth;
            card.setX(x);
            card.setY(y);
            card.setRadius(noteRadius);
            toolbar.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        ClipData data = ClipData.newPlainText("", "");
                        offset = new Point((int) event.getX(), (int) event.getY());
                        View.DragShadowBuilder shadowBuilder = new NoteDragShadow(card, offset);
                        card.startDrag(data, shadowBuilder, view, 0);
                        card.setVisibility(View.INVISIBLE);
                        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(card.getWindowToken(), 0);
                        return true;
                    } else return false;
                }
            });
        } else {
            noteText.setMinLines(100);
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
            case R.id.increaseFont:
                noteText.setTextSize(TypedValue.COMPLEX_UNIT_PX, noteText.getTextSize() * 1.1f);
                break;
            case R.id.decreaseFont:
                noteText.setTextSize(TypedValue.COMPLEX_UNIT_PX, noteText.getTextSize() * 0.9f);
                break;
            case R.id.fullscreen:
                setLayout();
                break;
            case R.id.exit:
                exit();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.note_menu, menu);
        return true;
    }

    private void exit(){
        if (!exited && media != null) {
            String title = titleText.getText().toString().trim();
            if (!title.equals(media.getFigureName() + "'s Note")) {
                media.setTitle(title);
            }

            media.setSummary(noteText.getText().toString().trim());
            if(!media.getSummary().isEmpty()){
                media.setStreamingID(media.getSummary());
            }

            handler.getParser().getThumbnailAPI().setThumbnailText(media);
            setTheme(handler.getPreferences().getTheme());

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

    class NoteDragShadow extends View.DragShadowBuilder {

        private Point _offset;

        NoteDragShadow(View view, Point offset) {
            super(view);
            _offset = offset;
        }

        @Override
        public void onProvideShadowMetrics (Point size, Point touch) {
            size.set(getView().getWidth(), getView().getHeight());
            touch.set(_offset.x, _offset.y);
        }

    }
}