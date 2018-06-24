package net.carlh.toast;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class Graph extends View {
    private Paint textPaint;
    private Paint gridPaint;
    private Paint axesPaint;
    private Paint dataPaint[];
    private int width;
    private int height;

    private int rangeMin[];
    private int rangeMax[];
    private int hDivisions = 4;
    private int vDivisions = 4;
    private String[] xLabels;
    private long timeRange;

    public static class Point {
        long x;
        float y;

        public Point(long x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private List<List<Point>> data;

    private final int textSizeDivisor = 32;
    private final int margin = 96;
    private final int fudge = 16;

    public Graph(Context context, AttributeSet attributes) {
        super(context, attributes);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xffffffff);

        axesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axesPaint.setColor(0xffffffff);
        axesPaint.setStrokeWidth(2);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(0xffffffff);

        dataPaint = new Paint[Datum.TYPE_COUNT];

        dataPaint[Datum.TYPE_TEMPERATURE] = new Paint(Paint.ANTI_ALIAS_FLAG);
        dataPaint[Datum.TYPE_TEMPERATURE].setColor(0xffff0000);
        dataPaint[Datum.TYPE_TEMPERATURE].setStrokeWidth(4);
        dataPaint[Datum.TYPE_TEMPERATURE].setStyle(Paint.Style.STROKE);

        dataPaint[Datum.TYPE_HUMIDITY] = new Paint(Paint.ANTI_ALIAS_FLAG);
        dataPaint[Datum.TYPE_HUMIDITY].setColor(0xffffff00);
        dataPaint[Datum.TYPE_HUMIDITY].setStrokeWidth(4);
        dataPaint[Datum.TYPE_HUMIDITY].setStyle(Paint.Style.STROKE);

        rangeMin = new int[Datum.TYPE_COUNT];
        rangeMax = new int[Datum.TYPE_COUNT];
        rangeMin[Datum.TYPE_TEMPERATURE] = 5;
        rangeMax[Datum.TYPE_TEMPERATURE] = 25;
        rangeMin[Datum.TYPE_HUMIDITY] = 0;
        rangeMax[Datum.TYPE_HUMIDITY] = 100;

        xLabels = new String[hDivisions + 1];
        xLabels[0] = "Midnight";
        xLabels[1] = "6am";
        xLabels[2] = "Noon";
        xLabels[3] = "6pm";
        xLabels[4] = "Midnight";

        data = new ArrayList<>();
        for (int i = 0; i < Datum.TYPE_COUNT; ++i) {
            data.add(new ArrayList<Point>());
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        width = w;
        height = h;
        textPaint.setTextSize(height / textSizeDivisor);
    }

    private float xPerUnit() {
        if (timeRange == 0) {
            return 0;
        } else {
            return (width - margin * 2.0f) / timeRange;
        }
    }

    private float yPerUnit(int type) {
        return (height - margin * 2.0f) / (rangeMax[type] - rangeMin[type]);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        /* Axes */
        canvas.drawLine(margin, margin, margin, height - margin, axesPaint);
        canvas.drawLine(margin, height - margin, width - margin, height - margin, axesPaint);

        /* Vertical grid lines */
        for (int i = 0; i <= hDivisions; ++i) {
            int pos = margin + i * (width - 2 * margin) / hDivisions;
            canvas.drawLine(pos, margin, pos, height - margin, gridPaint);
            Rect bounds = new Rect();
            textPaint.getTextBounds(xLabels[i], 0, xLabels[i].length(), bounds);
            canvas.drawText(xLabels[i], pos - bounds.width() / 2, height - fudge, textPaint);
        }

        /* Horizontal grid lines */
        for (int i = 0; i <= vDivisions; ++i) {
            int pos = margin + i * (height - 2 * margin) / vDivisions;
            canvas.drawLine(margin, pos, width - margin, pos, gridPaint);
            for (int j = 0; j < Datum.TYPE_COUNT; ++j) {
                int label = rangeMin[j] + ((rangeMax[j] - rangeMin[j]) / vDivisions) * (vDivisions - i);
                switch (j) {
                    case Datum.TYPE_TEMPERATURE:
                        textPaint.setTextAlign(Paint.Align.RIGHT);
                        canvas.drawText(Integer.toString(label), margin - fudge, pos + (height / (textSizeDivisor * 2)), textPaint);
                        break;
                    case Datum.TYPE_HUMIDITY:
                        textPaint.setTextAlign(Paint.Align.LEFT);
                        canvas.drawText(Integer.toString(label), width - margin + fudge, pos + (height / (textSizeDivisor * 2)), textPaint);
                        break;
                }
            }
        }

        for (int i = 0; i < Datum.TYPE_COUNT; ++i) {
            if (data.get(i) != null) {
                Path path = new Path();
                boolean first = true;
                for (Point j : data.get(i)) {
                    if (first) {
                        path.moveTo(j.x, j.y - rangeMin[i]);
                        first = false;
                    } else {
                        path.lineTo(j.x, j.y - rangeMin[i]);
                    }
                }
                Matrix sm = new Matrix();
                sm.setScale(xPerUnit(), -yPerUnit(i));
                path.transform(sm);
                path.offset(margin, height - margin);
                canvas.drawPath(path, dataPaint[i]);
            }
        }
    }

    public void setData(int type, ArrayList<Point> d) {
        data.set(type, d);
        invalidate();
    }

    /** @param range Time range in milliseconds */
    public void setTimeRange(long range) {
        timeRange = range;
    }
}
