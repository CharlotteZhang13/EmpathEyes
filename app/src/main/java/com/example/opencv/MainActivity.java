package com.example.opencv;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private File outputDirectory;
    private ExecutorService cameraExecutor;
    private long lastAnalyzedTimestamp = -6000;
    private long FRAME_INTERVAL_MILLIS = 6000;
    private FilterView filterView;
    private RadioGroup radioGroup;
    private Button cameraButton;
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

        cameraButton = findViewById(R.id.cameraButton);
        cameraButton.setOnClickListener(v -> {
            Bitmap bitmap = saveViewAsBitmap(filterView);
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_layout, null);
            ImageView imageView = dialogView.findViewById(R.id.imageView);
            imageView.setImageBitmap(bitmap);
            AlertDialog alertDialog1 = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setPositiveButton("Share", (dialog, which) -> {
                        Intent intent = new Intent(this, GaodeActivity.class);
                        DataClass.getInstance().updateCapturedBitmap(bitmap);
                        startActivity(intent);
                    })
                    .create();
            alertDialog1.show();
        });


        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selectedRadioButton = findViewById(checkedId);
            filterView.updateButton((String) selectedRadioButton.getText());
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
                int width = imageProxy.getWidth();
                int height = imageProxy.getHeight();
                ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer();
                byte[] rgbaData = new byte[buffer.remaining()];
                buffer.get(rgbaData);
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                ByteBuffer bitmapBuffer = ByteBuffer.wrap(rgbaData);
                bitmap.copyPixelsFromBuffer(bitmapBuffer);

                runOnUiThread(() -> {
                    filterView.updateBitmap(bitmap);
                    filterView.postInvalidate();
                    imageProxy.close();
                });
            }
    }

    public static Bitmap saveViewAsBitmap(View view) {
        int width = view.getWidth();
        int height = view.getHeight();
        if (width <= 0 || height <= 0) {
            int specSize = View.MeasureSpec.makeMeasureSpec(0 /* any */, View.MeasureSpec.UNSPECIFIED);
            view.measure(specSize, specSize);
            width = view.getMeasuredWidth();
            height = view.getMeasuredHeight();
        }
        if (width <= 0 || height <= 0) {
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        if(view.getRight() <= 0 || view.getBottom() <= 0) {
            view.layout(0, 0, width, height);
            view.draw(canvas);
        } else {
            view.layout(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
            view.draw(canvas);
        }

        return bitmap;
    }

}
