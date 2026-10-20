package com.feasycom.feasyblue.viewmodel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.feasycom.feasyblue.App
import com.feasycom.wifi.utils.BwNetUtils

class BluetoothViewModel : ViewModel() {

    // LiveData 用于广播数据
    private val _broadcastData = MutableLiveData<String>()

    // 懒加载 WiFi 管理器
    private val mWifiManager by lazy {
        App.context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    // 转换广播数据，如果 WiFi 连接则获取 SSID，否则返回空字符串
    val mBroadcastData = Transformations.switchMap(_broadcastData){
        MutableLiveData<String>().apply {
            value = if(BwNetUtils.isWifiConnected(mWifiManager)){
                BwNetUtils.getSsidString(mWifiManager.connectionInfo)
            }else {
                ""
            }
        }
    }

    // 广播接收器
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            when (action) {
                WifiManager.NETWORK_STATE_CHANGED_ACTION,
                LocationManager.PROVIDERS_CHANGED_ACTION -> _broadcastData.setValue(action)
            }
        }
    }

    // 根据系统版本设置 Intent 过滤器
    private val filter = IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
        }
    }

    // 解除注册广播接收器
    fun unregisterReceiver(){
        App.context?.applicationContext?.unregisterReceiver(mReceiver)
    }

    // 注册广播接收器
    fun registerReceiver(){
        App.context?.applicationContext?.registerReceiver(mReceiver, filter)
    }
}