package com.feasycom.feasyblue.activity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.method.TextKeyListener
import android.view.View
import androidx.core.widget.addTextChangedListener
import com.blankj.utilcode.util.LogUtils
import com.feasycom.ble.controler.FscBleCentralApi
import com.feasycom.ble.controler.FscBleCentralApiImp
import com.feasycom.ble.controler.FscBleCentralCallbacksImp
import com.feasycom.common.bean.ConnectType
import com.feasycom.common.bean.FscDevice
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.SettingBleConnectTestBinding
import com.feasycom.feasyblue.utils.HexKeyListener
import com.feasycom.feasyblue.utils.getBoolean
import com.feasycom.feasyblue.utils.getByteArray
import com.feasycom.feasyblue.utils.putBoolean
import com.feasycom.feasyblue.utils.putByteArray
import com.swallowsonny.convertextlibrary.hex2ByteArray
import java.util.Calendar

/**
 * BLE 连接测试页面
 * 适配 ViewBinding 模式，优化空安全和内存管理
 */
class BleConnectTestActivity : BaseActivity<SettingBleConnectTestBinding>(), View.OnClickListener {

    private lateinit var mFscDevice: FscDevice
    private lateinit var mBleApi: FscBleCentralApi
    private val testStatus = StringBuffer()

    // 修复：使用 Handler.Callback 避免内存泄漏
    private val handler = Handler(Looper.getMainLooper(), Handler.Callback { handleMessage(it) })

    private var mByteArray: ByteArray = byteArrayOf()
    private var okCount = 0
    private var bleConnectFailCount = 0
    private var testIsFinish = false
    private var connecting = false
    private val connectTimeout = 10000 // 连接超时时间（10秒）

    // 实现 BaseActivity 的 ViewBinding 获取方法
    override fun getViewBinding(): SettingBleConnectTestBinding {
        return SettingBleConnectTestBinding.inflate(layoutInflater)
    }

    @SuppressLint("SetTextI18n")
    override fun initView() {
        // 初始化工具栏
        initToolbar()

        // 从 Intent 获取设备信息（优化空安全）
        val fscDevice = intent?.getParcelableExtra<FscDevice>("FscDevice")
        if (fscDevice == null) {
            finish() // 设备信息为空，直接结束页面
            return
        }
        mFscDevice = fscDevice
        binding.bleTestToolbarTitle.text = fscDevice.name

        // 初始化 BLE API
        initBleApi()

        // 设置 HEX 显示模式
        initHexMode()

        // 初始化界面事件
        initEvent()
    }

    /**
     * 初始化工具栏
     */
    private fun initToolbar() {
        setSupportActionBar(binding.bleToolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }
        binding.bleToolbar.setNavigationOnClickListener { finish() }
    }

    /**
     * 初始化 BLE API 及回调
     */
    private fun initBleApi() {
        mBleApi = FscBleCentralApiImp.getInstance().apply {
            setCallbacks(object : FscBleCentralCallbacksImp() {
                // BLE 设备连接成功的回调
                override fun blePeripheralConnected(
                    gatt: BluetoothGatt?,
                    address: String?,
                    type: ConnectType?
                ) {
                    LogUtils.e(TAG, "APP BLE blePeripheralConnected: BLE连接成功.")
                    connecting = false
                    connectedToDo()
                    handler.removeMessages(4) // 移除超时消息
                    handler.sendEmptyMessageDelayed(1, 1000) // 延迟发送数据
                }

                override fun sendPacketProgress(
                    address: String?,
                    percentage: Float,
                    sendByte: ByteArray?
                ) {
                    LogUtils.e(TAG, "APP BLE sendPacketProgress percentage => $percentage")
                    if (percentage == 100.0f) {


                    }
                }

                // BLE 设备断开连接的回调
                override fun blePeripheralDisconnected(
                    gatt: BluetoothGatt?,
                    address: String,
                    status: Int
                ) {
                    LogUtils.e(TAG, "APP BLE blePeripheralDisconnected: BLE断开连接.")
                    connecting = false
                    handler.removeMessages(4) // 移除超时消息

                    // 获取重连延迟时间（优化空安全）
                    val reconnectDelay = binding.bleReconnectTimeEt.text.toString().toLongOrNull() ?: 1
                    handler.sendEmptyMessageDelayed(3, reconnectDelay * 1000)
                }

                override fun servicesFound(
                    gatt: BluetoothGatt?,
                    address: String?,
                    services: List<BluetoothGattService?>?
                ) {
                    super.servicesFound(gatt, address, services)

                    LogUtils.e(TAG, "APP BLE servicesFound => $address")
                    // 获取断开连接延迟时间（优化空安全）
                    val disconnectDelay = binding.bleDisconnectTimeEt.text.toString().toLongOrNull() ?: 1
                    handler.sendEmptyMessageDelayed(2, disconnectDelay * 1000)


                }
            })
        }
    }

    /**
     * 初始化 HEX 显示模式
     */
    private fun initHexMode() {
        binding.bleHexCheck.isOpened = getBoolean("bleHexCheck")
        mByteArray = getByteArray("bleSendEdit")

        binding.bleSendEditEt.apply {
            keyListener = if (binding.bleHexCheck.isOpened) HexKeyListener() else TextKeyListener.getInstance()
            setText(if (binding.bleHexCheck.isOpened) mByteArray.toHexString() else String(mByteArray))
        }
    }

