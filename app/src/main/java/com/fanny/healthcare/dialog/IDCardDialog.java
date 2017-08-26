package com.fanny.healthcare.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.StateSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.creative.pc700.IDCard;
import com.creative.pc700.SendCMDThread;
import com.fanny.healthcare.R;
import com.fanny.healthcare.activity.MainActivity;
import com.fanny.healthcare.bean.User;
import com.fanny.healthcare.dao.DBOpenHelper;
import com.fanny.healthcare.util.MyThread;
import com.fanny.healthcare.util.SocketUtil;
import com.fanny.healthcare.util.XORUtil;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static com.fanny.healthcare.activity.MainActivity.isLogin;

/**
 * 身份证对话框
 */
public class IDCardDialog extends DialogFragment {

    private TextView tv_name, tv_sex, tv_nation, tv_birthday, tv_address, tv_idcard_number,
            tv_department, tv_valid_day, tv_login_tag;
    private Button btn_scan, btn_stop;
    private ProgressBar pb_scan;
    private SendCMDThread mSendCMDThread;
    private MyThread mySocketThread;

//    private String isLogin="未获取返回值";

    /**
     * 定义身份证的上传数据
     */
    private byte[] sendBuffer = new byte[20];

    private Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0x07:
                    tv_login_tag.setText("用户是否注册：" + msg.obj);
                    break;
                case 0x08:
                    Toast.makeText(getActivity(), "socket未连接，请先进行socket连接！", Toast.LENGTH_SHORT).show();
                default:
                    break;
            }
        }
    };
    ;
    private AlertDialog dialog;

    public void initdata() {
        sendBuffer[0] = (byte) 0xEA;
        sendBuffer[1] = (byte) 0xEB;

        sendBuffer[2] = (byte) 0x09;//data区字节长度为5个字节

        sendBuffer[3] = (byte) 0x00; //北京地区
        sendBuffer[4] = (byte) 0x0a;

        sendBuffer[5] = (byte) 0x02;//健康管家
        sendBuffer[6] = (byte) 0x08;//身份验证
        sendBuffer[7] = (byte) 0x01;//设备序列号


        sendBuffer[17] = (byte) 0x00;//校验码
        sendBuffer[18] = (byte) 0xE5;
        sendBuffer[19] = (byte) 0xD4;

    }

//    @Override   //适合对自定义的layout进行设置，但没有标题
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View v = inflater.inflate(R.layout.dialog_idcard_info, container, false);
//        initView(v);
//        return v;
//    }

    @Override  //适合对简单dialog进行处理，可以利用Dialog.Builder直接返回Dialog对象
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_idcard_info, null);
        initView(view);


        builder.setTitle("身份证信息")
                .setView(view)
                .setCancelable(false);
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);


        /**
         * 监听服务器返回用户是否注册返回值
         */
