package com.yzm.myapplication

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_scanner.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ScannerActivity : AppCompatActivity() {


    private val mBluetoothAdapter: BluetoothAdapter? = null
    private val mBluetoothLeScanner: BluetoothLeScanner? = null

    var deviceList:MutableList<BluetoothDevice> = mutableListOf()
    var scannerAdapter:ScannerAdapter = ScannerAdapter(R.layout.item_scanner,deviceList)


    var isScanner = true

    var mScanCallback = object :ScanCallback(){
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if(result==null){
                return
            }
            if(result?.scanRecord?.serviceUuids == null){
                return
            }
            var device:BluetoothDevice = result!!.device


            for (uuid in result!!.scanRecord!!.serviceUuids){
                //过滤出自己想连接的蓝牙设备
                if((Constant.SMART_SERVICE_UUID as String).equals(uuid.toString(),true)){
                    if(!deviceList.contains(device)){
                        deviceList.add(device)
                    }
                }
            }
            scannerAdapter.notifyDataSetChanged()

        }
    }

    var mHandler:Handler = object : Handler(){

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        setContentView(R.layout.activity_scanner)


        // 开启蓝牙
        BleService.getmBluetoothAdapter()?.enable()

        recyc.adapter = scannerAdapter
        recyc.layoutManager = LinearLayoutManager(this)

        scannerAdapter.setOnItemClickListener { adapter, view, position ->

            BleService.connect(deviceList.get(position).address)

        }


        btn.setOnClickListener {
            if(isScanner){

                deviceList.clear()
                BleService.getmBluetoothLeScanner()?.startScan(mScanCallback)


                //断开蓝牙连接
                disConnectBle()
                //清除保存的MAC
                PreferenceUtils.putString(Constant.MAC, "")
                PreferenceUtils.putString(Constant.DEVICE_NAME, "")

                mHandler.postDelayed(
                    object :Runnable{
                        override fun run() {
                            BleService.getmBluetoothLeScanner()?.stopScan(mScanCallback)

                        }

                    },5000
                )

            }else{
                //断开蓝牙连接
                disConnectBle()
                //清除保存的MAC
                PreferenceUtils.putString(Constant.MAC, "")
                PreferenceUtils.putString(Constant.DEVICE_NAME, "")
            }
        }

        //当手机已经连接设备，直接建立连接
        if(!TextUtils.isEmpty(PreferenceUtils.getString(Constant.MAC))){
            BleService.connect(PreferenceUtils.getString(Constant.MAC))
        }


        btnCmd.setOnClickListener {

            var myIntent = Intent(ScannerActivity@this,CmdActivity::class.java)
            startActivity(myIntent)
        }

    }


    //断开蓝牙连接
    private fun disConnectBle() {
        if (!TextUtils.isEmpty(PreferenceUtils.getString(Constant.MAC)) && BleService.getInstance() != null) {
            BleService.disconnect(PreferenceUtils.getString(Constant.MAC))
            BleService.disconnectAll()
            BleService.close()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun belEvent(event: BleEvent) {

        if(event.isConnect){
            ToastUtil.showMessage("连接成功")
            tvDevice.text = "当前设备："+event.name

            btn.text = "断开"
            isScanner = false
        }else{
            tvDevice.text = "当前设备："

            btn.text = "扫描"
            isScanner = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }

}
