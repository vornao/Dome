package net.vornao.ddns.dome.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import net.vornao.ddns.dome.R;

public class ChartBarDrawer extends View {

    final float rectWidth;
    int width = 0;
    int height = 0;
    private final Rect r;
    private final Rect r1;
    private final Paint paint;
    private final Context context;

    public ChartBarDrawer(Context context, float rectWidth) {
        super(context);
        this.context = context;
        this.rectWidth = rectWidth;
        r1 = new Rect();
        r = new Rect();
        paint = new Paint();

    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int desiredWidth = 1000;
        int desiredHeight = 30;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);


        //Measure Width
        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(desiredWidth, widthSize);
        } else {

            width = desiredWidth;
        }

        //Measure Height
        if (heightMode == MeasureSpec.EXACTLY) {

            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(desiredHeight, heightSize);
        } else {

            height = desiredHeight;
        }

        //MUST CALL THIS
        Log.d("ONMEASURE", String.valueOf(width));
        setMeasuredDimension(width, height);

    }


    protected void onDraw(@NonNull Canvas canvas) {

        super.onDraw(canvas);
        Log.d("DRAW", "HELLO");


        if (rectWidth == 0) {
            r.set(0, 0, 0, 0);
            r1.set(getPaddingLeft(), 0, getWidth() - getPaddingLeft(), getHeight());

        } else {
            r.set(getPaddingLeft(), 0, (int) (getWidth() * rectWidth / 15), getHeight());
            r1.set((int) (getWidth() * rectWidth / 15), 0, getWidth() - getPaddingLeft(), getHeight());
        }

        paint.setColor(getResources().getColor(R.color.teal_700, context.getTheme()));
        paint.setStyle(Paint.Style.FILL);

        canvas.drawRect(r, paint);

        paint.setColor(getResources().getColor(R.color.light_grey, context.getTheme()));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(r1, paint);

    }
}