//        if(SocketUtil.socket.isConnected()){
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    InputStream inputStream = SocketUtil.getInputStream();
//                    BufferedReader br=new BufferedReader(new InputStreamReader(inputStream));
//                    try {
//
//                        while(!((isLogin=br.readLine())==null)){
//                            Log.e("while中","接收服务器的信息："+isLogin);
//                            isLogin=br.readLine();
//                            Message message = new Message();
//                            message.obj=isLogin;
//                            message.what = 0x07;
//                            myHandler.sendMessage(message);
//
//                        }
//                        br.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
////                inputStream.close();
//                }
//            }).start();
//        }else {
//            Message message=new Message();
//            message.what=0x08;
//            myHandler.sendMessage(message);
//        }
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (MainActivity.mPC700 != null) {
            mSendCMDThread = MainActivity.mPC700.getSendCMDThread();
//            initdata();
        }
        MainActivity.setUIHandler(mIDHandler);


    }

    private void initView(View view) {
        tv_name = (TextView) view.findViewById(R.id.tv_idcardinfo_name);
        tv_sex = (TextView) view.findViewById(R.id.tv_idcardinfo_sex);
        tv_birthday = (TextView) view.findViewById(R.id.tv_idcardinfo_birthday);
        tv_nation = (TextView) view.findViewById(R.id.tv_idcardinfo_nation);
        tv_idcard_number = (TextView) view.findViewById(R.id.tv_idcardinfo_number);
        tv_address = (TextView) view.findViewById(R.id.tv_idcardinfo_address);
        tv_department = (TextView) view.findViewById(R.id.tv_idcardinfo_grantdept);
        tv_valid_day = (TextView) view.findViewById(R.id.tv_idcardinfo_valid_day);
        pb_scan = (ProgressBar) view.findViewById(R.id.pb_scan);
        tv_login_tag = (TextView) view.findViewById(R.id.tv_login_tag);

        btn_scan = (Button) view.findViewById(R.id.btn_start);
        btn_stop = (Button) view.findViewById(R.id.btn_stop);
        btn_scan.setOnClickListener(listener);
        btn_stop.setOnClickListener(listener);
    }

    String socketData = "shifouzhuce";
    Handler myHandler01 = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0x07:
                    socketData = (String) msg.obj;

                    if (socketData != null) {
                        tv_login_tag.setText("用户是否注册：" + socketData);
                        Log.e("MyThread", "服务器传来消息" + socketData);
                        /**
                         * 根据返回值，给isRegister赋值，如果为未注册用户，则前往注册界面注册，若为注册用户，则直接登录进行检测。
                         */
                        MainActivity.isRegister = true;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Toast.makeText(getContext(), "前往检测", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        tv_login_tag.setText("用户是否注册：" + "");
                        Log.e("MyThread", "服务器传来空消息" + socketData);
                        MainActivity.isRegister = true;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Toast.makeText(getContext(), "注册用户", Toast.LENGTH_LONG).show();
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };

    View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_start: {
                    SocketUtil.receiveMsg = true;
                    //新PC600,900,700 只调用一次接口，下位机就会不停的刷。
                    //旧版PC600,900 调用一次接口,下位机只刷1次。
                    pb_scan.setVisibility(View.VISIBLE);
                    mSendCMDThread.IDCard_StartScan();

                    /**
                     * 监听服务器返回用户是否注册返回值
                     */
//                    mySocketThread=new MyThread();
//                    mySocketThread.start();

                    if (SocketUtil.socket != null) {
                        /**
                         * 接收服务器数据，保持监听
                         */

                        new Thread(new Runnable() {

                            @Override
                            public void run() {
                                InputStream in = SocketUtil.getInputStream();
                                while (SocketUtil.receiveMsg) {
                                    String RecMsg = "服务器无返回消息";
                                    if (in != null) {
                                        Log.e("SocketUtil", "in不为空");

                                        try {
                                            byte buffer[] = new byte[1024 * 4];
                                            int temp = 0;
                                            while ((temp = in.read(buffer)) != -1) {
                                                RecMsg = new String(buffer, 0, temp);
                                                Log.e("SocketUtil", RecMsg);

                                                Message message = new Message();
                                                message.what = 0x07;
                                                message.obj = RecMsg;
                                                myHandler01.sendMessage(message);

                                            }
                                            Log.e("SocketUtil", "无数据了");
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            Log.e("SocketUtil", e.toString());
                                        }
                                    } else {
                                        Log.e("SocketUtil", "in为空");
                                    }

//                            try {
//                                in.close();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
                                    Message message = new Message();
                                    message.what = 0x07;
                                    message.obj = RecMsg;
                                    myHandler01.sendMessage(message);

                                }

                            }
                        }).start();

                    }

//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            InputStream inputStream = SocketUtil.getInputStream();
//                            if (inputStream != null) {
//                                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
//
//                                try {
//                                    String receiveMsg = null;
//                                    while ((receiveMsg = br.readLine()) != null) {
//                                        receiveMsg = br.readLine();
////                                        isLogin=isLogin+"+1";
//                                        Log.e("IDCARD,while中", "接收服务器的信息：" + receiveMsg);
//                                        Message message = new Message();
//                                        message.obj = receiveMsg;
//                                        message.what = 0x07;
//                                        myHandler.sendMessage(message);
////                                        mHandler.obtainMessage(IDCardDialog.MSG_SERVERMSG,receiveMsg).sendToTarget();
//                                    }
//                                    inputStream.close();
//                                    br.close();
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//
//                        }
//                    }).start();

                }
                break;

                case R.id.btn_stop: {
                    SocketUtil.receiveMsg = false;
                    pb_scan.setVisibility(View.INVISIBLE);
                    mSendCMDThread.IDCard_StopScan();
                    /**
                     * 在此放置一个清楚数据库的功能
                     */
//                    SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
//                    db.delete("user","name=?",new String[]{"龚梦帆"});
//                    db.close();

                }
                break;
                default:
                    break;
            }
        }
    };

    public static final int MSG_IDCARD = 50;
    public static final int MSG_SERVERMSG = 70;
    private DBOpenHelper dbOpenHelper;
    private List<User> users;

    public Handler mIDHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {

                case MSG_SERVERMSG:
                    int code = (int) msg.obj;
                    Log.e("主界面传来消息", "接收服务器的信息：" + code);
//                    tv_login_tag.setText("用户是否注册：" + msg.obj);

                    break;
                case MSG_IDCARD: {
//                    Log.e("一开始扫描","接收服务器的信息："+isLogin);


                    pb_scan.setVisibility(View.INVISIBLE);
                    IDCard idCard = (IDCard) msg.obj;

                    dialog.setCanceledOnTouchOutside(true);
                    /**
                     * 扫描成功后，改变isSearch值
                     */
                    MainActivity.isSearch = true;
                    /**
                     * 显示用户信息在dialog上
                     */

                    tv_name.setText("姓名:" + idCard.getName());
                    tv_address.setText("住址:" + idCard.getAddress());
                    tv_birthday.setText("出生:" + idCard.getBirthday());
                    tv_department.setText("签发机关:" + idCard.getGrantDept());
                    tv_idcard_number.setText("公民身份号码:" + idCard.getIDCardNo());
                    tv_nation.setText("民族:" + idCard.getNation());
                    String sex = "1".equals(idCard.getSex()) ? "男" : "女";
                    tv_sex.setText("性别:" + sex);
                    tv_valid_day.setText("有效期限:" + idCard.getUserLifeBegin() + "-" + idCard.getUserLifeEnd());


                    /**
                     * 保存用户信息于本地数据库同时上传至服务器
                     */
                    final String idNumber = "ID" + idCard.getIDCardNo();
//                    String[] strArray=new String[18];
//                    int[] intArray=new int[18];
//                    //字符串分解
//                    for(int i=0;i<18;i=i+2){
//                        strArray[i]=idNumber.substring(i,i+2);
//                        intArray[i]= Integer.parseInt(strArray[i]);
//                        Log.e("idcard",""+intArray[i]);
//                    }
//                    for(int i=0;i<9;i++){
//                        sendBuffer[i+8]= (byte) intArray[i];
//                    }
//                    sendBuffer[17]= XORUtil.getXORByte(sendBuffer);

                    /**
                     * 上传至远程服务器
                     */

                    if (SocketUtil.socket.isConnected()) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                SocketUtil.SendDataString(idNumber);
                            }
                        }).start();

                    }

                    /**
                     * 保存至本地数据库
                     */

