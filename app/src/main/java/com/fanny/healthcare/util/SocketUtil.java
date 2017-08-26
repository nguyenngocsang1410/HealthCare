package com.fanny.healthcare.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.TreeMap;

/**
 * Created by Fanny on 17/4/20.
 */

public class SocketUtil {

    public static Socket socket;
    public static SocketAddress socAddress;

    public static InputStream in;
    public static OutputStream out;
    private char data[];
    private BufferedReader br;
    public static int rcvDataLength;

    private static byte[] buff = new byte[16];
    private byte[] sentbuff = new byte[16];

    public static int connectStaus=0;//0为未连接，1为已连接



    public static void setConnectStaus(int staus){
        connectStaus=staus;
    }

    public static void setSocket(Socket msocket){
        socket=msocket;
        in=getInputStream();
        out=getOutputStream();
    }
    public static Socket getSocket(){
        return socket;
    }
    public static InputStream getInputStream(){
        if(socket.isConnected()){
            try {
                in = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return in;
    }
    public static OutputStream getOutputStream(){
        if(socket.isConnected()){
            try {
                out = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return out;
    }

//    public int connect() {
//        try {
//            socket = new Socket();
//            socAddress = new InetSocketAddress(ip_address, Integer.getInteger(ip_port));
//            socket.setSoTimeout(3000);
//            if (socket == null || !socket.isConnected()) {
//                //网络未连接 －1
//                isConnect=false;
//                return -1;
//            } else {
//                //网络连接成功 1
//                isConnect=true;
//                in = socket.getInputStream();
//                out = socket.getOutputStream();
//                socket.setReceiveBufferSize(100 * 1024);
//                br = new BufferedReader(new InputStreamReader(in, "GBK"));
//                return 1;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            //网络连接异常
//            isConnect=false;
//            return 0;
//        }
//
//    }



//    public SocketUtil(char[] data) {
//        this.data = data;
//    }

    /**
     * 发送字符数据
     * @param senddata
     */
    public static void SendData(char senddata[]) {
        if (socket != null) {
            try {
                senddata[6]=CreatXOR(senddata,6);
                out.write((senddata.toString()+"\n").getBytes("GBK"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送字节数据
     * @param data
     */
    public static void SendDataByte(byte[] data){
        if(socket!=null){
            try {
                out.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送字符串数据
     * @param data
     */
    public static void SendDataString(String data){
        if(socket!=null){
            PrintWriter pw=new PrintWriter(out);
            pw.write(data);
            pw.flush();
        }
    }

    private static char CreatXOR(char RecData[], int len) {
        char uXOR;
        uXOR=RecData[0];
        for(int i=1;i<len;i++){
            uXOR= (char) (uXOR^RecData[i]);
        }
        if(uXOR==0) uXOR=6;
        return uXOR;

    }

    public static boolean ReceiveData() {
        if (socket == null) {
            return false;
        }
        try {
            rcvDataLength = in.read(buff);
            while (rcvDataLength > 0) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static String RecMsg="从服务器获取到的消息";

    public static boolean receiveMsg=true;

    public static String RecStringData() throws IOException {
        if(socket!=null){
            while (receiveMsg) {
                in= SocketUtil.getInputStream();
                if (in != null) {
                    Log.e( "SocketUtil","in不为空");


//                    try {
//                        byte buffer[]=new byte[1024*4];
//                        int temp=0;
//                        while ((temp=in.read(buffer))!=-1){
//                            RecMsg=new String(buffer,0,temp);
//                        }
//                        Log.e( "SocketUtil",RecMsg);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                        Log.e( "SocketUtil","nihao");
//                    }

//                    try {
                        BufferedReader br = new BufferedReader(new InputStreamReader(in));
                        String a=null;
                        String b=null;
                        while ((RecMsg = br.readLine()) != null) {
                             a=RecMsg;
                             b = br.readLine();
                        }
                        Log.e( "SocketUtil1","a"+a);
                        Log.e( "SocketUtil1","b"+b);
                        in.close();
                        br.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }

                }else {
                    Log.e( "SocketUtil","in为空");
                }
            }
        }
        return RecMsg;
    }



}
