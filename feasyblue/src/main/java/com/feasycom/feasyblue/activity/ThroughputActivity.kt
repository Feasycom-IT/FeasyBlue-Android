package com.feasycom.feasyblue.activity

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.method.TextKeyListener
import android.util.Log
import android.view.View
import android.widget.Chronometer
import androidx.core.net.toFile
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.feasycom.ble.controler.FscBleCentralApi
import com.feasycom.ble.controler.FscBleCentralApiImp
import com.feasycom.ble.controler.FscBleCentralCallbacksImp
import com.feasycom.common.bean.ConnectType
import com.feasycom.common.bean.FscDevice
import com.feasycom.common.utils.Constant
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivityThroughputBinding
import com.feasycom.feasyblue.permission.PermissionDescription
import com.feasycom.feasyblue.permission.PermissionInterceptor
import com.feasycom.feasyblue.utils.FileShareHelper
import com.feasycom.feasyblue.utils.FileUtils
import com.feasycom.feasyblue.utils.HexKeyListener
import com.feasycom.feasyblue.utils.bytesToHex
import com.feasycom.feasyblue.utils.getBoolean
import com.feasycom.feasyblue.utils.getByteArray
import com.feasycom.feasyblue.utils.getEncoding
import com.feasycom.feasyblue.utils.putBoolean
import com.feasycom.feasyblue.utils.putByteArray
import com.feasycom.spp.controler.FscSppCentralApi
import com.feasycom.spp.controler.FscSppCentralApiImp
import com.feasycom.spp.controler.FscSppCentralCallbacksImp
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import com.swallowsonny.convertextlibrary.hex2ByteArray
import com.swallowsonny.convertextlibrary.toHexString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import okhttp3.internal.toHexString
import java.io.File
import java.io.InputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.CRC32
import kotlin.system.measureTimeMillis

class ThroughputActivity : BaseActivity<ActivityThroughputBinding>(), View.OnClickListener {

    private lateinit var mFscDevice: FscDevice
    private lateinit var mDevice: BluetoothDevice
    private lateinit var mBleApi: FscBleCentralApi
    private lateinit var mSppApi: FscSppCentralApi
    private var mBytesToBeSent = 0
    private var mReceiveBufferHex = StringBuffer() // 接收数据缓存buffer hex string 类型
    private var mReceiveBuffer = StringBuffer() // 接收数据缓存buffer string类型
    private var mReceiveByteCount = 0
    private var mReceivePackageCount = 0
    private var mSendByteCountSend = 0
    private var mSendPackageCountSend = 0
    private var mDeviceMode = "BLE"
    private var mSendCRC = CRC32() // 发送crc
    private var mReceiveCRC = CRC32() // 接收crc

    private var mSendFileByte = 0

    private var mIsPause = false

    private var mIsSending = false

    private var mResultCode: Int = 0
    private var mData: Intent? = null
    private var mLastStopTime: Long = 0L

    private var mByteArray: ByteArray = byteArrayOf()

    private var address: String = ""
    private var mSendIntervalTime: Long = 0L

    private var receiveData: MutableList<String> = mutableListOf()
    private var bleSendData: MutableList<String> = mutableListOf()
    private var sppSendData: MutableList<String> = mutableListOf()

    private val hexKeyListener: HexKeyListener by lazy {
        HexKeyListener()
    }

    private lateinit var handler: Handler
    private val hexClickRunnable = Runnable {
        binding.hexCheck.isEnabled = true
    }

    /**
     * 实现 ViewBinding 获取方法
     */
    override fun getViewBinding(): ActivityThroughputBinding {
        return ActivityThroughputBinding.inflate(layoutInflater)
    }

