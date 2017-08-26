package com.fanny.healthcare.activity;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.fanny.healthcare.R;
import com.fanny.healthcare.fragment.BC401Fragment;
import com.fanny.healthcare.fragment.XYJC_TESTFragment;

public class NYJCActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nyjc);
        ActionBar actionBar=getSupportActionBar();
        actionBar.setTitle("尿液检测");

        initFragment();
    }
    private void initFragment() {
        BC401Fragment testFragment=new BC401Fragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fl_nyjc_content,testFragment).commit();
    }
}
