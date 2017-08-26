package com.fanny.healthcare.fragment;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.creative.pc700.SendCMDThread;
import com.fanny.healthcare.R;
import com.fanny.healthcare.activity.MainActivity;
import com.fanny.healthcare.util.MySpinnerArrayAdapter;
import com.fanny.healthcare.util.MyThread;
import com.fanny.healthcare.util.SocketUtil;
import com.fanny.healthcare.util.XORUtil;

import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.support.v7.widget.StaggeredGridLayoutManager.TAG;
import static com.fanny.healthcare.R.array.fanfan_color;
import static com.fanny.healthcare.R.array.nibp_result;

/**
 * Created by Fanny on 17/6/28.
 */

public class XYJC_TESTFragment extends Fragment implements View.OnClickListener {

    private View view;
    private SendCMDThread mSendCMDThread;
    private TextView tv_sys;
    private TextView tv_dia;
    private TextView tv_plus;
    private TextView tv_map;
    private Button btn_test;
    private RadioButton rb_adult;
    private RadioButton rb_baby;
    private RadioButton rb_child;
    private Spinner spn_nibp_data;
    private RadioGroup rg;

    String[] mNibpScope;//血压选择范围
    int mNibpInidata;//血压初始压力值

    ArrayAdapter<String> mAdapter;

    //socket上传的字节数据
    private byte[] sendBuffer = new byte[18];
    private Button btn_save;
    private ImageView im_heart;
    private TextView tv_rank;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = LinearLayout.inflate(getContext(), R.layout.fragment_xyjc_test, null);

        if (MainActivity.mPC700 != null) {
            mSendCMDThread = MainActivity.mPC700.getSendCMDThread();
        }

        initData();
        initView();
        return view;
    }

    private void initData() {
        sendBuffer[0] = (byte) 0xEA;
        sendBuffer[1] = (byte) 0xEB;

        sendBuffer[2] = (byte) 0x06;//data区字节长度为6个字节

        sendBuffer[3] = (byte) 0x00; //北京地区
        sendBuffer[4] = (byte) 0x0a;

        sendBuffer[5] = (byte) 0x02;//健康管家
        sendBuffer[6] = (byte) 0x01;//血压模块
        sendBuffer[7] = (byte) 0x00;
        sendBuffer[8] = (byte) 0x01;//设备序列号

        sendBuffer[15] = (byte) 0x00;//校验码
        sendBuffer[16] = (byte) 0xE5;
        sendBuffer[17] = (byte) 0xD4;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        MainActivity.setUIHandler(mXYCLHandler);
        super.onActivityCreated(savedInstanceState);
    }


    private void initView() {
        tv_sys = (TextView) view.findViewById(R.id.tv_nibp_sys);
        tv_dia = (TextView) view.findViewById(R.id.tv_nibp_dia);
        tv_plus = (TextView) view.findViewById(R.id.tv_nibp_pr);
        tv_map = (TextView) view.findViewById(R.id.tv_nibp_map);

        tv_rank = (TextView) view.findViewById(R.id.tv_rank);
        im_heart = (ImageView) view.findViewById(R.id.im_heart);

//        rg = (RadioGroup) view.findViewById(R.id.rb_group);
//        rb_adult = (RadioButton) view.findViewById(R.id.rb_adult);
//        rb_child = (RadioButton) view.findViewById(R.id.rb_child);
//        rb_baby = (RadioButton) view.findViewById(R.id.rb_baby);
//        setRadioButtonListener();
//
//        spn_nibp_data = (Spinner) view.findViewById(R.id.sp_nibp_data);
//        setSpinnerListener();
//
        btn_test = (Button) view.findViewById(R.id.btn_start_xyjc);
        btn_test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!MainActivity.bNIBP_Measuring) {
                    mSendCMDThread.nibp_startMeasure();
                    /**
                     * 开始测量，心跳开始
                     */


                    startAnimation();
                    btn_test.setText("停止测量");
                } else {
                    mSendCMDThread.nibp_stopMeasure();
                    /**
                     * 停止测量，心跳停止
                     */
                    stopAnimation();
                    btn_test.setText("开始测量");
                }
            }
        });

