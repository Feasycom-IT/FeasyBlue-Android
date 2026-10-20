package com.feasycom.feasyblue.logic

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.blankj.utilcode.util.LogUtils
import com.feasycom.common.utils.MsgLogger
import com.feasycom.feasyblue.App
import com.feasycom.feasyblue.interfaces.FeasyWiFiCallbacks
import com.feasycom.feasyblue.utils.getBoolean
import com.feasycom.feasyblue.utils.getInt
import com.feasycom.feasyblue.utils.getStr
import com.feasycom.wifi.bean.BluetoothDeviceWrapper
import com.feasycom.wifi.ble.FscBleCentralBwApiImp
import com.feasycom.wifi.ble.FscBleCentralBwCallbacks
import com.feasycom.wifi.ble.FscNetworkCentralBwCallbacks
import com.feasycom.wifi.ble.FscOtaCentralBwCallbacks

object BluetoothRepository : FscBleCentralBwCallbacks,
    FscNetworkCentralBwCallbacks,
    FscOtaCentralBwCallbacks {

    private const val TAG = "BluetoothRepository"

    private var rssiValue: Int = -100
    private var nameSwitch: Boolean = false
    private var nameValue: String = ""
    var mGoneDNS = true
    var isDisconnected = false
    var lostAtCommand = AtCommand.NONE
    private val mCallbacks = mutableListOf<FeasyWiFiCallbacks>()
    private const val connectTimeOutTime = 30000L
    private val mHandler = Handler(Looper.myLooper() ?: Looper.getMainLooper())
    private val mConnectTimeoutRunnable = Runnable {
        try {
            mCallbacks.forEach {
                it.onConnectTimeout()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        disconnect()
    }

    private val mFscBleCentralApi by lazy {
        FscBleCentralBwApiImp.getInstance(App.context).apply {
            initialize()
            setCallbacks(this@BluetoothRepository)
        }
    }

    fun isBtEnabled() = mFscBleCentralApi.isBtEnabled

    fun enabled() {
        mFscBleCentralApi.enabled()
    }

    fun registerViewCallback(callback: FeasyWiFiCallbacks) {
        if (mCallbacks.contains(callback)) {
            return
        }
        try {
            mCallbacks.add(callback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun unRegisterViewCallback(callback: FeasyWiFiCallbacks) {
        try {
            mCallbacks.remove(callback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startScan(time: Long = 0) {
        App.context?.let {
            rssiValue = it.getInt("rssiValue", -100)
            nameSwitch = it.getBoolean("nameSwitch", false)
            nameValue = it.getStr("nameValue", "")
        }
        mFscBleCentralApi.startScan(time)
    }

    fun stopScan() {
        mFscBleCentralApi.stopScan()
    }

    fun connect(address: String) {
        mHandler.postDelayed(mConnectTimeoutRunnable, connectTimeOutTime)
        mFscBleCentralApi.connect(address)
    }

    fun disconnect() {
        Log.e(TAG,"手机主动断开连接 6")

        mFscBleCentralApi.disconnect()
    }

    fun getDhcp() {
        lostAtCommand = AtCommand.GET_DHCP
        mFscBleCentralApi.getDhcp()
    }

    fun setDhcp(b: Boolean) {
        lostAtCommand = AtCommand.SET_DHCP
        mFscBleCentralApi.setDhcp(b)
    }

    fun getIp() {
        lostAtCommand = AtCommand.GET_IP
        mFscBleCentralApi.getIp()
    }

    fun getVersion() {
        lostAtCommand = AtCommand.GET_VERSION
        mFscBleCentralApi.getVersion()
    }

    fun getDNS() {
        lostAtCommand = AtCommand.GET_DNS
        mFscBleCentralApi.send("AT+DNS\r\n".toByteArray())
    }

    fun reset() {
        lostAtCommand = AtCommand.RESET
        mFscBleCentralApi.reset()
    }

    fun setIp(ip: String) {
        lostAtCommand = AtCommand.SET_IP
        mFscBleCentralApi.setIp(ip)
    }

    fun setGw(ip: String) {
        lostAtCommand = AtCommand.SET_GW
        mFscBleCentralApi.setGw(ip)
    }

    fun setMask(ip: String) {
        lostAtCommand = AtCommand.SET_MASK
        mFscBleCentralApi.setMask(ip)
    }

    fun setDns(ip: String) {
        lostAtCommand = AtCommand.SET_DNS
        mFscBleCentralApi.setDns(ip)
    }

    fun setNetwork(ssid: String, password: String) {
        lostAtCommand = AtCommand.SET_NETWORK
        mFscBleCentralApi.setNetwork(ssid, password, this)
    }

    fun startOTA(version: String) {
        lostAtCommand = AtCommand.OTA
        mFscBleCentralApi.startOTA(version, this)
    }

    fun send(byteArray: ByteArray) {
        mFscBleCentralApi.send(byteArray)
    }

    fun isConnect() = mFscBleCentralApi.isConnect

    override fun blePeripheralFound(// 把从FscBleCentralApiImp.jva里面扫描到的设备回调到BluetoothRepository.kt里面
        device: BluetoothDeviceWrapper?, rssi: Int, record: ByteArray?
    ) {
        if (rssiValue > rssi) return
        if (nameSwitch) {
            device?.name?.let {
                if (!it.contains(nameValue)) {
                    return
                }
            } ?: let {
                return
            }
        }
        device?.let {
            try {
                mCallbacks.forEach {
                    it.blePeripheralFound(device, record)// 把rssi信号值和名称过滤处理后，再回调给DeviceFragment.kt
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun blePeripheralConnected(gatt: BluetoothGatt?, device: BluetoothDevice?) {
        LogUtils.d(TAG,"APP WIFI 连接成功.")
        mHandler.removeCallbacks(mConnectTimeoutRunnable)
        try {
            mCallbacks.forEach {
                it.blePeripheralConnected()// 把连接成功的消息回调给DeviceFragment.kt
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun blePeripheralDisconnected(
        gatt: BluetoothGatt?, device: BluetoothDevice?, isPinError: Boolean
    ) {
        lostAtCommand = AtCommand.NONE
        try {
            mCallbacks.forEach {
                it.blePeripheralDisconnected()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun packetReceived(
        device: BluetoothDevice?, strValue: String?, hexString: String?, rawValue: ByteArray?
    ) {
        try {
            mCallbacks.forEach {
                it.packetReceived(strValue)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun packetSend(
        device: BluetoothDevice?, strValue: String?, hexString: String?, rawValue: ByteArray?
    ) {
        try {
            mCallbacks.forEach {
                it.packetSend(strValue)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun ipInformation(ip: String?) {
        LogUtils.d(TAG,"APP WIFI ipInformation ip => $ip")

        try {
            mCallbacks.forEach {
                it.ipInformation(ip)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun versionInformation(version: String?) {
        LogUtils.d(TAG,"APP WIFI versionInformation version => $version")

        try {
            mCallbacks.forEach {
                it.versionInformation(version)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun success(ip: String?) {

        ip?.let {
            try {
                mCallbacks.forEach {
                    it.success(ip)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun failure() {
        try {
            mCallbacks.forEach {
                it.failure()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onOtaSuccess() {
        try {
            mCallbacks.forEach {
                it.onOtaSuccess()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onOtaFailure() {
        try {
            mCallbacks.forEach {
                it.onOtaFailure()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onOtaProgress(progress: String?) {
        try {
            mCallbacks.forEach {
                it.onOtaProgress(progress)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onOtaNetworkIsNotConfigured() {
        try {
            mCallbacks.forEach {
                it.onOtaNetworkIsNotConfigured()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onOtaStartConfig() {
        try {
            mCallbacks.forEach {
                it.onOtaStartConfig()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}