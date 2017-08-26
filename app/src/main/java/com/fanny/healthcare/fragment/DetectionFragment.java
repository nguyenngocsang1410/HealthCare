package com.fanny.healthcare.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fanny.healthcare.R;

/**
 * Created by Fanny on 17/6/28.
 */

public class DetectionFragment extends Fragment{

    private View view;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = LinearLayout.inflate(getContext(), R.layout.fragment_detection,null);

        return view;
    }
}
