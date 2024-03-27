package com.example.opencv;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Debug;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.UiSettings;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.MyLocationStyle;
import com.amap.api.maps2d.model.Text;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import cn.leancloud.LCException;
import cn.leancloud.LCFile;
import cn.leancloud.LCObject;
import cn.leancloud.LCQuery;
import cn.leancloud.LeanCloud;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import com.squareup.picasso.Picasso;

public class GaodeActivity extends AppCompatActivity implements  LocationSource, AMapLocationListener {

    private String id;
    //显示地图需要的变量
    private MapView mapView;//地图控件
    private AMap aMap;//地图对象
    //定位需要的声明
    private AMapLocationClient mLocationClient = null;//定位发起端
    private AMapLocationClientOption mLocationOption = null;//定位参数
    private LocationSource.OnLocationChangedListener mListener = null;//定位监听器
    private Bundle savedInstanceState;
    private AMap.InfoWindowAdapter mAMapSpotAdapter;
    private boolean isFirstLoc = true;
    private Spinner mSpinner;
    private List<LCObject> mMarkerDataList = null;
    private SpinnerAdapter.OnSpinnerListener mOnspinnerListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gaode);
        this.savedInstanceState = savedInstanceState;

        getDatabase();
        try {
            initMap(savedInstanceState);
            initLoc();
            initAdapter();
            setMap();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, Configuration.REQUEST_COARSE_LOCATION);
        }

        mSpinner = findViewById(R.id.spinner);
        mOnspinnerListener = new SpinnerAdapter.OnSpinnerListener() {
            @Override
            public void onSpinnerClick(double lat, double lon) {
                aMap.moveCamera(CameraUpdateFactory.zoomTo(17));
                //将地图移动到定位点
                aMap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(lat, lon)));
            }
        };
    }

    protected void getDatabase(){
        LeanCloud.initialize(this, "nMKlsJWMeXPRTzUXRAtfRA24-gzGzoHsz", "jHVnjnAuhQ52D1BD6QLbKmaH", "https://nmklsjwm.lc-cn-n1-shared.com");
        this.id = getIntent().getStringExtra("id");
    }

    private void initMap(Bundle savedInstanceState){
        mapView = (MapView) findViewById(R.id.map);
        //必须要写
        mapView.onCreate(savedInstanceState);
        //获取地图对象
        aMap = mapView.getMap();
        //设置显示定位按钮 并且可以点击
        UiSettings settings = aMap.getUiSettings();
        //设置定位监听
        aMap.setLocationSource(this);
        // 是否显示定位按钮
        settings.setMyLocationButtonEnabled(true);
        // 是否可触发定位并显示定位层
        aMap.setMyLocationEnabled(true);

        //定位的小图标 默认是蓝点 这里自定义一团火，其实就是一张图片
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher));
        myLocationStyle.radiusFillColor(android.R.color.transparent);
        myLocationStyle.strokeColor(android.R.color.transparent);
        aMap.setMyLocationStyle(myLocationStyle);

        AMapLocationClient.updatePrivacyShow(GaodeActivity.this,true,true);
        AMapLocationClient.updatePrivacyAgree(GaodeActivity.this,true);
    }

    private void initAdapter(){
        mAMapSpotAdapter = new AMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                if ("".equals(marker.getTitle()) || marker.getTitle() == null) {
                    return null;
                }
                View infoContent = getLayoutInflater().inflate(R.layout.marker_layout, null);
                render(marker, infoContent);
                return infoContent;
            }

            @Override
            public View getInfoContents(Marker marker) {
                if ("".equals(marker.getTitle()) || marker.getTitle() == null) {
                    return null;
                }
                View infoContent = getLayoutInflater().inflate(R.layout.marker_layout, null);
                render(marker, infoContent);
                return infoContent;
            }

            private void render(Marker marker, View view){
                String comment = marker.getSnippet();
                TextView commentView = view.findViewById(R.id.comment);
                commentView.setText(comment);

                LCQuery<LCObject> query = new LCQuery<>("Markers");
                query.getInBackground(marker.getTitle()).subscribe(new Observer<LCObject>() {
                    public void onSubscribe(Disposable disposable) {}
                    @SuppressLint("UseCompatLoadingForDrawables")
                    public void onNext(LCObject todo) {
                        LCFile file = todo.getLCFile("Image");
                        ImageView imageView = view.findViewById(R.id.img);
                        String url = file.getThumbnailUrl(true, 90, 90);

                        Picasso.get()
                                .load(url)
                                .placeholder(getResources().getDrawable(R.drawable.loading))
                                .error(getResources().getDrawable(R.drawable.baseline_error_24))
                                .into(imageView);
                    }
                    public void onError(Throwable throwable) {}
                    public void onComplete() {}
                });
            }
        };
    }

    private void initLoc() throws Exception {
        mLocationClient = new AMapLocationClient(getApplicationContext());
        //设置定位回调监听
        mLocationClient.setLocationListener(this);
        //初始化定位参数
        mLocationOption = new AMapLocationClientOption();
        //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);
        //设置是否只定位一次,默认为false
        mLocationOption.setOnceLocation(false);
        //设置是否强制刷新WIFI，默认为强制刷新
        mLocationOption.setWifiActiveScan(true);
        //设置是否允许模拟位置,默认为false，不允许模拟位置
        mLocationOption.setMockEnable(false);
        //设置定位间隔,单位毫秒,默认为2000ms
        mLocationOption.setInterval(2000);
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        //启动定位
        mLocationClient.startLocation();
    }

    private void setMap(){
        aMap.setInfoWindowAdapter(mAMapSpotAdapter);
        aMap.setOnMarkerClickListener(marker -> {
            if (marker.isInfoWindowShown()) {
                marker.hideInfoWindow();
            } else {
                marker.showInfoWindow();
            }
            return true;
        });
    }

    static class Configuration {
        public static final int REQUEST_COARSE_LOCATION = 15;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case Configuration.REQUEST_COARSE_LOCATION:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)== PackageManager.PERMISSION_GRANTED) {
                    try {
                        initLoc();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    Toast.makeText(this, "Please enable location permissions", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    //定位回调函数
    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && amapLocation != null) {
            if (amapLocation.getErrorCode() == 0) {
                if (isFirstLoc) {
                    //设置缩放级别
                    aMap.moveCamera(CameraUpdateFactory.zoomTo(17));
                    //将地图移动到定位点
                    aMap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude())));
                    //点击定位按钮 能够将地图的中心移动到定位点
                    mListener.onLocationChanged(amapLocation);
                    //添加图钉
                    aMap.addMarker(getMarkerOptions(amapLocation.getLatitude(), amapLocation.getLongitude()));
                    //获取定位信息
                    StringBuffer buffer = new StringBuffer();
                    buffer.append(amapLocation.getCountry() + "" + amapLocation.getProvince() + "" + amapLocation.getCity() + "" + amapLocation.getProvince() + "" + amapLocation.getDistrict() + "" + amapLocation.getStreet() + "" + amapLocation.getStreetNum());
                    Toast.makeText(getApplicationContext(), buffer.toString(), Toast.LENGTH_LONG).show();
                    isFirstLoc = false;

                    if(id != null){
                        LCObject marker = LCObject.createWithoutData("Markers", id);
                        marker.put("latitude", amapLocation.getLatitude());
                        marker.put("longitude", amapLocation.getLongitude());
                        marker.saveInBackground().subscribe(new Observer<LCObject>() {
                            public void onSubscribe(Disposable disposable) {}
                            public void onNext(LCObject savedTodo) {
                                getMarkers();
                            }
                            public void onError(Throwable throwable) {
                            }
                            public void onComplete() {}
                        });
                    }
                }
            } else {
                Toast.makeText(getApplicationContext(), "定位失败", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void getMarkers(){
        LCQuery<LCObject> query = new LCQuery<>("Markers");
        query.findInBackground().subscribe(new Observer<List<LCObject>>() {
            public void onSubscribe(Disposable disposable) {}
            public void onNext(List<LCObject> markerDataList) {
                mMarkerDataList = markerDataList;
                SpinnerAdapter spinnerAdapter = new SpinnerAdapter(getApplicationContext(), mMarkerDataList, mOnspinnerListener);
                mSpinner.setAdapter(spinnerAdapter);

                for (int i = 0; i < markerDataList.size(); i++){
                    LCObject markerData = markerDataList.get(i);
                    if(markerData.getNumber("latitude") == null || markerData.getNumber("longitude") == null){
                        continue;
                    }
                    MarkerOptions options = new MarkerOptions()
                            .position(new LatLng(markerData.getNumber("latitude").doubleValue(), markerData.getNumber("longitude").doubleValue()))
                            .title(markerData.getObjectId())
                            .snippet(markerData.getString("comment"))
                            .period(60);
                    aMap.addMarker(options);
                }
            }
            public void onError(Throwable throwable) {}
            public void onComplete() {}
        });
    }

    //自定义一个图钉，并且设置图标，当我们点击图钉时，显示设置的信息
    private MarkerOptions getMarkerOptions(double lat, double lon) {
        //设置图钉选项
        MarkerOptions options = new MarkerOptions();
        //位置
        options.position(new LatLng(lat, lon));
        StringBuffer buffer = new StringBuffer();
        //标题
        options.title(buffer.toString());
        //设置多少帧刷新一次图片资源
        options.period(60);

        return options;

    }

    @Override
    public void activate(LocationSource.OnLocationChangedListener listener) {
        mListener = listener;
        mLocationClient.startLocation();
    }

    @Override
    public void deactivate() {
        mListener = null;
        mLocationClient.stopLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
}
