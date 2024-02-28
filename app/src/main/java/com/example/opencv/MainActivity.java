package com.example.opencv;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;

import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.ImageAnalysisConfig;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private File outputDirectory;
    private ExecutorService cameraExecutor;
//    private ImageView imgView;
    private long lastAnalyzedTimestamp = -6000;
    private long FRAME_INTERVAL_MILLIS = 6000;
    private FilterView filterView;
    private RadioGroup radioGroup;
    private SeekBar seekBar;
    private static float SEEKBARMAX = 100f;
    private static float SEEKBARPROGRESS = 0.2f;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        filterView = findViewById(R.id.filterView);
        radioGroup = findViewById(R.id.radio_group);
        seekBar = findViewById(R.id.seekBar);
        OpenCVLoader.initDebug();

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selectedRadioButton = findViewById(checkedId);
            filterView.updateButton((String) selectedRadioButton.getText());
            Log.d("-----------", (String) selectedRadioButton.getText());
        });

        seekBar.setMax((int)SEEKBARMAX);
        seekBar.setProgress((int)(SEEKBARMAX * SEEKBARPROGRESS));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                filterView.updateSlider(progress, SEEKBARMAX);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //检查app是否原来就有打开相机的权限
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            //没有权限就重新获取
            // requestPermissions会被外部调用，发送权限并收到回复后会外部调用MainActivity重写了的onRequestPermissionsResult的回调接口（见下面）
            ActivityCompat.requestPermissions(this, Configuration.REQUIRED_PERMISSIONS,
                    Configuration.REQUEST_CODE_PERMISSIONS);
        }

//        imgView = findViewById(R.id.imageView);
//        // 设置拍照按钮监听
//        Button camera_capture_button = findViewById(R.id.image_capture_button);
//        camera_capture_button.setOnClickListener(v -> takePhoto());

        // 设置照片等保存的位置
        outputDirectory = getOutputDirectory();

        cameraExecutor = Executors.newSingleThreadExecutor();

    }

    //发送权限后调用
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Configuration.REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "用户拒绝授予权限！", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

//    private void takePhoto() {
//
//    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider processCameraProvider = cameraProviderFuture.get();

                PreviewView viewFinder = (PreviewView)findViewById(R.id.viewFinder);
                Preview preview = new Preview.Builder()
                        .build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, new MyAnalyzer());

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                processCameraProvider.unbindAll();
                processCameraProvider.bindToLifecycle(MainActivity.this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e(Configuration.TAG, "用例绑定失败！" + e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private boolean allPermissionsGranted() {
        for (String permission : Configuration.REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private File getOutputDirectory() {
        File mediaDir = new File(getExternalMediaDirs()[0], getString(R.string.app_name));
        boolean isExist = mediaDir.exists() || mediaDir.mkdir();
        return isExist ? mediaDir : null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    static class Configuration {
        public static final String TAG = "CameraxBasic";
        public static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
        public static final int REQUEST_CODE_PERMISSIONS = 10;
        public static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    }

    private class MyAnalyzer implements ImageAnalysis.Analyzer{
        @SuppressLint("UnsafeOptInUsageError")
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {

//            long currentTimestamp = System.currentTimeMillis();
//            if (currentTimestamp - lastAnalyzedTimestamp >= FRAME_INTERVAL_MILLIS) {
                int width = imageProxy.getWidth();
                int height = imageProxy.getHeight();
                ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer();
                byte[] rgbaData = new byte[buffer.remaining()];
                buffer.get(rgbaData);
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                ByteBuffer bitmapBuffer = ByteBuffer.wrap(rgbaData);
                bitmap.copyPixelsFromBuffer(bitmapBuffer);


//                Mat mat = new Mat();
//                Utils.bitmapToMat(bitmap, mat);
//
//                Mat filter = Mat.zeros(3,3,CvType.CV_64F);
//                double[] data = {1.017277,0.027029,-0.044306 ,-0.006113,0.958479,0.047634,0.006379,0.248708,0.744913};
//                for(int i = 0; i<3; i++){
//                    for(int j = 0; j<3; j++){
//                        filter.put(i, j, data[i*3 + j]);
//                    }
//                }
//
//                Mat newMat = new Mat(height, width, CvType.CV_8UC4);
//                for(int row = 0; row<height; row++){
//                    for(int column = 0; column<width; column++){
//                        double[] dt = new double[4];
//                        dt[3] = mat.get(row, column)[3];
//                        for(int i = 0; i<3; i++){
//                            double r, g, b;
//                            b = mat.get(row, column)[0];
//                            g = mat.get(row, column)[1];
//                            r = mat.get(row, column)[2];
//                            double x, y, z;
//                            x = filter.get(0, i)[0];
//                            y = filter.get(1, i)[0];
//                            z = filter.get(2, i)[0];
//
//                            double result = r*x + g*y + b*z;
//                            if(result>=255){
//                                dt[2-i] = 255;
//                            } else if(result<=0){
//                                dt[2-i] = 0;
//                            } else {
//                                dt[2-i] = result;
//                            }
//                        }
//                        newMat.put(row, column, dt);
//                    }
//                }
//
//                Bitmap bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//                Utils.matToBitmap(newMat,bm);

                runOnUiThread(() -> {
//                    imgView.setImageBitmap(bm);
                    filterView.updateBitmap(bitmap);
                    filterView.postInvalidate();
                    imageProxy.close();
                });
//                lastAnalyzedTimestamp = currentTimestamp;
            }
//        }
    }
}
