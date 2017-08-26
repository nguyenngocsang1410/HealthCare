package com.fanny.healthcare.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.creative.base.BLEReader;
import com.creative.base.BaseDate;
import com.creative.pc700.ECG12Config;
import com.creative.pc700.IDCard;
import com.creative.pc700.IIAPCallBack;
import com.creative.pc700.IPC700CallBack;
import com.creative.pc700.PC700;
import com.creative.pc700.SendCMDThread;
import com.creative.pc700.StatusMsg;
import com.fanny.healthcare.R;
import com.fanny.healthcare.dialog.IDCardDialog;
import com.fanny.healthcare.fragment.DetectionFragment;
import com.fanny.healthcare.fragment.DoctorFragment;
import com.fanny.healthcare.fragment.FileFragment;
import com.fanny.healthcare.fragment.MoreFragment;
import com.fanny.healthcare.fragment.TWJC_TESTFragment;
import com.fanny.healthcare.fragment.XTJC_TESTFragment;
import com.fanny.healthcare.fragment.XYANGJC_TESTFragment;
import com.fanny.healthcare.fragment.XYJC_TESTFragment;
import com.fanny.healthcare.util.MySpinnerArrayAdapter;
import com.fanny.healthcare.util.SharePrefrenceUtil;
import com.fanny.healthcare.util.SocketUtil;
import com.rd.PageIndicatorView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Inflater;

import devlight.io.library.ntb.NavigationTabBar;

public class MainActivity extends Activity implements View.OnClickListener {
    private String TAG = "MainActivity";

    public static PC700 mPC700;
    private NavigationTabBar navigationTabBar;
    private String[] colors;
    private DetectionFragment detecFragment;
    private FileFragment fileFragment;
    private DoctorFragment doctorFragment;
    private MoreFragment moreFragment;
    private ArrayList<Fragment> fragments;
    private int[] imageId;
    private SendCMDThread mSendCMDThread;

    public static boolean bNIBP_Measuring;

    private Handler searchHandler;


    private Socket socket;
    private Handler pbHandler;
    public static String isLogin = "服务器返回数据";

    private Button tv_test;
    private boolean test_mode;
    private LinearLayout ll_search;
    private LinearLayout ll_test;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);




        /**
         * 监听home键的系统事件
         */
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                showExitDialog();
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(receiver, filter);

        /**
         * 定义图片资源数组
         */
        imageId = new int[]{R.drawable.old1, R.drawable.old2, R.drawable.old3};
        int[] colorId = new int[]{};

        /**
         * 定义底部导航的颜色数组
         */
        colors = getResources().getStringArray(R.array.default_preview);

        /**
         * 定义idcard的sp
         */
        String idcard_history= (String) SharePrefrenceUtil.getData(getApplicationContext(),"search_history","");
        SharePrefrenceUtil.saveParam(getApplicationContext(),"search_history",idcard_history);

        /**
         * 定义ip地址的sp
         */
//        SharePrefrenceUtil.selectParam(getApplicationContext(),"ip_history");
        String ip_history = (String) SharePrefrenceUtil.getData(getApplicationContext(), "ip_history", "");
        SharePrefrenceUtil.saveParam(getApplicationContext(),"ip_history",ip_history);
//        SharePrefrenceUtil.saveParam(getApplicationContext(),"ip_history","192.168.0.100,");

        /**
         * 初始化fragment
         */
