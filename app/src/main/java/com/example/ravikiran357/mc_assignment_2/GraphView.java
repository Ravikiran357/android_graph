package com.example.ravikiran357.mc_assignment_2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.view.View;

/**
 * GraphView creates a scaled line or bar graph with x and y axis labels.
 * @author Arno den Hond
 *
 */
public class GraphView extends View {

    public static boolean BAR = false;
    public static boolean LINE = true;

    private Paint paint;
    private float[] valuesx;
    private float[] valuesy;
    private float[] valuesz;
    private String[] horlabels;
    private String[] verlabels;
    private String title;
    private boolean type;

    public GraphView(Context context, float[] values, float[] valuesy, float[] valuesz, String title, String[] horlabels, String[] verlabels, boolean type) {
        super(context);
        if (values == null)
            values = new float[0];
        else
            this.valuesx = values;
        if (valuesy == null)
            valuesy = new float[0];
        else
            this.valuesy = valuesy;
        if (valuesz == null)
            valuesz = new float[0];
        else
            this.valuesz = valuesz;

        if (title == null)
            title = "";
        else
            this.title = title;
        if (horlabels == null)
            this.horlabels = new String[0];
        else
            this.horlabels = horlabels;
        if (verlabels == null)
            this.verlabels = new String[0];
        else
            this.verlabels = verlabels;
        this.type = type;
        paint = new Paint();
    }

    public void setValues(float[] newValues, float[] newValuesy, float[] newValuesz)
    {
        this.valuesx = newValues;
        this.valuesy = newValuesy;
        this.valuesz = newValuesz;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float border = 20;
        float horstart = border * 2;
        float height = getHeight();
        float width = getWidth() - 1;
//        float max = getMax();
        float max = 15;
//        float min = getMin();
        float min = -15;
        float diff = max - min;
        float graphheight = height - (2 * border);
        float graphwidth = width - (2 * border);


        paint.setTextAlign(Align.LEFT);
        int vers = verlabels.length - 1;
        for (int i = 0; i < verlabels.length; i++) {
            paint.setColor(Color.DKGRAY);
            float y = ((graphheight / vers) * i) + border;
            canvas.drawLine(horstart, y, width, y, paint);
            paint.setColor(Color.WHITE);
            canvas.drawText(verlabels[i], 0, y, paint);
        }
        int hors = horlabels.length - 1;
        for (int i = 0; i < horlabels.length; i++) {
            paint.setColor(Color.DKGRAY);
            float x = ((graphwidth / hors) * i) + horstart;
            canvas.drawLine(x, height - border, x, border, paint);
            paint.setTextAlign(Align.CENTER);
            if (i==horlabels.length-1)
                paint.setTextAlign(Align.RIGHT);
            if (i==0)
                paint.setTextAlign(Align.LEFT);
            paint.setColor(Color.WHITE);
            canvas.drawText(horlabels[i], x, height - 4, paint);
        }

        paint.setTextAlign(Align.CENTER);
        canvas.drawText(title, (graphwidth / 2) + horstart, border - 4, paint);

        if (max != min) {
            paint.setColor(Color.LTGRAY);
            if (type == BAR) {
                float datalength = valuesx.length;
                float colwidth = (width - (2 * border)) / datalength;
                for (int i = 0; i < valuesx.length; i++) {
                    float val = valuesx[i] - min;
                    float rat = val / diff;
                    float h = graphheight * rat;
                    canvas.drawRect((i * colwidth) + horstart, (border - h) + graphheight, ((i * colwidth) + horstart) + (colwidth - 1), height - (border - 1), paint);

                    //
                    float valy = valuesy[i] - min;
                    float raty = valy / diff;
                    float hy = graphheight * raty;
                    canvas.drawRect((i * colwidth) + horstart, (border - hy) + graphheight, ((i * colwidth) + horstart) + (colwidth - 1), height - (border - 1), paint);

                    //
                    float valz = valuesz[i] - min;
                    float ratz = valz / diff;
                    float hz = graphheight * ratz;
                    canvas.drawRect((i * colwidth) + horstart, (border - hz) + graphheight, ((i * colwidth) + horstart) + (colwidth - 1), height - (border - 1), paint);
                }
            } else {
                float datalength = valuesx.length;
                float colwidth = (width - (2 * border)) / datalength;
                float halfcol = colwidth / 2;
                float lasth = 0;
                float lasthy = 0;
                float lasthz = 0;
                for (int i = 0; i < valuesx.length; i++) {
                    float val = valuesx[i] - min;
                    float rat = val / diff;
                    float h = graphheight * rat;
                    if (i > 0)
                        paint.setColor(Color.GREEN);
                    paint.setStrokeWidth(2.0f);
                    canvas.drawLine(((i - 1) * colwidth) + (horstart + 1) + halfcol, (border - lasth) + graphheight, (i * colwidth) + (horstart + 1) + halfcol, (border - h) + graphheight, paint);
                    lasth = h;

                    //
                    float valy = valuesy[i] - min;
                    float raty = valy / diff;
                    float hy = graphheight * raty;
                    if (i > 0)
                        paint.setColor(Color.RED);
                    paint.setStrokeWidth(2.0f);
                    canvas.drawLine(((i - 1) * colwidth) + (horstart + 1) + halfcol, (border - lasthy) + graphheight, (i * colwidth) + (horstart + 1) + halfcol, (border - hy) + graphheight, paint);
                    lasthy = hy;

                    //
                    float valz = valuesz[i] - min;
                    float ratz = valz / diff;
                    float hz = graphheight * ratz;
                    if (i > 0)
                        paint.setColor(Color.BLUE);
                    paint.setStrokeWidth(2.0f);
                    canvas.drawLine(((i - 1) * colwidth) + (horstart + 1) + halfcol, (border - lasthz) + graphheight, (i * colwidth) + (horstart + 1) + halfcol, (border - hz) + graphheight, paint);
                    lasthz = hz;
                }
            }
        }
    }

    private float getMax() {
        float largest = Integer.MIN_VALUE;
        for (int i = 0; i < valuesx.length; i++)
            if (valuesx[i] > largest)
                largest = valuesx[i];

        //largest = 3000;
        return largest;
    }

    private float getMin() {
        float smallest = Integer.MAX_VALUE;
        for (int i = 0; i < valuesx.length; i++)
            if (valuesx[i] < smallest)
                smallest = valuesx[i];

        //smallest = 0;
        return smallest;
    }

}