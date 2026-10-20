package com.feasycom.feasyblue.activity

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.LogUtils
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivityGatewayBinding
import com.feasycom.feasyblue.dialog.LoadDialogFragment
import com.feasycom.feasyblue.dialog.MessageDialog
import com.feasycom.feasyblue.dialog.WFBaseDialog
import com.feasycom.feasyblue.interfaces.FeasyWiFiCallbacks
import com.feasycom.feasyblue.logic.BluetoothRepository
import com.feasycom.feasyblue.utils.getInt
import com.feasycom.feasyblue.utils.getStr
import com.feasycom.feasyblue.utils.putInt
import com.feasycom.feasyblue.utils.putStr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.*

// 1. 补充泛型参数 ActivityGatewayBinding
class GatewayActivity : BaseActivity<ActivityGatewayBinding>(), FeasyWiFiCallbacks {

    private val mProtocolList: MutableList<String> by lazy {
        resources.getStringArray(R.array.gateway).toMutableList()
    }

    private val requests: Deque<ByteArray> = LinkedList()

    private lateinit var mWaitDialog: LoadDialogFragment

    private lateinit var mMessageDialog: WFBaseDialog

    private var isSuccessFlag = false

    private var isReadingTopic = false

    private val subscribedTopics = mutableListOf<String>()

    // 2. 实现基类要求的 getViewBinding 方法
    override fun getViewBinding(): ActivityGatewayBinding {
        return ActivityGatewayBinding.inflate(layoutInflater)
    }