    @SuppressLint("ClickableViewAccessibility", "MissingPermission")
    override fun initView() {
        // 初始化Handler
        handler = Handler(Looper.myLooper() ?: Looper.getMainLooper())

        initToolbar()

        // 获取并处理 FscDevice 对象
        intent?.getParcelableExtra<FscDevice>("FscDevice")?.let {
            mFscDevice = it
            binding.toolbarTitle.text = it.name
            mDevice = it.device
            mDeviceMode = it.mode
            binding.toolbarSubtitle.text = resources.getString(R.string.connecting)
            address = mFscDevice.address
            binding.sendButton.isEnabled = false
            binding.sendFileButton.isEnabled = false
        }

        // 获取并处理 BluetoothDevice 对象
        intent?.getParcelableExtra<BluetoothDevice>("BluetoothDevice")?.let {
            mDevice = it
            binding.toolbarTitle.text = it.name
            mDeviceMode = Constant.SPP_MODE
            address = mDevice.address
            binding.toolbarSubtitle.text = resources.getString(R.string.through_connected)
        }

        // 检查设备是否初始化
        if (!::mFscDevice.isInitialized && !::mDevice.isInitialized) return

        // 根据连接模式初始化相应的 API
        if (mDeviceMode == Constant.BLE_MODE) {
            with(binding.switchServiceButton) {
                visibility = View.VISIBLE
                isEnabled = false
            }
            mBleApi = FscBleCentralApiImp.getInstance()
            mBleApi.setCallbacks(object : FscBleCentralCallbacksImp() {
                override fun blePeripheralConnected(
                    gatt: BluetoothGatt?,
                    address: String,
                    connectType: ConnectType
                ) {
                    LogUtils.d(TAG, "APP BLE blePeripheralConnected: BLE连接成功.")
                    uiDeviceConnected()
                }

                override fun blePeripheralDisconnected(
                    gatt: BluetoothGatt?,
                    address: String,
                    status: Int
                ) {
                    LogUtils.e(TAG, "APP BLE blePeripheralDisconnected: BLE断开连接.")
                    uiDeviceDisconnected()
                }

                override fun packetReceived(
                    address: String,
                    strValue: String,
                    hexString: String,
                    data: ByteArray
                ) {
                    LogUtils.d(
                        TAG, "APP BLE packetReceived BLE收包回调: address => $address \n" +
                                " strValue => $strValue" +
                                " data => ${data.bytesToHex()}."
                    )
                    uiReceiveData(strValue, hexString, data)
                }

                override fun sendPacketProgress(
                    address: String,
                    percentage: Float,
                    sendByte: ByteArray
                ) {
                    uiSendDataProgress(percentage, sendByte.size)
                }

                override fun packetSend(
                    address: String,
                    strValue: String,
                    data: ByteArray
                ) {//数据发送完成的回调
                  /*  LogUtils.e(
                        TAG, "APP BLE packetSend BLE发包回调: address => $address \n" +
                                " strValue => $strValue \n" +
                                " data => ${data.bytesToHex()}"
                    )*/
                    uiSendData(data)
                    if (bleSendData.size > 1000) {
                        bleSendData.removeAt(0)
                    }
                    bleSendData.add((if (binding.hexCheck.isOpened) data.bytesToHex() else strValue))
                }

                override fun onReadRemoteRssi(rssi: Int) {
                    updateRssiView(rssi)
                }

                override fun readResponse(
                    p0: String?,
                    p1: BluetoothGattCharacteristic?,
                    p2: String?,
                    p3: String?,
                    p4: ByteArray?
                ) {
                    LogUtils.d(
                        TAG, "APP BLE readResponse p0 => $p0 \n" +
                                " p2 => $p2 \n" + "p3 => $p3"
                    )
                }

            })
            if (::mFscDevice.isInitialized) {
                mBleApi.connect(address, true)
            }
            binding.rssiValueIconRl.visibility = View.VISIBLE
        } else {
            with(binding.switchServiceButton) {
                visibility = View.GONE
                isEnabled = false
            }
            mSppApi = FscSppCentralApiImp.getInstance()
            mSppApi.setCallbacks(object : FscSppCentralCallbacksImp() {
                override fun sppPeripheralConnected(
                    device: BluetoothDevice,
                    connectType: ConnectType
                ) {
                    LogUtils.d(TAG, "APP SPP sppPeripheralConnected: SPP连接成功.")
                    uiDeviceConnected()
                }

                override fun sppPeripheralDisconnected(address: String) {
                    LogUtils.e(TAG, "APP SPP sppPeripheralDisconnected: SPP断开连接.")
                    uiDeviceDisconnected()
                }

                override fun packetReceived(
                    address: String,
                    strValue: String,
                    hexString: String,
                    data: ByteArray
                ) {
                    LogUtils.d(
                        TAG, "APP SPP packetReceived SPP收包回调: address => $address \n" +
                                " strValue => $strValue" +
                                " data => ${data.bytesToHex()}"
                    )
                    uiReceiveData(strValue, hexString, data)
                }

                override fun sendPacketProgress(
                    address: String,
                    percentage: Float,
                    sendByte: ByteArray
                ) {
                    uiSendDataProgress(percentage, sendByte.size)
                }

                override fun packetSend(
                    address: String?,
                    strValue: String,
                    hexString: String,
                    data: ByteArray
                ) {
                 /*   LogUtils.e(
                        TAG, "APP SPP packetSend SPP发包回调: address => $address \n" +
                                " strValue => $strValue \n" +
                                " data => ${data.bytesToHex()}"
                    )*/
                    uiSendData(data)
                    if (sppSendData.size > 1000) {
                        sppSendData.removeAt(0)
                    }
                    sppSendData.add((if (binding.hexCheck.isOpened) data.bytesToHex() else strValue))
                }
            })
            if (::mFscDevice.isInitialized) {
                mSppApi.connect(address)
            }
            binding.rssiValueIconRl.visibility = View.GONE
        }
        binding.rateText.text = getString(R.string.rate, 0.0f)
        binding.hexCheck.isOpened = getBoolean("hexCheck")
        mByteArray = getByteArray("sendEdit")
        binding.editByteCount.text = getString(R.string.byteString, mByteArray.size)
        if (binding.hexCheck.isOpened) {
            with(binding.sendEdit) {
                keyListener = hexKeyListener
                setText(mByteArray.toHexString())
            }
        } else {
            with(binding.sendEdit) {
                keyListener = TextKeyListener.getInstance()
                setText(String(mByteArray))
            }
            binding.sendEdit.keyListener = TextKeyListener.getInstance()
        }

        // 初始化 UI 和事件
        initUi()
        initEvent()
    }

