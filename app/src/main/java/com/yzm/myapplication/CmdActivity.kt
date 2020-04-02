package com.yzm.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_cmd.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.StringBuilder

class CmdActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        setContentView(R.layout.activity_cmd)

        btn.setOnClickListener {
            val data = ByteArray(7)
            data[0] = 0xAA.toByte()
            data[1] = 0x07.toByte()
            data[2] = 0xC9.toByte()
            data[3] = 0x00.toByte()
            data[4] = 0x00.toByte()
            data[5] = 0x00.toByte()
            BleService.writeCMD(data)
        }


    }

    var content = ""

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun dataEvent(event: DataEvent) {


        var type = ""
        if(event.isMySend){
            type = "发送数据："
        }else{
            type = "接收数据："
        }

        val stringBuilder = StringBuilder(event.data.size)
        for (byteChar in event.data) stringBuilder.append(
            String.format(
                "%02X ",
                byteChar
            )
        )

        content = content +"\n" + type+stringBuilder
        tv.text = content

    }

    override fun onDestroy() {
        super.onDestroy()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }


}

