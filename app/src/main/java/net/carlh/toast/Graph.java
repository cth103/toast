package net.carlh.toast;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.provider.ContactsContract;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Graph extends View {
    private Paint xLabelPaint;
    private Paint yLabelPaint[] = new Paint[DataType.COUNT];
    private Paint lineLabelPaint[] = new Paint[DataType.COUNT];
    private Paint gridPaint;
    private Paint axesPaint;
    private Paint labelRectStroke;
    private Paint labelRectFill;
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

    public static class Plot {
        String zone;
        int type;
        List<Point> data;
        Paint linePaint;
        Paint labelPaint;

        Plot(String zone, int type, List<Point> data, int color) {
            this.zone = zone;
            this.type = type;
            this.data = data;
            linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            linePaint.setColor(color);
            linePaint.setStrokeWidth(4);
            linePaint.setStyle(Paint.Style.STROKE);
            labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            labelPaint.setColor(color);
        }
    }

    private List<Plot> plots = new ArrayList<>();

    private final int textSizeDivisor = 32;
    private final int margin = 96;
    private final int fudge = 16;

    private final int[][] color = {
            // DataType.TEMPERATURE
            {
                    0xffff0000,
                    0xff990000,
                    0xffb20000,
                    0xffe50000,
                    0xffff4c4c,
                    0xfeff9999
            },
            // DataType.HUMIDITY
            {
                    0xffffff00,
                    0xff666600,
                    0xff7f7f00,
                    0xffcccc00,
                    0xffffff33,
                    0xffffffe5,
            }
    };

    private int[] nextColor = { 0, 0 };

    public Graph(Context context, AttributeSet attributes) {
        super(context, attributes);
        xLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        xLabelPaint.setColor(0xffffffff);

        yLabelPaint[DataType.TEMPERATURE] = new Paint(Paint.ANTI_ALIAS_FLAG);
        yLabelPaint[DataType.TEMPERATURE].setColor(0xffff0000);
        yLabelPaint[DataType.TEMPERATURE].setTextAlign(Paint.Align.RIGHT);

        yLabelPaint[DataType.HUMIDITY] = new Paint(Paint.ANTI_ALIAS_FLAG);
        yLabelPaint[DataType.HUMIDITY].setColor(0xffffff00);
        yLabelPaint[DataType.HUMIDITY].setTextAlign(Paint.Align.LEFT);

        lineLabelPaint[DataType.TEMPERATURE] = new Paint(Paint.ANTI_ALIAS_FLAG);
        lineLabelPaint[DataType.TEMPERATURE].setColor(0xffff0000);

        lineLabelPaint[DataType.HUMIDITY] = new Paint(Paint.ANTI_ALIAS_FLAG);
        lineLabelPaint[DataType.HUMIDITY].setColor(0xffffff00);

        axesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axesPaint.setColor(0xffffffff);
        axesPaint.setStrokeWidth(2);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(0xffffffff);

        labelRectStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelRectStroke.setColor(0xffffffff);
        labelRectStroke.setStyle(Paint.Style.STROKE);

        labelRectFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelRectFill.setColor(0xaa222222);
        labelRectFill.setStyle(Paint.Style.FILL);

        rangeMin = new int[2];
        rangeMax = new int[2];
        rangeMin[DataType.TEMPERATURE] = 5;
        rangeMax[DataType.TEMPERATURE] = 25;
        rangeMin[DataType.HUMIDITY] = 0;
        rangeMax[DataType.HUMIDITY] = 100;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        width = w;
        height = h;
        xLabelPaint.setTextSize(height / textSizeDivisor);
        for (int i = 0; i < DataType.COUNT; ++i) {
            yLabelPaint[i].setTextSize(height / textSizeDivisor);
            lineLabelPaint[i].setTextSize(height / textSizeDivisor);
        }
        for (Plot p: plots) {
            p.labelPaint.setTextSize(height / textSizeDivisor);
        }
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
            if (xLabels != null) {
                xLabelPaint.getTextBounds(xLabels[i], 0, xLabels[i].length(), bounds);
                canvas.drawText(xLabels[i], pos - bounds.width() / 2, height - fudge, xLabelPaint);
            }
        }

        /* Horizontal grid lines */
        for (int i = 0; i <= vDivisions; ++i) {
            int pos = margin + i * (height - 2 * margin) / vDivisions;
            canvas.drawLine(margin, pos, width - margin, pos, gridPaint);
            for (int j = 0; j < DataType.COUNT; ++j) {
                int label = rangeMin[j] + ((rangeMax[j] - rangeMin[j]) / vDivisions) * (vDivisions - i);
                switch (j) {
                    case DataType.TEMPERATURE:
                        canvas.drawText(Integer.toString(label), margin - fudge, pos + (height / (textSizeDivisor * 2)), yLabelPaint[j]);
                        break;
                    case DataType.HUMIDITY:
                        canvas.drawText(Integer.toString(label), width - margin + fudge, pos + (height / (textSizeDivisor * 2)), yLabelPaint[j]);
                        break;
                }
            }
        }

        for (Plot p: plots) {
            Path path = new Path();
            boolean first = true;
            for (Point j: p.data) {
                if (first) {
                    path.moveTo(j.x, j.y - rangeMin[p.type]);
                    first = false;
                } else {
                    path.lineTo(j.x, j.y - rangeMin[p.type]);
                }
            }
            Matrix sm = new Matrix();
            sm.setScale(xPerUnit(), -yPerUnit(p.type));
            path.transform(sm);
            path.offset(margin, height - margin);
            canvas.drawPath(path, p.linePaint);
        }

        /* Calculate the width and line height of the labels list */
        Rect labels = new Rect();
        int lineHeight = 0;
        for (Plot p: plots) {
            Rect bounds = new Rect();
            p.labelPaint.getTextBounds(p.zone, 0, p.zone.length(), bounds);
            labels.union(bounds);
            if (lineHeight == 0) {
                lineHeight = (int) p.labelPaint.getTextSize();
            }
        }

        labels = new Rect(0, 0, labels.width(), lineHeight * plots.size());

        /* Where to put the labels list */
        int x = width - 2 * margin - labels.width();
        int y = height - 2 * margin - labels.height();

        canvas.drawRect(x, y, x + labels.width() + 32, y + labels.height() + 16, labelRectFill);
        canvas.drawRect(x, y, x + labels.width() + 32, y + labels.height() + 16, labelRectStroke);
        for (Plot p: plots) {
            canvas.drawText(p.zone, x + 16, y + lineHeight, p.labelPaint);
            y += p.labelPaint.getTextSize();
        }
    }

    public void setData(String zone, int type, ArrayList<Point> d) {
        boolean done = false;
        for (Plot p: plots) {
            if (p.zone.equals(zone) && p.type == type) {
                p.data = d;
                done = true;
            }
        }
        if (!done) {
            plots.add(new Plot(zone, type, d, color[type][nextColor[type]]));
            ++nextColor[type];
        }
        invalidate();
    }

    /** @param range Time range in milliseconds */
    public void setTimeRange(long range) {
        timeRange = range;
    }

    public void setXLabels(String[] xLabels) {
        this.xLabels = xLabels;
        invalidate();
    }

    public void setXDivisions(int d) {
        hDivisions = d;
        invalidate();
    }

}
