package com.yzm.myapplication;

/**
 * Created by Autil On 2020/4/1
 */
public class DataEvent {

    public byte[] data;
    public boolean isMySend;

    public DataEvent(byte[] data,boolean isMySend){
       this.data = data;
       this.isMySend = isMySend;

    }
}
