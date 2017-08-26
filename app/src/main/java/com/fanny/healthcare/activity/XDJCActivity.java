package com.fanny.healthcare.activity;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.IdRes;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.creative.draw.ECG12SurfaceView;
import com.creative.drawWave.ECGView.ECGWaveView;
import com.creative.filemanage.PC700FileOperation;
import com.creative.pc700.ECG12Config;
import com.creative.pc700.PC700ECG12Thread;
import com.fanny.healthcare.R;
import com.fanny.healthcare.fragment.XYJC_TESTFragment;
import com.fanny.healthcare.util.SharePrefrenceUtil;
import com.fanny.healthcare.util.SocketUtil;
import com.fanny.healthcare.util.XORUtil;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static com.fanny.healthcare.fragment.XYJC_TESTFragment.MSG_DEVICE_VERSION;

public class XDJCActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "ECGTest";

    /**
     * 保存心电的文件操作
     */
    private PC700FileOperation fileOperation;
    //创建ECG目录
    private String filePath = Environment.getExternalStorageDirectory().getPath() + "/" + "ECG";
    private String fileName = "4";//保存心电的文件名
    private String filePath1=filePath+"/"+fileName+".ini";
    private String filePath2=filePath+"/"+fileName+".ECG";



    public static ECG12SurfaceView mECG12SurfaceView;
    private ImageButton btn_ecg_start;
    private Button btn_ecg_stop;
    private static TextView tv_heartRate;
    private static TextView tv_saturation;
    private static TextView tv_leadFall;
    private Button btn_filter;
    private Button btn_filter_param;
    private RadioGroup rg_speed;
    private RadioGroup rg_gain;
    private TextView tv_filter;
    private TextView tv_time;

    //socket上传的字节数据
    private byte[] sendBuffer = new byte[14];
    private Button btn_save;
    private DataInputStream in;
    private DataOutputStream out;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置当前activity常亮 必须放在setContentView之前
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_xdjc);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("心电检测");

        initData();
        initview();


    }

    private void initData() {
        sendBuffer[0] = (byte) 0xEA;
        sendBuffer[1] = (byte) 0xEB;

        sendBuffer[2] = (byte) 0x02;//data区字节长度为2个字节

        sendBuffer[3] = (byte) 0x00; //北京地区
        sendBuffer[4] = (byte) 0x0a;

        sendBuffer[5] = (byte) 0x02;//健康管家
        sendBuffer[6] = (byte) 0x04;//心电模块
        sendBuffer[7] = (byte) 0x00;//设备序列号
        sendBuffer[8] = (byte) 0x01;

        sendBuffer[10] = (byte) 0x00;//预留位
        sendBuffer[11] = (byte) 0x00;//校验码
        sendBuffer[12] = (byte) 0xE5;
        sendBuffer[13] = (byte) 0xD4;
    }

    private void initview() {

        mECG12SurfaceView = (ECG12SurfaceView) findViewById(R.id.ecg12_surfaceview);
        /**
         * 设置12导心电样式
         */
        setECG12Style(mECG12SurfaceView);

        btn_ecg_start = (ImageButton) findViewById(R.id.btn_ecg_start);
//        btn_ecg_stop = (Button) findViewById(R.id.btn_ecg_stop);
//        btn_filter = (Button) findViewById(R.id.bt_filter);
//        btn_filter_param = (Button) findViewById(R.id.bt_filterPara);
        btn_ecg_start.setOnClickListener(this);
//        btn_ecg_stop.setOnClickListener(this);
//        btn_filter.setOnClickListener(this);
//        btn_filter_param.setOnClickListener(this);

        tv_heartRate = (TextView) findViewById(R.id.tv_heart_rate);
//        tv_saturation = (TextView) findViewById(R.id.tv_saturation);
//        tv_leadFall = (TextView) findViewById(R.id.tv_leadFall);

        rg_speed = (RadioGroup) findViewById(R.id.rg_speed);
        rg_gain = (RadioGroup) findViewById(R.id.rg_gain);
        initRadioGrupView();

        tv_filter = (TextView) findViewById(R.id.tv_filter);
        tv_filter.setText("高通" + SmoothH + "Hz" + "/肌电" + SmoothL + "Hz" + "/工频" + SmoothP + "Hz");

        tv_time = (TextView) findViewById(R.id.tv_time);

        fileOperation = new PC700FileOperation(fileCallBack);

        btn_save = (Button) findViewById(R.id.btn_save_xindian);
        btn_save.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Log.d(TAG,"ECG 下位机唤醒");
        if (MainActivity.mPC700 != null) {
            MainActivity.mPC700.wakeUp();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Log.d(TAG,"ecg onPause--");
        if (MainActivity.mPC700 != null) {
            try {
                MainActivity.mPC700.stopECG12Measure();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (MainActivity.mPC700 != null) {
            MainActivity.mPC700.sleep(); //下位机休眠
        }
        if (fileOperation != null) {
            fileOperation.close();
            fileOperation = null;
        }
    }

    /**
     * 12导联心电，文件操作
     */
    PC700FileOperation.IFileOperateCallBack fileCallBack = new PC700FileOperation.IFileOperateCallBack() {
        @Override
        public void ECG12_ReplayTime(int time, int filesize) {
            new Thread(new TimeRunnable(MSG_REPLAY, time)).start();
        }

        @Override
        public void ECG12_HeartRateReplay(final int hr) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv_heartRate.setText("" + hr);

                    /**
                     * 心率动画
                     */
                    Alpha(tv_heartRate);

                    /**
                     * socket数据
                     */
                    sendBuffer[9] = (byte) hr;
                }
            });
        }

        @Override
        public void ECG12_ReplayConfig(ECG12Config ecg12Config) {
            if (mECG12SurfaceView != null) {
                mECG12SurfaceView.setSampleRate(ecg12Config.SampleRate);
                mECG12SurfaceView.setAdValue(ecg12Config.ADValue);
                mECG12SurfaceView.setSpeed(ECG12SurfaceView.ECGSPEED_2);
                mECG12SurfaceView.setGain(10);
                mECG12SurfaceView.setBackgroundColor(getResources().getColor(R.color.ecg_bg));
                mECG12SurfaceView.setWaveColor(getResources().getColor(R.color.ecg_wave));
                mECG12SurfaceView.setLeadTextColor(getResources().getColor(R.color.ecr_lead));
            }
        }

        @Override
        public void ECG12_ReplayData(ECG12Config.ECG12Data[] ecg12Datas, int baseValue) {
            if (mECG12SurfaceView != null) {
                mECG12SurfaceView.addWave(ecg12Datas, baseValue);
            }
        }

        @Override
        public void ECG12_SaveException(String s) {

        }

        @Override
        public void ECG12_ReplayException(String s) {

        }
    };

    private void initRadioGrupView() {
        findViewById(R.id.speed_0).setOnClickListener(this);
        findViewById(R.id.speed_1).setOnClickListener(this);
        findViewById(R.id.speed_2).setOnClickListener(this);
        findViewById(R.id.speed_3).setOnClickListener(this);

        findViewById(R.id.gain_0).setOnClickListener(this);
        findViewById(R.id.gain_1).setOnClickListener(this);
        findViewById(R.id.gain_2).setOnClickListener(this);
    }

    private void setECG12Style(ECG12SurfaceView ecg12Style) {
        ecg12Style.setWaveColor(Color.rgb(0, 100, 0));
        ecg12Style.setBackgroundColor(Color.WHITE);
        ecg12Style.setLeadTextColor(Color.BLUE);
        ecg12Style.setWavePaintWidth(2);

    }


    private boolean isRun=false;
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_ecg_start:
                if(!isRun){
                    try {
                        tv_heartRate.setText("- -");
                        MainActivity.mPC700.startECG12Measure();
                        btn_ecg_start.setBackgroundResource(R.mipmap.xdbtn_1);
                        isRun=!isRun;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else {
                    try {
                        btn_ecg_start.setBackgroundResource(R.mipmap.xdbtn_2);
                        MainActivity.mPC700.stopECG12Measure();
                        isRun=!isRun;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
//            case R.id.btn_ecg_stop:
//
//                try {
//                    MainActivity.mPC700.stopECG12Measure();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                break;
//            case R.id.bt_filter:
//                setFilter();
//                break;
//            case R.id.bt_filterPara:
//                setSmoothHParam();
//                break;
            case R.id.speed_0:
                setSpeed(ECGWaveView.ECGSPEED_0);
                break;
            case R.id.speed_1:
                setSpeed(ECGWaveView.ECGSPEED_1);
                break;
            case R.id.speed_2:
                setSpeed(ECGWaveView.ECGSPEED_2);
                break;
            case R.id.speed_3:
                setSpeed(ECGWaveView.ECGSPEED_3);
                break;
            case R.id.gain_0:
                setGain(5);
                break;
            case R.id.gain_1:
                setGain(10);
                break;
            case R.id.gain_2:
                setGain(20);
                break;
            case R.id.btn_save_xindian:
                /**
                 * 心率平均值 ——> 数据发送至服务器
                 */
                Log.e("xindianvalue", String.valueOf(sendBuffer[9]) + "---" + sendBuffer[10]);
                if (SocketUtil.socket.isConnected()) {
                    sendBuffer[10] = XORUtil.getXORByte(sendBuffer);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            SocketUtil.SendDataByte(sendBuffer);
                        }
                    }).start();
                } else {
                    Toast.makeText(XDJCActivity.this, "socket未连接", Toast.LENGTH_SHORT);
                }

                /**
                 * .ini 文件和 .ecg 文件 ——> 数据上传至服务器
                 */

                /**
                 * 创建文件，
                 */
                createIniFile();

                if (fileOperation != null) {
                    /**
                     * 上传操作
                     */

                    new Thread(new Runnable() {
                        @Override
                        public void run() {



                            /**
                             * 将创建的文件保存至本地
                             */
                            fileOperation.startSave(fileName, filePath);

                            ArrayList<String> files=new ArrayList<String>();
                            files.add(filePath1);
                            files.add(filePath2);

                            /**
                             * 延时一秒，等到文件保存本地成功
                             */
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            /**
                             * 再读取"文件"出来上传
                             */
//                            readIniFileTest1();
//                            readIniFileTest2();
                            /**
                             * 测试值为null 不知道为什么咧
                             */
//                            Map<String, String> patient = fileOperation.readIniFile(fileName, filePath, "Patient");
//                            if(patient!=null){
//                                Log.e("patient",""+patient.toString());
//                            }else {
//                                Log.e("patient","空值");
//                            }
                            if (SocketUtil.socket.isConnected() && files.size()>1) {

                                for(int i=0;i<files.size();i++){
                                    String path=files.get(i);
                                    //读取本地ini和ecg文件
                                    File file=new File(path);
                                    try {
                                        in = new DataInputStream(new BufferedInputStream(new FileInputStream(path)));
                                        out = new DataOutputStream(SocketUtil.socket.getOutputStream());
                                        out.writeUTF(file.getName());
                                        out.flush();
                                        out.writeLong((long)file.length());
                                        out.flush();
                                        int bufferSize=1024*8;
                                        byte[] buf=new byte[bufferSize];
                                        while (true){
                                            int read=0;
                                            if(in !=null){
                                                read= in.read(buf);
                                            }
                                            if(read==-1){
                                                break;
                                            }
                                            out.write(buf,0,read);
                                        }
                                        out.flush();
//                                        in.close();
                                        Log.e("FileSend","文件上传完成");
                                    } catch (Exception e) {
                                        Log.e("FileSend","文件上传失败");
                                        Log.e(TAG,""+e.getMessage());
                                        e.printStackTrace();
                                    }
//                                    finally {
//                                        if(out!=null){
//                                            try {
//                                                out.close();
//                                            } catch (IOException e) {
//                                                e.printStackTrace();
//                                            }
//                                        }
//                                    }
                                }

                            }else {
                                Log.e("FileSend","请再次保存");
                            }

//                            if(out!=null){
//                                try {
//                                    in.close();
//                                    out.flush();
//                                    out.close();
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                            }

                            }
                        }).start();
                    }
                    break;

                    default:
                        break;
                }
        }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if(out!=null){
//            try {
//                in.close();
//                out.flush();
//                out.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }

    private void setSpeed(float speed) {
        mECG12SurfaceView.setSpeed(speed);
        mECG12SurfaceView.screenClear();
    }

    private void setGain(int gain) {
        mECG12SurfaceView.setGain(gain);
    }


    /**
     * 滤波设置对话框
     */
    //控件
    private AlertDialog filterDialog = null;
    private RadioButton radio0 = null;
    private RadioButton radio12 = null;
    private Button dialog_button = null;
    private RadioButton radio4 = null;
    private RadioButton radio5 = null;
    private RadioButton radio6 = null;
    private RadioButton radio7 = null;
    private RadioButton radio8 = null;

    private RadioButton radio13 = null;
    private RadioButton radio14 = null;
    private RadioButton radio9 = null;
    private RadioButton radio10 = null;
    private RadioButton radio19 = null;

    //初始值
    private boolean LSmooth = true;//低通滤波开关
    private boolean HSmooth = true;//高通滤波开关
    private double SmoothH = 0.5;//高通滤波初始值
    private int SmoothL = 30;//低通滤波初始值
    private float SmoothP = 50.0f;//工频滤波初始值

    private void setFilter() {
        PC700ECG12Thread.setTranslateData(false);//暂停
        filterDialog =
                new AlertDialog.Builder(XDJCActivity.this, android.R.style.Theme_DeviceDefault_Light_DialogWhenLarge).setCancelable(false).create();
        filterDialog.show();
        Window window = filterDialog.getWindow();
        window.setContentView(R.layout.dialog_filter_info);
        /**
         * 初始化界面
         */

        radio0 = (RadioButton) window.findViewById(R.id.radio0);
        radio9 = (RadioButton) window.findViewById(R.id.radio9);
        radio4 = (RadioButton) window.findViewById(R.id.radio4);
        radio5 = (RadioButton) window.findViewById(R.id.radio5);
        radio6 = (RadioButton) window.findViewById(R.id.radio6);
        radio7 = (RadioButton) window.findViewById(R.id.radio7);
        radio8 = (RadioButton) window.findViewById(R.id.radio8);
        radio10 = (RadioButton) window.findViewById(R.id.radio10);
        radio19 = (RadioButton) window.findViewById(R.id.radio19);

        radio12 = (RadioButton) window.findViewById(R.id.radio12);
        radio13 = (RadioButton) window.findViewById(R.id.radio13);
        radio14 = (RadioButton) window.findViewById(R.id.radio14);
        dialog_button = (Button) window.findViewById(R.id.dialog_button);

        RadioGroup hFilterRadioGroup = (RadioGroup) window.findViewById(R.id.radioGroup1);

        /**
         * 高通滤波
         */
        if (HSmooth) {
            HSmooth = true;
            SmoothH = 0.5;
            hFilterRadioGroup.check(R.id.radio0);
        } else {
            hFilterRadioGroup.check(R.id.radio12);
        }

        hFilterRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                HSmooth = true;
                if (checkedId == radio0.getId()) {
                    SmoothH = 0.5;
                } else if (checkedId == radio12.getId()) {
                    SmoothH = 0;
                    HSmooth = false;
                }
                tv_filter.setText("高通" + SmoothH + "Hz" + "/肌电" + SmoothL + "Hz" + "/工频" + SmoothP + "Hz");
                PC700ECG12Thread.setSmoothDouble(true);
                setSmoothParase();
            }
        });

        /**
         * 低通滤波
         */
        RadioGroup LFilterRadioGroup = (RadioGroup) window.findViewById(R.id.radioGroup2);
        switch (SmoothL) {
            case 27:
                LSmooth = true;
                LFilterRadioGroup.check(R.id.radio4);
                break;
            case 30:
                LSmooth = true;
                LFilterRadioGroup.check(R.id.radio5);
                break;
            case 35:
                LSmooth = true;
                LFilterRadioGroup.check(R.id.radio6);
                break;
            case 75:
                LSmooth = true;
                LFilterRadioGroup.check(R.id.radio7);
                break;
            case 100:
                LSmooth = true;
                LFilterRadioGroup.check(R.id.radio8);
                break;
            case 150:
                LSmooth = true;
                LFilterRadioGroup.check(R.id.radio9);
                break;
            default:
                LFilterRadioGroup.check(R.id.radio13);
                LSmooth = false;
                break;
        }
        LFilterRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                LSmooth = true;
                if (checkedId == radio4.getId()) {
                    SmoothL = 27;
                } else if (checkedId == radio5.getId()) {
                    SmoothL = 30;
                } else if (checkedId == radio6.getId()) {
                    SmoothL = 35;
                } else if (checkedId == radio7.getId()) {
                    SmoothL = 75;
                } else if (checkedId == radio8.getId()) {
                    SmoothL = 100;
                } else if (checkedId == radio9.getId()) {
                    SmoothL = 150;
                } else if (checkedId == radio13.getId()) {
                    SmoothL = 0;
                    LSmooth = false;
                }
                tv_filter.setText("高通" + SmoothH + "Hz" + "/肌电" + SmoothL + "Hz" + "/工频" + SmoothP + "Hz");
                PC700ECG12Thread.setSmoothDouble(true);
                setSmoothParase();
            }
        });

        /**
         * 工频滤波
         */
        RadioGroup GPFilterRadioGroup = (RadioGroup) window.findViewById(R.id.radioGroup3);
        if (SmoothP == 50.0f) {
            GPFilterRadioGroup.check(R.id.radio10);//50.0f
        } else if (SmoothP == 60.0f) {
            GPFilterRadioGroup.check(R.id.radio19);//60.0f
        } else {
            GPFilterRadioGroup.check(R.id.radio14);//关
        }
        GPFilterRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                if (checkedId == radio10.getId()) {
                    SmoothP = 50.0f;
                } else if (checkedId == radio19.getId()) {
                    SmoothP = 60.0f;
                } else {
                    SmoothP = 0.0f;
                }
                tv_filter.setText("高通" + SmoothH + "Hz" + "/肌电" + SmoothL + "Hz" + "/工频" + SmoothP + "Hz");
                PC700ECG12Thread.setSmoothDouble(true);
                setSmoothParase();
            }
        });

        dialog_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PC700ECG12Thread.setTranslateData(true);
                filterDialog.dismiss();
            }
        });

    }

    /**
     * 初始化高通滤波参数
     */
    private void setSmoothParase() {
        double fpfilterflag_defval = 0;
        int fpfilterflag_ts = 0;
        int fpfilterflag_recover_ts = 0;
        float fpfilterflag_frq = 0;
        int flag = 0;
        flag = (Integer) SharePrefrenceUtil.getData(XDJCActivity.this, "flag", 0);
        String tag = (String) SharePrefrenceUtil.getData(XDJCActivity.this, "fpfilterflag_defval", "");
        if (tag != null && !tag.equals("")) {
            fpfilterflag_defval = Double.parseDouble(tag);
        }

        tag = (String) SharePrefrenceUtil.getData(XDJCActivity.this, "fpfilterflag_ts", "");
        if (tag != null && !tag.equals("")) {
            fpfilterflag_ts = Integer.valueOf(tag);
        }

        tag = (String) SharePrefrenceUtil.getData(XDJCActivity.this, "fpfilterflag_recover_ts", "");
        if (tag != null && !tag.equals("")) {
            fpfilterflag_recover_ts = Integer.valueOf(tag);
        }

        tag = (String) SharePrefrenceUtil.getData(XDJCActivity.this, "fpfilterflag_frq", "");
        if (tag != null && !tag.equals("")) {
            fpfilterflag_frq = Float.valueOf(tag);
        }

        PC700ECG12Thread.setFilterParam(SmoothL, LSmooth, HSmooth, SmoothP, flag, fpfilterflag_defval, fpfilterflag_ts, fpfilterflag_recover_ts, fpfilterflag_frq);
    }

    /**
     * 设置滤波参数
     */
    private int flag = 0;
    private RadioButton radio15 = null;
    private RadioButton radio16 = null;
    private Button fpfilterflag_sure = null;
    private EditText fpfilterflag_defval = null;//如果输入信号大于该值时自动复位高通滤波
    private EditText fpfilterflag_ts = null;//复位的延时时间
    private EditText fpfilterflag_recover_ts = null;//复位操作持续时间
    private EditText fpfilterflag_frq = null;//复位期间滤波器截止频率

    private void setSmoothHParam() {


        PC700ECG12Thread.setTranslateData(false);//

        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_DialogWhenLarge);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_filterparam, null);
        flag = (Integer) SharePrefrenceUtil.getData(this, "flag", 0);
        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.fpfilterflag);
        radio15 = (RadioButton) view.findViewById(R.id.fpfilterflagoff);
        radio16 = (RadioButton) view.findViewById(R.id.fpfilterflagon);
        if (flag == 0) {
            radioGroup.check(R.id.fpfilterflagoff);
        } else {
            radioGroup.check(R.id.fpfilterflagon);
        }
        fpfilterflag_defval = (EditText) view.findViewById(R.id.fpfilterflag_defval);
        fpfilterflag_ts = (EditText) view.findViewById(R.id.fpfilterflag_ts);
        fpfilterflag_recover_ts = (EditText) view.findViewById(R.id.fpfilterflag_recover_ts);
        fpfilterflag_frq = (EditText) view.findViewById(R.id.fpfilterflag_frq);
        fpfilterflag_sure = (Button) view.findViewById(R.id.fpfilterflag_sure);

        String tag = (String) SharePrefrenceUtil.getData(this, "fpfilterflag_defval", "300000");
        fpfilterflag_defval.setText(tag);
        tag = (String) SharePrefrenceUtil.getData(this, "fpfilterflag_ts", "30");
        fpfilterflag_ts.setText(tag);
        tag = (String) SharePrefrenceUtil.getData(this, "fpfilterflag_recover_ts", "5000");
        fpfilterflag_recover_ts.setText(tag);
        tag = (String) SharePrefrenceUtil.getData(this, "fpfilterflag_frq", "0.667");
        fpfilterflag_frq.setText(tag);

        fpfilterflag_sure.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.e("", "===" + fpfilterflag_defval.getText() + "|" + fpfilterflag_ts.getText());
                if (fpfilterflag_defval == null || fpfilterflag_defval.getText().toString().equals("") || fpfilterflag_ts == null || fpfilterflag_ts.getText().toString().equals("")
                        || fpfilterflag_recover_ts == null || fpfilterflag_recover_ts.getText().toString().equals("")
                        || fpfilterflag_frq == null || fpfilterflag_frq.getText().toString().equals("")) {
                    Toast.makeText(XDJCActivity.this, "不能为空", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    SharePrefrenceUtil.saveParam(XDJCActivity.this, "flag", flag);
                    SharePrefrenceUtil.saveParam(XDJCActivity.this, "fpfilterflag_defval", fpfilterflag_defval.getText().toString());
                    SharePrefrenceUtil.saveParam(XDJCActivity.this, "fpfilterflag_ts", fpfilterflag_ts.getText().toString());
                    SharePrefrenceUtil.saveParam(XDJCActivity.this, "fpfilterflag_recover_ts", fpfilterflag_recover_ts.getText().toString());
                    SharePrefrenceUtil.saveParam(XDJCActivity.this, "fpfilterflag_frq", fpfilterflag_frq.getText().toString());
                    setSmoothParase();// 初始化滤波器
                    PC700ECG12Thread.setTranslateData(true); // 继续预览波形
                    filterDialog.dismiss();
                }
            }
        });

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                if (checkedId == radio15.getId()) {
                    flag = 0;
                } else if (checkedId == radio16.getId()) {
                    flag = 1;
                }
            }
        });

        builder.setView(view);
        filterDialog = builder.show();
        filterDialog.setCancelable(false);

    }

    /**
     * 心率动画
     */
    public static void Alpha(View v) {
        AnimationSet animationSet = new AnimationSet(true);
        AlphaAnimation alphaAnimation = new AlphaAnimation((float) 0.3, 1);
        alphaAnimation.setDuration(500);
        animationSet.addAnimation(alphaAnimation);
        v.startAnimation(animationSet);
        animationSet = null;
        alphaAnimation = null;
    }

    public static TextView getHeartRateView() {
        return tv_heartRate;
    }

