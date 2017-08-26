package com.fanny.healthcare.fragment;

import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.creative.base.InputStreamReader;
import com.creative.base.Ireader;
import com.creative.base.Isender;
import com.creative.base.OutputStreamSender;
import com.creative.bc401.BC401;
import com.creative.bc401.IBC401CallBack;
import com.creative.bc401.SendCMDThread;
import com.creative.bc401.UIData;

import com.fanny.healthcare.R;
import com.fanny.healthcare.activity.MainActivity;
import com.fanny.healthcare.util.MyBluetooth;
import com.fanny.healthcare.util.SharePrefrenceUtil;

import java.io.IOException;
import java.util.List;

/**
 * BC401 尿液分析仪 ,蓝牙连接
 * 不需要唤醒下位机
 */
public class BC401Fragment extends Fragment {
    public static final String TAG = "BC401Fragment";

    private TextView tv_URO, tv_BLD, tv_BIL, tv_KET, tv_LEU,tv_GLU, tv_PRO,
            tv_PH, tv_NIT, tv_SG, tv_VC, tv_Time, tv_BluetoothState ,tv_BondSN;

    private Button btn_BlueConn, btn_GetData, btn_Clear, btn_SynTime, btn_Measure,
            btn_Off, btn_Del,btn_UnBondDev;

    private ProgressBar pb_connect;

