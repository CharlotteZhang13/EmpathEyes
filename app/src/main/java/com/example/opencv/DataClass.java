package com.example.opencv;

import android.graphics.Bitmap;
import java.io.IOException;


public class DataClass {
    private Bitmap capturedBitmap;
    private static DataClass dataClass = new DataClass();

    private DataClass(){

    }

    public static DataClass getInstance() {
        return dataClass;
    }

    public Bitmap getCapturedBitmap(){
        return capturedBitmap;
    }

    public void updateCapturedBitmap(Bitmap bitmap) throws IOException {
        capturedBitmap = bitmap;


    }
}