//        initFragment();

        /**
         * 初始化底部导航
         */
        initBottomUI();



        /**
         *初始化一个pc700的构造函数
         */
        try {
            mPC700 = PC700.getInstance(this, m700CallBack);
            /**
             * 开始进行数据通信，接受设备发送来的数据进行解析，同时也可向设备发送数据
             */
            mPC700.Start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /**
         * 获取发送命令的对象，控制设备相关动作
         */
//        mSendCMDThread = mPC700.getSendCMDThread();
//        mSendCMDThread.
    }


    private static Handler mHandler;

    /**
     * 更新UI handler
     */
    public static void setUIHandler(Handler handler) {
        mHandler = handler;
    }


    private void initFragment() {
        detecFragment = new DetectionFragment();
        fileFragment = new FileFragment();
        doctorFragment = new DoctorFragment();
        moreFragment = new MoreFragment();
        fragments = new ArrayList<>();
        fragments.add(detecFragment);
        fragments.add(fileFragment);
        fragments.add(doctorFragment);
        fragments.add(moreFragment);

    }

    private int layouts[] = {R.layout.fragment_detection, R.layout.fragment_file, R.layout.fragment_doctor, R.layout.fragment_more};

    public static boolean isSearch = false;
    public static boolean isRegister=true;

    private void initBottomUI() {
        /**
         * 初始化各个界面内容
         */
        final ViewPager viewPager = (ViewPager) findViewById(R.id.vp_content);
        viewPager.setAdapter(new PagerAdapter() {

            @Override
            public int getCount() {
                return 4;
            }

            @Override
            public boolean isViewFromObject(final View view, final Object object) {
                return view.equals(object);
            }

            @Override
            public void destroyItem(final View container, final int position, final Object object) {
                ((ViewPager) container).removeView((View) object);
            }

            @Override
            public Object instantiateItem(final ViewGroup container, final int position) {
                final View view = LayoutInflater.from(
                        getBaseContext()).inflate(layouts[position], null, false);
                /**
                 * 处理每个界面的内容
                 */
                /**
                 * 检测界面
                 */
                if (position == 0) {
                    /**
                     * viewpager
                     * 界面
                     */

                    /**
                     * 模拟测试按钮
                     */
                    test_mode = false;
                    tv_test = (Button) view.findViewById(R.id.tv_test);
                    tv_test.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(test_mode ==false){
                                test_mode =true;
                                ll_test.setVisibility(View.VISIBLE);
                                tv_test.setText("测试中");
                            }else {
                                test_mode=false;
                                ll_test.setVisibility(View.GONE);
                                tv_test.setText("测试模式");
                            }
                        }
                    });

                    ViewPager viewPager1 = (ViewPager) view.findViewById(R.id.vp_detection);
                    ll_search = (LinearLayout) view.findViewById(R.id.ll_search);
                    ll_test = (LinearLayout) view.findViewById(R.id.ll_test);

                    viewPager1.setAdapter(new PagerAdapter() {

                        @Override
                        public int getCount() {
                            return 3;
                        }

                        @Override
                        public boolean isViewFromObject(View view, Object object) {
                            return view == object;
                        }

                        @Override
                        public Object instantiateItem(ViewGroup container, int position) {
                            ImageView im = new ImageView(getApplicationContext());
                            im.setImageResource(imageId[position]);
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                            im.setLayoutParams(params);
                            container.addView(im);
                            return im;
                        }

                        @Override
                        public void destroyItem(final View container, final int position, final Object object) {
                            ((ViewPager) container).removeView((View) object);
                        }
                    });
                    PageIndicatorView pageIndicatorView = (PageIndicatorView) view.findViewById(R.id.vp_indictor_detection);
                    pageIndicatorView.setViewPager(viewPager1);

                    /**
                     * 检测功能界面的显示和隐藏
                     */
                    if (isSearch == false) {
//                    搜索身份证界面
                        ll_test.setVisibility(View.GONE);
                        ll_search.setVisibility(View.VISIBLE);
                    } else {
                        //测试界面
                        ll_test.setVisibility(View.VISIBLE);
                        ll_search.setVisibility(View.GONE);
                    }

                    // 登录界面控件
                    TextView tvTest = (TextView) view.findViewById(R.id.tv_start_test);
                    TextView tvStop = (TextView) view.findViewById(R.id.tv_stop_search);
                    final TextView tvLogin = (TextView) view.findViewById(R.id.tv_login_socket);
                    TextView tvLoginPutin= (TextView) view.findViewById(R.id.tv_login_putin);

                    final ProgressBar pgSearch = (ProgressBar) view.findViewById(R.id.pg_search);
                    final TextView tvSearch = (TextView) view.findViewById(R.id.tv_search);

                    // 检测界面
                    ImageButton imXYJC = (ImageButton) view.findViewById(R.id.btn_xyjc);
                    ImageButton imTZJC = (ImageButton) view.findViewById(R.id.btn_tzjc);
                    ImageButton imXTJC = (ImageButton) view.findViewById(R.id.btn_xtjc);
                    ImageButton imXDJC = (ImageButton) view.findViewById(R.id.btn_xdjc);
                    ImageButton imYYBL = (ImageButton) view.findViewById(R.id.btn_yybl);
                    ImageButton imXYANGJC = (ImageButton) view.findViewById(R.id.btn_xyangjc);
                    ImageButton imXZSX = (ImageButton) view.findViewById(R.id.btn_xzsx);
                    ImageButton imXNS = (ImageButton) view.findViewById(R.id.btn_xns);
                    ImageButton imZNSB = (ImageButton) view.findViewById(R.id.btn_znsb);

                    // 更新ui界面
                    searchHandler = new Handler() {
                        @Override
                        public void handleMessage(Message msg) {
                            switch (msg.what) {
                                case 1:
                                    pgSearch.setVisibility(View.GONE);
                                    tvSearch.setText("扫描成功，请登陆进行检测");
                                    /**
                                     * 需要上传扫描后的用户信息至服务器进行查询验证
                                     * 根据服务器返回的结果码进行操作
                                     */

                                    /**
                                     * 服务器返回"已注册"结果码，则保存用户信息，作为此次测量的数据持有者
                                     */


                                    /**
                                     * 服务器返回"未注册"信息码，创建用户信息，上传至服务器，并保存作为此次测量的数据持有者
                                     */


                                    isSearch = true;
                                    break;
                                case 2:
                                    pgSearch.setVisibility(View.GONE);
                                    tvSearch.setText("扫描失败，请重新扫描");
                                    isSearch = false;
                                    break;

                            }
                            super.handleMessage(msg);
                        }
                    };

                    tvLoginPutin.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            if(SocketUtil.getSocket()!=null) {

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

                                                        if (RecMsg.equals("")) {
                                                            MainActivity.isRegister = false;
//                                                        isSearch = true;
                                                        } else {
                                                            MainActivity.isRegister = true;
//                                                        isSearch = true;
                                                        }

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

                                        }

                                    }
                                }).start();

                            }


                            final AlertDialog.Builder registerDialog = new AlertDialog.Builder(MainActivity.this);
                            View view = getLayoutInflater().inflate(R.layout.activity_user_register, null);
