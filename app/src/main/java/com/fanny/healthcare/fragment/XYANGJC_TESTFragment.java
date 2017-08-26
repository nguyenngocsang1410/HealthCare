package com.fanny.healthcare.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import com.creative.draw.SpO2SurfaceView;
import com.creative.pc700.SendCMDThread;
import com.fanny.healthcare.R;
import com.fanny.healthcare.activity.MainActivity;
import com.fanny.healthcare.util.SocketUtil;
import com.fanny.healthcare.util.XORUtil;

import java.util.Locale;

/**
 * Created by Fanny on 17/6/28.
 */

public class XYANGJC_TESTFragment extends Fragment implements View.OnClickListener {

    private View view;
    private SendCMDThread mSendCMDThread;
    private TextView tv_spo2;
    private TextView tv_pr;
    private TextView tv_pi;
    public static SpO2SurfaceView sv_spo2;
    private ImageView iv_spo2_pulse;

    //socket上传的字节数据
    private byte[] sendBuffer = new byte[18];
    private Button btn_save;
//    private Button btn_test;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = LinearLayout.inflate(getContext(), R.layout.fragment_xyangjc_test, null);

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

        sendBuffer[2] = (byte) 0x05;//data区字节长度为3个字节

        sendBuffer[3] = (byte) 0x00; //北京地区
        sendBuffer[4] = (byte) 0x0a;

        sendBuffer[5] = (byte) 0x02;//健康管家
        sendBuffer[6] = (byte) 0x03;//血氧模块
        sendBuffer[7] = (byte) 0x00;//设备序列号
        sendBuffer[8] = (byte) 0x01;

        sendBuffer[15] = (byte) 0x00;//校验码
        sendBuffer[16] = (byte) 0xE5;
        sendBuffer[17] = (byte) 0xD4;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        MainActivity.setUIHandler(mXYANGCLHandler);
        super.onActivityCreated(savedInstanceState);
    }

    private void initView() {
        tv_spo2 = (TextView) view.findViewById(R.id.tv_spo2_value);
        tv_pr = (TextView) view.findViewById(R.id.tv_pr_value);
        tv_pi = (TextView) view.findViewById(R.id.tv_pi_value);

        sv_spo2 = (SpO2SurfaceView) view.findViewById(R.id.sv_spo2);
        sv_spo2.setScope(255, 0);

//        iv_spo2_pulse = (ImageView) view.findViewById(R.id.iv_spo2_pulse);

//        btn_save = (Button) view.findViewById(R.id.btn_save_xueyang);
//        btn_save.setOnClickListener(this);

//        btn_test= (Button) view.findViewById(R.id.btn_test_xueyang);
//        btn_test.setOnClickListener(this);
    }


    public static final int MSG_SPO2_PARAM = 13;
    public static final int MSG_SPO2_PULSE = 14;
    public static final int MSG_SPO2_PULSE_OFF = 15;


    private Handler mXYANGCLHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SPO2_PARAM: {
//                    if(sv_spo2!=null){
//                        sv_spo2.clean(); //清屏
//                    }
                    Bundle bundle = msg.getData();
                    if (bundle.getInt("SPO") != 0 && bundle.getInt("PR") != 0 && bundle.getFloat("PI") != 0) {
                        tv_spo2.setText(bundle.getInt("SPO") + "");
                        tv_pr.setText(bundle.getInt("PR") + "");
                        tv_pi.setText(bundle.getFloat("PI") + "");
                    }
                    if (!bundle.getBoolean("bPro")) {
                        Toast.makeText(getActivity(), "探头脱落", Toast.LENGTH_SHORT).show();
//                        sv_spo2.clean(); //清屏
                        sv_spo2.setKeepScreenOn(true);//保持最后一组数据图形
                    }

                    /**
                     * socket数据部分
                     */
                    sendBuffer[9] = (byte) bundle.getInt("SPO");
                    sendBuffer[10] = (byte) bundle.getInt("PR");

                    float f = bundle.getFloat("PI");
                    Log.e("血氧数据", "" + f);
                    int IntH = (int) f;
                    int IntL = (int) (f * 10 - IntH * 10);

                    sendBuffer[11] = (byte) IntH;
                    sendBuffer[12] = (byte) IntL;

                    if (bundle.getBoolean("bPro") == false) {
                        sendBuffer[13] = 0x00;//脱落
                    } else {
                        sendBuffer[13] = 0x01;//未脱落
                    }
//                    sendBuffer[11] = Byte.parseByte(String.valueOf(bundle.getBoolean("bPro")));
                    sendBuffer[14] = (byte) bundle.getInt("mode");
                    sendBuffer[15] = XORUtil.getXORByte(sendBuffer);

                    /**
                     * 数据发送至服务器
                     */
                    if(sendBuffer[9]>0 && sendBuffer[10]>0 && f>0) {

                        Log.e("xueyangvalue", String.valueOf(sendBuffer[9]) + "---" + sendBuffer[10]);
                        if (SocketUtil.socket.isConnected()) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    SocketUtil.SendDataByte(sendBuffer);
                                }
                            }).start();
                        } else {
                            Toast.makeText(getActivity(), "socket未连接", Toast.LENGTH_SHORT);
                        }

                    }
                }
                break;
                case MSG_SPO2_PULSE: {
                    showSpO2Pulse(true);

                }
                break;
                case MSG_SPO2_PULSE_OFF: {
                    showSpO2Pulse(false);
                }
                break;


                default:
                    break;
            }
        }
    };

    /**
     * 设置SpO2搏动标记
     *
     * @param isShow
     */
    private void showSpO2Pulse(boolean isShow) {
        if (isShow) {
//            iv_spo2_pulse.setVisibility(View.VISIBLE);
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mXYANGCLHandler.sendEmptyMessage(MSG_SPO2_PULSE_OFF);
                }
            }.start();
        } else {
//            iv_spo2_pulse.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
//            case R.id.btn_test_xueyang:
//                if (MainActivity.mPC700 != null) {
//                    mSendCMDThread = MainActivity.mPC700.getSendCMDThread();
//                }
//                break;

//            case R.id.btn_save_xueyang:
//                /**
//                 * 数据发送至服务器
//                 */
//                Log.e("xueyangvalue", String.valueOf(sendBuffer[9]) + "---" + sendBuffer[10]);
//                if(SocketUtil.socket.isConnected()){
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            SocketUtil.SendDataByte(sendBuffer);
//                        }
//                    }).start();
//                }else {
//                    Toast.makeText(getActivity(),"socket未连接",Toast.LENGTH_SHORT);
//                }
//                break;
            default:
                break;
        }
    }
}
