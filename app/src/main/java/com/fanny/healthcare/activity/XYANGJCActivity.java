package com.fanny.healthcare.activity;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.fanny.healthcare.R;
import com.fanny.healthcare.fragment.XYANGJC_TESTFragment;
import com.fanny.healthcare.fragment.XYJC_TESTFragment;

public class XYANGJCActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xyangjc);

        ActionBar actionBar=getSupportActionBar();
        actionBar.setTitle("血氧检测");

        initFragment();
    }

    private void initFragment() {
        XYANGJC_TESTFragment testFragment=new XYANGJC_TESTFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fl_xyang_content,testFragment).commit();
    }
}
