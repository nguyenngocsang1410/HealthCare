package com.fanny.healthcare.util;

import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Fanny on 17/7/23.
 */

public class MyThread extends Thread{

    private String RecMsg=null;

    @Override
    public void run() {
        super.run();

            if(SocketUtil.socket!=null){
                while (true) {
                    InputStream inputStream = SocketUtil.getInputStream();
                    if (inputStream != null) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

                        try {
                            while ((RecMsg = br.readLine()) != null) {
                                RecMsg = br.readLine();
                                setRecMsg(RecMsg);
                            }
//                        inputStream.close();
//                        br.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }else {
                        break;
                    }
                }
            }


    }

    public void setRecMsg(String msg){
        this.RecMsg=msg;
    }
    public String getRecMsg(){
        return  this.RecMsg;
    }
}
