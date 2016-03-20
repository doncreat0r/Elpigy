package com.bobrik.elpigy;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by DBobrik on 020 20.03.2016.
 */
public class PABarView extends View {

    private Paint barPaintGreen, barPaintYellow, barPaintRed;
    private int mSliceHeight; // updated when size changed
    private int mNumOfSlices = 10;
    private double mDistance;

    public PABarView(Context context, AttributeSet attrs){
        super(context, attrs);

        barPaintGreen = new Paint();
        barPaintGreen.setColor(Elpigy.COLOR_GREEN);
        barPaintYellow = new Paint();
        barPaintYellow.setColor(Elpigy.COLOR_YELLOW);
        barPaintRed = new Paint();
        barPaintRed.setColor(Elpigy.LIGHT_RED);
    }

    public void setDistance(double distance) {
        mDistance = distance;
        invalidate();
        requestLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Paint p;
        for (int i = 0; i < mNumOfSlices; i++) {
            if (i < mNumOfSlices*70/100) // green slices ends at 70%
                p = barPaintGreen;
            else if (i < mNumOfSlices-1) // last one will be red
                p = barPaintYellow;
            else
                p = barPaintRed;

            if ((mNumOfSlices - i)*(90/mNumOfSlices) + 22 > mDistance*100)
                p.setStyle(Paint.Style.STROKE);
            else
                p.setStyle(Paint.Style.FILL_AND_STROKE);
            canvas.drawRect(this.getPaddingLeft(),
                            this.getPaddingTop() * (i+1) + i * mSliceHeight,
                            this.getMeasuredWidth() - this.getPaddingRight(),
                            this.getPaddingTop() * (i+1) + (i+1) * mSliceHeight, p);
        }
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh) {
        mSliceHeight = (h - getPaddingBottom() - getPaddingTop() * mNumOfSlices) / mNumOfSlices;
        super.onSizeChanged(w, h, oldw, oldh);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int w = /*getPaddingLeft() + getPaddingRight() + */getSuggestedMinimumWidth();
        int h = getSuggestedMinimumHeight() + getPaddingBottom() + getPaddingTop();

        setMeasuredDimension(w, h);
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