    private MyBluetooth myBluetooth;
    private static BC401Fragment mInstance;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_bc401, container, false);
        mInstance = this;
        initView(v);
        initBluetooth();
        return v;
    }

    public static  BC401Fragment getInstance(){
        return  mInstance;
    }

    private void initBluetooth() {
        // 初始化蓝牙操作
        myBluetooth = new MyBluetooth(getActivity(), mHandler);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                //--蓝牙状态
                case MyBluetooth.MSG_BLUETOOTH_OPENING: {
                    tv_BluetoothState.setText(getString(R.string.bluetooth_open));
                }
                    break;
                case MyBluetooth.MSG_BLUETOOTH_OPENING_FAIL: {
                    if(pb_connect!=null){
                        pb_connect.setVisibility(View.GONE);
                    }
                    tv_BluetoothState.setText(getString(R.string.bluetooth_open_fail) );
                }
                    break;
                case MyBluetooth.MSG_BLUETOOTH_DISCOVERYING: {
                    tv_BluetoothState.setText(getString(R.string.bluetooth_discoverying));
                }
                    break;
                case MyBluetooth.MSG_BLUETOOTH_CONNECTING: {
                    tv_BluetoothState.setText(getString(R.string.bluetooth_connecting));
                }
                    break;
                case MyBluetooth.MSG_BLUETOOTH_CONNECTED: {

                    if(pb_connect!=null){
                        pb_connect.setVisibility(View.GONE);
                    }

                    tv_BluetoothState.setText(getString(R.string.bluetooth_connected));
                    startReceiveData(true, MyBluetooth.mLocalSocket);
                }
                    break;
                case MyBluetooth.MSG_BLUETOOTH_CONNECT_FAIL: {

                    if(pb_connect!=null){
                        pb_connect.setVisibility(View.GONE);
                    }

                    tv_BluetoothState.setText(getString(R.string.bluetooth_connect_fail));
                }
                    break;
                case MyBluetooth.MSG_BLUETOOTH_DISCOVERYED: {
                    tv_BluetoothState.setText(getString(R.string.bluetooth_discoveryed));
                    if(pb_connect!=null){
                        pb_connect.setVisibility(View.GONE);
                    }
                }
                    break;
                case MyBluetooth.MSG_BLUETOOTH_SEARCH_TIMEOUT:{
                    if(pb_connect!=null){
                        pb_connect.setVisibility(View.GONE);
                    }
                    tv_BluetoothState.setText(getString(R.string.bluetooth_discovery_time_out));
                }
                    break;
                //---数据部分
                case MSG_ONE_DATA:{
                    UIData data = (UIData) msg.obj;
                    setTVtext(tv_BIL, ""+data.getBil());
                    setTVtext(tv_URO, ""+data.getUro());
                    setTVtext(tv_BLD, ""+data.getBld());
                    setTVtext(tv_KET, ""+data.getKet());
                    setTVtext(tv_LEU, ""+data.getLeu());
                    setTVtext(tv_GLU, ""+data.getGlu());
                    setTVtext(tv_PRO, ""+data.getPro());
                    setTVtext(tv_PH, ""+data.getPh());
                    setTVtext(tv_NIT, ""+data.getNit());
                    setTVtext(tv_SG, ""+data.getSg());
                    setTVtext(tv_VC, ""+data.getVc());
                    setTVtext(tv_Time, ""+data.getTime());
                }
                    break;
                case MSG_ALL_DATA:{

                }
                    break;
                case MSG_NO_DATA:{
                    Toast.makeText(getActivity(), getString(R.string.no_data), Toast.LENGTH_SHORT).show();
                }
                    break;
                case MSG_DEL_DATA_SUCCESS:{
                    Toast.makeText(getActivity(), getString(R.string.del_data_success), Toast.LENGTH_SHORT).show();
                }
                    break;
                case MSG_SYN_TIME_SUCCESS:{
                    Toast.makeText(getActivity(), getString(R.string.synch_time_success), Toast.LENGTH_SHORT).show();
                }
                    break;
                case MSG_BOND_DEV_SN:{
//                    tv_BondSN.setText((String)msg.obj );
                }
                    break;
                default: break;
            }
        }
    };

    private void initView(View view){
        tv_URO = (TextView) view.findViewById(R.id.tvUro);
        tv_BLD = (TextView) view.findViewById(R.id.tvBld);
        tv_BIL = (TextView) view.findViewById(R.id.tvBil);
        tv_KET = (TextView) view.findViewById(R.id.tvKet);
        tv_LEU = (TextView) view.findViewById(R.id.tvLeu);
        tv_GLU = (TextView) view.findViewById(R.id.tvGlu);
        tv_PRO = (TextView) view.findViewById(R.id.tvPro);
        tv_PH = (TextView) view.findViewById(R.id.tvPh);
        tv_NIT = (TextView) view.findViewById(R.id.tvNit);
        tv_SG = (TextView) view.findViewById(R.id.tvSg);
        tv_VC = (TextView) view.findViewById(R.id.tvVc);
        tv_Time = (TextView) view.findViewById(R.id.tvTime);
        tv_BluetoothState = (TextView) view.findViewById(R.id.tvBluetoothState);

        pb_connect= (ProgressBar) view.findViewById(R.id.pb_blu_con);

        /**
         * v1.0版本暂不绑定序列号
         */
//        tv_BondSN = (TextView) view.findViewById(R.id.tvDevSN);

        btn_BlueConn = (Button) view.findViewById(R.id.btnStart);
        btn_GetData = (Button) view.findViewById(R.id.btnGetData);
        btn_Clear = (Button) view.findViewById(R.id.btnClear);
        btn_SynTime = (Button) view.findViewById(R.id.btnSynTime);
        btn_Measure = (Button) view.findViewById(R.id.btnMeasure);
        btn_Off = (Button) view.findViewById(R.id.btnOffDev);
        btn_Del = (Button) view.findViewById(R.id.btnDel);
        btn_UnBondDev = (Button) view.findViewById(R.id.btnUnbond);

        btn_BlueConn.setOnClickListener(listener);
        btn_GetData.setOnClickListener(listener);
        btn_Clear.setOnClickListener(listener);
        btn_UnBondDev.setOnClickListener(listener);
        btn_SynTime.setOnClickListener(listener);
        btn_Measure.setOnClickListener(listener);
        btn_Off.setOnClickListener(listener);
        btn_Del.setOnClickListener(listener);

//        String devSN = (String) SharePrefrenceUtil.getData(getActivity(),"device_sn","");
//        tv_BondSN.setText(devSN);
    }

    private View.OnClickListener listener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnStart: {
//                    String devSN = (String) SharePrefrenceUtil.getData(getActivity(),"device_sn","");
//                    if(TextUtils.isEmpty(devSN)){
//                        showSNInputDialog();
//                    }else {
//                        if(myBluetooth!=null){
//                            myBluetooth.startDiscovery(devSN);
//                        }
//                    }

                    if(MyBluetooth.blueStatus!=MyBluetooth.BLU_STATUS_CONNECTED){
                        pb_connect.setVisibility(View.VISIBLE);
                        if(myBluetooth!=null){
                            myBluetooth.startDiscoveryConn();
                        }
                    }


                }
                break;
                case R.id.btnGetData: {
                    /**
                     * 说明：
                     * 1. app端主动连接尿液仪时，是主从关系。
                     * 试纸测试过程中，蓝牙会断开，出结果几秒后，尿液仪会主动连接app端，
                     * 并自动上传数据，这时蓝牙是从主关系。
                     * 2.  bc401 按2次“OK”键立即测试，bc401待机过程中也会自动断开蓝牙，
                     *
                     */

                    //SendCMDThread.getAllTransData();
                    SendCMDThread.getOneTransData();
                }
                break;
                case R.id.btnClear: {
                    clearTextView();
                }
                break;
                case R.id.btnSynTime: {
                    //SendCMDThread.getDeviceTime();
                    SendCMDThread.sendSynTime();
                }
                break;
                case R.id.btnOffDev: {
                    SendCMDThread.sendOffDev();
                }
                break;
                case R.id.btnMeasure: {
                    SendCMDThread.startMeasure();
                }
                break;
                case R.id.btnDel: {
                    SendCMDThread.sendDelData();
                }
                break;
                case R.id.btnUnbond:{ //解除SN绑定,可以连接其他尿液分析仪
//                    tv_BondSN.setText("");
//                    SharePrefrenceUtil.saveParam(getActivity(), "device_sn", "");
                    disConnect();
                }
                    break;
                default:break;
            }
        }
    };

    private void clearTextView(){
        setTVtext(tv_BIL, "--");
        setTVtext(tv_URO, "--");
        setTVtext(tv_BLD, "--");
        setTVtext(tv_KET, "--");
        setTVtext(tv_LEU, "--");
        setTVtext(tv_GLU, "--");
        setTVtext(tv_PRO, "--");
        setTVtext(tv_PH, "--");
        setTVtext(tv_NIT, "--");
        setTVtext(tv_SG, "--");
        setTVtext(tv_VC, "--");
        setTVtext(tv_Time,"--");
//        setTVtext(tv_BluetoothState,"--");
    }

    private void setTVtext(TextView tv, String msg) {
        if (tv != null && msg !=null) {
            if ("0".equals(msg) || "".equals(msg) || "0.0".equals(msg)) {
                tv.setText("--");
            } else {
                tv.setText(msg);
            }
        }
    }

    /**
     * 开始接收设备数据
     */
    public  void startReceiveData(boolean start,BluetoothSocket socket){
        if (start) {
            try {
                if (socket != null) {
                    String conDeviceName = socket.getRemoteDevice().getName();
                    Log.d(TAG, "startReceiveSocket name->"+conDeviceName);

                    InputStreamReader reader = new InputStreamReader(socket.getInputStream());
                    OutputStreamSender sender = new OutputStreamSender(socket.getOutputStream());
                    //启动接收
                    startReceive( conDeviceName, reader,sender);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if(socket!=null){
                try {
                    Log.d(TAG, "startReceiveData socket->close");
                    socket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }finally {
                    socket=null;
                }
            }
            stopReceive();
        }
    }

    //停止搜索
    private  void cancelDiscovery(){
        if(myBluetooth!=null){
            myBluetooth.stopDiscovery();
        }
    }

    //断开蓝牙连接
    private void disConnect(){
        if(myBluetooth!=null){
            myBluetooth.disConnected();
            startReceiveData(false, MyBluetooth.mRemoteSocket);
            startReceiveData(false, MyBluetooth.mLocalSocket);
        }
    }

    private BC401 bc401;
    public void startReceive(String blueName, Ireader iReader, Isender iSender){
        stopReceive();
        bc401 = new BC401(iReader, iSender, new BC401CallBack());
        bc401.startState();
    }

    public  void stopReceive(){
        if(bc401 !=null){
            bc401.stopState();
            bc401 = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(MainActivity.mPC700!=null){
            MainActivity.mPC700.Pause();
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopReceive();
        if(myBluetooth!=null){
            myBluetooth.disConnected();
        }
        myBluetooth =null;
        if(MainActivity.mPC700!=null){
            MainActivity.mPC700.Continue();
        }
    }

    /* 获取一条数据 */
    public static final byte MSG_ONE_DATA =0x21;
    /* 获取全部数据 */
    public static final byte MSG_ALL_DATA =0x22;
    /* 没有记录*/
    public static final byte MSG_NO_DATA =0x23;
    /* 同步时间成功*/
    public static final byte MSG_SYN_TIME_SUCCESS =0x24;
    /* 删除成功*/
    public static final byte MSG_DEL_DATA_SUCCESS =0x25;
    /* 绑定设备序列号 */
    public static final byte MSG_BOND_DEV_SN =0x26;

    private  class BC401CallBack implements IBC401CallBack {

        @Override
        public void onDevAck() {

        }

        @Override
        public void onSynTimeSuccess() {
            //Log.d(TAG, "onSynTime Success");
            mHandler.sendEmptyMessage(MSG_SYN_TIME_SUCCESS);
        }

        @Override
        public void onReadTime(String s) {

        }

        @Override
        public void onDevStartMeasure() {

        }

        @Override
        public void onDelDataSuccess() {
                mHandler.sendEmptyMessage(MSG_DEL_DATA_SUCCESS);
        }

        @Override
        public void onGetOneData(UIData data) {
            if(data !=null){

                Log.e(TAG, "id:"+data.getId()+" user:"+data.getUser()+" time:"+data.getTime()+
                        " item:"+data.getItem() +" uro:"+data.getUro()+" bld:"+data.getBld()+" bil:"+data.getBil()+" ket:"+
                        data.getKet()+" glu:"+ data.getGlu()+" pro:"+data.getPro()+" ph:"+data.getPh()+" nit:"+data.getNit()+
                        " leu:"+data.getLeu()+" sg:"+data.getSg() +" vc:"+data.getVc());

                mHandler.obtainMessage(MSG_ONE_DATA, data).sendToTarget();
            }else{
                mHandler.sendEmptyMessage(MSG_NO_DATA);
            }
        }

        @Override
        public void onGetAllData(List<UIData> dataList) {
            if(dataList !=null){

                for(UIData data: dataList){
                    Log.e(TAG, "id:"+data.getId()+" user:"+data.getUser()+" time:"+data.getTime()+
                            " item:"+data.getItem() +" uro:"+data.getUro()+" bld:"+data.getBld()+" bil:"+data.getBil()+" ket:"+
                            data.getKet()+" glu:"+ data.getGlu()+" pro:"+data.getPro()+" ph:"+data.getPh()+" nit:"+data.getNit()+
                            " leu:"+data.getLeu()+" sg:"+data.getSg() +" vc:"+data.getVc());
                }
                Log.e(TAG, "count->"+dataList.size());

                mHandler.obtainMessage(MSG_ONE_DATA, dataList.get(0)).sendToTarget(); //获取最新的一条记录
            }else{
                mHandler.sendEmptyMessage(MSG_NO_DATA);
            }
        }

        @Override
        public void onException(String s) {

        }

        @Override
        public void OnConnectLose() {
            mHandler.sendEmptyMessage(MyBluetooth.MSG_BLUETOOTH_CONNECT_FAIL);
        }
    }

    /**
     * 当多台尿液分析仪打开时，一体机不知道会连接哪台，所以加SN，指定连接设备
     */
    private void showSNInputDialog(){
        final EditText editText = new EditText(getActivity());
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.input_urine_sn))
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(editText)
                .setPositiveButton(getString(R.string.ok),new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String devSN = editText.getText().toString();
                        if(!TextUtils.isEmpty(devSN) && TextUtils.isDigitsOnly(devSN) && devSN.length() == 4){
                            //Log.d(TAG,"devSN:"+devSN);
                            SharePrefrenceUtil.saveParam(getActivity(), "device_sn", devSN);
                            mHandler.obtainMessage(MSG_BOND_DEV_SN,devSN).sendToTarget();
                            if(myBluetooth!=null){
                                myBluetooth.startDiscovery(devSN);
                            }
                        }else {
                            Toast.makeText(getActivity(),getString(R.string.input_sn_err), Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }


}
