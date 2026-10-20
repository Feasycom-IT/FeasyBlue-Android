package com.feasycom.feasyblue.activity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import com.feasycom.ble.controler.FscBleCentralApi
import com.feasycom.ble.controler.FscBleCentralApiImp
import com.feasycom.ble.controler.FscBleCentralCallbacksImp
import com.feasycom.common.bean.ConnectType
import com.feasycom.common.bean.FscDevice
import com.feasycom.common.utils.Constant
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivityParameterModifyInformationBinding
import com.feasycom.feasyblue.utils.getOrderedStringSet
import com.feasycom.spp.controler.FscSppCentralApi
import com.feasycom.spp.controler.FscSppCentralApiImp
import com.feasycom.spp.controler.FscSppCentralCallbacksImp
import java.util.Calendar

/**
 * 参数修改信息页面
 * 适配 ViewBinding 模式，优化空安全和内存管理
 */
class ParameterModifyInformationActivity :
    BaseActivity<ActivityParameterModifyInformationBinding>() {

    private lateinit var commandSet: LinkedHashSet<String>
    private var bleApi: FscBleCentralApi? = null // 改为可空，避免未初始化风险
    private var sppApi: FscSppCentralApi? = null // 改为可空，避免未初始化风险
    private lateinit var mFscDevice: FscDevice
    private var deviceMode = Constant.BLE_MODE
    private val modifyInformation = StringBuffer()

    // 实现 BaseActivity 的抽象方法，提供 ViewBinding 实例
    override fun getViewBinding(): ActivityParameterModifyInformationBinding {
        return ActivityParameterModifyInformationBinding.inflate(layoutInflater)
    }

    @SuppressLint("MissingPermission")
    override fun initView() {
        // 1. 获取并校验传递的设备信息（优化空安全）
        mFscDevice = intent.getParcelableExtra<FscDevice>("FscDevice") ?: run {
            finish()
            return
        }

        // 2. 设置工具栏（改用 ViewBinding 访问控件）
        setSupportActionBar(binding.paramModifyInfo)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }
        binding.paramModifyInfo.setNavigationOnClickListener {
            disconnect()
            finish()
        }

        // 3. 设置标题（改用 ViewBinding 访问控件）
        binding.paramModifyTitle.text = mFscDevice.device.name

        // 4. 初始化基础数据
        deviceMode = mFscDevice.mode
        commandSet = getOrderedStringSet(this)

        // 5. 校验基础数据，无效则结束页面
        if (commandSet.isEmpty()) {
            finish()
            return
        }

        // 6. 根据连接模式初始化蓝牙 API 并连接
        initBluetoothApiAndConnect()

        // 7. 初始化设备信息展示
        initDeviceInfoDisplay()
    }

    /**
     * 初始化蓝牙 API 并建立连接（拆分逻辑，提高可读性）
     */
    @SuppressLint("MissingPermission")
    private fun initBluetoothApiAndConnect() {
        if (deviceMode == Constant.BLE_MODE) {
            initBleApi()
            bleApi?.connectToModify(mFscDevice.address)
        } else {
            initSppApi()
            sppApi?.connectToModify(mFscDevice.address)
        }
    }

    /**
     * 初始化 BLE 蓝牙 API 及回调
     */
    private fun initBleApi() {
        bleApi = FscBleCentralApiImp.getInstance()
        bleApi?.setCallbacks(object : FscBleCentralCallbacksImp() {
            override fun blePeripheralConnected(
                gatt: BluetoothGatt?,
                address: String,
                connectType: ConnectType
            ) {
                bleApi?.sendATCommand(mFscDevice.address, commandSet)
            }

            override fun blePeripheralDisconnected(
                gatt: BluetoothGatt?,
                address: String,
                status: Int
            ) {
                if (!binding.paramModifyInfoTv.text.contains(getString(R.string.disconnected))) {
                    addState(getString(R.string.disconnected))
                }
            }

            override fun atCommandCallBack(
                command: String?,
                param: String?,
                type: Int,
                status: Int
            ) {
                uiATCommand(status, type, command, param)
            }

            override fun startATCommand() {
                addState(getString(R.string.openEngineSuccess) + "\r\n")
            }

            override fun endATCommand() {
                disconnect()
            }
        })
    }

    /**
     * 初始化 SPP 蓝牙 API 及回调
     */
    private fun initSppApi() {
        sppApi = FscSppCentralApiImp.getInstance()
        sppApi?.setCallbacks(object : FscSppCentralCallbacksImp() {
            override fun sppPeripheralConnected(
                device: BluetoothDevice,
                connectType: ConnectType
            ) {
                super.sppPeripheralConnected(device, connectType)
                sppApi?.sendATCommand(mFscDevice.address, commandSet)
            }

            override fun sppPeripheralDisconnected(address: String?) {
                super.sppPeripheralDisconnected(address)
                if (!binding.paramModifyInfoTv.text.contains(getString(R.string.disconnected))) {
                    addState(getString(R.string.disconnected))
                }
            }

            override fun atCommandCallBack(
                command: String?,
                param: String?,
                type: Int,
                status: Int
            ) {
                uiATCommand(status, type, command, param)
            }

            override fun startATCommand() {
                addState(getString(R.string.openEngineSuccess) + "\r\n")
            }

            override fun endATCommand() {
                disconnect()
            }
        })
    }

    /**
     * 初始化设备信息展示
     */
    @SuppressLint("MissingPermission")
    private fun initDeviceInfoDisplay() {
        modifyInformation.append("${resources.getString(R.string.name)} ${mFscDevice.device.name}\r\n")
            .append("${resources.getString(R.string.addr)} ${mFscDevice.device.address}\r\n\r\n")
        runOnUiThread {
            binding.paramModifyInfoTv.text = modifyInformation
        }
    }

    /**
     * 处理 AT 指令回调
     */
    private fun uiATCommand(status: Int, type: Int, command: String?, param: String?) {
        when (status) {
            Constant.COMMAND_SUCCESSFUL -> handleCommandSuccess(type, command, param)
            Constant.COMMAND_FAILED -> handleCommandFailure(type, command)
            Constant.COMMAND_TIME_OUT -> handleCommandTimeout(type, command, param)
        }
    }

    /**
     * 处理 AT 指令成功
     */
    private fun handleCommandSuccess(type: Int, command: String?, param: String?) {
        when (type) {
            Constant.COMMAND_STATE_QUERY -> {
                addState("${getString(R.string.read)} $command ${getString(R.string.success)}\r\n")
                addState("$param \r\n")
            }
            Constant.COMMAND_STATE_SET -> {
                addState("${getString(R.string.modify)} $command ${getString(R.string.success)}\r\n")
            }
        }
    }

    /**
     * 处理 AT 指令失败
     */
    private fun handleCommandFailure(type: Int, command: String?) {
        Log.e("vvv","========handleCommandFailure=======${command}====")

        when (type) {
            Constant.COMMAND_STATE_QUERY -> {
                addState("${getString(R.string.read)} $command ${getString(R.string.failed)}\r\n")
            }
            Constant.COMMAND_STATE_SET -> {
                addState("${getString(R.string.modify)} $command ${getString(R.string.failed)}\r\n")
            }
        }
        runOnUiThread {
            binding.paramModifyInfoTv.setTextColor(Color.RED)
        }
    }

    /**
     * 处理 AT 指令超时
     */
    private fun handleCommandTimeout(type: Int, command: String?, param: String?) {
        when (type) {
            Constant.COMMAND_STATE_QUERY -> {
                addState("${getString(R.string.read)} $command ${getString(R.string.timeout)}\r\n")
                addState("$param \r\n")
            }
            Constant.COMMAND_STATE_SET -> {
                addState("${getString(R.string.modify)} $command ${getString(R.string.timeout)}\r\n")
            }
        }
    }

    /**
     * 添加状态信息到文本框
     */
    private fun addState(message: String) {
        val currentTime = Calendar.getInstance().run {
            String.format(
                "【%02d:%02d:%02d.%03d】",
                get(Calendar.HOUR_OF_DAY),
                get(Calendar.MINUTE),
                get(Calendar.SECOND),
                get(Calendar.MILLISECOND)
            )
        }
        modifyInformation.append("$currentTime$message")
        runOnUiThread {
            binding.paramModifyInfoTv.text = modifyInformation
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
        // 清空回调和 API 引用，避免内存泄漏
      /*  bleApi?.setCallbacks(null)
        sppApi?.setCallbacks(null)
        bleApi = null
        sppApi = null*/
    }

    /**
     * 断开蓝牙连接
     */
    private fun disconnect() {
        if (deviceMode == Constant.BLE_MODE) {
            bleApi?.disconnect(mFscDevice.address)
        } else {
            sppApi?.disconnect(mFscDevice.address)
        }
    }

    companion object {
        fun activityStart(context: Context, fscDevice: FscDevice) {
            val intent = Intent(context, ParameterModifyInformationActivity::class.java)
            intent.putExtra("FscDevice", fscDevice)
            context.startActivity(intent)
        }
    }
}