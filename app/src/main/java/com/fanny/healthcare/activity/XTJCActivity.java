package com.fanny.healthcare.activity;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.fanny.healthcare.R;
import com.fanny.healthcare.fragment.XTJC_TESTFragment;
import com.fanny.healthcare.fragment.XYANGJC_TESTFragment;

public class XTJCActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xtjc);

        ActionBar actionBar=getSupportActionBar();
        actionBar.setTitle("血糖检测");

        initFragment();
    }
    private void initFragment() {
        XTJC_TESTFragment testFragment=new XTJC_TESTFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fl_xt_content,testFragment).commit();
    }
}