//    public static TextView getSaturationView() {
//        return tv_saturation;
//    }
//
//    public static TextView getLeadFallView() {
//        return tv_leadFall;
//    }

    public static final int MSG_ECG_WAVE = 20;
    public static final int MSG_ECG_STATUS_CH = 21;
    public static final int MSG_ECG_GAIN = 22;
    public static final int MSG_ECG_START_TIME = 23;


    /*************************************************************************
     *  设置  界面的测量时间time
     *  *******************************************************/
    int timeEnd = 0; //reduce
    int timeStart = 0; //add
    int minute = 0;
    int second = 0;
    String sMinute = "00";
    String sSecond = "00";
    private static final int MSG_SAVE = 1;
    private static final int MSG_REPLAY = 2;

    final Handler timeHandler = new Handler() {

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SAVE: {
                    timeStart++;
                    if (timeStart % 60 == 0) {
                        minute++;
                        if (minute < 10) {
                            sMinute = "0" + minute;
                        } else {
                            sMinute = "" + minute;
                        }
                    }
                    second = timeStart % 60;
                    if (second < 10) {
                        sSecond = "0" + second;
                    } else {
                        sSecond = "" + second;
                    }
                    tv_time.setText(sMinute + ":" + sSecond);
                }
                break;
                case MSG_REPLAY: {
                    timeEnd--;
                    if (timeEnd < 0) {
                        return;
                    }
                    minute = timeEnd / 60;
                    if (minute < 10) {
                        sMinute = "0" + minute;
                    } else {
                        sMinute = "" + minute;
                    }
                    second = timeEnd % 60;
                    if (second < 10) {
                        sSecond = "0" + second;
                    } else {
                        sSecond = "" + second;
                    }
                    tv_time.setText(sMinute + ":" + sSecond);
                }
                break;
                default:
                    break;
            }
            // super.handleMessage(msg);
        }
    };

    private boolean bStopTime = false;
    Message message = new Message();

    //心电存储回放计时器
    public class TimeRunnable implements Runnable {
        int what = 0;

        public TimeRunnable(int msg, int endTime) {
            what = msg;
            // init param
            timeEnd = endTime;
            timeStart = 0;
            minute = 0;
            second = 0;
            sMinute = "00";
            sSecond = "00";
            bStopTime = false;
        }

        @Override
        public void run() {
            while (!bStopTime) {
                try {
                    Thread.sleep(1000);     // sleep 1000ms
                    Message message = new Message();
                    message.what = what;
                    timeHandler.sendMessage(message);
                } catch (Exception e) {
                }
            }
        }
    }


    /*************************************************************************
     *  上传心电图文件的操作
     *  *******************************************************/

    /**
     * 创建.ini文件并设值
     * windows服务器插件通过.ini和.ecg文件来生成心电图片
     * 这里通过socket上传.ini和.ecg文件至服务器，由服务器进行图片生成
     */
    String var1 = "北京航天光华电子技术有限公司";
    String var2 = "1234568";
    String var3 = "李四";
    String var4 = "男";
    String var5 = "20";

    private void createIniFile() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        /**
         * 获取当前时间
         */
        String time = sdf.format(System.currentTimeMillis());

        //病人信息
        LinkedHashMap<String, String> inMap1 = new LinkedHashMap<>();
        fileOperation.setIniFileInfo(inMap1, ";", "医院/诊所/诊断中心", "origin", var1);
        fileOperation.setIniFileInfo(inMap1, ";", "病历号/ID/唯一标识", "id", var2);
        fileOperation.setIniFileInfo(inMap1, ";", "姓名", "name", var3);
        fileOperation.setIniFileInfo(inMap1, ";", "性别", "sex", var4);
        fileOperation.setIniFileInfo(inMap1, ";", "年龄", "age", var5);
        fileOperation.setIniFileInfo(inMap1, ";", "检查时间", "exmatime", time);
        fileOperation.setIniFileSection("Patient", inMap1);

        //心电参数
        LinkedHashMap<String, String> inMap2 = new LinkedHashMap<>();
        fileOperation.setIniFileInfo(inMap2, ";", "采样率", "samplerate", "500");
        fileOperation.setIniFileInfo(inMap2, ";", "AD单位", "advalue", "262");
        fileOperation.setIniFileInfo(inMap2, ";", "基线值", "baseline", "32768");
        fileOperation.setIniFileInfo(inMap2, ";", "通道数", "channels", "13");
        fileOperation.setIniFileInfo(inMap2, ";", "每通道字节数", "channelbyte", "4");
        fileOperation.setIniFileInfo(inMap2, ";", "分析通道(0表示第一通道)", "analysechannel", "1");
        fileOperation.setIniFileInfo(inMap2, ";", "通道描述", "channelname", "I II III aVR aVL aVF V1 V2 V3 V4 V5 V6 EVT");
        fileOperation.setIniFileSection("DataInfo", inMap2);

    }

    //读取生成.ini文件的例子
    public void readIniFileTest1() {
        Map<String, String> map = fileOperation.readIniFile(fileName, filePath, "Patient");
        for (Map.Entry<String, String> data : map.entrySet()) {
            Log.e(TAG, "key:" + data.getKey() + " value:" + data.getValue());
        }
    }

    //读取生成.ini文件的例子
    public void readIniFileTest2() {
        Map<String, String> map = fileOperation.readIniFile(fileName, filePath, "DataInfo");
        for (Map.Entry<String, String> data : map.entrySet()) {
            Log.e(TAG, "key:" + data.getKey() + " value:" + data.getValue());
        }
    }

}
