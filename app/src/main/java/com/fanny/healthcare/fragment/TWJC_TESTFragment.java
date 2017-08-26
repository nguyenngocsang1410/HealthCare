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
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.creative.pc700.SendCMDThread;
import com.fanny.healthcare.R;
import com.fanny.healthcare.activity.MainActivity;
import com.fanny.healthcare.util.SocketUtil;
import com.fanny.healthcare.util.XORUtil;
import com.jaredrummler.android.widget.AnimatedSvgView;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

/**
 * Created by Fanny on 17/6/28.
 */

public class TWJC_TESTFragment extends Fragment implements View.OnClickListener {

    private View view;
    private SendCMDThread mSendCMDThread;
    private TextView tv_temp;

    //socket上传的字节数据
    private byte[] sendBuffer = new byte[15];
    private Button btn_save_temp;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        view = LinearLayout.inflate(getContext(), R.layout.fragment_twjc_test, null);


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

        sendBuffer[2] = (byte) 0x03;//data区字节长度为3个字节

        sendBuffer[3] = (byte) 0x00; //北京地区
        sendBuffer[4] = (byte) 0x0a;

        sendBuffer[5] = (byte) 0x02;//健康管家
        sendBuffer[6] = (byte) 0x06;//体温模块
        sendBuffer[7] = (byte) 0x00;
        sendBuffer[8] = (byte) 0x01;//设备序列号

        sendBuffer[12] = (byte) 0x00;//校验码
        sendBuffer[13] = (byte) 0xE5;
        sendBuffer[14] = (byte) 0xD4;


    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        MainActivity.setUIHandler(mTWCLHandler);
        super.onActivityCreated(savedInstanceState);
    }

    private void initView() {
//        AnimatedSvgView svgView = (AnimatedSvgView) view.findViewById(R.id.animated_svg_view);
//        svgView.start();

        tv_temp = (TextView) view.findViewById(R.id.tv_templeture_value);
//        btn_save_temp = (Button) view.findViewById(R.id.btn_save_temp);
//        btn_save_temp.setOnClickListener(this);
    }


    public static final int MSG_TEMP = 24;


    private Handler mTWCLHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {

                case MSG_TEMP: {
//                    Log.d(TAG,"体温:" + msg.obj + " 单位:" + msg.arg2 + " 结果状态:" + msg.arg1);
                    String strTemp;
                    float temp;
                    if (msg.arg2 == 0) { // 摄氏度
                        temp = (float) msg.obj * 1.8f + 32;
                        String strF = String.format(Locale.US, "%.1f", (temp)); //保留1位小数
                        strTemp = msg.obj + " ℃" + "->" + strF + " ℉";
                    } else { //华氏度
                        temp = ((float) msg.obj - 32) / 1.8f;
                        strTemp = msg.obj + " ℉" + "->" + temp + " ℃";
                    }

//                    tv_temp.setText(strTemp);//显示两个单位
                    /**
                     * temp数据解析上传 例如temp＝36.6  (log打印结果为msg.arg2==0代表摄氏度)
                     * 或者数据保存本地
                     */

                    float f = (float) msg.obj;
                    tv_temp.setText("" + f);

                    Log.e("twjc", "体温结果：" + f);
                    /**
                     * 数据解析，获取整数位和小数位(十进制)
                     */
                    int IntH = (int) f;
                    int IntL = (int) (f * 10 - IntH * 10);
                    Log.e("twjc", "体温结果（十进制），整数位：" + IntH);
                    Log.e("twjc", "体温结果十进制），小数位：" + IntL);

                    /***
                     * 数据解析：十进制数据转换为16进制数据
                     */
                    String HexH = Integer.toHexString(IntH);
                    String HexL = Integer.toHexString(IntL);
                    Log.e("twjc", "体温结果（字符串），整数位：" + HexH);
                    Log.e("twjc", "体温结果（字符串），小数位：" + HexL);

                    /**
                     * 保存字节数据
                     */
//                    sendBuffer[9] = (byte) Integer.parseInt(HexH);
//                    sendBuffer[10] = (byte) Integer.parseInt(HexL);
                    Log.e("twjc", "体温结果（十六进制），整数位：" + Integer.parseInt(HexH));
                    Log.e("twjc", "体温结果（十六进制），小数位：" + Integer.parseInt(HexL));

                    sendBuffer[9] = (byte) IntH;
                    sendBuffer[10] = (byte) IntL;
                    sendBuffer[11] = 0x00;
                    //校验码的重新获取
                    sendBuffer[12] = XORUtil.getXORByte(sendBuffer);



                    /**
                     * 测量完毕后，自动数据上传至服务器
                     */
                Log.e("tempvalue", String.valueOf(sendBuffer[9]) + "---" + sendBuffer[10]);

                if (SocketUtil.socket.isConnected()) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            SocketUtil.SendDataByte(sendBuffer);
                        }
                    }).start();
                } else {
                    Toast.makeText(getActivity(), "检查网络，socket未连接", Toast.LENGTH_SHORT).show();
                }

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
//            case R.id.btn_save_temp:
//                /**
//                 * 数据发送至服务器
//                 */
//                Log.e("tempvalue", String.valueOf(sendBuffer[9]) + "---" + sendBuffer[10]);
//                if (SocketUtil.socket.isConnected()) {
//
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            SocketUtil.SendDataByte(sendBuffer);
//                        }
//                    }).start();
//                } else {
//                    Toast.makeText(getActivity(), "socket未连接", Toast.LENGTH_SHORT);
//                }
//                break;
            default:
                break;
        }
    }
}