//                            final EditText et_register= (EditText) view.findViewById(R.id.et_idcard);
                            final AutoCompleteTextView auto_idcard= (AutoCompleteTextView) view.findViewById(R.id.auto_tv_idcard);

                            // 获取搜索记录文件内容
                            String history = (String) SharePrefrenceUtil.getData(getApplicationContext(),"search_history","");

                            // 用逗号分割内容返回数组
                            String[] history_arr = history.split(",");

                            ArrayAdapter<String> arr_adapter=new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_spinner_dropdown_item,history_arr);

                            // 保留前50条数据
                            if (history_arr.length > 50) {
                                String[] newArrays = new String[50];
                                // 实现数组之间的复制
                                System.arraycopy(history_arr, 0, newArrays, 0, 50);
                                arr_adapter = new ArrayAdapter<String>(MainActivity.this,
                                        android.R.layout.simple_dropdown_item_1line, history_arr);
                            }

                            // 设置适配器
                            auto_idcard.setAdapter(arr_adapter);

                            registerDialog.setTitle("手动输入注册：") ;
                            registerDialog.setView(view);
                            registerDialog.setPositiveButton("登录", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final String idcard=auto_idcard.getText().toString();

                                    if (SocketUtil.getSocket()!=null && SocketUtil.socket.isConnected()){
                                        if(idcard.length()==18){

                                            // 获取搜索框信息
                                            String text = auto_idcard.getText().toString();
                                            String old_text= (String) SharePrefrenceUtil.getData(getApplicationContext(),"search_history","");
//                                            SharedPreferences mysp = getSharedPreferences("search_history", 0);
//                                            String old_text = mysp.getString("history", "暂时没有搜索记录");

                                            // 利用StringBuilder.append新增内容，逗号便于读取内容时用逗号拆分开
                                            StringBuilder builder = new StringBuilder(old_text);
                                            builder.append(text + ",");

                                            // 判断搜索内容是否已经存在于历史文件，已存在则不重复添加
                                            if (!old_text.contains(text + ",")) {
//                                                SharedPreferences.Editor myeditor = mysp.edit();
//                                                myeditor.putString("history", builder.toString());
//                                                myeditor.commit();
                                                SharePrefrenceUtil.saveParam(getApplicationContext(),"search_history",builder.toString());
                                                Toast.makeText(MainActivity.this, text + "添加成功", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(MainActivity.this, text + "已存在", Toast.LENGTH_SHORT).show();
                                            }

                                            isSearch = true;
                                            /**
                                             * 将身份信息发送给服务器验证
                                             */
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    SocketUtil.SendDataString(idcard);
                                                }
                                            }).start();

                                            if(isRegister=true){
                                                registerDialog.setCancelable(true);
                                            }else {
                                                registerDialog.setCancelable(false);
                                            }

                                        }else {
                                            Toast.makeText(MainActivity.this,"请输入合法的身份证号码",Toast.LENGTH_SHORT).show();
                                            isSearch = false;
                                        }
                                    }else {
                                        Toast.makeText(MainActivity.this,"检查网络连接",Toast.LENGTH_SHORT).show();
                                    }

                                }
                            })
                            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                            registerDialog.create().show();



                        }
                    });

                    tvLogin.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            /**
                             * 弹出连接socket对话框
                             */
//                            if (SocketUtil.connectStaus != 1 || !SocketUtil.getSocket().isConnected()) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                View view = getLayoutInflater().inflate(R.layout.dialog_socket_connect, null);

