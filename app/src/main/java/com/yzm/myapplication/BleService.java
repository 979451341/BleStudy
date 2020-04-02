package com.yzm.myapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Autil On 2020/3/31
 */
public class BleService extends Service {


    private static final String TAG = "BleService";

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private static   BluetoothManager mBluetoothManager;
    private static  BluetoothAdapter mBluetoothAdapter;
    private static  BluetoothLeScanner mBluetoothLeScanner;
    private static List<BluetoothGatt> mBluetoothGatts = new ArrayList<BluetoothGatt>();
    private static BluetoothGatt gatts;

    private static BleService instance;

    public static BleService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        /**
         * 适配Android 8.0+
         */
        NotificationManager manager = (NotificationManager) getApplication().getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel mChannel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mChannel = new NotificationChannel("蓝牙学习项目", "蓝牙学习项目", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(mChannel);
            Notification notification = new Notification.Builder(getApplicationContext(), "蓝牙学习项目").build();
            startForeground(110, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        initBle();



        return super.onStartCommand(intent, flags, startId);
    }


    private void initBle(){
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return;

            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return;
        }
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a mBluetoothLeScanner.");
            return;
        }



    }

    public static void close(BluetoothGatt bg) {
        if (bg == null) {
            return;
        }
        bg.close();
    }

    private static BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (BluetoothGatt.GATT_SUCCESS == status) {
//开启监听成功，可以像设备写入命令了
            } else {


                BleService.disConnectAndClose(gatt);

                Log.e(TAG, "    onDescriptorWrite is " + status);
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            Log.e(TAG, "    onConnectionStateChange newState is " + newState + "      onConnectionStateChange status is " + status);
            if (newState == BluetoothProfile.STATE_CONNECTED) {

                Log.i("BluetoothGatt", "连接成功\n" + "onConnectionStateChange status is: " + status + "onConnectionStateChange newState is: " + newState);

                EventBus.getDefault().post(new BleEvent(true,gatt.getDevice().getName(),gatt.getDevice().getAddress()));
                PreferenceUtils.putString(Constant.DEVICE_NAME, gatt.getDevice().getName());
                PreferenceUtils.putString(Constant.MAC, gatt.getDevice().getAddress());

                Log.e(TAG, "设备连接上 开始扫描服务");
                    boolean discoverServices = gatt.discoverServices();
                    Log.e(TAG, "discoverServices is " + discoverServices);
                    if (discoverServices) {


                    } else {
                        BleService.disConnectAndClose(gatt);

                    }

            }  else {
                BleService.disConnectAndClose(gatt);

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                String address = gatt.getDevice().getAddress();
                bindCharas(address, getSupportedGattServices(address));

            } else {
                BleService.disConnectAndClose(gatt);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                EventBus.getDefault().post(new DataEvent(characteristic.getValue(),false));
            }else{
                BleService.disConnectAndClose(gatt);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // value为设备发送的数据，根据数据协议进行解析
            EventBus.getDefault().post(new DataEvent(characteristic.getValue(),false));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            String value = BleUtils.bytesToHexString(characteristic.getValue());
            EventBus.getDefault().post(new DataEvent(characteristic.getValue(),true));
            Log.i(TAG, "onCharacteristicWrite: " + value);
            Log.e(TAG, "发送成功");
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }
    };

    private static void bindCharas(String address, List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            // Loops through available Characteristics.
            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                String uuid_str = gattCharacteristic.getUuid().toString();
                if (uuid_str.equalsIgnoreCase(Constant.SMART_TAG_WRITE_UUID)) {
                    BluetoothGattCharacteristic mWriteCharacteristic = gattCharacteristic;
                }
                if (uuid_str.equalsIgnoreCase(Constant.SMART_TAG_READ_UUID)) {
                    BluetoothGattCharacteristic mReadCharacteristic = gattCharacteristic;
                    setCharacteristicNotification(address, mReadCharacteristic, true);
                }
            }
        }
    }


    public static void setCharacteristicNotification(String address, BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {

        BluetoothGatt mBluetoothGatt = null;
        for (BluetoothGatt bg : mBluetoothGatts) {
            if (bg.getDevice().getAddress().equalsIgnoreCase(address)) {
                mBluetoothGatt = bg;
            }
        }

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        boolean openNotify = mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        Log.e(TAG, "setCharacteristicNotification is " + openNotify);
        if (openNotify) {
            if (Constant.SMART_TAG_READ_UUID.equalsIgnoreCase(characteristic.getUuid().toString())) {
                BluetoothGattDescriptor descriptor = characteristic .getDescriptor(UUID.fromString(Constant.CLIENT_CHARACTERISTIC_CONFIG));
                if (descriptor != null) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    boolean isWriteDescriptor = mBluetoothGatt.writeDescriptor(descriptor);
                    Log.e(TAG, "writeDescriptor is " + isWriteDescriptor);
                }
            }
        } else {

            BleService.disConnectAndClose(gatts);
            close();

        }
    }

    public static void close() {
        if (gatts != null)
            gatts.close();
        for (BluetoothGatt bg : mBluetoothGatts) {
            close(bg);
        }
        mBluetoothGatts.clear();
    }


    public static List<BluetoothGattService> getSupportedGattServices(String address) {
        BluetoothGatt mBluetoothGatt = null;
        for (BluetoothGatt bg : mBluetoothGatts) {
            if (bg.getDevice().getAddress().equalsIgnoreCase(address)) {
                mBluetoothGatt = bg;
            }
        }
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");

            return null;
        }
        if (mBluetoothGatts == null)
            return null;
        return mBluetoothGatt.getServices();
    }

    public static  BluetoothManager getmBluetoothManager() {
        return mBluetoothManager;
    }


    public static  BluetoothAdapter getmBluetoothAdapter() {
        return mBluetoothAdapter;
    }


    public static  BluetoothLeScanner getmBluetoothLeScanner() {
        return mBluetoothLeScanner;
    }


    public static boolean connect(final String address) {
        Log.i(TAG, "Connect :" + address);
        if (mBluetoothAdapter == null || TextUtils.isEmpty(address)) {
            Log.i(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        if (mBluetoothGatts != null && mBluetoothGatts.size() > 0) {
            for (BluetoothGatt bg : mBluetoothGatts) {
                if (bg.getDevice().getAddress().equalsIgnoreCase(address)) {
                    Log.i(TAG, "Trying to use an existing mBluetoothGatt for connection.");
                    if (mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT) == STATE_CONNECTED) {
                        Log.i("TAG", mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT) + "");
                        EventBus.getDefault().post(new BleEvent(true,device.getName(),device.getAddress()));
                        PreferenceUtils.putString(Constant.DEVICE_NAME, device.getName());
                        return true;
                    } else {
                        if (bg.connect()) {
                            //ToastUtil.showMessage(getResources().getString(R.string.connect) + ":" + address);
                            EventBus.getDefault().post(new BleEvent(true,device.getName(),device.getAddress()));
                            PreferenceUtils.putString(Constant.DEVICE_NAME, device.getName());
                            return true;
                        } else {
                            disConnectAndClose(bg);
                            break;
                        }
                    }
                }
            }
        }

        gatts = device.connectGatt(instance, false, mGattCallback);
        if (gatts != null)
            mBluetoothGatts.add(gatts);
        return true;
    }


    public static void disconnect(String address) {
        if (!TextUtils.isEmpty(address)) {
            if (mBluetoothAdapter != null && gatts != null)
                gatts.disconnect();
            if (mBluetoothAdapter == null || mBluetoothGatts == null) {
                Log.w(TAG, "BluetoothAdapter not initialized");
                return;
            }
            for (BluetoothGatt bg : mBluetoothGatts) {
                if (bg.getDevice().getAddress().equalsIgnoreCase(address)) {
                    bg.disconnect();
                }
            }
        } else {
            Log.w(TAG, "BluetoothDevice is empty");
        }

    }
    public static void disconnectAll() {
        if (mBluetoothAdapter == null || mBluetoothGatts == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        for (BluetoothGatt bg : mBluetoothGatts) {
            if (bg != null) {
                bg.disconnect();
            }
        }
    }

    public static void writeCMD(byte[] cmd) {
        final StringBuilder stringBuilder = new StringBuilder(cmd.length);
        for (byte byteChar : cmd)
            stringBuilder.append(String.format("%02X ", byteChar));
        Log.i(TAG, "Write CMD:" + stringBuilder.toString());
//        if (cmd.length >= 3 && cmd[2] != (byte) 0xC9) {
//            isBeep = false;
//        }
        String mac = PreferenceUtils.getString(Constant.MAC);
        BluetoothGatt mBluetoothGatt = null;
        for (BluetoothGatt bg : mBluetoothGatts) {
            if (bg.getDevice().getAddress().equalsIgnoreCase(mac)) {
                mBluetoothGatt = bg;
            }
        }
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            lostConnect();
            return;
        }

        BluetoothGattCharacteristic characteristic = getWriteChara(mac);
        if (characteristic != null) {
            boolean write = characteristic.setValue(cmd);
            mBluetoothGatt.writeCharacteristic(characteristic);
        }
    }


    public static BluetoothGattCharacteristic getWriteChara(String address) {
        BluetoothGatt mBluetoothGatt = null;
        for (BluetoothGatt bg : mBluetoothGatts) {
            if (bg.getDevice().getAddress().equalsIgnoreCase(address)) {
                mBluetoothGatt = bg;
            }
        }

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            lostConnect();
            return null;
        }

        String uuid = null;
        for (BluetoothGattService gattService : mBluetoothGatt.getServices()) {
            uuid = gattService.getUuid().toString();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            // Loops through available Characteristics.
            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                uuid = gattCharacteristic.getUuid().toString();
                if (uuid.equalsIgnoreCase(Constant.SMART_TAG_WRITE_UUID)) {
                    return gattCharacteristic;
                }
            }
        }
        return null;
    }

    private static void lostConnect(){
        EventBus.getDefault().post(new BleEvent(false,"",""));
        PreferenceUtils.putString(Constant.DEVICE_NAME, "");
        ToastUtil.showMessage("蓝牙未连接");
    }

    private static void disConnectAndClose(BluetoothGatt gatt){
        lostConnect();

        gatt.disconnect();
        close(gatt);
        mBluetoothGatts.remove(gatt);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