    private fun updateRssiView(rssiValue: Int) {
        runOnUiThread {
            when {
                rssiValue < -100 -> {
                    binding.rssiIconAiv.setImageResource(R.drawable.rssi0)
                    binding.rssiValueAtv.text = rssiValue.toString()
                }

                rssiValue < -85 -> {
                    binding.rssiIconAiv.setImageResource(R.drawable.rssi1)
                    binding.rssiValueAtv.text = rssiValue.toString()
                }

                rssiValue < -70 -> {
                    binding.rssiIconAiv.setImageResource(R.drawable.rssi2)
                    binding.rssiValueAtv.text = rssiValue.toString()
                }

                rssiValue < -65 -> {
                    binding.rssiIconAiv.setImageResource(R.drawable.rssi3)
                    binding.rssiValueAtv.text = rssiValue.toString()
                }

                rssiValue < -50 -> {
                    binding.rssiIconAiv.setImageResource(R.drawable.rssi4)
                    binding.rssiValueAtv.text = rssiValue.toString()
                }

                rssiValue < -35 -> {
                    binding.rssiIconAiv.setImageResource(R.drawable.rssi5)
                    binding.rssiValueAtv.text = rssiValue.toString()
                }

                else -> {
                    binding.rssiIconAiv.setImageResource(R.drawable.rssi5)
                    binding.rssiValueAtv.text = rssiValue.toString()
                }
            }
        }
    }

