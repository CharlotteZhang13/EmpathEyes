package com.example.opencv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.graphics.Canvas;

import java.util.Objects;

public class FilterView extends View {

    private Paint mPaint = new Paint();
    private Bitmap bitmap;
    private String colorBlindess = "Protanomaly";
    private float reading = 20;
    private float maxReading = 100;

    public FilterView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPaint.setAntiAlias(true);
    }

    public void updateBitmap(Bitmap m_bitmap){

        bitmap = m_bitmap;
        bitmap = adjustPhotoRotation(bitmap, 90);
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

        if(bitmap != null){
//            ColorMatrix protanomaly = new ColorMatrix(new float[]{
//                    0.203876f,	0.990338f,	-0.194214f,0,0,
//                    0.112975f,	0.794542f,	0.092483f,0,0,
//                    -0.005222f,	-0.041043f,	1.046265f,0,0,
//                    0, 0, 0, 1, 0,
//            });
//            ColorMatrix deuteranomaly = new ColorMatrix(new float[]{
//                    0.392952f,	0.823610f,	-0.216562f, 0, 0,
//                    0.263559f,	0.690210f,	0.046232f, 0, 0,
//                    -0.011910f,	0.040281f,	0.971630f, 0, 0,
//                    0, 0, 0, 1, 0,
//            });
//            ColorMatrix tritanomaly = new ColorMatrix(new float[]{
//                    1.278864f,	-0.125333f,	-0.153531f, 0, 0,
//                    -0.084748f,	0.957674f,	0.127074f, 0, 0,
//                    -0.000989f,	0.601151f,	0.399838f, 0, 0,
//                    0, 0, 0, 1, 0,
//            });
//            if(Objects.equals(index, "Protanomaly")){
//                mPaint.setColorFilter(new ColorMatrixColorFilter(protanomaly));
//            } else if(Objects.equals(index, "Deuteranomaly")){
//                mPaint.setColorFilter(new ColorMatrixColorFilter(deuteranomaly));
//            }else if(Objects.equals(index, "Tritanomaly")){
//                mPaint.setColorFilter(new ColorMatrixColorFilter(tritanomaly));
//            }
            ColorMatrix filterMatrix = FilterMatrixClass.getInstance().produceFilterMatrix(this.colorBlindess,this.reading, this.maxReading);
            mPaint.setColorFilter(new ColorMatrixColorFilter(filterMatrix));

            canvas.drawBitmap(bitmap, null, new Rect(0, 0, 1200, 1200 * bitmap.getHeight() / bitmap.getWidth()), mPaint);
        }
    }

}
