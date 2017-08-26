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

import java.util.Locale;

/**
 * Created by Fanny on 17/6/28.
 */

public class XTJC_TESTFragment extends Fragment implements View.OnClickListener{

    private View view;
    private SendCMDThread mSendCMDThread;
    private TextView tv_glu;
    private TextView tv_ua;
    private TextView tv_chol;
    private Spinner sp_glu_device;

    //socket上传的字节数据
    private byte[] sendBuffer=new byte[18];
    private Button btn_save;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = LinearLayout.inflate(getContext(), R.layout.fragment_xtjc_test,null);

        if(MainActivity.mPC700!=null){
            mSendCMDThread = MainActivity.mPC700.getSendCMDThread();
        }

        initData();
        initView();
        return view;
    }

    private void initData() {
        sendBuffer[0]= (byte) 0xEA;
        sendBuffer[1]= (byte) 0xEB;

        sendBuffer[2]= (byte) 0x06;//data区字节长度为6个字节

        sendBuffer[3]= (byte) 0x00; //北京地区
        sendBuffer[4]= (byte) 0x0a;

        sendBuffer[5]= (byte) 0x02;//健康管家
        sendBuffer[6]= (byte) 0x02;//血糖模块
        sendBuffer[7]= (byte) 0x00;//设备序列号
        sendBuffer[8]= (byte) 0x01;

        sendBuffer[15]= (byte) 0x00;//校验码
        sendBuffer[16]= (byte) 0xE5;
        sendBuffer[17]= (byte) 0xD4;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        MainActivity.setUIHandler(mXTCLHandler);
        super.onActivityCreated(savedInstanceState);
    }

    private void initView() {

        tv_glu = (TextView) view.findViewById(R.id.tv_glu_value);
        tv_ua = (TextView) view.findViewById(R.id.tv_ua_value);
        tv_chol = (TextView) view.findViewById(R.id.tv_chol_value);

//        sp_glu_device = (Spinner) view.findViewById(R.id.sp_glu_device);
//        setSpinnerListener();

        btn_save = (Button) view.findViewById(R.id.btn_save_xuetang);
        btn_save.setOnClickListener(this);

    }