    private fun initToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.let {
            it.setDisplayShowTitleEnabled(false)
            it.setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initEvent() {
        binding.sendButton.setOnClickListener(this)
        binding.sendFileButton.setOnClickListener(this)
        binding.sendStatusBtn.setOnClickListener(this)
        binding.switchServiceButton.setOnClickListener(this)
        binding.export.setOnClickListener(this)
        binding.clearButton.setOnClickListener(this)
        binding.intervalSendCheck.setOnClickListener(this)
        binding.hexCheck.setOnClickListener(this)
        binding.sendNewLine.setOnClickListener(this)

        // ========== 修复：intervalSendTime 文本监听 ==========
        binding.intervalSendTime.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                mSendIntervalTime = s?.toString()?.toLongOrNull() ?: 0L
                LogUtils.d(TAG, "APP initEvent mSendIntervalTime => $mSendIntervalTime")
                setSendInterval(mSendIntervalTime)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // ========== 修复：sendEdit 文本监听 ==========
        binding.sendEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                mByteArray = if (s.isNullOrEmpty()) {
                    byteArrayOf()
                } else {
                    if (binding.hexCheck.isOpened) {
                        s.toString().hex2ByteArray()
                    } else {
                        s.toString().toByteArray()
                    }
                }

                putByteArray("sendEdit", mByteArray)
                mBytesToBeSent = mByteArray.size
                binding.editByteCount.text = getString(R.string.byteString, mBytesToBeSent)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.stopBtn.setOnClickListener {
            mIsPause = false
            mIsSending = false
            binding.sendStatusBtn.setImageDrawable(resources.getDrawable(R.drawable.start))
            stopSend()
            binding.group2.visibility = View.VISIBLE
            binding.group3.visibility = View.GONE
        }

        binding.chronometer.onChronometerTickListener = Chronometer.OnChronometerTickListener {
            runOnUiThread {
                try {
                    if ((SystemClock.elapsedRealtime() - binding.chronometer.base) != 0L) {
                        binding.rateText.text = getString(
                            R.string.rate,
                            mSendFileByte / ((SystemClock.elapsedRealtime() - binding.chronometer.base) / 1000f)
                        )
                        Log.v(TAG,"has sent Bytes:"+mSendFileByte+",   "+"used Time:"+((SystemClock.elapsedRealtime() - binding.chronometer.base) / 1000f)+",   "+"text:"+binding.rateText.text)
                    }
                } catch (e: ArithmeticException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun setSendInterval(interval: Long) {
        if (mDeviceMode == Constant.BLE_MODE) {
            mBleApi.setSendInterval(address, interval)
        } else {
            mSppApi.setSendInterval(address, interval)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != SELECT_FILE) {
            return
        }
        if (resultCode == FILE_PATH || resultCode == FILE_SIZE) {
            mResultCode = resultCode
            mData = data
            sendFile()
        }
    }

    private fun sendFile() {
        binding.rateText.text = getString(R.string.rate, 0.0f)
        when (mResultCode) {
            FILE_SIZE -> {
                val size = mData?.getIntExtra("size", 0)!!
                val sizeStr = mData?.getStringExtra("sizeStr")!!
                LogUtils.d(TAG, "FeasyBlue APP sendFile size => $size  sizeStr => $sizeStr")
                binding.selectedFileTv.text =
                    getString(R.string.selectedFile, "$sizeStr ${getString(R.string.testFile)}")
                if (mIsSending) {
                    stopSend()
                }
                sendFile {
                    if (mDeviceMode == Constant.BLE_MODE) {
                        mBleApi.sendFile(size)
                    } else {
                        mSppApi.sendFile(size)
                    }
                }
            }

            FILE_PATH -> {
                mData?.data?.let {
                    binding.selectedFileTv.text =
                        getString(R.string.selectedFile, getFileNameByUri(it))
                    contentResolver.openInputStream(it)?.let { inputStream ->
                        if (mIsSending) {
                            stopSend()
                        }
                        sendFile {
                            if (mDeviceMode == Constant.BLE_MODE) {
                                mBleApi.sendFile(inputStream)
                            } else {
                                mSppApi.sendFile(inputStream)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getFileNameByUri(uri: Uri): String {
        return when (uri.scheme) {
            "content" -> {
                try {
                    contentResolver.query(uri, null, null, null, null)?.use {
                        if (it.moveToFirst()) {
                            it.getString(
                                it.getColumnIndexOrThrow(
                                    MediaStore.Downloads.DISPLAY_NAME
                                )
                            )
                        } else {
                            " "
                        }
                    } ?: " "
                } catch (e: Exception) {
                    " "
                }
            }

            "file" -> {
                uri.toFile().name
            }

            else -> " "
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.intervalSendCheck.isOpened = false
        disconnect()
        handler.removeCallbacks(hexClickRunnable)
        sppSendData.clear()
        bleSendData.clear()
    }

    private fun disconnect() {
        if (mDeviceMode == Constant.BLE_MODE) {
            mBleApi.disconnect()
        } else {
            mSppApi.disconnect()
        }
    }

    fun uiDeviceConnected() {
        runOnUiThread {
            binding.toolbarSubtitle.text = resources.getString(R.string.through_connected)
            binding.sendButton.isEnabled = true
            binding.sendFileButton.isEnabled = true
            binding.sendStatusBtn.isEnabled = true
            if (mDeviceMode == Constant.BLE_MODE) {
                binding.switchServiceButton.isEnabled = true
                binding.sendFileButton.isEnabled = true
            }
        }
    }

    fun uiDeviceDisconnected() {
        runOnUiThread {
            binding.toolbarSubtitle.text = resources.getString(R.string.through_disconnected)
            binding.sendButton.isEnabled = false
            binding.sendFileButton.isEnabled = false
            binding.sendStatusBtn.isEnabled = false
            binding.sendStatusBtn.setImageDrawable(resources.getDrawable(R.drawable.start))
            mIsSending = false
            if (mDeviceMode == Constant.BLE_MODE) {
                binding.switchServiceButton.isEnabled = false
            }
            binding.chronometer.stop()
        }
    }

    fun uiReceiveData(strValue: String, hexString: String, data: ByteArray) {
        if (mReceiveBuffer.length > 1024) {
            mReceiveBuffer.delete(0, mReceiveBuffer.length)
            mReceiveBuffer.setLength(0)
            mReceiveBufferHex.delete(
                0,
                mReceiveBufferHex.length
            )
            mReceiveBufferHex.setLength(0)
        }

        mReceiveBuffer.append(strValue)
        mReceiveBufferHex.append(hexString)
        mReceiveCRC.update(data)
        runOnUiThread {
            Log.e("vvvv", "=========binding.hexCheck.isOpened========" + binding.hexCheck.isOpened)
            binding.receiveEdit.setText(String(if (binding.hexCheck.isOpened) mReceiveBufferHex else mReceiveBuffer))
            receiveData.add(String(if (binding.hexCheck.isOpened) mReceiveBufferHex else mReceiveBuffer))
            binding.receiveEdit.setSelection(
                binding.receiveEdit.text.length,
                binding.receiveEdit.text.length
            )
            binding.crcReceive.text =
                getString(R.string.CRC, mReceiveCRC.value.toHexString().uppercase(Locale.ROOT))
            mReceiveByteCount += data.size
            mReceivePackageCount += 1
            binding.receiveTextView.text =
                getString(R.string.receive, mReceiveByteCount, mReceivePackageCount)
        }
    }

    @SuppressLint("SetTextI18n")
    fun uiSendDataProgress(progress: Float, byteSize: Int) {
        runOnUiThread {
            if (progress > 0) {
                binding.numberProgressBar.progress = progress.toInt()
                val df = if (progress < 1) DecimalFormat("0.00") else DecimalFormat("#.00")
                binding.numberProgressCount.text = "${df.format(progress)}"
            }

            mSendFileByte += byteSize
            if (progress.toInt() == 100) {
                mIsSending = false
                mIsPause = false
                binding.export.isEnabled = true
                binding.switchServiceButton.isEnabled = true
                binding.sendFileButton.isEnabled = true
                binding.sendStatusBtn.setImageDrawable(resources.getDrawable(R.drawable.start))
                binding.rateText.text = getString(
                    R.string.rate, try {
                        mSendFileByte / ((SystemClock.elapsedRealtime() - binding.chronometer.base) / 1000f)
                    } catch (e: ArithmeticException) {
                        0f
                    }
                )
                binding.chronometer.stop()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun uiSendData(data: ByteArray) {
        mSendCRC.update(data)
        mSendByteCountSend += data.size
        runOnUiThread {
            binding.crcSend.text =
                getString(R.string.CRC, mSendCRC.value.toHexString().uppercase(Locale.ROOT))
            mSendPackageCountSend += 1
            binding.sendTextView.text =
                getString(R.string.send, mSendByteCountSend, mSendPackageCountSend)
        }
    }

    private fun addCRLF(data: ByteArray): ByteArray {
        val crlf: ByteArray = byteArrayOf('\r'.code.toByte(), '\n'.code.toByte())
        val newData = ByteArray(data.size + crlf.size)
        System.arraycopy(data, 0, newData, 0, data.size)
        System.arraycopy(crlf, 0, newData, data.size, crlf.size)
        return newData
    }

    private fun sendFile(sendFileFunction: () -> Unit) {
        mIsPause = false
        mIsSending = true
        binding.export.isEnabled = false
        sendFileFunction()
        binding.numberProgressBar.progress = 0
        binding.group2.visibility = View.GONE
        binding.group3.visibility = View.VISIBLE
        binding.switchServiceButton.isEnabled = false
        binding.sendFileButton.isEnabled = false
        binding.sendStatusBtn.setImageDrawable(resources.getDrawable(R.drawable.pause))
        mSendFileByte = 0
        binding.chronometer.base = SystemClock.elapsedRealtime()
        val hour = ((SystemClock.elapsedRealtime() - binding.chronometer.base) / 1000 / 60)
        binding.chronometer.format = "0${hour}:%s"
        binding.chronometer.start()
    }

    /**
     * 移除原 getLayout() 方法（ViewBinding 无需返回布局ID）
     */
    // override fun getLayout() = R.layout.activity_throughput

    override fun onClick(v: View?) {
        binding.intervalSendTime.clearFocus()
        when (v?.id) {
            R.id.sendButton -> {
                send()
            }

            R.id.sendFileButton -> {
                val intent = Intent(this@ThroughputActivity, SelectFileActivity::class.java)
                startActivityForResult(intent, SELECT_FILE)
            }

            R.id.send_status_btn -> {
                if (mIsPause) {
                    continueSend()
                    binding.sendStatusBtn.setImageDrawable(resources.getDrawable(R.drawable.pause))
                    mIsPause = false
                    mIsSending = true
                } else {
                    if (mIsSending) {
                        pauseSend()
                        binding.sendStatusBtn.setImageDrawable(resources.getDrawable(R.drawable.start))
                        mIsPause = true
                        mIsSending = true
                    } else {
                        sendFile()
                        mIsPause = false
                        mIsSending = true
                    }
                }
            }

            R.id.switchServiceButton -> {
                ServiceSelectActivity.activityStart(this, mFscDevice)
            }

            R.id.export -> {

                if (System.currentTimeMillis() - lastExportClick < CLICK_INTERVAL) return
                lastExportClick = System.currentTimeMillis()

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    requestPermissionsAndShareFiles()
                } else {
                    saveAndShareFilesUsingScopedStorage()
                }
            }

            R.id.clearButton -> {
                initUi()
            }

            R.id.intervalSendCheck -> {
                if (binding.sendEdit.text.isBlank()) return

                lifecycleScope.launch(Dispatchers.IO) {
                    var iteration = 0
                    while (binding.intervalSendCheck.isOpened) {
                        send()

                        delay(10)

                        iteration++
                        if (iteration % 100 == 0) {
                            if (isMemoryCritical()) {
                                delay(100)
                                clearTempData()
                            }
                        }

                        yield()
                    }
                }
            }

            R.id.hexCheck -> {

                binding.hexCheck.isEnabled = false
                handler.postDelayed(hexClickRunnable, 300)
                measureTimeMillis {
                    putBoolean("hexCheck", binding.hexCheck.isOpened)
                    if (binding.hexCheck.isOpened) {
                        binding.receiveEdit.setText(mReceiveBufferHex)

                        val hexString = mByteArray.toHexString()
                        with(binding.sendEdit) {
                            keyListener = hexKeyListener
                            setText(hexString)
                            try {
                                setSelection(hexString.length)
                            } catch (e: IndexOutOfBoundsException) {
                                setSelection(hexString.length - 1)
                            }
                        }
                    } else {
                        binding.receiveEdit.setText(mReceiveBuffer)
                        if (mByteArray.isNotEmpty()) {
                            val string = mByteArray.getEncoding()
                            runOnUiThread {
                                with(binding.sendEdit) {
                                    keyListener = TextKeyListener.getInstance()
                                    setText(string)
                                    try {
                                        setSelection(string.length)
                                    } catch (e: IndexOutOfBoundsException) {
                                        setSelection(string.length - 1)
                                    }
                                }
                            }
                        } else {
                            runOnUiThread {
                                with(binding.sendEdit) {
                                    keyListener = TextKeyListener.getInstance()
                                }
                            }
                        }
                    }
                    binding.receiveEdit.setSelection(
                        binding.receiveEdit.text.length,
                        binding.receiveEdit.text.length
                    )
                }
            }

            R.id.send_new_line -> {
                putBoolean("sendNewLine", binding.sendNewLine.isOpened)
                if (binding.sendNewLine.isOpened) {
                } else {
                }
            }
        }
    }


    private val CLICK_INTERVAL = 800
    private var lastExportClick = 0L
    private fun isMemoryCritical(): Boolean {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        return (usedMemory > maxMemory * 0.7)
    }

    private fun clearTempData() {
        System.gc()
    }

    private fun send() {
        try {
            if (binding.sendEdit.text.isNotBlank()) {
                val isSendNewLine = getBoolean("sendNewLine", false)
                val apiToSend = if (mDeviceMode == Constant.BLE_MODE) mBleApi else mSppApi

                if (isSendNewLine) {
                    if (isOpenFscCommand(mByteArray)) {
                        ToastUtils.showShort(getString(R.string.error_open_fsc))
                        return
                    }
                    val addCRLFByteArray = addCRLF(mByteArray)
                    apiToSend.send(addCRLFByteArray)
                } else {
                    if (containsAtCommand(mByteArray)) {
                        ToastUtils.showShort(getString(R.string.error_send_at))
                        return
                    }
                    apiToSend.send(mByteArray)
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun isOpenFscCommand(data: ByteArray): Boolean {
        val openFscBytes = "\$OpenFscAtEngine\$".toByteArray()
        return data.contentEquals(openFscBytes)
    }

    private fun containsAtCommand(data: ByteArray): Boolean {
        val atBytes = "AT".toByteArray()
        if (data.size < atBytes.size) return false

        for (i in 0..data.size - atBytes.size) {
            var found = true
            for (j in atBytes.indices) {
                if (data[i + j] != atBytes[j]) {
                    found = false
                    break
                }
            }
            if (found) return true
        }
        return false
    }

    // 请求权限并分享文件
    private fun requestPermissionsAndShareFiles() {


        XXPermissions.with(this)

            .permission(PermissionLists.getReadMediaAudioPermission())
            .permission(PermissionLists.getReadMediaAudioPermission())
            .permission(PermissionLists.getReadMediaImagesPermission())

            .interceptor(PermissionInterceptor())
            .description(PermissionDescription())
            .request(OnPermissionCallback { grantedList, deniedList ->
                val allGranted = deniedList.isEmpty()
                if (!allGranted) {
                    return@OnPermissionCallback
                }
                saveAndShareFiles()
            })
    }

    // 使用分区存储保存和分享文件（API 版本大于等于 33）
    private fun saveAndShareFilesUsingScopedStorage() {
        val directory = File(getExternalFilesDir(null), "ExportedData")
        if (!directory.exists()) directory.mkdirs()

        val suffix = if (mDeviceMode == Constant.BLE_MODE) "BLE" else "SPP"
        val receiveDataFile = createFileInDirectory(directory, suffix, "_ReceiveData")
        val sendDataFile = createFileInDirectory(directory, suffix, "_SendData")

        // 写入数据
        val isReceiveDataSaved = FileUtils.exportDataToTxt(receiveData, receiveDataFile)
        val isSendDataSaved = FileUtils.exportDataToTxt(
            if (mDeviceMode == Constant.BLE_MODE) bleSendData else sppSendData,
            sendDataFile
        )

        // 分享文件
        if (isReceiveDataSaved) {
            shareFile(receiveDataFile)
        }
        if (isSendDataSaved) {
            shareFile(sendDataFile)
        }
    }

    // 创建文件并返回文件对象
    private fun createFileInDirectory(directory: File, suffix: String, fileType: String): File {
        val fileName =
            "${SimpleDateFormat("yyyy-MM-dd_HH_mm_ss").format(Date())}_${suffix}$fileType.txt"
        val file = File(directory, fileName)
        Log.d(TAG, "$fileType file created: ${file.path}")
        return file
    }

    // 分享文件的封装方法
    private fun shareFile(file: File) {
        //  val fileUri = FileShareHelper.getFileUri(this, file)
        FileShareHelper.shareFile(this, file, "application/pdf")  // 可根据需要修改 MIME 类型
    }

    // 保存和分享文件的通用逻辑（适用于 API 版本小于 33）
    private fun saveAndShareFiles() {
        val directory = File(getExternalFilesDir(null), "ExportedData")
        if (!directory.exists()) directory.mkdirs()

        val suffix = if (mDeviceMode == Constant.BLE_MODE) "BLE" else "SPP"
        val receiveDataFile = createFileInDirectory(directory, suffix, "_ReceiveData")
        val sendDataFile = createFileInDirectory(directory, suffix, "_SendData")

        // 写入数据
        val isReceiveDataSaved = FileUtils.exportDataToTxt(receiveData, receiveDataFile)
        val isSendDataSaved = FileUtils.exportDataToTxt(
            if (mDeviceMode == Constant.BLE_MODE) bleSendData else sppSendData,
            sendDataFile
        )

        // 分享文件
        if (isReceiveDataSaved) {
            shareFile(receiveDataFile)
        }
        if (isSendDataSaved) {
            shareFile(sendDataFile)
        }
    }

    private fun initUi() {
        mReceiveBuffer.delete(0, mReceiveBuffer.length)
        mReceiveBufferHex.delete(0, mReceiveBufferHex.length)
        binding.receiveEdit.text.clear()
        mReceiveByteCount = 0
        mSendFileByte = 0
        mReceivePackageCount = 0
        mSendByteCountSend = 0
        mSendPackageCountSend = 0

        binding.receiveTextView.text =
            getString(R.string.receive, mReceiveByteCount, mReceivePackageCount)
        binding.sendTextView.text =
            getString(R.string.send, mSendByteCountSend, mSendPackageCountSend)
        binding.crcSend.text = getString(R.string.CRC, "00000000")
        binding.crcReceive.text = getString(R.string.CRC, "00000000")
        mSendCRC.reset()
        mReceiveCRC.reset()

        getBoolean("sendNewLine", false).let {
            binding.sendNewLine.isOpened = it
        }
    }

    private fun stopSend() {
        binding.switchServiceButton.isEnabled = true
        binding.sendFileButton.isEnabled = true
        if (mDeviceMode == Constant.BLE_MODE) {
            mBleApi.stopSend()
        } else {
            mSppApi.stopSend()
        }
        binding.chronometer.stop()
    }

    private fun pauseSend() {
        binding.switchServiceButton.isEnabled = true
        binding.sendFileButton.isEnabled = true
        if (mDeviceMode == Constant.BLE_MODE) {
            mBleApi.pauseSend(address)
        } else {
            mSppApi.pauseSend(address)
        }
        binding.chronometer.stop()
        mLastStopTime = SystemClock.elapsedRealtime()
    }

    private fun continueSend() {
        binding.switchServiceButton.isEnabled = false
        binding.sendFileButton.isEnabled = false
        if (mDeviceMode == Constant.BLE_MODE) {
            mBleApi.continueSend(address)
        } else {
            mSppApi.continueSend(address)
        }
        val intervalOnPause = SystemClock.elapsedRealtime() - mLastStopTime
        binding.chronometer.base = binding.chronometer.base + intervalOnPause
        binding.chronometer.start()
    }

    companion object {
        private const val TAG = "ThroughputActivity"
        const val SELECT_FILE = 0x0001
        const val FILE_SIZE = 2000
        const val FILE_PATH = 2001
        private const val REQUEST_EXTERNAL_STORAGE = 1
        private val PERMISSIONS_STORAGE = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        fun activityStart(context: Context, fscDevice: FscDevice) {
            val intent = Intent(context, ThroughputActivity::class.java)
            intent.putExtra("FscDevice", fscDevice)
            context.startActivity(intent)
        }

        fun activityStart(context: Context, device: BluetoothDevice) {
            val intent = Intent(context, ThroughputActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra("BluetoothDevice", device)
            context.startActivity(intent)
        }
    }
}