//                    dbOpenHelper = new DBOpenHelper(getActivity());
//                    users = new ArrayList<User>();
//                    String userName = idCard.getName();
//                    String userSex = idCard.getSex();
////                    String userNation = getNationName(idCard.getNation());
//                    String userBirthday = idCard.getBirthday();
//                    String userIdcardNo = idCard.getIDCardNo();
//                    users.add(new User(userName, userSex, userBirthday, userIdcardNo));
//                    Log.e("user信息", users.get(0).toString());
//
//                    SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
////                    db.execSQL("insert into user(name,sex,birthday,idcardno) values(?,?,?,?)", new Object[]{userName, userSex, userBirthday, userIdcardNo});
//
//                    Cursor cursor=db.query("user",new String[]{"name"},null,null,null,null,null);
//                    while (cursor.moveToNext()) {
//                        String ssname = cursor.getString(cursor.getColumnIndex("name"));
//                        if(!ssname.equals(userName)){
//                            db.execSQL(
//                                    "insert into user(name,sex,birthday,idcardno) values(?,?,?,?)", new Object[]{userName, userSex, userBirthday, userIdcardNo});
////                    db.close();
//                        }
//                    }
//
//                    Cursor cursor1=db.query("user",null,null,null,null,null,null);
//                    if(cursor1!=null && cursor1.getCount()>0){
//                        while (cursor1.moveToNext()){
//                            Log.e("查询结果", cursor1.getString(cursor1.getColumnIndex("name")));
//                            Log.e("查询结果", cursor1.getString(cursor1.getColumnIndex("sex")));
//                            Log.e("查询结果", cursor1.getString(cursor1.getColumnIndex("birthday")));
//                            Log.e("查询结果", cursor1.getString(cursor1.getColumnIndex("idcardno")));
//                        }
//                    }
//                    db.close();