//
        btn_save = (Button) view.findViewById(R.id.btn_save_xueya);
        btn_save.setOnClickListener(this);
    }


    Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0x01:
                    boolean isAni = (boolean) msg.obj;
                    if (isAni) {
                        im_heart.setVisibility(View.VISIBLE);
                    } else {
                        im_heart.setVisibility(View.INVISIBLE);
                    }
                    break;
            }
        }
    };
    private boolean isRun = true;
    private Timer timer;
    TimerTask timerTask ;

    private void startAnimation() {
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = 0x01;
                msg.obj = isRun;
                myHandler.sendMessage(msg);
                isRun = !isRun;
            }
        };
        timer.schedule(timerTask, 0, 500);
    }

    private void stopAnimation() {
        Message msg = new Message();
        msg.what = 0x01;
        msg.obj = true;
        myHandler.sendMessage(msg);
        timer.cancel();
    }


    public static final int MSG_DEVICE_VERSION = 1;

    public static final int MSG_GLU_TYPE = 9;
    public static final int MSG_GLU = 10;
    public static final int MSG_UA = 11;
    public static final int MSG_CHOL = 12;
    public static final int MSG_SPO2_PARAM = 13;
    public static final int MSG_SPO2_PULSE = 14;
    public static final int MSG_SPO2_PULSE_OFF = 15;

    public static final int MSG_ECG_WAVE = 20;
    public static final int MSG_ECG_STATUS_CH = 21;
    public static final int MSG_ECG_GAIN = 22;
    public static final int MSG_ECG_START_TIME = 23;

    public static final int MSG_TEMP = 24;

    public static final int MSG_NIBP_REAL_SYS = 30;
    public static final int MSG_NIBP_RESULT = 31;
    public static final int MSG_NIBP_LEAK_RESULT = 32;
    public static final int MSG_NIBP_ERROR = 33;
    public static final int MSG_NIBP_TYPE = 34;
    public static final int MSG_NIBP_SETTING = 35;

    private Handler mXYCLHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_DEVICE_VERSION: {
//                    tv_version.setText(msg.obj+"");
                }
                break;

                case MSG_NIBP_LEAK_RESULT: {
                    //tv_nibp_result.setText("10秒的漏气量："+msg.arg1+" mmHg");
                }
                break;
                case MSG_NIBP_RESULT: {
                    Bundle bundle = msg.getData();
                    tv_sys.setText(bundle.getInt("SYS") + "");
                    tv_dia.setText(bundle.getInt("DIA") + "");
                    tv_plus.setText(bundle.getInt("PLUS") + "");
                    tv_map.setText(bundle.getInt("MAP") + "");

                    int rank = bundle.getInt("rank");
                    /**
                     * rank - 血压结果等级 数值为1-6 对应 最佳、正常、临高、轻高、中高、重高
                     */
                    if (rank == 1) {
                        tv_rank.setText("最佳血压");
                    } else if (rank == 2) {
                        tv_rank.setText("正常血压");
                    } else if (rank == 3) {
                        tv_rank.setText("临高血压");
                    } else if (rank == 4) {
                        tv_rank.setText("轻高血压");
                    } else if (rank == 5) {
                        tv_rank.setText("中高血压");
                    } else if (rank == 6) {
                        tv_rank.setText("重高血压");
                    } else {
                        tv_rank.setText("无等级");
                    }


                    /**
                     * 测量完毕，按钮提示
                     */
                    btn_test.setText("开始测量");
                    stopAnimation();

                    /**
                     * 将数据打包上传socket
                     *
                     */
                    sendBuffer[9] = (byte) bundle.getInt("SYS");
                    sendBuffer[10] = (byte) bundle.getInt("DIA");
                    sendBuffer[11] = (byte) bundle.getInt("PLUS");
                    sendBuffer[12] = (byte) bundle.getInt("MAP");
                    sendBuffer[13] = (byte) bundle.getInt("rank");

                    /**
                     * 测量完毕后，数据自动上传至服务器
                     */
                    Log.e("xueyavalue", String.valueOf(sendBuffer[9]) + "---" + sendBuffer[10]);
                    if (SocketUtil.socket.isConnected()) {
                        sendBuffer[15] = XORUtil.getXORByte(sendBuffer);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                SocketUtil.SendDataByte(sendBuffer);
                            }
                        }).start();
                    } else {
                        Toast.makeText(getActivity(), "socket未连接", Toast.LENGTH_SHORT);
                    }


//                    int result = bundle.getInt("rank");
//                    if(result > 0 /*&& result<= nibp_result.length*/){
//                        tv_nibp_result.setText(nibp_result[result-1]);
//                    }else{
//                        Log.e(TAG,"nibp 血压测量错误:"+result);
//                    }
                }
                break;
                case MSG_NIBP_ERROR: {
                    int index = msg.arg1;
                    if (index > 0) {
//                        tv_nibp_result.setText(nibp_err[index-1]);
                        sendBuffer[14] = Byte.parseByte("false");
                    } else {
                        sendBuffer[14] = Byte.parseByte("true");
                    }
                }
                break;
                case MSG_NIBP_REAL_SYS: {
//                    tv_sys.setText(msg.arg1+""); //袖带压
                }
                break;
                case MSG_NIBP_TYPE: {
                    Toast.makeText(getActivity(), "血压病人类型，设置成功", Toast.LENGTH_SHORT).show();
                }
                break;
                case MSG_NIBP_SETTING: {
                    Toast.makeText(getActivity(), "压力值，设置成功", Toast.LENGTH_SHORT).show();
                }
                break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_save_xueya:
                /**
                 * 数据发送至服务器
                 */
                Log.e("xueyavalue", String.valueOf(sendBuffer[9])+"---"+sendBuffer[10]);
                if(SocketUtil.socket.isConnected()){
                    sendBuffer[15]= XORUtil.getXORByte(sendBuffer);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            SocketUtil.SendDataByte(sendBuffer);
                        }
                    }).start();
                }else {
                    Toast.makeText(getActivity(),"socket未连接",Toast.LENGTH_SHORT);
                }
                break;
        }
    }

