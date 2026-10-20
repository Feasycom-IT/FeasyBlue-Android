package com.feasycom.feasyblue.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.MediaStore
import android.text.method.TextKeyListener
import android.view.View
import android.widget.Chronometer
import android.widget.Toast
import androidx.core.net.toFile
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.LogUtils
import com.feasycom.encrypted.controler.FscEncryptApi
import com.feasycom.encrypted.controler.FscEncryptApiImp
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivityTcpBw256commBinding
import com.feasycom.feasyblue.utils.*
import com.feasycom.wifi.tcp.FscClientBwBwCallback
import com.feasycom.wifi.tcp.TCPClientManager
import com.feasycom.wifi.utils.BwNetUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class BW256BCommTCPActivity : BaseActivity<ActivityTcpBw256commBinding>(), FscClientBwBwCallback {

    lateinit var mFscEncryptApiImp: FscEncryptApi
    private var receivePasswordInfo: String? = null
    private var sendPasswordInfo: ByteArray? = null
    private var mBytesToBeSent = 0
    private var mReceiveBuffer = StringBuffer()
    private var mReceiveByteCount = 0

    private var mSendByteCountSend = 0

    private var mSendFileByte = 0

    private var mResultCode: Int = 0
    private var mData: Intent? = null
    private var mLastStopTime: Long = 0L

    private var mIsPause = false
    private var mIsSending = false

    private var mByteArray: ByteArray = byteArrayOf()

    // 实现父类 ViewBinding 获取方法
    override fun getViewBinding(): ActivityTcpBw256commBinding {
        return ActivityTcpBw256commBinding.inflate(layoutInflater)
    }

    override fun initView() {
        host = intent?.getStringExtra("tcpServer")
        port = intent?.getIntExtra("tcpPort", -1)

        // 通过 binding 访问视图
        binding.header.backGroup.visibility = View.VISIBLE
        binding.header.titleText.text = getString(R.string.tcp_client_connecting)

        mFscEncryptApiImp = FscEncryptApiImp.getInstance()
        sendPasswordInfo = mFscEncryptApiImp.encrypted

        TCPClientManager.initClient(this)
        LogUtils.d(TAG, "APP WIFI TCP initView host => $host  port => $port")
        TCPClientManager.connect(host!!, port!!)

        binding.rateTextView.text = getString(R.string.rate, 0.0f)
        mByteArray = getByteArray("sendEdit")
        binding.sendByteCountTv.text = getString(R.string.byteString, mByteArray.size)
        with(binding.sendDataEt) {
            keyListener = TextKeyListener.getInstance()
            setText(String(mByteArray))
        }
        binding.sendDataEt.keyListener = TextKeyListener.getInstance()

        initUi()
        initEvent()
    }

    private fun initUi() {
        mReceiveBuffer.delete(0, mReceiveBuffer.length)
        binding.receiveDataEt.text.clear()
        mReceiveByteCount = 0
        mSendFileByte = 0
        mSendByteCountSend = 0

        binding.receiveDataTv.text = getString(
            R.string.receive1, mReceiveByteCount
            /**, mReceivePackageCount**/
        )
        binding.sendDataTv.text = getString(
            R.string.send1, mSendByteCountSend
            /**, mSendPackageCountSend**/
        )

        getBoolean("sendNewLine", false).let {
            binding.sendNewLineSwitchView.isOpened = it
        }
    }

    override fun connected() {
        LogUtils.d(TAG, "APP WIFI TCP bw256b tcp 连接成功")
        TCPClientManager.sendMessage(sendPasswordInfo!!)
    }

    override fun disconnected() {
        LogUtils.e(TAG, "APP WIFI TCP bw256b tcp 断开连接")
        runOnUiThread {
            receivePasswordInfo = null
            binding.header.titleText.text = getString(R.string.tcp_client_disconnected)
            binding.tcpConnectBtn.isEnabled = true
            binding.tcpDisconnectBtn.isEnabled = false
            binding.sendDataBtn.isEnabled = false
            binding.sendFileButton.isEnabled = false
            binding.startImageButton.isEnabled = false
            binding.upgradeButton.isEnabled = false
            binding.isWifiModeButton.isEnabled = false
            binding.isSerialPortModeButton.isEnabled = false
            binding.openAirCommandBtn.isEnabled = false
            binding.closeAirCommandBtn.isEnabled = false

            binding.startImageButton.setImageDrawable(resources.getDrawable(R.drawable.start))
            mIsSending = false
            binding.chronometer.stop()
        }
    }

    override fun connectFailed() {
        super.connectFailed()
        binding.header.titleText.text = getString(R.string.tcp_client_connect_failed)
    }

    override fun packetSend(strValue: String, data: ByteArray) {
//        LogUtils.d(TAG, "APP WIFI TCP packetSend strValue => $strValue")
        uiSendData(data)
    }

    private fun uiSendData(data: ByteArray) {
        mSendByteCountSend += data.size
        runOnUiThread {
            binding.sendDataTv.text = getString(
                R.string.send1, mSendByteCountSend
                /**, mSendPackageCountSend**/
            )
        }
    }

    override fun sendPacketProgress(percentage: Float, data: ByteArray) {
        uiSendDataProgress(percentage, data.size)
    }

    private fun uiSendDataProgress(progress: Float, byteSize: Int) {
        runOnUiThread {
            if (progress > 0) {
                binding.otaProgress.progress = progress.toInt()
                val df = if (progress < 1) DecimalFormat("0.00") else DecimalFormat("#.00")
                binding.progressCount.text = "${df.format(progress)}"
            }

            mSendFileByte += byteSize
            if (progress.toInt() == 100) {
                mIsSending = false
                mIsPause = false
                binding.startImageButton.isEnabled = false
                binding.startImageButton.setImageDrawable(resources.getDrawable(R.drawable.start))
                binding.rateTextView.text = getString(
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

    override fun packetReceived(receiveData: String, rawValue: ByteArray) {
        LogUtils.d(TAG, "APP WIFI TCP channelRead receiveData => $receiveData")
        Thread {
            uiReceiveData(receiveData, rawValue)
        }.start()
    }

    private fun uiReceiveData(strValue: String, data: ByteArray) {
        if (mReceiveBuffer.length > 1024) {
            mReceiveBuffer.delete(0, mReceiveBuffer.length)
            mReceiveBuffer.setLength(0)
        }

        mReceiveBuffer.append(strValue)
        runOnUiThread {
            binding.receiveDataEt.setText(String(mReceiveBuffer))
            binding.receiveDataEt.setSelection(binding.receiveDataEt.text.length, binding.receiveDataEt.text.length)
            mReceiveByteCount += data.size
            binding.receiveDataTv.text = getString(
                R.string.receive1, mReceiveByteCount
                /**, mReceivePackageCount**/
            )

            if (receivePasswordInfo == null) {
                if (mFscEncryptApiImp.verifyEncrypted(data)) {
                    LogUtils.d(TAG, "APP WIFI TCP 鉴权成功")
                    binding.header.titleText.text = getString(R.string.tcp_conect_success)
                    receivePasswordInfo = strValue
                    binding.tcpConnectBtn.isEnabled = false
                    binding.tcpDisconnectBtn.isEnabled = true
                    binding.sendDataBtn.isEnabled = true
                    binding.isWifiModeButton.isEnabled = true
                    binding.isSerialPortModeButton.isEnabled = true
                    binding.openAirCommandBtn.isEnabled = true
                } else {
                    LogUtils.e(TAG, "APP WIFI TCP 鉴权失败")
                    binding.header.titleText.text = getString(R.string.tcp_connect_fail)
                    binding.tcpConnectBtn.isEnabled = true
                    TCPClientManager.disconnect()
                }
            } else {
                if ("\$OK,Opened\$" == strValue) {
                    binding.upgradeButton.isEnabled = true
                    binding.openAirCommandBtn.isEnabled = false
                    binding.closeAirCommandBtn.isEnabled = true
                } else if ("\$OK,Closed\$" == strValue) {
                    binding.upgradeButton.isEnabled = false
                    binding.openAirCommandBtn.isEnabled = true
                    binding.closeAirCommandBtn.isEnabled = false
                }
            }
        }
    }

    private fun initEvent() {
        // 返回按钮事件
        binding.header.backTv.setOnClickListener {
            TCPClientManager.disconnect()
            BW256BScanActivity.activityStart(this, if (getBoolean("haveUpgrade")) 0 else 1)
            finish()
        }

        binding.header.backImg.setOnClickListener {
            TCPClientManager.disconnect()
            BW256BScanActivity.activityStart(this, if (getBoolean("haveUpgrade")) 0 else 1)
            finish()
        }

        // 清空按钮
        binding.clearButton.setOnClickListener {
            initUi()
        }

        // 发送数据输入框文本变化监听
        binding.sendDataEt.addTextChangedListener(onTextChanged = { s, _, _, _ ->
            mByteArray = if (s!!.isEmpty()) {
                byteArrayOf()
            } else {
                s.toString().toByteArray()
            }

            putByteArray("sendEdit", mByteArray)
            mBytesToBeSent = mByteArray.size
            binding.sendByteCountTv.text = getString(R.string.byteString, mBytesToBeSent)
        })

        // 发送数据按钮
        binding.sendDataBtn.setOnClickListener {
            try {
                if (binding.sendDataEt.text.isNotBlank()) {
                    val isSendNewLine = getBoolean("sendNewLine", false)
                    val byteArrayString = String(mByteArray)

                    if (isSendNewLine) {
                        if (byteArrayString == "\$OpenFscAtEngine\$") {
                            Toast.makeText(this, getString(R.string.error_open_fsc), Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        val addCRLFByteArray = addCRLF(mByteArray)
                        TCPClientManager.sendMessage(addCRLFByteArray)
                    } else {
                        if (byteArrayString.contains("AT")) {
                            Toast.makeText(this, getString(R.string.error_send_at), Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        TCPClientManager.sendMessage(mByteArray)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // TCP 连接按钮
        binding.tcpConnectBtn.setOnClickListener {
            LogUtils.d(TAG, "APP WIFI TCP HOST => $host  PORT => $port")
            TCPClientManager.connect(host!!, port!!)
        }

        // TCP 断开按钮
        binding.tcpDisconnectBtn.setOnClickListener {
            TCPClientManager.disconnect()
            receivePasswordInfo = null
        }

        // 打开空中命令按钮
        binding.openAirCommandBtn.setOnClickListener {
            TCPClientManager.sendMessage("\$OpenFscAtEngine\$")
        }

        // 关闭空中命令按钮
        binding.closeAirCommandBtn.setOnClickListener {
            TCPClientManager.sendMessage("\$CloseFscAtEngine\$")
        }

        // WiFi 模式按钮
        binding.isWifiModeButton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                TCPClientManager.sendMessage("\$OpenFscAtEngine\$")
                delay(1000)
                TCPClientManager.sendMessage(addCRLF("AT+TPEN=1".toByteArray()))
                delay(1000)
                TCPClientManager.sendMessage("\$CloseFscAtEngine\$")
                delay(1000)
                putBoolean("isWifiMode", true)
                lifecycleScope.launch(Dispatchers.Main) {
                    binding.sendFileButton.isEnabled = true
                    BwNetUtils.showToast(
                        this@BW256BCommTCPActivity,
                        getString(R.string.entered_wifi_transparent)
                    )
                }
            }
        }

        // 串口模式按钮
        binding.isSerialPortModeButton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                TCPClientManager.sendMessage("\$OpenFscAtEngine\$")
                delay(1000)
                TCPClientManager.sendMessage(addCRLF("AT+TPEN=0".toByteArray()))
                delay(1000)
                TCPClientManager.sendMessage("\$CloseFscAtEngine\$")
                delay(1000)
                putBoolean("isWifiMode", false)
                lifecycleScope.launch(Dispatchers.Main) {
                    binding.sendFileButton.isEnabled = true
                    BwNetUtils.showToast(
                        this@BW256BCommTCPActivity,
                        getString(R.string.entered_serial_port_transparent)
                    )
                }
            }
        }

        // 升级按钮
        binding.upgradeButton.setOnClickListener {
            BW256BOtaActivity.activityStart(this, host!!, port!!, 2)
            finish()
        }

        // 发送换行开关
        binding.sendNewLineSwitchView.setOnClickListener {
            putBoolean("sendNewLine", binding.sendNewLineSwitchView!!.isOpened)
        }

        // 选择文件按钮
        binding.sendFileButton.setOnClickListener {
            val intent = Intent(this@BW256BCommTCPActivity, SelectFileActivity::class.java)
            startActivityForResult(intent, SELECT_FILE)
        }

        // 开始/暂停发送按钮
        binding.startImageButton.setOnClickListener {
            if (mIsPause) {
                continueSend()
                binding.startImageButton.setImageDrawable(resources.getDrawable(R.drawable.pause))
                mIsPause = false
                mIsSending = true
            } else {
                if (mIsSending) {
                    pauseSend()
                    binding.startImageButton.setImageDrawable(resources.getDrawable(R.drawable.start))
                    mIsPause = true
                    mIsSending = true
                } else {
                    val isWifiMode = getBoolean("isWifiMode")
                    sendFile(isWifiMode)
                    mIsPause = false
                    mIsSending = true
                }
            }
        }

        // 停止发送按钮
        binding.stopImageButton.setOnClickListener {
            mIsPause = false
            mIsSending = false
            binding.startImageButton.setImageDrawable(resources.getDrawable(R.drawable.start))
            TCPClientManager.stopSend()
            binding.sendDataLl.visibility = View.VISIBLE
            binding.sendFileStatusRl.visibility = View.GONE
            binding.fileTransferProgressLl.visibility = View.GONE
            binding.isWifiModeButton.visibility = View.VISIBLE
            binding.isSerialPortModeButton.visibility = View.VISIBLE
        }

        // 计时器监听
        binding.chronometer.onChronometerTickListener = Chronometer.OnChronometerTickListener {
            runOnUiThread {
                try {
                    if ((SystemClock.elapsedRealtime() - binding.chronometer.base) != 0L) {
                        binding.rateTextView.text = getString(
                            R.string.rate,
                            mSendFileByte / ((SystemClock.elapsedRealtime() - binding.chronometer.base) / 1000f)
                        )
                    }
                } catch (e: ArithmeticException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun continueSend() {
        binding.sendFileButton.isEnabled = false
        TCPClientManager.continueSend()
        val intervalOnPause = SystemClock.elapsedRealtime() - mLastStopTime
        binding.chronometer.base = binding.chronometer.base + intervalOnPause
        binding.chronometer.start()
    }

    private fun pauseSend() {
        TCPClientManager.pauseSend()
        binding.chronometer.stop()
        mLastStopTime = SystemClock.elapsedRealtime()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != SELECT_FILE) {
            return
        }
        if (resultCode == FILE_PATH || resultCode == FILE_SIZE) {
            mResultCode = resultCode
            mData = data
            val isWifiMode = getBoolean("isWifiMode")
            sendFile(isWifiMode)
        }
    }

    private fun sendFile(isWifiMode: Boolean) {
        binding.rateTextView.text = getString(R.string.rate, 0.0f)
        when (mResultCode) {
            FILE_SIZE -> {
                val size = mData?.getIntExtra("size", 0)!!
                val sizeStr = mData?.getStringExtra("sizeStr")
                binding.selectedFileTv.text =
                    getString(R.string.selectedFile, "$sizeStr ${getString(R.string.testFile)}")
                if (mIsSending) {
                    TCPClientManager.stopSend()
                }
                sendFile {
                    Thread {
                        Thread.sleep(50)
                        TCPClientManager.sendFile(size, isWifiMode)
                    }.start()
                }
            }
            FILE_PATH -> {
                mData?.data?.let {
                    binding.selectedFileTv.text = getString(R.string.selectedFile, getFileNameByUri(it))
                    contentResolver.openInputStream(it)?.let { inputStream ->
                        if (mIsSending) {
                            stopSend()
                        }
                        sendFile {
                            Thread {
                                Thread.sleep(50)
                                TCPClientManager.sendFile(inputStream, isWifiMode)
                            }.start()
                        }
                    }
                }
            }
        }
    }

    private fun stopSend() {
        TCPClientManager.stopSend()
        binding.chronometer.stop()
    }

    private fun sendFile(sendFileFunction: () -> Unit) {
        mIsPause = false
        mIsSending = true
        sendFileFunction()
        binding.otaProgress.progress = 0
        binding.sendDataLl.visibility = View.GONE
        binding.sendFileStatusRl.visibility = View.VISIBLE
        binding.fileTransferProgressLl.visibility = View.VISIBLE
        binding.isWifiModeButton.visibility = View.GONE
        binding.isSerialPortModeButton.visibility = View.GONE
        binding.sendFileButton.isEnabled = false
        binding.startImageButton.isEnabled = true
        binding.startImageButton.setImageDrawable(resources.getDrawable(R.drawable.pause))
        mSendFileByte = 0
        binding.chronometer.base = SystemClock.elapsedRealtime()
        val hour = ((SystemClock.elapsedRealtime() - binding.chronometer.base) / 1000 / 60)
        binding.chronometer.format = "0${hour}:%s"
        binding.chronometer.start()
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

    private fun addCRLF(data: ByteArray): ByteArray {
        val crlf: ByteArray = byteArrayOf('\r'.code.toByte(), '\n'.code.toByte())
        val newData = ByteArray(data.size + crlf.size)
        System.arraycopy(data, 0, newData, 0, data.size)
        System.arraycopy(crlf, 0, newData, data.size, crlf.size)
        return newData
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        const val TAG = "BW256BCommTCP"
        const val SELECT_FILE = 0x0001
        const val FILE_PATH = 0x0002
        const val FILE_SIZE = 0x0003
        private var host: String? = null
        private var port: Int? = -1

        fun activityStart(context: Context, tcpServer: String, tcpPort: Int) {
            val intent = Intent(context, BW256BCommTCPActivity::class.java)
            intent.putExtra("tcpServer", tcpServer)
            intent.putExtra("tcpPort", tcpPort)
            context.startActivity(intent)
        }
    }
}