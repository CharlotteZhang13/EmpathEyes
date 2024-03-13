package com.example.opencv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.graphics.Canvas;

public class FilterView extends View {

    private Paint mPaint = new Paint();
    private Bitmap canvasBitmap;
    private String colorBlindess = "Protanomaly";
    private float reading = 20;
    private float maxReading = 100;

    public FilterView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPaint.setAntiAlias(true);
    }

    public void updateBitmap(Bitmap m_bitmap){
        canvasBitmap = m_bitmap;
        canvasBitmap = adjustPhotoRotation(canvasBitmap, 90);
    }

    public void updateButton(String i){
        colorBlindess = i;
    }
    public void updateSlider(float reading, float maxReading){
        this.reading = reading;
        this.maxReading = maxReading;
    }

    Bitmap adjustPhotoRotation(Bitmap bm, final int orientationDegree) {
        Matrix m = new Matrix();
        m.setRotate(orientationDegree, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
        Bitmap bm1 = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
        return bm1;
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(canvasBitmap != null){
            ColorMatrix filterMatrix = FilterMatrixClass.getInstance().produceFilterMatrix(this.colorBlindess,this.reading, this.maxReading);
            mPaint.setColorFilter(new ColorMatrixColorFilter(filterMatrix));
            canvas.drawBitmap(canvasBitmap, null, new Rect(0, 0, 1200, 1200 * canvasBitmap.getHeight() / canvasBitmap.getWidth()), mPaint);
        }
    }

}
