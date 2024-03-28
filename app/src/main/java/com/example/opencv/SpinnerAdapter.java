package com.example.opencv;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.compose.ui.graphics.ExperimentalGraphicsApi;

import com.squareup.picasso.Picasso;

import java.util.List;

import cn.leancloud.LCFile;
import cn.leancloud.LCObject;

public class SpinnerAdapter extends BaseAdapter {

    private Context mContext;
    private List<LCObject> mMarkerDataList;
    OnSpinnerListener mOnspinnerListener;

    public SpinnerAdapter(Context context, List<LCObject> markerDataList, OnSpinnerListener onspinnerListener){
        this.mContext = context;
        this.mMarkerDataList = markerDataList;
        this.mOnspinnerListener = onspinnerListener;
    }

    @Override
    public int getCount() {
        return mMarkerDataList.size();
    }

    @Override
    public Object getItem(int position) {
        return mMarkerDataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public interface OnSpinnerListener{
        public void onSpinnerClick(double lat, double lon);
    }

    @SuppressLint({"ViewHolder", "UseCompatLoadingForDrawables"})
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater mLayoutInflator = LayoutInflater.from(mContext);
        convertView = mLayoutInflator.inflate(R.layout.spinner_layout, null);
        LCObject markerData = mMarkerDataList.get(position);
        if(convertView != null && markerData != null){
            ImageView imageView = convertView.findViewById(R.id.imageview);
            TextView textView = convertView.findViewById(R.id.textview);

            textView.setText(markerData.getString("comment"));
            LCFile file = markerData.getLCFile("Image");
            if(file != null){
                String url = file.getThumbnailUrl(true, 90, 90);
                Picasso.get()
                        .load(url)
                        .placeholder(mContext.getResources().getDrawable(R.drawable.loading))
                        .error(mContext.getResources().getDrawable(R.drawable.baseline_error_24))
                        .into(imageView);
            }

            if(markerData.getNumber("latitude") != null && markerData.getNumber("longitude") != null){
                convertView.setOnClickListener(v ->
                        mOnspinnerListener.onSpinnerClick((Double) markerData.getNumber("latitude"), (Double)markerData.getNumber("longitude"))
                );
            }
        }
        return convertView;
    }
}
