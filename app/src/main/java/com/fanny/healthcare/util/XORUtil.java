package com.fanny.healthcare.util;

/**
 * Created by Fanny on 17/7/18.
 */

public class XORUtil {

    public static byte getXORByte(byte[] data){
        int size=data.length;
        /**
         * 例如体温检测数据 data＝[0xea,0xeb,0x03,0x00,0x0a,0x02,0x06,0x00,0x01,0x24,0x06,0x00,0x00,0xe5,0xd4]
         */
        byte des=data[0];
        /**
         * 异或运算
         */

        for(int i=1;i<size-3;i++){
            des= (byte) (des^data[i]);
        }
        return des;//字节数组的异或校验位的字节
    }

    public static byte getHexByte(byte data){
        byte des=0x00;
        return des;
    }

    public static byte DecToHex(byte data){
        byte result=0;

        return result;
    }
}
