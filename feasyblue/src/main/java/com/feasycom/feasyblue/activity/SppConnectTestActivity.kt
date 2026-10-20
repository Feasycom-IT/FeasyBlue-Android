package com.feasycom.feasyblue.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.text.method.TextKeyListener
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.LogUtils
import com.feasycom.common.bean.FscDevice
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.SettingSppConnectTestBinding
import com.feasycom.feasyblue.utils.FEShare
import com.feasycom.feasyblue.utils.FileUtils
import com.feasycom.feasyblue.utils.HexKeyListener
import com.feasycom.feasyblue.utils.getBoolean
import com.feasycom.feasyblue.utils.getByteArray
import com.feasycom.feasyblue.utils.getEncoding
import com.feasycom.feasyblue.utils.putBoolean
import com.feasycom.feasyblue.utils.putByteArray
import com.swallowsonny.convertextlibrary.hex2ByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.IndexOutOfBoundsException
import java.util.*
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

/**
 * SPP 连接测试页面
 * 适配 ViewBinding 模式，优化空安全和内存管理
 */
class SppConnectTestActivity : BaseActivity<SettingSppConnectTestBinding>(), View.OnClickListener {

    // 初始化一些变量
    private val share = FEShare.getInstance()
    private val strState = StringBuffer()
    private var cStart: Calendar = Calendar.getInstance()
    private var cEnd: Calendar = Calendar.getInstance()

    private var okCount = 0
    private var failCount = 0
    private var mByteArray: ByteArray = byteArrayOf()
    private var isFinish = false
    private var isTesting = false
    private var mResultCode: Int = 0
    private var mData: Intent? = null
    private lateinit var mFscDevice: FscDevice

    // 懒加载 HexKeyListener
    private val hexKeyListener: HexKeyListener by lazy { HexKeyListener() }

    // 修复：使用 Handler 避免内存泄漏
    private val handler = Handler(Looper.getMainLooper())
    private val hexClickRunnable = Runnable {
        binding.sppHexCheck.isEnabled = true
    }

    // 实现 BaseActivity 的 ViewBinding 获取方法
    override fun getViewBinding(): SettingSppConnectTestBinding {
        return SettingSppConnectTestBinding.inflate(layoutInflater)
    }

    // 初始化视图
    override fun initView() {
        initToolbar()

        // 从 Intent 获取设备信息（优化空安全）
        val fscDevice = intent?.getParcelableExtra<FscDevice>("FscDevice")
        if (fscDevice == null) {
            finish() // 设备信息为空，直接结束页面
            return
        }
        mFscDevice = fscDevice
        binding.sppTestToolbarTitle.text = fscDevice.name

        // 初始化 HEX 模式
        initHexMode()

        // 初始化事件
        initEvent()
    }

    // 初始化工具栏
    private fun initToolbar() {
        setSupportActionBar(binding.sppToolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }
        binding.sppToolbar.setNavigationOnClickListener { finish() }
    }

    // 初始化 HEX 显示模式
    private fun initHexMode() {
        binding.sppHexCheck.isOpened = getBoolean("sppHexCheck")
        mByteArray = getByteArray("sppSendEdit")

        binding.sppSendEditEt.apply {
            keyListener = if (binding.sppHexCheck.isOpened) hexKeyListener else TextKeyListener.getInstance()
            setText(if (binding.sppHexCheck.isOpened) mByteArray.toHexString() else String(mByteArray))
        }
    }

    // 初始化事件
    private fun initEvent() {
        // 绑定点击事件
        binding.sppStartTestBtn.setOnClickListener(this)
        binding.sppStopTestBtn.setOnClickListener(this)
        binding.sppClearDataBtn.setOnClickListener(this)
        binding.sppSendFileTestBtn.setOnClickListener(this)
        binding.sppHexCheck.setOnClickListener(this)

        // 输入框文本变化监听
        binding.sppSendEditEt.addTextChangedListener { s ->
            mByteArray = when {
                s.isNullOrEmpty() -> byteArrayOf()
                binding.sppHexCheck.isOpened -> s.toString().hex2ByteArray()
                else -> s.toString().toByteArray()
            }
            putByteArray("sppSendEdit", mByteArray)
        }
    }

