package com.fanny.healthcare.activity;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.fanny.healthcare.R;
import com.fanny.healthcare.fragment.TWJC_TESTFragment;
import com.fanny.healthcare.fragment.XTJC_TESTFragment;

public class TWJCActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /**
         * 去标题栏
         */
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN , WindowManager.LayoutParams. FLAG_FULLSCREEN);

        setContentView(R.layout.activity_twjc);
//        getSupportActionBar().hide();
        ActionBar actionBar=getSupportActionBar();
        actionBar.setTitle("体温检测");

        initFragment();
    }

    private void initFragment() {
        TWJC_TESTFragment testFragment=new TWJC_TESTFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fl_tw_content,testFragment).commit();
    }
}