//                    if(mySocketThread.getRecMsg()!=null){


//                    Bitmap userIMG = BitmapFactory.decodeByteArray(idCard.getWlt(), 0, idCard.getWlt().length);
//			        Log.i("idcard","userIMG == null:"+(userIMG == null)+" idCard.getWlt()==null:"+(idCard.getWlt()==null));
//                    ((ImageView) findViewById(R.id.idcardinfo_img)).setImageBitmap(userIMG);
//                    byte2File(userIMG,idCard.getWlt());
//
//			        for(int i=0;i<idCard.getWlt().length;i++) {
//				        byte[] temp = idCard.getWlt();
//				        Log.d("idcard",Integer.toHexString(temp[i])+" ");
//			        }
                }
                break;
            }
        }
    };

    private final String[] nations = new String[]{"解码错", "汉", "蒙古", "回", "藏", "维吾尔", "苗", "彝", "壮", "布依", "朝鲜", "满", "侗", "瑶", "白", "土家", "哈尼", "哈萨克", "傣", "黎", "傈僳", "佤", "畲", "高山", "拉祜", "水", "东乡", "纳西", "景颇", "柯尔克孜", "土", "达斡尔", "仫佬", "羌", "布朗", "撒拉", "毛南", "仡佬", "锡伯", "阿昌", "普米", "塔吉克", "怒", "乌孜别克", "俄罗斯", "鄂温克", "德昴", "保安", "裕固", "京", "塔塔尔", "独龙", "鄂伦春", "赫哲", "门巴", "珞巴", "基诺", "编码错", "其他", "外国血统"};

    public String getNationName(String nation) {

        if (!nation.matches("[0-9]{2}")) {
            new Exception("民族代码错误");
        }

        int nationcode = Integer.parseInt(nation);
        if (nationcode >= 1 && nationcode <= 56) {
            nation = this.nations[nationcode];
        } else if (nationcode == 97) {
            nation = "其他";
        } else if (nationcode == 98) {
            nation = "外国血统中国籍人士";
        } else {
            nation = "编码错误";
        }

        return nation;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSendCMDThread != null) {
            mSendCMDThread.IDCard_StopScan();
        }
    }

    //    private void byte2File(Bitmap bitmap, byte[] src) {
//        File mFile = new File(Environment.getExternalStorageDirectory().toString() + "/bbb.jpg");
//        try {
//            mFile.createNewFile();
//            //new FileOutputStream(mFile).write(src);
//            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(mFile));
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos);
//            bos.flush();
//            bos.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

}
