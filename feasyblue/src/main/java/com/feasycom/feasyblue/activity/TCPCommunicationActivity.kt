package com.feasycom.feasyblue.activity

import android.content.Context
import android.content.Intent
import android.os.*
import android.view.View
import androidx.lifecycle.coroutineScope
import com.blankj.utilcode.util.LogUtils
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivityTcpCommunicationBinding
import com.feasycom.wifi.tcp.FscClientBwBwCallback
import com.feasycom.wifi.tcp.TCPClientManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class TCPCommunicationActivity : BaseActivity<ActivityTcpCommunicationBinding>(), FscClientBwBwCallback {

    private lateinit var mHandler: Handler
    private var sb = StringBuilder()

    // 从Intent获取的参数
    private var mHost: String? = null
    private var mPort: String? = null

    /**
     * 弱引用Handler，避免内存泄漏
     */
    class MyHandler(activity: TCPCommunicationActivity) : Handler(Looper.getMainLooper()) {
        private val mWeakReference: WeakReference<TCPCommunicationActivity> = WeakReference(activity)

        override fun handleMessage(msg: Message) {
            val activity = mWeakReference.get() ?: return

            when (msg.what) {
                1 -> {
                    // TCP连接成功
                    activity.binding.header.titleText.text = activity.getString(R.string.tcp_client_connected)
                    activity.binding.sendDataAcb.isEnabled = true
                    activity.binding.sendDataEt.isEnabled = true
                    activity.binding.openAirCommandBtn.isEnabled = true
                }
                2 -> {
                    // TCP断开连接
                    activity.binding.header.titleText.text = activity.getString(R.string.tcp_client_disconnected)
                    activity.binding.sendDataAcb.isEnabled = false
                    activity.binding.sendDataEt.isEnabled = false
                    activity.binding.upgradeButton.isEnabled = false
                    activity.binding.closeAirCommandBtn.isEnabled = false
                }
                3 -> {
                    // 接收数据
                    val strValue = msg.data.getString("strValue") ?: return
                    activity.sb.append(strValue)
                    activity.binding.receiveDataEt.setText(activity.sb.toString())

                    when (strValue) {
                        "\$OK,Opened\$" -> {
                            activity.binding.upgradeButton.isEnabled = true
                            activity.binding.openAirCommandBtn.isEnabled = false
                            activity.binding.closeAirCommandBtn.isEnabled = true
                        }
                        "\$OK,Closed\$" -> {
                            activity.binding.upgradeButton.isEnabled = false
                            activity.binding.openAirCommandBtn.isEnabled = true
                            activity.binding.closeAirCommandBtn.isEnabled = false
                        }
                    }
                }
                4 -> {
                    // 连接失败
                    activity.binding.header.titleText.text = activity.getString(R.string.tcp_client_connect_failed)
                    activity.binding.sendDataAcb.isEnabled = false
                    activity.binding.sendDataEt.isEnabled = false
                }
            }
        }
    }

    /**
     * 实现父类的 ViewBinding 获取方法
     */
    override fun getViewBinding(): ActivityTcpCommunicationBinding {
        return ActivityTcpCommunicationBinding.inflate(layoutInflater)
    }

    /**
     * 初始化View（父类抽象方法）
     */
    override fun initView() {
        // 获取Intent参数
        getIntentParams()

        // 初始化Header
        setupHeader()

        // 初始化控件状态
        initViewStates()

        // 初始化Handler
        mHandler = MyHandler(this)

        // 初始化TCP客户端并连接
        initTcpClient()

        // 初始化点击事件
        initEvent()
    }

    /**
     * 获取Intent传递的参数
     */
    private fun getIntentParams() {
        mHost = intent?.getStringExtra("tcpServer")
        mPort = intent?.getStringExtra("tcpPort")

        // 参数空值检查
        if (mHost.isNullOrEmpty() || mPort.isNullOrEmpty()) {
            LogUtils.e(TAG, "TCP服务器地址或端口为空")
            finish()
            return
        }
    }

    /**
     * 设置Header
     */
    private fun setupHeader() {
        binding.header.backGroup.visibility = View.VISIBLE
        binding.header.titleText.text = getString(R.string.tcp_client_connecting)
    }

    /**
     * 初始化控件状态
     */
    private fun initViewStates() {
        binding.receiveDataEt.isEnabled = false
        binding.sendDataEt.isEnabled = false
        binding.sendDataAcb.isEnabled = false

        // 初始禁用相关按钮
        binding.openAirCommandBtn.isEnabled = false
        binding.closeAirCommandBtn.isEnabled = false
        binding.upgradeButton.isEnabled = false
    }

    /**
     * 初始化TCP客户端并连接
     */
    private fun initTcpClient() {
        try {
            val intPort = mPort!!.toInt()
            TCPClientManager.initClient(this)
            TCPClientManager.connect(mHost!!, intPort)
        } catch (e: Exception) {
            LogUtils.e(TAG, "TCP端口转换失败: ${e.message}")
            binding.header.titleText.text = e.message
            finish()
        } catch (e: Exception) {
            LogUtils.e(TAG, "TCP客户端初始化失败: ${e.message}")
            connectFailed()
        }
    }

    /**
     * 初始化点击事件
     */
    private fun initEvent() {
        // 返回按钮事件
        binding.header.backImg.setOnClickListener {
            TCPClientManager.disconnect()
            finish()
        }

        binding.header.backTv.setOnClickListener {
            TCPClientManager.disconnect()
            finish()
        }

        // 清空按钮事件
        binding.clearButton.setOnClickListener {
            binding.receiveDataEt.setText("")
            binding.sendDataEt.setText("")
            sb.clear()
        }

        // 打开空中命令按钮
        binding.openAirCommandBtn.setOnClickListener {
            TCPClientManager.sendMessage("\$OpenFscAtEngine\$")
        }

        // 关闭空中命令按钮
        binding.closeAirCommandBtn.setOnClickListener {
            TCPClientManager.sendMessage("\$CloseFscAtEngine\$")
        }

        // 升级按钮
        binding.upgradeButton.setOnClickListener {
            try {
                val port = mPort!!.toInt()
                BW256BOtaActivity.activityStart(this, mHost!!, port, 1)
                finish()
            } catch (e: NumberFormatException) {
                LogUtils.e(TAG, "升级端口转换失败: ${e.message}")
            }
        }

        // 发送数据按钮
        binding.sendDataAcb.setOnClickListener {
            val msg = binding.sendDataEt.text.toString()
            if (msg.isNotBlank()) {
                TCPClientManager.sendMessage(msg.toByteArray())
            }
        }
    }

    /**
     * TCP连接成功回调
     */
    override fun connected() {
        mHandler.sendEmptyMessage(1)

        // 优化：降低心跳检测频率（原1秒一次太频繁）
      /*  lifecycle.coroutineScope.launch(Dispatchers.IO) {
            while (true) {
                delay(10000) // 10秒一次心跳检测
                if (!isFinishing && !isDestroyed) { // 检查Activity状态
                    val isPing = TCPClientManager.ping(mHost!!)
                    LogUtils.e(TAG, "APP TCP connected isPing => $isPing")

                    if (!isPing) {
                        TCPClientManager.disconnect()
                        break
                    }
                } else {
                    break // Activity销毁时停止检测
                }
            }
        }*/
    }

    /**
     * TCP断开连接回调
     */
    override fun disconnected() {
        mHandler.sendEmptyMessage(2)
    }

    /**
     * TCP连接失败回调
     */
    override fun connectFailed() {
        super.connectFailed()
        mHandler.sendEmptyMessage(4)
    }

    /**
     * 接收数据包回调
     */
    override fun packetReceived(
        strValue: String,
        rawValue: ByteArray,
    ) {
        val message = Message()
        message.what = 3
        val bundle = Bundle()
        bundle.putString("strValue", strValue)
        message.data = bundle
        mHandler.sendMessage(message)
    }

    /**
     * 读取响应回调
     */
    override fun readResponse(
        strValue: String?,
        hexString: String?,
        rawValue: ByteArray?,
        timestamp: String?
    ) {
        super.readResponse(strValue, hexString, rawValue, timestamp)
    }

    /**
     * 页面销毁时清理资源
     */
    override fun onDestroy() {
        super.onDestroy()

        // 断开TCP连接
        TCPClientManager.disconnect()

        // 移除Handler所有回调和消息
        mHandler.removeCallbacksAndMessages(null)

        // 清空字符串构建器
        sb.clear()
    }

    companion object {
        const val TAG = "TCPCommunication"

        /**
         * 启动TCP通信页面
         */
        fun activityStart(context: Context, tcpServer: String, tcpPort: String) {
            val intent = Intent(context, TCPCommunicationActivity::class.java)
            intent.putExtra("tcpServer", tcpServer)
            intent.putExtra("tcpPort", tcpPort)
            context.startActivity(intent)
        }
    }
}