//    private void setSpinnerListener() {
//        sp_glu_device.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                mSendCMDThread.glu_SetType((int) id+1);
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//
//            }
//        });
//        sp_glu_device.setSelection(1);
//    }


    public static final int MSG_GLU_TYPE=9;
    public static final int MSG_GLU=10;
    public static final int MSG_UA=11;
    public static final int MSG_CHOL=12;

    private Handler mXTCLHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {

                case MSG_GLU_TYPE:{
                    String str="";
                    if(msg.arg1 == 1){
                        str="怡成";
                    }else {
                        str="百捷";
                    }
                    Toast.makeText(getActivity(),str+"设置成功",Toast.LENGTH_SHORT).show();
                }
                break;
                case MSG_CHOL:{
                    String temp;
                    if (msg.arg2 == 0) {  // mmol/L
                        //temp = msg.obj + " mmol/L" + " ->" + String.format(Locale.US, "%.2f", (float)msg.obj * 38.67f ) + " mg/dl";
                        temp = msg.obj  + "->" + String.format(Locale.US, "%.2f", (float)msg.obj * 38.67f ); //保留2位小数

                    } else {
                        //temp = msg.obj + " mg/dl" + " ->" + String.format(Locale.US, "%.2f",(float)msg.obj / 38.67f) + " mmol/L";
                        temp = msg.obj + "->" + String.format(Locale.US, "%.2f",(float)msg.obj / 38.67f);
                    }

                    /**
                     * socket数据
                     */
                    float f= (float) msg.obj;
                    int IntH=(int)f;
                    int IntL= (int) (f*100-IntH*100);
                    sendBuffer[13]= (byte) IntH;
                    sendBuffer[14]= (byte) IntL;

                    if(msg.arg1 == 0){
                        temp = temp+"，正常";
                    }else  if(msg.arg1 == 1){
                        temp = temp+"，偏低";
                    }else {
                        temp = temp+"，偏高";
                    }
                    tv_chol.setText(temp);
                }
                break;
                case MSG_GLU:{
                    //Log.d(TAG,"血糖:" + msg.obj + " 结果:" + msg.arg1 + " 单位:" + msg.arg2);
                    String temp;
                    if(msg.arg2 == 0){ //mmol/L
                        //temp= msg.obj+" mmol/l"+" ->"+String.format(Locale.US, "%.1f",(float)msg.obj * 18f)+" mg/dl";
                        temp= msg.obj+"->"+ String.format(Locale.US, "%.1f",(float)msg.obj * 18f);

                    }else{ //mg/dl
                        //temp=msg.obj+" mg/dl"+" ->"+String.format(Locale.US, "%.1f",(float)msg.obj / 18f)+" mmol/l";
                        temp=msg.obj+"->"+ String.format(Locale.US, "%.1f",(float)msg.obj / 18f);
                    }

                    /**
                     * socket数据
                     */
                    float f= (float) msg.obj;
                    int IntH=(int)f;
                    int IntL= (int) (f*100-IntH*100);
                    sendBuffer[9]= (byte) IntH;
                    sendBuffer[10]= (byte) IntL;


                    if(msg.arg1 == 0){
                        temp = temp+"，正常";
                    }else  if(msg.arg1 == 1){
                        temp = temp+"，偏低";
                    }else {
                        temp = temp+"，偏高";
                    }
                    tv_glu.setText(temp);
                }
                break;
                case MSG_UA:{ // 正常参考值,  男：149～416umol/L。女：89～357umo1/L ,
                    String temp;
                    float uaMmol;
                    int sex =0;

                    if (msg.arg2 == 0) { // mmol/L
                        uaMmol = (float) msg.obj;
                        //temp = msg.obj + " mmol/L" + " ->" + String.format(Locale.US, "%.2f",(float)msg.obj * 16.81f) + " mg/dl";
                        temp = msg.obj + "->" + String.format(Locale.US, "%.2f",(float)msg.obj * 16.81f);
                    } else {
                        uaMmol = (float)msg.obj / 16.81f;
                        //temp = msg.obj + " mg/dl" + " ->" + String.format(Locale.US, "%.2f",uaMmol) + " mmol/L";
                        temp = msg.obj + "->" + String.format(Locale.US, "%.2f",uaMmol);
                    }

                    /**
                     * socket数据
                     */
                    float f= (float) msg.obj;
                    int IntH=(int)f;
                    int IntL= (int) (f*100-IntH*100);
                    sendBuffer[11]= (byte) IntH;
                    sendBuffer[12]= (byte) IntL;


                    float uaUmol = uaMmol *1000;
                    if(sex ==0){ // 男
                        if(uaUmol<149){
                            temp = temp+" ,偏低";
                        }else if(uaUmol>416){
                            temp = temp+" ,偏高";
                        }else {
                            temp = temp+" ,正常";
                        }
                    }else { //女
                        if(uaUmol<89){
                            temp = temp+" ,偏低";
                        }else if(uaUmol>357){
                            temp = temp+" ,偏高";
                        }else {
                            temp = temp+" ,正常";
                        }
                    }
                    tv_ua.setText(temp);
                }
                break;



                default:
                    /**
                     * 数据发送至服务器
                     */
//                    if(!tv_ua.getText().toString().equals("") && !tv_glu.getText().toString().equals("") && !tv_chol.getText().toString().equals("")){
//                        Log.e("xuetangvalue", String.valueOf(sendBuffer[9])+"---"+sendBuffer[10]);
//                        if(SocketUtil.socket.isConnected()){
//                            sendBuffer[15]= XORUtil.getXORByte(sendBuffer);
//                            new Thread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    SocketUtil.SendDataByte(sendBuffer);
//                                }
//                            }).start();
//                        }else {
//                            Toast.makeText(getActivity(),"socket未连接",Toast.LENGTH_SHORT);
//                        }
//                    }


                    break;
            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_save_xuetang:
                /**
                 * 数据发送至服务器
                 */
                Log.e("xuetangvalue", String.valueOf(sendBuffer[9])+"---"+sendBuffer[10]);
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
}