    // 处理点击事件
    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.spp_start_test_btn -> {
                binding.sppStartTestBtn.isEnabled = false
                binding.sppStopTestBtn.isEnabled = true
                isTesting = true
                connect()
            }
            R.id.spp_stop_test_btn -> {
                binding.sppStartTestBtn.isEnabled = true
                binding.sppStopTestBtn.isEnabled = false
                isTesting = false
                share.disConnect()
            }
            R.id.spp_clear_data_btn -> {
                strState.setLength(0)
                binding.sppReceiveEt.setText("")
            }
            R.id.spp_send_file_test_btn -> {
                val intent = Intent(this@SppConnectTestActivity, SelectFileActivity::class.java)
                startActivityForResult(intent, ThroughputActivity.SELECT_FILE)
            }
            R.id.spp_hex_check -> {
                binding.sppHexCheck.isEnabled = false
                handler.postDelayed(hexClickRunnable, 500)

                measureTimeMillis {
                    putBoolean("sppHexCheck", binding.sppHexCheck.isOpened)

                    if (binding.sppHexCheck.isOpened) {
                        // HEX 模式
                        val hexString = mByteArray.toHexString()
                        binding.sppSendEditEt.apply {
                            keyListener = hexKeyListener
                            setText(hexString)
                            setSelectionSafe(hexString.length)
                        }
                    } else {
                        // 普通文本模式
                        if (mByteArray.isNotEmpty()) {
                            val string = mByteArray.getEncoding()
                            binding.sppSendEditEt.apply {
                                keyListener = TextKeyListener.getInstance()
                                setText(string)
                                setSelectionSafe(string.length)
                            }
                        } else {
                            binding.sppSendEditEt.keyListener = TextKeyListener.getInstance()
                        }
                    }
                }
            }
        }
    }

    // 安全设置光标位置，避免索引越界
    private fun EditText.setSelectionSafe(index: Int) {
        try {
            setSelection(index)
        } catch (e: IndexOutOfBoundsException) {
            setSelection(if (index > 0) index - 1 else 0)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        LogUtils.d(TAG, "APP SPP onActivityResult: $requestCode    $resultCode")
        binding.sppStartTestBtn.isEnabled = false

        if (requestCode != ThroughputActivity.SELECT_FILE) return

        if (resultCode == FILE_PATH || resultCode == FILE_SIZE) {
            mResultCode = resultCode
            mData = data
            sendFile()
        }
    }

    // 发送文件/生成测试数据
    private fun sendFile() {
        when (mResultCode) {
            FILE_SIZE -> handleFileSizeMode()
            FILE_PATH -> handleFilePathMode()
        }
    }

    // 处理文件大小模式（生成测试数据）
    private fun handleFileSizeMode() {
        val size = mData?.getIntExtra("size", 0) ?: 0
        if (size <= 0) {
            Toast.makeText(this, "无效的文件大小: $size", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val startTime = System.currentTimeMillis()
                val testData = withContext(Dispatchers.IO) {
                    FileUtils.createTestData1(size) // 在 IO 线程生成测试数据
                }
                val endTime = System.currentTimeMillis()
                Log.d(TAG, "测试数据生成耗时: ${endTime - startTime} ms")

                withContext(Dispatchers.Main) {
                    if (testData.isNotEmpty()) {
                        binding.sppStartTestBtn.isEnabled = true
                        binding.sppSendEditEt.setText("测试数据生成成功，大小：${testData.size / 1024} KB")
                        mByteArray = testData
                    } else {
                        Toast.makeText(this@SppConnectTestActivity, "测试数据生成失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@SppConnectTestActivity,
                        "生成测试数据时出错：${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // 处理文件路径模式（读取本地文件）
    private fun handleFilePathMode() {
        val uri = mData?.data ?: run {
            Toast.makeText(this, "文件URI为空", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val startTime = System.currentTimeMillis()
                val byteArray = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStreamToByteArray(inputStream) // 优化后的文件读取
                    }
                }
                val endTime = System.currentTimeMillis()
                Log.d(TAG, "文件读取耗时: ${endTime - startTime} ms")

                withContext(Dispatchers.Main) {
                    if (byteArray != null && byteArray.isNotEmpty()) {
                        binding.sppStartTestBtn.isEnabled = true
                        binding.sppSendEditEt.setText("文件加载成功，大小：${byteArray.size / 1024} KB")
                        mByteArray = byteArray
                    } else {
                        Toast.makeText(this@SppConnectTestActivity, "文件读取失败或为空", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@SppConnectTestActivity,
                        "读取文件时出错：${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // 16进制字符串转普通字符串
    fun hexStringToString(hexString: String, charset: String = "UTF-8"): String {
        require(hexString.length % 2 == 0) { "Hex string must have an even length" }

        val byteArray = hexString.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()

        return try {
            String(byteArray, charset(charset))
        } catch (e: Exception) {
            String(byteArray) // 兜底处理
        }
    }

    // InputStream 转 ByteArray（分块读取，优化性能）
    @Throws(IOException::class)
    private fun inputStreamToByteArray(inputStream: InputStream): ByteArray {
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(8 * 1024) // 8KB 分块
        var bytesRead: Int
        while (inputStream.read(chunk).also { bytesRead = it } != -1) {
            buffer.write(chunk, 0, bytesRead)
        }
        return buffer.toByteArray()
    }

    // 连接设备并发送数据
    @SuppressLint("MissingPermission")
    private fun connect() {
        if (isFinish || !isTesting) return

        val remoteDevice = share.bluetoothAdapter.getRemoteDevice(mFscDevice.address.toString())
        val strCount = (okCount + 1).toString()

        // 添加测试状态
        addState(getString(R.string.No_) + strCount + getString(R.string.No_2) + getString(R.string.startTest))
        addState(getString(R.string.start) + getString(R.string.connect) + "[" + remoteDevice.name + "]")
        appendInterval()

        thread {
            if (share.connect(remoteDevice) && isTesting) {
                Log.i(TAG, "连接成功")
                connectedToDo()

                try {
                    Thread.sleep(200)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

                // 发送数据
                val length = share.writes(mByteArray)
                if (length > 0) {
                    addState("${getString(R.string.btnSends)}${mByteArray.size}bytes\r\n****************************")
                    successful()
                } else {
                    addState(getString(R.string.btnSends) + getString(R.string.failure) + "\n****************************")
                    failure()
                }
            } else {
                // 连接失败
                val failMsg = getString(R.string.connect) + getString(R.string.failure) + "\n****************************"
                addState(failMsg)
                failure()
            }
        }
    }

    // 添加状态信息到文本框
    private fun addState(text: CharSequence) {
        // 限制日志长度，避免内存溢出
        if (strState.length > 10000) {
            strState.delete(0, 3000)
        }

        cStart = cEnd
        val c = Calendar.getInstance()
        cEnd = c

        // 格式化时间
        val strTime = String.format(
            "【%02d:%02d:%02d】",
            c[Calendar.HOUR_OF_DAY],
            c[Calendar.MINUTE],
            c[Calendar.SECOND]
        )

        strState.append("${strTime}${text}\r\n")

        // 更新UI
        runOnUiThread {
            binding.sppReceiveEt.setText(strState)
            if (binding.sppReceiveEt.text.length > 100) {
                binding.sppReceiveEt.setSelection(binding.sppReceiveEt.text.length - 1)
            }
        }
    }

    // 添加时间间隔信息
    private fun appendInterval() {
        val interval = cEnd.time.time - cStart.time.time
        val intervalStr = """ - $interval ms
"""
        // 安全替换，避免索引越界
        if (strState.length >= 2) {
            strState.replace(strState.length - 2, strState.length, intervalStr)
        } else {
            strState.append(intervalStr)
        }

        runOnUiThread {
            binding.sppReceiveEt.setText(strState)
            if (binding.sppReceiveEt.text.length > 100) {
                binding.sppReceiveEt.setSelection(binding.sppReceiveEt.text.length - 1)
            }
        }
    }

    // 连接成功后的处理
    @SuppressLint("MissingPermission")
    private fun connectedToDo() {
        val successMsg = getString(R.string.connect) + "[" + share.device.name + "]" + getString(R.string.success)
        addState(successMsg)
        appendInterval()
    }

    // 数据发送成功后的处理
    private fun successful() {
        okCount++
        runOnUiThread { binding.sppTestSuccessCountTv.text = okCount.toString() }

        thread {
            if (!isTesting) return@thread

            try {
                // 获取并校验断开连接时间
                val disconnectTime = getValidatedTime(
                    binding.sppDisconnectTimeEt.text.toString(),
                    binding.sppDisconnectTimeEt
                )
                LogUtils.d(TAG, "APP SPP successful disconnectTime => $disconnectTime")
                Thread.sleep(disconnectTime.toLong())

                // 断开连接
                share.disConnect()

                // 获取并校验重连时间
                val reconnectTime = getValidatedTime(
                    binding.sppReconnectTimeEt.text.toString(),
                    binding.sppReconnectTimeEt
                )
                LogUtils.d(TAG, "APP SPP successful connectTime => $reconnectTime")
                Thread.sleep(reconnectTime.toLong())

                // 重新连接
                connect()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    // 验证时间输入（修复前导0问题）
    private fun getValidatedTime(
        input: String,
        editText: EditText,
        defaultTime: Long = 1000
    ): Long {
        return try {
            val sanitizedInput = input.trimStart('0').ifEmpty { "1" }
            val time = sanitizedInput.toLong() * 1000 // 转换为毫秒
            if (time <= 0) defaultTime else time
        } catch (e: NumberFormatException) {
            runOnUiThread { editText.setText("1") }
            defaultTime
        }
    }

    // 数据发送失败后的处理
    private fun failure() {
        failCount++
        runOnUiThread { binding.sppTestFailCountTv.text = failCount.toString() }

        thread {
            if (!isTesting) return@thread

            try {
                // 获取并校验重连时间
                val reconnectTime = getValidatedTime(
                    binding.sppReconnectTimeEt.text.toString(),
                    binding.sppReconnectTimeEt
                )
                LogUtils.d(TAG, "APP SPP failure connectTime => $reconnectTime")
                Thread.sleep(reconnectTime.toLong())

                // 重新连接
                connect()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    // ByteArray 转 16进制字符串扩展方法（修复未解析错误）
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

    // 活动销毁时的处理
    override fun onDestroy() {
        super.onDestroy()
        // 清理资源，避免内存泄漏
        isFinish = true
        isTesting = false
        share.disConnect()
        handler.removeCallbacks(hexClickRunnable) // 移除未执行的Runnable
    }

    // 定义伴生对象，包含静态常量和方法
    companion object {
        private const val TAG = "SppConnectTestActivity"
        const val FILE_PATH = 1001 // 文件路径返回码
        const val FILE_SIZE = 1002 // 文件大小返回码

        /**
         * 启动 SPP 连接测试页面
         */
        fun activityStart(context: Context, fscDevice: FscDevice) {
            Intent(context, SppConnectTestActivity::class.java).apply {
                putExtra("FscDevice", fscDevice)
            }.also {
                context.startActivity(it)
            }
        }
    }
}