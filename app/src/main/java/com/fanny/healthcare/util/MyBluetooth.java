package com.fanny.healthcare.util;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.creative.bc401.SendCMDThread;
import com.creative.bluetooth.BluetoothOpertion;
import com.creative.bluetooth.BluetoothOpertion.ExceptionCode;
import com.creative.bluetooth.IBluetoothCallBack;
import com.fanny.healthcare.fragment.BC401Fragment;

import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 *  BC401蓝牙操作
 */
public class MyBluetooth {
	private static final String TAG = "MyBluetooth";

	public String mDevSN; //设备名后缀序列号
	private BluetoothDevice mConnectedDevice = null;
	
	/* SDK中的蓝牙操作类 */
	private static BluetoothOpertion mBluetoothOper;
	/* 蓝牙是否连接成功 */
	public static  boolean bConnected = false;		
	/* 手动取消搜索*/
	public boolean bCancelFind = false;
	/* 本地主动连接上设备的socket*/
	public static BluetoothSocket mLocalSocket;
	/* 蓝牙设备主动连接本地的socket*/
	public static BluetoothSocket mRemoteSocket;

	
	//------------ bluetooth msg ---------------------
	
	/* 正在打开蓝牙 */
	public static final byte MSG_BLUETOOTH_OPENING = 0x00;
	/* 搜索设备  */
	public static final byte MSG_BLUETOOTH_DISCOVERYING = 0x01;
	/* 正在连接设备 */
	public static final byte MSG_BLUETOOTH_CONNECTING = 0x02;
	/* 连接成功  */
	public static final byte MSG_BLUETOOTH_CONNECTED = 0x03;
	/*连接失败 */
	public static final byte MSG_BLUETOOTH_CONNECT_FAIL = 0x04;
	/* 打开蓝牙失败 */
	public static final byte MSG_BLUETOOTH_OPENING_FAIL = 0x05;
	/* 搜索完成  */
	public static final byte MSG_BLUETOOTH_DISCOVERYED = 0x06;
	/* 搜索超时  */
	public static final byte MSG_BLUETOOTH_SEARCH_TIMEOUT = 0x07;


	//------------bluetooth status ---------------------
	
	/* 当前蓝牙状态——正常 */
	public static final int BLU_STATUS_NORMAL = 0;
	/* 当前蓝牙状态——搜索中  */
	public static final int BLU_STATUS_DISCOVERING = 1;
	/* 蓝牙状态————搜索完成 */
//	public static final int BLU_STATUS_DISCOVERYED = 2;
	/* 当前蓝牙状态——连接中  */
	public static final int BLU_STATUS_CONNECTING = 3;
	/* 当前蓝牙状态——连接上 */
	public static final int BLU_STATUS_CONNECTED = 4;
	/* 当前蓝牙状态 */
	public static int blueStatus = 0;
	/* 用于通知的Handler */
	private Handler mHandler;

	public MyBluetooth(Context context, Handler handler) {
		mHandler = handler;
		mBluetoothOper = new BluetoothOpertion(context, new MyBluetoothCallBack());
	}
	