//    private void setRadioButtonListener() {
//        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
//
//                mSendCMDThread.nibp_setPatientType(checkedId); //设置病人类型
//
//                switch (checkedId){
//                    case R.id.rb_adult:
//                        mNibpScope=getResources().getStringArray(R.array.nibp_adult_scope);
////                        ArrayAdapter<String> adapter=
////                                new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_item,mNibpScope);
////                        spn_nibp_data.setAdapter(adapter);
//                        mAdapter=new MySpinnerArrayAdapter(getActivity(),mNibpScope);
//                        spn_nibp_data.setAdapter(mAdapter);
//                        spn_nibp_data.setSelection(6);
//                        break;
//                    case R.id.rb_child:
//                        mNibpScope=getResources().getStringArray(R.array.nibp_child_scope);
////                        ArrayAdapter<String> adapter1=
////                                new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_item,mNibpScope);
////                        spn_nibp_data.setAdapter(adapter1);
//                        mAdapter=new MySpinnerArrayAdapter(getActivity(),mNibpScope);
//                        spn_nibp_data.setAdapter(mAdapter);
//                        spn_nibp_data.setSelection(3);
//                        break;
//                    case R.id.rb_baby:
//                        mNibpScope=getResources().getStringArray(R.array.nibp_baby_scope);
////                        ArrayAdapter<String> adapter2=
////                                new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_item,mNibpScope);
////                        spn_nibp_data.setAdapter(adapter2);
//                        mAdapter=new MySpinnerArrayAdapter(getActivity(),mNibpScope);
//                        spn_nibp_data.setAdapter(mAdapter);
//                        spn_nibp_data.setSelection(2);
//                        break;
//                }
//            }
//        });
//    }


//    private void setSpinnerListener() {
//        mNibpScope=getResources().getStringArray(R.array.nibp_adult_scope);
//        mAdapter=new MySpinnerArrayAdapter(getActivity(),mNibpScope);
//        spn_nibp_data.setAdapter(mAdapter);
//        spn_nibp_data.setSelection(6);
//        spn_nibp_data.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                if(mNibpScope!=null){
//                    mNibpInidata=Integer.valueOf(mNibpScope[(int) id]);
//                    mSendCMDThread.nibp_setPressure(mNibpInidata);
//                }
//
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//
//            }
//        });
//    }


    /**
     * 子线程方法实现：设置心跳搏动标记
     *
     * @param isShow
     */

//    private Handler mHandler;
//    private HandlerThread thread = new HandlerThread("MyHandlerThread");
//
//
//    private void initHandler() {
//        thread.start();//创建一个HandlerThread并启动它
//        mHandler = new Handler(thread.getLooper());//使用HandlerThread的looper对象创建Handler，如果使用默认的构造方法，很有可能阻塞UI线程
//
//    }
//
//    //实现耗时操作的线程
//    Runnable mBackgroundRunnable1 = new Runnable() {
//
//        @Override
//        public void run() {
//            //----------模拟耗时的操作，开始---------------
//            try {
//                Thread.sleep(500);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
////                    showHeartPulse(false);
//            Message msg = new Message();
//            msg.what = 0x01;
//            UIHandler.sendMessage(msg);
//            //----------模拟耗时的操作，结束---------------
//        }
//    };
//    Runnable mBackgroundRunnable2 = new Runnable() {
//
//        @Override
//        public void run() {
//            //----------模拟耗时的操作，开始---------------
//            try {
//                Thread.sleep(500);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
////                    showHeartPulse(false);
//            Message msg = new Message();
//            msg.what = 0x02;
//            UIHandler.sendMessage(msg);
//            //----------模拟耗时的操作，结束---------------
//        }
//
//    };


//    Handler UIHandler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            super.handleMessage(msg);
//            switch (msg.what) {
//                case 0x01:
//                    showHeartPulse(false, true);
//                    break;
//                case 0x02:
//                    showHeartPulse(true, true);
//                    break;
//            }
//        }
//    };


//    private void showHeartPulse(boolean isShow, boolean isTest) {
//        if (isShow) {
//            im_heart.setVisibility(View.VISIBLE);
////            if(mBackgroundRunnable2!=null){
//                mHandler.removeCallbacks(mBackgroundRunnable2);
////            }
//            mHandler.post(mBackgroundRunnable1);//将线程post到Handler中
//
//        } else {
//            im_heart.setVisibility(View.INVISIBLE);
//
//            if (isTest) {
////                if(mBackgroundRunnable1!=null){
//                    mHandler.removeCallbacks(mBackgroundRunnable1);
////                }
//                mHandler.post(mBackgroundRunnable2);
//            } else {
//
//
//                mHandler.removeCallbacks(mBackgroundRunnable1);
//                mHandler.removeCallbacks(mBackgroundRunnable2);
//
//
//                im_heart.setVisibility(View.VISIBLE);
//            }
//
//        }
//    }

}
