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
 * Created by Fanny on 17/6/30.
 */

public class XYJC_HISTORYFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = LinearLayout.inflate(getContext(), R.layout.fragment_xyjc_history,null);
        return view;
    }
}