//                                final EditText et_ip = (EditText) view.findViewById(R.id.et_socket_ip);
//                                final Spinner spinner_ip= (Spinner) view.findViewById(R.id.spinner_ip);
//                                String[] stringArrayIP = getResources().getStringArray(R.array.ip_address);
//                                spinner_ip.setAdapter(new MySpinnerArrayAdapter(getBaseContext(),stringArrayIP));

                                final AutoCompleteTextView auto_ip= (AutoCompleteTextView) view.findViewById(R.id.auto_tv_ip);
                                // 获取搜索ip记录文件内容
                                String history = (String) SharePrefrenceUtil.getData(getApplicationContext(),"ip_history","");

                                // 用逗号分割内容返回数组
                                String[] history_arr = history.split(",");

                                ArrayAdapter<String> arr_adapter=new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_spinner_dropdown_item,history_arr);

                                // 保留前50条数据
                                if (history_arr.length > 50) {
                                    String[] newArrays = new String[50];
                                    // 实现数组之间的复制
                                    System.arraycopy(history_arr, 0, newArrays, 0, 50);
                                    arr_adapter = new ArrayAdapter<String>(MainActivity.this,
                                            android.R.layout.simple_dropdown_item_1line, history_arr);
                                }

                                // 设置适配器
                                auto_ip.setAdapter(arr_adapter);

                                final EditText et_port = (EditText) view.findViewById(R.id.et_socket_port);
                                Button btn_connect = (Button) view.findViewById(R.id.btn_socket_connect);
                                Button btn_disconnect = (Button) view.findViewById(R.id.btn_socket_disconnect);
                                final ProgressBar pb_socket = (ProgressBar) view.findViewById(R.id.pb_socket);

                                builder.setTitle("socket连接")
                                        .setView(view)
                                        .setCancelable(false);
                                final AlertDialog dialog = builder.create();
                                dialog.setCanceledOnTouchOutside(false);
                                dialog.show();

                                pbHandler = new Handler() {
                                    @Override
                                    public void handleMessage(Message msg) {
                                        switch (msg.what) {
                                            case 3:
                                                if (myProgressbar != null) {
                                                    myProgressbar.setVisibility(View.GONE);
                                                    Toast.makeText(MainActivity.this, "socket连接成功", Toast.LENGTH_SHORT).show();
                                                    dialog.dismiss();
                                                }

                                                break;
                                            case 4:
                                                if (myProgressbar != null) {
                                                    myProgressbar.setVisibility(View.GONE);
                                                    Toast.makeText(MainActivity.this, "socket连接失败", Toast.LENGTH_SHORT).show();
                                                    dialog.setCanceledOnTouchOutside(true);
                                                }
                                                break;
                                            case 5:
                                                if (myProgressbar != null) {
                                                    myProgressbar.setVisibility(View.GONE);
                                                    Toast.makeText(MainActivity.this, "socket连接异常", Toast.LENGTH_SHORT).show();
                                                    dialog.setCanceledOnTouchOutside(true);
                                                }
                                                break;
                                            default:
                                                break;
                                        }
                                    }
                                };


                                btn_connect.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {

                                        if(SocketUtil.socket==null || !SocketUtil.socket.isConnected() || SocketUtil.getSocket()==null || SocketUtil.connectStaus==0){

                                            String text=auto_ip.getText().toString();
                                            //保存ip地址
//                                            if(!text.equals("")){
                                                String[] arr=text.split(".");
                                                if(arr.length<4){
                                                    // 获取搜索框信息
                                                    String old_text= (String) SharePrefrenceUtil.getData(getApplicationContext(),"ip_history","");
//                                            SharedPreferences mysp = getSharedPreferences("search_history", 0);
//                                            String old_text = mysp.getString("history", "暂时没有搜索记录");

                                                    // 利用StringBuilder.append新增内容，逗号便于读取内容时用逗号拆分开
                                                    StringBuilder builder = new StringBuilder(old_text);
                                                    builder.append(text + ",");

                                                    // 判断搜索内容是否已经存在于历史文件，已存在则不重复添加
                                                    if (!old_text.contains(text + ",")) {
//                                                SharedPreferences.Editor myeditor = mysp.edit();
//                                                myeditor.putString("history", builder.toString());
//                                                myeditor.commit();
                                                        SharePrefrenceUtil.saveParam(getApplicationContext(),"ip_history",builder.toString());
                                                        Toast.makeText(MainActivity.this, text + "添加成功", Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        Toast.makeText(MainActivity.this, text + "已存在", Toast.LENGTH_SHORT).show();
                                                    }
                                                }else {
                                                    Toast.makeText(MainActivity.this, text + "添加失败", Toast.LENGTH_SHORT).show();
                                                }
//                                            }
                                            /**
                                             * 如果socket未连接，则进入连接界面进行socket连接
                                             */
                                            /**
                                             * 搜索progressbar
                                             */
                                            createProgressBar(getApplicationContext());

                                            new Thread(new Runnable() {

                                                @Override
                                                public void run() {

                                                    try {
//                                                    String strIP = et_ip.getText().toString();
                                                        String strIP= auto_ip.getText().toString();
                                                        String strPort = et_port.getText().toString();

                                                        Log.e("ip", strIP);
                                                        Log.e("port", strPort);

                                                        socket = new Socket(strIP, Integer.parseInt(strPort));
//                                            SocketAddress socketAddress = new InetSocketAddress(strIP, Integer.getInteger(strPort));
//                                            socket.connect(socketAddress);
                                                        socket.setSoTimeout(3000);

                                                        if (socket != null && socket.isConnected()) {
                                                            /**
                                                             * 保存socket的ip和端口号
                                                             */
                                                            SharePrefrenceUtil.saveParam(getApplicationContext(), "SocketIp", strIP);
                                                            SharePrefrenceUtil.saveParam(getApplicationContext(), "SocketPort", strPort);
                                                            /**
                                                             * 设置全局socket
                                                             */
                                                            SocketUtil.setSocket(socket);
                                                            SocketUtil.setConnectStaus(1);//设置socket连接状态
//                                                        Toast.makeText(getApplicationContext(),"socket连接成功",Toast.LENGTH_SHORT).show();
                                                            Log.e("socket", "连接成功");



                                                            /**
                                                             * 通知界面更新
                                                             */
                                                            if (searchHandler != null && pbHandler != null) {
                                                                Message message = new Message();
                                                                message.what = 3;
//                                                                searchHandler.sendMessage(message);
                                                                pbHandler.sendMessage(message);
                                                            }

                                                        } else {

                                                            /**
                                                             * 未连接
                                                             */
                                                            SocketUtil.setConnectStaus(0);
//                                                        Toast.makeText(getApplicationContext(),"socket连接失败",Toast.LENGTH_SHORT).show();
                                                            Log.e("socket", "连接失败");
                                                            if (searchHandler != null && pbHandler != null) {
                                                                Message message = new Message();
                                                                message.what = 4;
//                                                                searchHandler.sendMessage(message);
                                                                pbHandler.sendMessage(message);
                                                            }

                                                        }

                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                        SocketUtil.setConnectStaus(0);
                                                        Log.e("socket", "连接异常" + e.getMessage());
                                                        if (searchHandler != null && pbHandler != null) {
                                                            Message message = new Message();
                                                            message.what = 5;
//                                                            searchHandler.sendMessage(message);
                                                            pbHandler.sendMessage(message);
                                                        }
//                                                    Toast.makeText(getApplicationContext(),"socket连接异常",Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            }).start();
                                        }else {
                                            Toast.makeText(MainActivity.this, "用户已经连接socket成功，可直接登录检测", Toast.LENGTH_SHORT).show();
                                        }

                                    }
                                });

                                btn_disconnect.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        dialog.dismiss();
                                    }
                                });
//                            } else if (SocketUtil.connectStaus == 1 && SocketUtil.getSocket().isConnected()) {
//                                Toast.makeText(MainActivity.this, "用户已经连接socket成功，可直接登录检测", Toast.LENGTH_SHORT).show();

//                            }
                        }
                    });

                    tvTest.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
