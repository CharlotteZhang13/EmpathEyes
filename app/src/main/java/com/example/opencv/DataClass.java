package com.example.opencv;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import android.Manifest;

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
