package com.fanny.healthcare.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Created by Fanny on 17/7/12.
 */

public class MySpinnerArrayAdapter extends ArrayAdapter<String> {
    private Context mContext;
    private String[] mStringArray;

    public MySpinnerArrayAdapter(Context context, String[] strings) {
        super(context, android.R.layout.simple_spinner_item,strings);
        mContext=context;
        mStringArray=strings;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        /**
         * 修改spinner展开后的字体
         */
        if(convertView==null){
            LayoutInflater inflater=LayoutInflater.from(mContext);
            convertView=inflater.inflate(android.R.layout.simple_spinner_dropdown_item,parent,false);
        }
        TextView tv= (TextView) convertView.findViewById(android.R.id.text1);
        tv.setText(mStringArray[position]);
        tv.setTextSize(22f);
        tv.setTextColor(Color.GREEN);

        return convertView;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        /**
         * 修改spinner选择后结果的字体颜色
         */
        if(convertView==null){
            LayoutInflater inflater=LayoutInflater.from(mContext);
            convertView=inflater.inflate(android.R.layout.simple_spinner_item,parent,false);
        }
        TextView tv= (TextView) convertView.findViewById(android.R.id.text1);
        tv.setText(mStringArray[position]);
        tv.setTextSize(20f);
        tv.setTextColor(Color.RED);
        return convertView;
    }
}