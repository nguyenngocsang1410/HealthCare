package com.fanny.healthcare.activity;

import android.app.Activity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.creative.pc700.SendCMDThread;
import com.fanny.healthcare.R;
import com.fanny.healthcare.fragment.XYJC_TESTFragment;

public class XYJCActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xyjc);

        ActionBar actionBar=getSupportActionBar();
        actionBar.setTitle("血压检测");

        initFragment();

    }

    private void initFragment() {
        XYJC_TESTFragment testFragment=new XYJC_TESTFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fl_content,testFragment).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.xyjc_history:

                break;
            case R.id.xyjc_putin:

                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