//                                ll_search.setVisibility(View.GONE);
//                                ll_test.setVisibility(View.VISIBLE);
//                                isSearch=true;
//                                if(mPC700!=null){
//                                    mSendCMDThread.IDCard_StartScan();
//                                }


                            if (SocketUtil.socket != null) {
                                /**
                                 * 扫描对话框
                                 */
                                IDCardDialog idCardDialog = new IDCardDialog();
                                idCardDialog.show(getFragmentManager(), "id_card");



                            } else {
                                Toast.makeText(MainActivity.this, "socket连接失败，请重新连接", Toast.LENGTH_SHORT).show();
                            }

                        }
                    });

                    tvStop.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (isSearch == true && isRegister==true) {

                                Toast.makeText(MainActivity.this, "扫描成功，进行检测", Toast.LENGTH_SHORT).show();
                                ll_search.setVisibility(View.GONE);
                                ll_test.setVisibility(View.VISIBLE);

                            } else {
                                Toast.makeText(MainActivity.this, "扫描失败，请重新扫描身份证", Toast.LENGTH_SHORT).show();
                            }

                        }
                    });
//                    }

                    /**
                     * 血压检测
                     */
                    imXYJC.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent XYJC_Intent = new Intent(MainActivity.this, XYJCActivity.class);
                            startActivity(XYJC_Intent);
                        }
                    });
                    /**
                     * 血氧检测
                     */
                    imXYANGJC.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent XYANGJC_Intent = new Intent(MainActivity.this, XYANGJCActivity.class);
                            startActivity(XYANGJC_Intent);
                        }
                    });
                    /**
                     * 血糖检测
                     */
                    imXTJC.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent XTJC_Intent = new Intent(MainActivity.this, XTJCActivity.class);
                            startActivity(XTJC_Intent);
                        }
                    });
                    /**
                     * 体温检测
                     */
                    imTZJC.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent TZJC_Intent = new Intent(MainActivity.this, TWJCActivity.class);
                            startActivity(TZJC_Intent);
                        }
                    });
                    /**
                     * 心电检测
                     */
                    imXDJC.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent ZDJC_Intent = new Intent(MainActivity.this, XDJCActivity.class);
                            startActivity(ZDJC_Intent);
                        }
                    });
                    /**
                     * 尿液检测
                     */
                    imXNS.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent NYJC_Intent = new Intent(MainActivity.this, NYJCActivity.class);
                            startActivity(NYJC_Intent);
                        }
                    });
                    /**
                     * 智能手环——脉搏血氧检测
                     */
                    imZNSB.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent ZNSB_Intent = new Intent(MainActivity.this, ZNSBActivity2.class);
                            startActivity(ZNSB_Intent);

                        }
                    });
                }


                /**
                 * 档案界面
                 */
                if (position == 1) {

                }
                /**
                 * 医生界面
                 */
                if (position == 2) {

                }
                /**
                 * 更多界面
                 */
                if (position == 3) {

                }

                container.addView(view);
                return view;
            }
        });


        navigationTabBar = (NavigationTabBar) findViewById(R.id.ntb);
        ArrayList<NavigationTabBar.Model> models = new ArrayList<>();
        models.add(new NavigationTabBar.Model.Builder(getResources().getDrawable(R.drawable.heart), Color.parseColor(colors[0]))
                .title("检测").build());
        models.add(new NavigationTabBar.Model.Builder(getResources().getDrawable(R.drawable.file), Color.parseColor(colors[1]))
                .title("档案").build());
        models.add(new NavigationTabBar.Model.Builder(getResources().getDrawable(R.drawable.doctor), Color.parseColor(colors[2]))
                .title("医生").build());
        models.add(new NavigationTabBar.Model.Builder(getResources().getDrawable(R.drawable.more), Color.parseColor(colors[3]))
                .title("更多").build());

        navigationTabBar.setModels(models);

        navigationTabBar.setViewPager(viewPager, 0);

        navigationTabBar.setTitleMode(NavigationTabBar.TitleMode.ALL);
        navigationTabBar.setBadgeGravity(NavigationTabBar.BadgeGravity.BOTTOM);
        navigationTabBar.setBadgePosition(NavigationTabBar.BadgePosition.CENTER);
        navigationTabBar.setTypeface("fonts/custom_font.ttf");
        navigationTabBar.setIsBadged(false);
        navigationTabBar.setIsTitled(true);
        navigationTabBar.setIsTinted(true);
        navigationTabBar.setIsBadgeUseTypeface(true);
        navigationTabBar.setBadgeBgColor(Color.RED);
        navigationTabBar.setBadgeTitleColor(Color.WHITE);
        navigationTabBar.setIsSwiped(true);
        navigationTabBar.setBgColor(Color.WHITE);
        navigationTabBar.setBadgeSize(10);
        navigationTabBar.setTitleSize(10);
        navigationTabBar.setIconSizeFraction((float) 0.5);

        navigationTabBar.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(final int position) {
                navigationTabBar.getModels().get(position).hideBadge();
//                changeFragment(position);
            }

            @Override
            public void onPageScrollStateChanged(final int state) {

            }
        });

        navigationTabBar.postDelayed(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < navigationTabBar.getModels().size(); i++) {
                    final NavigationTabBar.Model model = navigationTabBar.getModels().get(i);
                    navigationTabBar.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            model.showBadge();
                        }
                    }, i * 100);
                }
            }
        }, 500);
    }

    /**
     * 创建progressvar进度条
     */
    private ProgressBar myProgressbar;

    private void createProgressBar(Context context) {
        LinearLayout rootLayout = (LinearLayout) findViewById(R.id.Parentcontent);
        LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rootParams.gravity = Gravity.CENTER;
        myProgressbar = new ProgressBar(context);
        myProgressbar.setLayoutParams(rootParams);
        myProgressbar.setVisibility(View.VISIBLE);
        rootLayout.addView(myProgressbar);
    }


