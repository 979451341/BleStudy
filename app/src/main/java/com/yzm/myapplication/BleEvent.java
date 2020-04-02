package com.yzm.myapplication;

/**
 * Created by Autil On 2020/4/1
 */
public class BleEvent {

    public BleEvent(boolean isC,String n,String m){
        isConnect = isC;
        name = n;
        mac = m;
    }

    public boolean isConnect=false;
    public String name;
    public String mac;


}
