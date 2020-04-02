package com.yzm.myapplication;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

/**
 * Created by Autil On 2020/3/31
 */
public class ScannerAdapter extends BaseQuickAdapter<BluetoothDevice, BaseViewHolder> {
    public ScannerAdapter(int layoutResId, @Nullable List<BluetoothDevice> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, BluetoothDevice item) {

        helper.setText(R.id.tv,item.getName());
    }
}