//    private void changeFragment(int indexofChild) {
//        getSupportFragmentManager().beginTransaction()
//                .replace(R.id.content_layout,fragments.get(indexofChild)).commit();
//    }


    /**
     * 实现的接口pc700
     */
    private IPC700CallBack m700CallBack = new IPC700CallBack() {
        @Override
        public void onGetDeviceName(String s) {
            Log.e(TAG, "获取到的设备信息：" + s);
        }

        @Override
        public void onGetDeviceVersion(String s, String s1) {
            Log.e(TAG, "获取到的设备硬件版本：" + s + ",设备软件版本为：" + s1);
        }

        /**
         * 血压NIBP部分
         * @param i
         */
        @Override
        public void NIBP_GetPatientType(int i) {
            Log.e(TAG, "获取到病人类型为：" + i);
        }

        @Override
        public void NIBP_StartStaticAdjusting() {
            Log.e(TAG, "血压开始静态压校准");
        }

        @Override
        public void NIBP_StartDynamicAdjusting() {
            Log.e(TAG, "血压开始动态压校准");
        }

        @Override
        public void NIBP_startCheckLeakage() {
            Log.e(TAG, "血压开始漏气检测");
        }

        @Override
        public void NIBP_stopCheckLeakage() {
            Log.e(TAG, "血压停止漏气检测");
        }

        @Override
        public void NIBP_CheckLeakageResult(int i) {
            Log.e(TAG, "血压漏气检测结果：" + i);
        }

        @Override
        public void NIBP_PatientTypeSet() {
            Log.e(TAG, "病人类型设置成功");
            mHandler.obtainMessage(XYJC_TESTFragment.MSG_NIBP_TYPE).sendToTarget();
        }

        @Override
        public void NIBP_PressureSet() {
            Log.e(TAG, "初始压力设置成功");
            mHandler.sendEmptyMessage(XYJC_TESTFragment.MSG_NIBP_SETTING);
        }

        @Override
        public void NIBP_StartMeasure() {
            Log.e(TAG, "血压开始测量");
            bNIBP_Measuring = true;
        }

        @Override
        public void NIBP_StopMeasure() {
            Log.e(TAG, "血压停止测量");
            bNIBP_Measuring = false;
        }

        @Override
        public void NIBP_GetMeasureResult(int sys, int map, int dia, int pr, boolean err, int rank) {
            Log.e(TAG, "血压测量结束，获取到的测量结果为：" + "收缩压为：" + sys + ",平均压为：" + map + "，舒张压为：" + dia + ",脉率为：" + pr + ",心率是否正常：" + err + ",血压结果等级为：" + rank);
            Message msg = mHandler.obtainMessage(XYJC_TESTFragment.MSG_NIBP_RESULT);
            Bundle bundle = msg.getData();
            bundle.putInt("SYS", sys);
            bundle.putInt("DIA", dia);
            bundle.putInt("PLUS", pr);
            bundle.putInt("MAP", map);
            bundle.putInt("rank", rank);
            mHandler.sendMessage(msg);
        }

        @Override
        public void NIBP_GetMeasureERROR(int type) {
            Log.e(TAG, "血压测量错误信息：" + type);
            mHandler.obtainMessage(XYJC_TESTFragment.MSG_NIBP_RESULT, type, 0).sendToTarget();
        }

        @Override
        public void NIBP_GetState(int state) {
            Log.e(TAG, "获取到的血压状态：" + state);
            /**
             * 参数:
             state -
             0x00: 测量结束
             0x01: 模块忙或测量正在进行中
             0xFF: 模块故障或未接入
             0xD0：模块接入
             0xD1：模块拔出
             */
        }

        @Override
        public void NIBP_GetModule(int module, String softVer, String hardVer) {
            Log.e(TAG, "获取到的模块类型：" + module + ",软件版本：" + softVer + ",硬件版本：" + hardVer);
        }

        @Override
        public void NIBP_GetRealData(int data) {
            Log.e(TAG, "血压测量实时状态：" + data);
        }

        /**
         * 血氧Sp02部分
         * @param
         */
        @Override
        public void SPO_GetMode(int mode) {
            Log.e(TAG, "血氧工作模式：" + mode);
        }

        @Override
        public void SPO_GetState(int state, String sv, String hv) {
            Log.e(TAG, "血氧测量状态：" + state + "血氧软件版本：" + sv + ",血氧硬件版本:" + hv);
        }

        @Override
        public void SPO_GetWave(List<BaseDate.Wave> list) {
            Log.e(TAG, "血氧波形数据：" + list);
            if (XYANGJC_TESTFragment.sv_spo2 != null) {
                for (BaseDate.Wave wave : list) {
                    XYANGJC_TESTFragment.sv_spo2.addData(wave.data); //血氧波形数据
                    if (wave.flag == 1) { //血氧搏动标记
                        mHandler.sendEmptyMessage(XYANGJC_TESTFragment.MSG_SPO2_PULSE);
                    }
                }
            }
        }

        @Override
        public void SPO_GetParam(int nSpo2, int nPR, float nPI, boolean bProbe, int mode) {
            Log.e(TAG, "血氧值：" + nSpo2 + ",脉率值：" + nPR + ",血流灌注值：" + nPI + "探头状态：" + bProbe + ",用户模式：" + mode);

            Message msg = mHandler.obtainMessage(XYANGJC_TESTFragment.MSG_SPO2_PARAM);
            Bundle bundle = msg.getData();
            bundle.putInt("SPO", nSpo2);
            bundle.putInt("PR", nPR);
            bundle.putFloat("PI", nPI);
            bundle.putBoolean("bPro", bProbe);
            bundle.putInt("mode", mode);
            mHandler.sendMessage(msg);
        }

        /**
         * 血糖部分GLU
         * @param type
         */
        @Override
        public void GLU_GetType(int type) {
            Log.e(TAG, "血糖仪器类别：" + type);
            mHandler.obtainMessage(XTJC_TESTFragment.MSG_GLU_TYPE, type, 0).sendToTarget();
        }

        @Override
        public void GLU_GetGLUResult(int type, float data, int unit) {
            Log.e(TAG, "血糖检测结果：：" + type + ",血糖值：" + data + "单位" + unit);
            mHandler.obtainMessage(XTJC_TESTFragment.MSG_GLU, type, unit, data).sendToTarget();
        }

        @Override
        public void GLU_GetUAResult(int type, float data, int unit) {
            Log.e(TAG, "尿酸检测结果：：" + type + ",血糖值：" + data + "单位" + unit);
            mHandler.obtainMessage(XTJC_TESTFragment.MSG_UA, type, unit, data).sendToTarget();
        }

        @Override
        public void GLU_GetCHOLResult(int type, float data, int unit) {
            Log.e(TAG, "总胆固醇检测结果：：" + type + ",血糖值：" + data + "单位" + unit);
            mHandler.obtainMessage(XTJC_TESTFragment.MSG_CHOL, type, unit, data).sendToTarget();
        }

        /**
         * 体温部分TEMP
         * @param type
         * @param data
         * @param unit
         */
        @Override
        public void TMP_GetResult(int type, float data, int unit) {
            Log.e(TAG, "体温检测结果：" + "类型：" + type + ",体温值：" + data + "单位" + unit);
            mHandler.obtainMessage(TWJC_TESTFragment.MSG_TEMP, type, unit, data).sendToTarget();
        }

        /**
         * 12导心电测量部分ECG
         * @param
         */
        @Override
        public void ECG12_PrepareMeasure(int time) {
            Log.e(TAG, "导心电测量准备阶段，倒计时：" + time);
            if (XDJCActivity.mECG12SurfaceView != null) {
                XDJCActivity.mECG12SurfaceView.drawLable("" + time, Color.BLUE, 80);
            }

        }

        @Override
        public void ECG12_StartMeasure() {
            Log.e(TAG, "导心电开始测量");
        }

        @Override
        public void ECG12_StopMeasure() {
            Log.e(TAG, "导心电停止测量");
        }

        @Override
        public void ECG12_GetData(ECG12Config.ECG12Data[] ecg12Datas, int baseVale) {
            Log.e(TAG, "获取导心电波形数据：" + ecg12Datas + "，心电基线值：" + baseVale);
            if (XDJCActivity.mECG12SurfaceView != null) {
                XDJCActivity.mECG12SurfaceView.addWave(ecg12Datas, baseVale);
            }
        }

        @Override
        public void ECG12_GetHeartRate(final int hr) {
            Log.e(TAG, "导心率为：" + hr);
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView tvHeartRate = XDJCActivity.getHeartRateView();
                    tvHeartRate.setText("" + hr);
                    XDJCActivity.Alpha(tvHeartRate);
                }
            });
        }

        @Override
        public void ECG12_GetSaturation(final String saturation) {
            Log.e(TAG, "导心电饱和度：" + saturation);
            //导联接触不好会导致饱和，此时导联并没有脱落
//            MainActivity.this.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    TextView tvHeartRate = XDJCActivity.getSaturationView();
//                    tvHeartRate.setText(saturation);
//                }
//            });
        }

        @Override
        public void ECG12_GetLeadFall(final String lead) {
            Log.e(TAG, "导心电联脱落状态：" + lead);
//            MainActivity.this.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    TextView tvLeadFall = XDJCActivity.getLeadFallView();
//                    tvLeadFall.setText(lead);
//                }
//            });
        }

        @Override
        public void ECG12_GetECGConfig(ECG12Config ecg12Config) {
            Log.e(TAG, "导心电波形的相关配置：" + ecg12Config);
            if (XDJCActivity.mECG12SurfaceView != null) {
                XDJCActivity.mECG12SurfaceView.setSampleRate(ecg12Config.SampleRate);
                XDJCActivity.mECG12SurfaceView.setAdValue(ecg12Config.ADValue);
                XDJCActivity.mECG12SurfaceView.setGain(10);
            }
        }

        /**
         * 单导心电测量部分
         * @param nHWMajor
         * @param nHWMinor
         * @param nSWMajor
         * @param nSWMinor
         */
        @Override
        public void onGetECGVer(int nHWMajor, int nHWMinor, int nSWMajor, int nSWMinor) {
            Log.e(TAG, "单导联心电模块版本,硬件主版本：" + nHWMajor + ",硬件次版本：" + nHWMinor + ",软件版本：" + nSWMajor + ",软件次版本：" + nSWMinor);

        }

        @Override
        public void onGetECGAction(int status) {
            Log.e(TAG, "单导联心电测量状态改变：" + status);
            /**
             * 参数:
             status - 0:待机
             1: 开始测量,begin to measure
             2: 停止测量,stop measuring
             0xFF:测量出错，模块故障或未接入,measure error。
             */
            mHandler.obtainMessage(XDJCActivity.MSG_ECG_STATUS_CH, status, 0).sendToTarget();
        }

        @Override
        public void onGetECGRealTime(BaseDate.ECGData ecgData, int hr, boolean bLeadoff, int i1) {
            Log.e(TAG, "单导联心电实时数据：" + ecgData + "，心率为：" + hr);
            Message msg = mHandler.obtainMessage(XDJCActivity.MSG_ECG_WAVE, ecgData);
            Bundle data = new Bundle();
            data.putBoolean("bLeadOff", bLeadoff);
            data.putInt("hr", hr);
            msg.setData(data);
            mHandler.sendMessage(msg);
            if (ecgData.frameNum == 1) {//帧率=0时，波形准备阶段，>0,波形测试开始
//                XDJCActivity.v_ecg.clean();
                mHandler.obtainMessage(XDJCActivity.MSG_ECG_START_TIME).sendToTarget();
            }
            for (BaseDate.Wave wave : ecgData.data) {
//                XDJCActivity.v_ecg.addData(wave.data);
            }
        }

        @Override
        public void onGetECGGain(int hGain, int dGain) {
            Log.e(TAG, "单导联心电硬件增益：" + hGain + ",显示增益：" + dGain);
            mHandler.obtainMessage(XDJCActivity.MSG_ECG_GAIN, hGain, dGain).sendToTarget();
        }

        @Override
        public void onGetECGResult(int nResult, int nHR) {
            Log.e(TAG, "单导联心电测量结果：" + nResult + ",心率：" + nHR);
            /**
             * 参数:
             nResult - 测量结果
             0X00 波形未见异常 ;
             0X01 波形疑似心跳稍快请注意休息;
             0X02 波形疑似心跳过快请注意休息;
             0X03 波形疑似阵发性心跳过快请咨询医生;
             0X04 波形疑似心跳稍缓请注意休息;
             0X05 波形疑似心跳过缓请注意休息;
             0X06 波形疑似偶发心跳间期缩短请咨询医生;
             0X07 波形疑似心跳间期不规则请咨询医生;
             0X08 波形疑似心跳稍快伴有偶发心跳间期缩短请咨询医生;
             0X09 波形疑似心跳稍缓伴有偶发心跳间期缩短请咨询医生;
             0X0A 波形疑似心跳稍缓伴有心跳间期不规则请咨询医生;
             0X0B 波形有漂移请重新测量;
             0X0C 波形疑似心跳过快伴有波形漂移请咨询医生;
             0X0D 波形疑似心跳过缓伴有波形漂移请咨询医生;
             0X0E 波形疑似偶发心跳间期缩短伴有波形漂移请咨询医生;
             0X0F 波形疑似心跳间期不规则伴有波形漂移请咨询医生;
             0XFF 信号较差请重新测量.
             nHR - 心率值 0~255
             */
            nResult = (nResult == 255 ? 16 : nResult);
            mHandler.obtainMessage(XDJCActivity.MSG_ECG_STATUS_CH, 4, nResult, nHR).sendToTarget();
        }


        /**
         * 卡、证信息部分
         * @param
         * @param
         */
        @Override
        public void SMA_GetInfo(int code, IDCard idCard) {
            Log.e(TAG, "结果代码：" + code + ",信息：" + idCard);
            /**
             * 参数:
             code - 结果代码
             结果正确StatusMsg.IDCardErrorCode.ERROR_NORMAL
             错误结果
             StatusMsg.IDCardErrorCode.ERROR_MODEERROR
             StatusMsg.IDCardErrorCode.ERROR_MODEBUSY
             StatusMsg.IDCardErrorCode.ERROR_NOFIND

             IDCard - 信息
             */
            if (idCard != null && code == StatusMsg.IDCardErrorCode.ERROR_NORMAL) {
                mHandler.obtainMessage(IDCardDialog.MSG_IDCARD, idCard).sendToTarget();
                mHandler.obtainMessage(IDCardDialog.MSG_SERVERMSG, code).sendToTarget();

                /**
                 * 通知界面更新
                 */
                if (searchHandler != null) {
                    Message message = new Message();
                    message.what = 1;
                    searchHandler.sendMessage(message);
                }

            } else {
                if (searchHandler != null) {
                    Message message = new Message();
                    message.what = 2;
                    searchHandler.sendMessage(message);
                }
            }
        }

        /**
         * 电池状态
         * @param
         * @param
         * @param
         */
        @Override
        public void onBatteryStatus(int chargeLevel, int charge, int ac) {
            Log.e(TAG, "电量等级：" + chargeLevel + ",充电状态：" + charge);
        }

        @Override
        public void onException(String err) {
            Log.e(TAG, "错误信息：" + err);
        }

        @Override
        public void OnConnectLose() {

        }
    };

    private IIAPCallBack m900CallBack = new IIAPCallBack() {
        @Override
        public void onIAP_enter() {

        }

        @Override
        public void onIAP_start() {

        }

        @Override
        public void onIAP_programing(int i) {

        }

        @Override
        public void onIAP_check(String s) {

        }

        @Override
        public void onIAP_end() {

        }

        @Override
        public void onIAP_version(int i, int i1, byte b) {

        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        //Log.d(TAG,"onPause-关掉屏幕，下位机睡眠，省电");
        if (mPC700 != null) {
            mPC700.sleep();//下位机休眠
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Log.d(TAG,"onResume-屏幕亮起，下位机唤醒");
        if (mPC700 != null) {
            mPC700.wakeUp();//下位机唤醒
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPC700 != null) {
            /**
             * 停止数据通信，不再接受设备发送来的数据
             */
            mPC700.Stop(); //关闭接口
            mPC700 = null;
        }
        Process.killProcess(Process.myPid());
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                showExitDialog();
                break;
            case KeyEvent.KEYCODE_HOME:
                //注意home键是系统事件，只能通过广播监听
                showExitDialog();
                break;
            case KeyEvent.KEYCODE_MENU:
                showExitDialog();
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        showExitDialog();
        super.onBackPressed();
    }

    private void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("友情提示");
        builder.setMessage("确定要退出健康检测吗？");
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }
}