    override fun initView() {
        BluetoothRepository.registerViewCallback(this)
        requests.clear()
        // 3. 替换 findViewById 为 ViewBinding 调用
        binding.header.titleText.text = getString(R.string.title_gateway)
        binding.header.backGroup.visibility = View.VISIBLE

        //声明一个下拉列表的数组适配器
        val starAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(this, R.layout.item_select, mProtocolList)
        //设置数组适配器的布局样式
        starAdapter.setDropDownViewResource(R.layout.item_dropdown)
        //设置下拉框的数组适配器
        binding.spinner.adapter = starAdapter
        //给下拉框设置选择监听器，一旦用户选中某一项，就触发监听器的onItemSelected方法
        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                binding.httpGroup.visibility = View.GONE
                binding.tcpGroup.visibility = View.GONE
                binding.mqttGroup.visibility = View.GONE
                putInt("selection", position)
                when (position) {
                    0 -> {
                        binding.connectBtn.visibility = View.GONE
                    }
                    1 -> {
                        // MQTT
                        binding.mqttGroup.visibility = View.VISIBLE
                        binding.connectBtn.visibility = View.VISIBLE
                        readTopic()
//                        BluetoothRepository.send("AT+PROFILE=1\r\n".toByteArray())
                    }
                    2 -> {
                        // TCP
                        binding.tcpGroup.visibility = View.VISIBLE
                        binding.connectBtn.visibility = View.VISIBLE
//                        BluetoothRepository.send("AT+PROFILE=2\r\n".toByteArray())
                    }
                    3 -> {
                        // HTTP
                        binding.httpGroup.visibility = View.VISIBLE
                        binding.connectBtn.visibility = View.VISIBLE
//                        BluetoothRepository.send("AT+PROFILE=3\r\n".toByteArray())
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        initEvent()
    }

    override fun onResume() {
        super.onResume()
        val selection = getInt("selection", 0)
        binding.spinner.setSelection(selection)
        when(selection) {
            0 -> {
                binding.httpGroup.visibility = View.GONE
                binding.tcpGroup.visibility = View.GONE
                binding.mqttGroup.visibility = View.GONE
                binding.connectBtn.visibility = View.GONE
            }
            1 -> {
                binding.httpGroup.visibility = View.GONE
                binding.tcpGroup.visibility = View.GONE
                binding.mqttGroup.visibility = View.VISIBLE
                binding.connectBtn.visibility = View.VISIBLE
            }
            2 -> {
                binding.httpGroup.visibility = View.GONE
                binding.mqttGroup.visibility = View.GONE
                binding.tcpGroup.visibility = View.VISIBLE
                binding.connectBtn.visibility = View.VISIBLE
                val tcpServer = getStr("tcpServer", "")
                val tcpPort = getStr("tcpPort", "")
                binding.tcpServerEt.setText(tcpServer)
                binding.tcpServerEt.setSelection(tcpServer.length)
                binding.tcpPortEt.setText(tcpPort)
                binding.tcpPortEt.setSelection(tcpPort.length)
            }
        }
    }

    // 4. 移除过时的 getLayout 方法（基类已通过 ViewBinding 加载布局）

    private fun initEvent() {
        binding.header.backImg.setOnClickListener {
            finish()
        }
        binding.header.backTv.setOnClickListener {
            finish()
        }
        binding.connectBtn.setOnClickListener {
            // MQTT 模式下先校验：用户填的 topic 是否已经在模块里订阅过
            if (binding.spinner.selectedItemPosition == 1 && isTopicDuplicated()) {
                showDuplicateTopicDialog()
                return@setOnClickListener
            }
            when (binding.spinner.selectedItemPosition) {
                1 -> {
                    if (binding.mqttBrokerEt.text.isNotBlank() && binding.mqttClientEt.text.isNotBlank()) {
                        LogUtils.d(TAG, "APP MQTT initEvent: AT+BROKER => ${binding.mqttBrokerEt.text}\n")
                        requests.add("AT+BROKER=${binding.mqttBrokerEt.text}\r\n".toByteArray())
                        if (binding.mqttPortEt.text.isNotBlank()) {
                            val port = try {
                                binding.mqttPortEt.text.toString().toInt()
                            } catch (e: Exception) {
                                1883
                            }
                            LogUtils.d(TAG, "APP MQTT initEvent: AT+PORT=${port}")
                            requests.add("AT+PORT=${port}\r\n".toByteArray())
                        }
                        LogUtils.d(TAG, "APP MQTT initEvent: AT+CLIENTID => ${binding.mqttClientEt.text}")
                        requests.add("AT+CLIENTID=${binding.mqttClientEt.text}\r\n".toByteArray())
                        if (binding.mqttUserNameEt.text.isNotBlank()) {
                            LogUtils.d(TAG, "APP MQTT initEvent: AT+USERNAME=${binding.mqttUserNameEt.text}\n")
                            requests.add("AT+USERNAME=${binding.mqttUserNameEt.text}\r\n".toByteArray())
                        }
                        if (binding.mqttPasswordEt.text.isNotBlank()) {
                            LogUtils.d(TAG, "APP MQTT initEvent: AT+MQTTPWD => ${binding.mqttPasswordEt.text}")
                            requests.add("AT+MQTTPWD=${binding.mqttPasswordEt.text}\r\n".toByteArray())
                        }
                        if (binding.mqttTopicEt.text.isNotBlank()) {
                            //requests.add("AT+PUBTPC=${binding.mqttTopicEt.text}\r\n".toByteArray()) // 支持旧版的BW固件
                            requests.add("AT+SUBTPC=${binding.mqttTopicEt.text}\r\n".toByteArray()) // 支持新的BW固件
                        }
                        requests.add("AT+REBOOT\r\n".toByteArray())
                    }
                }

                2 -> {
                    if (binding.tcpServerEt.text.toString().isNotBlank() && binding.tcpPortEt.text.toString()
                            .isNotBlank()
                    ) {
                        LogUtils.d(TAG, "APP MQTT initEvent: AT+TCPCFG => ${binding.tcpServerEt.text},${binding.tcpPortEt.text}\n")
                        val tcpServer = binding.tcpServerEt.text.toString()
                        val tcpPort = binding.tcpPortEt.text.toString()
                        putStr("tcpServer", tcpServer)
                        putStr("tcpPort", tcpPort)
                        requests.add("AT+TCPCFG=$tcpServer,$tcpPort\r\n".toByteArray())
//                        requests.add("AT+REBOOT\r\n".toByteArray())
                    }
                }
                3 -> {
                    if (binding.httpEt.text.toString().isNotBlank()) {
                        LogUtils.d(TAG, "APP MQTT initEvent: AT+HTTPCFG => ${binding.httpEt.text}")
                        requests.add("AT+HTTPCFG=${binding.httpEt.text}\r\n".toByteArray())
                        requests.add("AT+REBOOT\r\n".toByteArray())
                    }
                }
            }
            if (requests.isNotEmpty()) {
                mWaitDialog = LoadDialogFragment.newInstance(getString(R.string.send_config))
                mWaitDialog.show(supportFragmentManager, "config")
                sendAt()
            }
        }
    }

    private fun sendAt() {
        if (requests.isNotEmpty()) {
            val byteArray = requests.remove()
            BluetoothRepository.send(byteArray)
        } else {
            when (getInt("selection", -1)) {
                2 -> {
                    if (::mWaitDialog.isInitialized && mWaitDialog.isVisible) {
                        mWaitDialog.dismiss()
                        runOnUiThread {
                            TCPCommunicationActivity.activityStart(this, getStr("tcpServer",""), getStr("tcpPort", ""))
                        }
                    }
                }
            }

        }
    }

    override fun blePeripheralDisconnected() {
        super.blePeripheralDisconnected()
        // TODO: 断开连接
        BluetoothRepository.isDisconnected = true
        lifecycleScope.launch(Dispatchers.Main) {
            if (::mWaitDialog.isInitialized && mWaitDialog.isVisible) {
                mWaitDialog.dismiss()
                val message =
                    if (isSuccessFlag) getString(R.string.config_failed) else getString(R.string.config_success)
                mMessageDialog = MessageDialog.Builder(this@GatewayActivity)
                    .setMessage(message)
                    .setConfirm(getString(R.string.ok))
                    .setCancel(null)
                    .setListener {
                        mMessageDialog.dismiss()
                        finish()
                    }
                    .show()
            } else {
                // 返回上一层
                finish()
            }
        }
    }


    private fun readTopic() {
        if (!BluetoothRepository.isConnect()) {
            return
        }
        isReadingTopic = true
        BluetoothRepository.send("AT+SUBTPC\r\n".toByteArray())
    }

    /**
     * 解析 AT+SUBTPC 的查询结果，提取模块当前已订阅的主题。
     */
    private fun parseSubscribedTopics(response: String?): List<String> {
        if (response.isNullOrBlank()) return emptyList()
        return response.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith(SUBTPC_PREFIX) }
            .map { it.removePrefix(SUBTPC_PREFIX).substringBeforeLast(",").trim() }
            .filter { it.isNotEmpty() }
            .toList()
    }

    /**
     * 判断用户输入的 topic 是否与模块已订阅的主题重复。
     */
    private fun isTopicDuplicated(): Boolean {
        val input = binding.mqttTopicEt.text.toString().trim()
        if (input.isEmpty()) return false
        // 取「主题名,内容」里的主题名部分，规则与 parseSubscribedTopics() 保持一致
        val inputTopicName = input.substringBeforeLast(",").trim()
        if (inputTopicName.isEmpty()) return false
        val duplicated = subscribedTopics.any { it == inputTopicName }
        return duplicated
    }

    private fun showDuplicateTopicDialog() {
        mMessageDialog = MessageDialog.Builder(this@GatewayActivity)
            .setMessage(getString(R.string.topic_duplicated))
            .setConfirm(getString(R.string.confirm))
            .setCancel(null)
            .show()
    }

    override fun packetReceived(strValue: String?) {
        super.packetReceived(strValue)
        if (isReadingTopic) {
            isReadingTopic = false
            subscribedTopics.clear()
            subscribedTopics.addAll(parseSubscribedTopics(strValue))
        }
        if (strValue!!.contains("ERR001")) {
            LogUtils.e(TAG, "APP MQTT 因为发送了相同的topic，所以收到了ERR001")
            isSuccessFlag = true
        }
        sendAt()
    }

    override fun packetSend(strValue: String?) {
        super.packetSend(strValue)
        LogUtils.d(TAG, "APP MQTT packetSend 发送数据 => $strValue")
    }

    override fun onDestroy() {
        super.onDestroy()
        BluetoothRepository.unRegisterViewCallback(this)
    }

    companion object {
        const val TAG: String = "GatewayActivity"

        /** AT+SUBTPC 查询结果里每条记录的前缀 */
        private const val SUBTPC_PREFIX = "+SUBTPC="

        fun activityStart(context: Context) {
            Intent(context, GatewayActivity::class.java).apply {
                context.startActivity(this)
            }
        }
    }
}
