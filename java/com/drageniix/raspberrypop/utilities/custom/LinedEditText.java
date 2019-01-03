package com.drageniix.raspberrypop.utilities.custom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.drageniix.raspberrypop.R;

public class LinedEditText extends android.support.v7.widget.AppCompatEditText {

    // the vertical offset scaling factor (10% of the height of the text)
    private static final float VERTICAL_OFFSET_SCALING_FACTOR = 0.09f;

    // the dashed line scale factors
    private static final float DASHED_LINE_ON_SCALE_FACTOR = 0.008f;
    private static final float DASHED_LINE_OFF_SCALE_FACTOR = 0.0125f;

    // the paint we will use to draw the lines
    private Paint dashedLinePaint;

    // a reusable rect object
    private Rect reuseableRect;

    public LinedEditText(Context context) {
        super(context);
        init();
    }

    public LinedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LinedEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setBackgroundColor(getContext().getResources().getColor(R.color.note));
        setTextColor(Color.BLACK);
        setTextSize(14);

        setLineSpacing(
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6.0f,
                getResources().getDisplayMetrics()), 1.0f);

        // instantiate the rect
        reuseableRect = new Rect();

        // instantiate the paint
        dashedLinePaint = new Paint();
        dashedLinePaint.setARGB(200, 0, 0, 0);
        dashedLinePaint.setStyle(Paint.Style.STROKE);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {

        // set the path effect based on the view width
        dashedLinePaint.setPathEffect(
                new DashPathEffect(
                        new float[]{
                                getWidth() * DASHED_LINE_ON_SCALE_FACTOR,
                                getWidth() * DASHED_LINE_OFF_SCALE_FACTOR},
                        0));

        // get the height of the view
        int height = getHeight();

        // set the vertical offset basef off of the view width
        int verticalOffset = (int) (getLineHeight() * VERTICAL_OFFSET_SCALING_FACTOR);

        // the number of lines equals the height divided by the line height
        int numberOfLines = height / getLineHeight();
        if (getLineCount() > numberOfLines) { // set the number of lines to the line count
            numberOfLines = getLineCount();
        }

        // get the baseline for the first line
        int baseline = getLineBounds(0, reuseableRect);

        // for each line
        for (int i = 0; i < numberOfLines; i++) {
            // draw the line
            canvas.drawLine(
                    reuseableRect.left,                   // left
                    baseline + verticalOffset,      // top
                    reuseableRect.right,                  // right
                    baseline + verticalOffset,      // bottom
                    dashedLinePaint);

            // get the baseline for the next line
            baseline += getLineHeight();
        }

        super.onDraw(canvas);
    }
}