	/**
	 * 连接指定的设备
	 * @param devSN
	 */
	public void startDiscovery(String devSN) {
		mDevSN = devSN;
		if(mBluetoothOper.getBluetoothAdapter()!=null){
			blueStatus = BLU_STATUS_NORMAL;
			startDiscoveryConn();
		}
	}
	
	
	/**
	 * 搜索蓝牙设备(经典蓝牙 2.0)
	 */
	public void startDiscoveryConn() {
		if (blueStatus == BLU_STATUS_NORMAL){
			mLocalSocket = null;
			if(!openBluetooth()){
				return;
			}

			Set<BluetoothDevice> bondDev = mBluetoothOper.getBondedDevices();
			if (bondDev != null && bondDev.size() > 0){

				Log.e("寻找设备",".........");

				for(BluetoothDevice blueDevice: bondDev){

					Log.e("发现设备：",""+blueDevice.getName());

					if(checkName(blueDevice.getName())){
						Log.e(TAG, "正在连接 name connect->1");
						bDiscovery = false;							
						blueStatus = BLU_STATUS_CONNECTING; 
						mHandler.sendEmptyMessage(MSG_BLUETOOTH_CONNECTING);
						mBluetoothOper.connect(blueDevice);
						return;						
					}
				}
			}
			Log.e(TAG, "未配对 ，discovery->2");
			blueStatus = BLU_STATUS_DISCOVERING;
			mHandler.sendEmptyMessage(MSG_BLUETOOTH_DISCOVERYING);
			mBluetoothOper.discovery();
			bDiscovery = true;
		}else {
			Log.e(TAG, "蓝牙状态为："+blueStatus);
		}
	}
	
	
	/* 开启蓝牙打开超时的定时器 */
	private Timer openBlueTimer;
	/* 打开蓝牙是否超时 */
	private boolean bOpenBlueTimeOut = false;	
	/* 本次是否有过搜索	*/
	private boolean bDiscovery = false;
	
	
	/**
	 * 打开手机蓝牙
	 * @return
	 */
	private boolean openBluetooth(){
		bOpenBlueTimeOut = false;
		if(!mBluetoothOper.open()){
			mHandler.sendEmptyMessage(MSG_BLUETOOTH_OPENING);
			mBluetoothOper.open();
			
			openBlueTimer = new Timer();
			openBlueTimer.schedule(new TimerTask() {
				
				@Override
				public void run() {
					bOpenBlueTimeOut = true;
					mHandler.sendEmptyMessage(MSG_BLUETOOTH_OPENING_FAIL);				
				}
			}, 10 * 1000);
			
			while(!mBluetoothOper.isOpen() && !bOpenBlueTimeOut){
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			openBlueTimer.cancel();
		}
		
		return !bOpenBlueTimeOut;
	}
	
	/**
	 * 取消搜索
	 */
	public void stopDiscovery(){
		if(blueStatus == BLU_STATUS_DISCOVERING
				|| blueStatus == BLU_STATUS_CONNECTING){
			bDiscovery = true;
			mBluetoothOper.stopDiscovery();
			bCancelFind = true;			
			bConnected = false;
			blueStatus = BLU_STATUS_NORMAL;
		}
	}

	public boolean checkName(String devName){
		//此处将"BC01"修改为了"EMP-Ui",取消设备序列号的验证条件
//		if(!TextUtils.isEmpty(devName) && devName.contains("EMP") && devName.length()>4 &&
//				devName.substring(devName.length()-4 ,devName.length()).equals(mDevSN)){
		if(!TextUtils.isEmpty(devName) && devName.contains("EMP") && devName.length()>4){
			return  true;
		}else
			return false;
	}
	
	/**
	 * 断开与当前设备的连接
	 */
	public void disConnected() {
		if (mLocalSocket != null && bConnected) {
			bConnected = false;
			blueStatus = BLU_STATUS_NORMAL;
			mBluetoothOper.disConnect(mLocalSocket);
		}
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private class MyBluetoothCallBack implements IBluetoothCallBack {

		@Override
		public void onFindDevice(BluetoothDevice device) {
			if(device!=null){
				String devName = device.getName();
				Log.d(TAG, "onFindDevice ->"+devName);
				if(checkName(devName)){
					blueStatus = BLU_STATUS_CONNECTING;
					mHandler.sendEmptyMessage(MSG_BLUETOOTH_CONNECTING);
					mBluetoothOper.connect(device);
				}
			}
		}

		@Override
		public void onDiscoveryCompleted(List<BluetoothDevice> devices) {
			if (blueStatus != BLU_STATUS_CONNECTING && blueStatus != BLU_STATUS_CONNECTED) {
				blueStatus = BLU_STATUS_NORMAL;
				mHandler.sendEmptyMessage(MSG_BLUETOOTH_DISCOVERYED);
			}
		}

		@Override
		public void onConnected(BluetoothSocket socket) {
			blueStatus = BLU_STATUS_CONNECTED;
			bConnected = true;
			if(socket!=null){
				mLocalSocket = socket;
				mConnectedDevice =mLocalSocket.getRemoteDevice();
			}
			
			Log.d(TAG, "onConnected listen-->1");
			mBluetoothOper.listenConnectLoacalDevice(mConnectedDevice.getName());
			
			if(bCancelFind){
				blueStatus = BLU_STATUS_NORMAL;
				disConnected();
				bConnected = false;
				bCancelFind = false;
			}else{			
				mHandler.sendEmptyMessage(MSG_BLUETOOTH_CONNECTED);
			}
		}

		@Override
		public void onConnectFail(String err) {
			mLocalSocket = null;
			if (bDiscovery) {
				blueStatus = BLU_STATUS_NORMAL;
				mHandler.sendEmptyMessage(MSG_BLUETOOTH_CONNECT_FAIL);
			} else {				
				blueStatus = BLU_STATUS_DISCOVERING;
				bDiscovery = true;
				mHandler.sendEmptyMessage(MSG_BLUETOOTH_DISCOVERYING);
				//Log.d(TAG,"onConnectFail,调用 discovery()->");
				mBluetoothOper.discovery();
			}																	
		}

		@Override
		public void onException(int exception) {
			switch (exception) {
			case ExceptionCode.NOBLUETOOTHADAPTER:
					Log.d(TAG, "设备无蓝牙");
				break;
			case ExceptionCode.DISCOVERYTIMEOUT:{
				Log.d(TAG, "onException 搜索超时");
				blueStatus = BLU_STATUS_NORMAL;
				mHandler.sendEmptyMessage(MSG_BLUETOOTH_SEARCH_TIMEOUT );
			}
				break;
			default: break;
			}
		}
		
		@Override
		public void onConnectLocalDevice(BluetoothSocket remoteSocket) {
			Log.d(TAG, "onConnectLocalDevice--->远程设备主动连接");
			mRemoteSocket = remoteSocket;
			if(BC401Fragment.getInstance() !=null){
				BC401Fragment.getInstance().startReceiveData(true, remoteSocket);
			}
			SendCMDThread.getAllTransData();
		}
		
	}
	
	/**
	 * 获取连接成功的设备
	 * @return
	 */
	public static BluetoothDevice getConDevice() {
		if (mLocalSocket != null)
			return mLocalSocket.getRemoteDevice();
		return null;
	}
	
}