    /**
     * 初始化界面事件
     */
    private fun initEvent() {
        // 绑定点击事件
        binding.bleStartTestBtn.setOnClickListener(this)
        binding.bleClearDataBtn.setOnClickListener(this)
        binding.bleHexCheck.setOnClickListener(this)

        // 输入框文本变化监听
        binding.bleSendEditEt.addTextChangedListener { s ->
            mByteArray = when {
                s.isNullOrEmpty() -> byteArrayOf()
                binding.bleHexCheck.isOpened -> s.toString().hex2ByteArray()
                else -> s.toString().toByteArray()
            }
            putByteArray("bleSendEdit", mByteArray)
        }
    }

    // 处理点击事件
    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.ble_start_test_btn -> {
                binding.bleStartTestBtn.isEnabled = false
                connect(mFscDevice.address)
            }
            R.id.ble_clear_data_btn -> {
                testStatus.setLength(0)
                binding.bleReceiveEt.setText("")
            }
            R.id.ble_hex_check -> {
                binding.bleHexCheck.isEnabled = false
                handler.postDelayed({ binding.bleHexCheck.isEnabled = true }, 500)
                toggleHexCheck()
            }
        }
    }

    // 处理 Handler 消息
    private fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            1 -> sendData() // 发送数据
            2 -> disconnect() // 断开连接
            3 -> connect(mFscDevice.address) // 重新连接
            4 -> handleConnectTimeout() // 连接超时
        }
        return true
    }

    // 处理连接超时
    private fun handleConnectTimeout() {
        connecting = false
        bleConnectFailCount++ // 修复：原代码错误累加 okCount，改为累加失败次数

        val bleConnectFailed = "${getString(R.string.connect)}${getString(R.string.failure)}\n****************************"
        addState(bleConnectFailed)

        runOnUiThread { binding.bleTestFailCountTv.text = bleConnectFailCount.toString() }

        if (connecting) disconnect()

        // 发起重连
        val reconnectDelay = binding.bleReconnectTimeEt.text.toString().toLongOrNull() ?: 1
        handler.sendEmptyMessageDelayed(3, reconnectDelay * 1000)
        LogUtils.e(TAG, "APP BLE 连接超时失败，发起重连!")
    }

    // 切换 HEX 显示模式
    private fun toggleHexCheck() {
        putBoolean("bleHexCheck", binding.bleHexCheck.isOpened)
        val text = if (binding.bleHexCheck.isOpened) mByteArray.toHexString() else String(mByteArray)

        binding.bleSendEditEt.apply {
            keyListener = if (binding.bleHexCheck.isOpened) HexKeyListener() else TextKeyListener.getInstance()
            setText(text)

            // 优化光标位置
            if (text.isNotEmpty()) {
                setSelection(text.length - 1)
            }
        }
    }

    // 添加状态信息到测试状态字符串
    private fun addState(text: CharSequence) {
        // 限制日志长度，避免内存溢出
        if (testStatus.length > 10000) {
            testStatus.delete(0, 3000)
        }

        // 格式化时间
        val currentTime = Calendar.getInstance()
        val strTime = String.format(
            "【%02d:%02d:%02d】",
            currentTime[Calendar.HOUR_OF_DAY],
            currentTime[Calendar.MINUTE],
            currentTime[Calendar.SECOND]
        )

        testStatus.append("$strTime$text\r\n")

        // 更新UI
        runOnUiThread {
            binding.bleReceiveEt.setText(testStatus)
            // 滚动到最后一行
            val lastIndex = testStatus.length.coerceAtMost(binding.bleReceiveEt.text.length - 1)
            binding.bleReceiveEt.setSelection(lastIndex)
        }
    }

    // BLE 连接成功后的处理
    private fun connectedToDo() {
        val successMsg = "${getString(R.string.connect)}[${mFscDevice.name}]${getString(R.string.success)}"
        addState(successMsg)

        addState("${getString(R.string.btnSends)}${mByteArray.size}bytes\r\n****************************")
        okCount++
        runOnUiThread { binding.bleTestSuccessCountTv.text = okCount.toString() }

    }

    // 连接 BLE 设备
    @SuppressLint("MissingPermission")
    private fun connect(address: String) {
        if (testIsFinish || connecting) return

        // 添加测试开始状态

        addState("${getString(R.string.start)}${getString(R.string.connect)}[${mFscDevice.name}]")
        addState("${getString(R.string.No_)}${okCount + 1}${getString(R.string.No_2)}${getString(R.string.startTest)}")
        // 发起连接
        mBleApi.connect(address, getBoolean("facpSwitchOpen", false))
        connecting = true
        // 设置连接超时监听
        handler.sendEmptyMessageDelayed(4, connectTimeout.toLong())
    }

    // 发送数据
    @SuppressLint("MissingPermission")
    private fun sendData() {
        mBleApi.send(mByteArray)
    }

    // 断开 BLE 连接
    @SuppressLint("MissingPermission")
    private fun disconnect() {
        mBleApi.disconnect(mFscDevice.address)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理资源，避免内存泄漏
        testIsFinish = true
        handler.removeCallbacksAndMessages(null) // 移除所有未处理的消息
        disconnect() // 断开连接
    }

    // 核心修复：实现 ByteArray 转 16 进制字符串的扩展方法
    private fun ByteArray.toHexString(): String {
        val hexChars = CharArray(size * 2)
        val hexArray = "0123456789ABCDEF".toCharArray()

        for (i in indices) {
            val v = this[i].toInt() and 0xFF
            hexChars[i * 2] = hexArray[v ushr 4]
            hexChars[i * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    companion object {
        private const val TAG = "BleConnectTest"

        /**
         * 启动 BLE 连接测试页面
         */
        fun activityStart(context: Context, fscDevice: FscDevice) {
            Intent(context, BleConnectTestActivity::class.java).apply {
                putExtra("FscDevice", fscDevice)
            }.also {
                context.startActivity(it)
            }
        }
    }
}