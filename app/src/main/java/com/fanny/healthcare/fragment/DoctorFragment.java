package com.fanny.healthcare.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.fanny.healthcare.R;

/**
 * Created by Fanny on 17/6/28.
 */

public class DoctorFragment extends Fragment{

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = LinearLayout.inflate(getContext(), R.layout.fragment_doctor,null);
        return view;
    }
}
