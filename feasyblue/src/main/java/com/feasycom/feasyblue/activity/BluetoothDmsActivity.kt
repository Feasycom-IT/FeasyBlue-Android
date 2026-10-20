package com.feasycom.feasyblue.activity

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope // 关键导入
import com.blankj.utilcode.util.LogUtils
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivityBluetoothDistributionNetworkBinding
import com.feasycom.feasyblue.dialog.LoadDialogFragment
import com.feasycom.feasyblue.dialog.MessageDialog
import com.feasycom.feasyblue.dialog.WFBaseDialog
import com.feasycom.feasyblue.interfaces.FeasyWiFiCallbacks
import com.feasycom.feasyblue.logic.AtCommand
import com.feasycom.feasyblue.logic.BluetoothRepository
import com.feasycom.feasyblue.viewmodel.BluetoothViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 蓝牙配网页面
 * 修复 ViewBinding 类型不匹配 + lifecycleScope 未解析问题
 */
class BluetoothDmsActivity : BaseActivity<ActivityBluetoothDistributionNetworkBinding>(), FeasyWiFiCallbacks {

    // 优化 ViewModel 获取方式（使用 ViewModelProvider 最新写法）
    private val mViewModel by lazy {
        ViewModelProvider(this)[BluetoothViewModel::class.java]
    }

    private var mWaitDialog: LoadDialogFragment? = null // 改为可空，避免未初始化异常
    private var mMessageDialog: WFBaseDialog? = null   // 改为可空，避免未初始化异常
    private var ip: String = ""

    // 实现 BaseActivity 的 ViewBinding 获取方法
    override fun getViewBinding(): ActivityBluetoothDistributionNetworkBinding {
        return ActivityBluetoothDistributionNetworkBinding.inflate(layoutInflater)
    }

    override fun initView() {
        BluetoothRepository.registerViewCallback(this)

        // 修复：直接通过 binding.header 访问 View 控件，不使用 HeaderBinding
        binding.header.titleText.text = getString(R.string.title_bluetooth)
        binding.header.backGroup.visibility = View.VISIBLE

        initEvent()
    }

    /**
     * 初始化所有事件监听
     */
    private fun initEvent() {
        // 注册网络广播
        mViewModel.registerReceiver()

        // 返回按钮事件（修复：直接访问 binding.header 下的控件）
        binding.header.backImg.setOnClickListener { finish() }
        binding.header.backTv.setOnClickListener { finish() }

        // DHCP 开关状态监听
        binding.dhcpSwitch.setOnCheckedChangeListener { _, isChecked ->
            BluetoothRepository.setDhcp(isChecked)
            LogUtils.d(TAG, "APP WIFI initEvent isChecked => $isChecked")

            binding.dhcpGroup.visibility = if (isChecked) View.GONE else View.VISIBLE
            binding.dnsGroup.visibility = if (isChecked || !BluetoothRepository.mGoneDNS) View.GONE else View.VISIBLE

            LogUtils.d(TAG, "APP WIFI initEvent mGoneDNS => ${BluetoothRepository.mGoneDNS}")
        }

        // 配置网络按钮点击事件
        binding.configNetwork.setOnClickListener {
            // 校验 SSID 非空
            val ssid = binding.ssidEv.text.toString().trim()
            if (ssid.isBlank()) {
                showMessageDialog(getString(R.string.ssid_empty))
                return@setOnClickListener
            }

            // 修复：确保 lifecycleScope 可访问（已导入 androidx.lifecycle.lifecycleScope）
            lifecycleScope.launch(Dispatchers.IO) {
                if (!binding.dhcpSwitch.isChecked) {
                    // 获取并校验 IP 相关参数
                    val ip = binding.ipEv.text.toString().trim()
                    val gw = binding.gwEv.text.toString().trim()
                    val mask = binding.maskEv.text.toString().trim()
                    val dns = binding.dnsEv.text.toString().trim()

                    // 校验 IP/GW/MASK 格式
                    when {
                        !ip.matches(IP_GW_MASK_REGEX) -> showErrorDialog(R.string.ip_error)
                        !gw.matches(IP_GW_MASK_REGEX) -> showErrorDialog(R.string.gw_error)
                        !mask.matches(IP_GW_MASK_REGEX) -> showErrorDialog(R.string.mask_error)
                        else -> {
                            // 配置静态 IP 参数
                            showWaitDialog(R.string.configure_network)
                            BluetoothRepository.setIp(ip)
                            delay(200)
                            BluetoothRepository.setGw(gw)
                            delay(200)
                            BluetoothRepository.setMask(mask)
                            delay(200)
                            BluetoothRepository.setDns(dns)
                            delay(200)
                            BluetoothRepository.setNetwork(ssid, binding.passwordEv.text.toString().trim())
                        }
                    }
                } else {
                    // DHCP 模式，直接配置网络
                    showWaitDialog(R.string.configure_network)
                    BluetoothRepository.setNetwork(ssid, binding.passwordEv.text.toString().trim())
                }
            }
        }

        // 获取 DHCP 状态
        BluetoothRepository.getDhcp()

        // 监听广播数据更新 SSID
        mViewModel.mBroadcastData.observe(this) { ssid ->
            binding.ssidEv.setText(ssid)
        }
    }

    /**
     * 显示普通提示对话框
     */
    private fun showMessageDialog(message: String) {
        mMessageDialog = MessageDialog.Builder(this@BluetoothDmsActivity)
            .setMessage(message)
            .setConfirm(getString(R.string.ok))
            .setCancel(null)
            .show()
    }

    /**
     * 显示错误提示对话框（协程版）
     */
    private suspend fun showErrorDialog(messageId: Int) {
        withContext(Dispatchers.Main) {
            showMessageDialog(getString(messageId))
        }
    }

    /**
     * 显示加载对话框（协程版）
     */
    private suspend fun showWaitDialog(messageId: Int) {
        withContext(Dispatchers.Main) {
            mWaitDialog = LoadDialogFragment.newInstance(getString(messageId))
            mWaitDialog?.show(supportFragmentManager, "config")
        }
    }

    /**
     * 配网成功回调
     */
    override fun success(ip: String) {
        LogUtils.d(TAG, "APP WIFI success 配网成功 ip => $ip")
        this.ip = ip
        showDialog(true)
    }

    /**
     * 配网失败回调
     */
    override fun failure() {
        LogUtils.d(TAG, "APP WIFI failure 配网失败.")
        showDialog(false)
    }

    /**
     * 显示配网结果对话框
     */
    private fun showDialog(isSuccess: Boolean) {
        lifecycleScope.launch {
            // 关闭加载对话框
            mWaitDialog?.dismiss()

            // 构建结果对话框
            mMessageDialog = if (isSuccess) {
                MessageDialog.Builder(this@BluetoothDmsActivity)
                    .setTitle(getString(R.string.configure_result_success))
                    .setMessage(getString(R.string.configure_result_success_item, ip))
                    .setConfirm(getString(R.string.ok))
                    .setCancel(null)
                    .show()
            } else {
                MessageDialog.Builder(this@BluetoothDmsActivity)
                    .setMessage(getString(R.string.configure_result_failed))
                    .setConfirm(getString(R.string.ok))
                    .setCancel(null)
                    .show()
            }
        }
    }

    /**
     * 蓝牙断开连接回调
     */
    override fun blePeripheralDisconnected() {
        BluetoothRepository.isDisconnected = true
        lifecycleScope.launch(Dispatchers.Main) {
            // 关闭现有对话框
            mMessageDialog?.dismiss()
            mWaitDialog?.dismiss()

            // 显示断开连接提示框
            mMessageDialog = MessageDialog.Builder(this@BluetoothDmsActivity)
                .setMessage(getString(R.string.configure_result_failed))
                .setConfirm(getString(R.string.ok))
                .setCancel(null)
                .setListener(object : MessageDialog.OnListener {
                    override fun onConfirm(dialog: WFBaseDialog?) {
                        dialog?.dismiss()
                        finish() // 确认后返回上一页
                    }

                    override fun onCancel(dialog: WFBaseDialog?) {}
                })
                .show()
        }
    }

    /**
     * 接收蓝牙数据包回调
     */
    override fun packetReceived(strValue: String?) {
        LogUtils.d(TAG, "APP WIFI packetReceived strValue => $strValue")
        strValue ?: return // 空值直接返回，避免后续判断

        when {
            // 处理 DHCP 状态
            strValue.contains("+DHCP") -> {
                val dhcpValue = strValue.split("=")[1].split("\r\n")[0]
                lifecycleScope.launch(Dispatchers.Main) {
                    binding.dhcpSwitch.isChecked = dhcpValue == "1"
                    binding.dhcpGroup.visibility = if (dhcpValue == "0") View.VISIBLE else View.GONE
                }
            }

            // 处理错误响应
            strValue.contains("ERR") -> {
                val errorLog = when (BluetoothRepository.lostAtCommand) {
                    AtCommand.GET_DHCP -> "获取DHCP 失败"
                    AtCommand.SET_DHCP -> "设置DHCP 失败"
                    AtCommand.GET_DNS -> "获取DNS 失败"
                    AtCommand.SET_DNS -> "设置DNS 失败"
                    AtCommand.GET_IP -> "获取IP 失败"
                    AtCommand.SET_IP -> "设置IP 失败"
                    AtCommand.SET_GW -> "设置GW 失败"
                    AtCommand.SET_MASK -> "设置MASK 失败"
                    AtCommand.SET_NETWORK -> "设置网络 失败"
                    else -> null
                }
                errorLog?.let { LogUtils.e(TAG, "APP WIFI packetReceived: $it") }
            }

            // 处理成功响应
            strValue.contains("OK") -> {
                val successLog = when (BluetoothRepository.lostAtCommand) {
                    AtCommand.GET_DHCP -> "获取DHCP 成功"
                    AtCommand.SET_DHCP -> "设置DHCP 成功"
                    AtCommand.GET_DNS -> "获取DNS 成功"
                    AtCommand.SET_DNS -> "设置DNS 成功"
                    AtCommand.GET_IP -> "获取IP 成功"
                    AtCommand.SET_IP -> "设置IP 成功"
                    AtCommand.SET_GW -> "设置GW 成功"
                    AtCommand.SET_MASK -> "设置MASK 成功"
                    AtCommand.SET_NETWORK -> "设置网络 成功"
                    else -> null
                }
                successLog?.let { LogUtils.e(TAG, "APP WIFI packetReceived: $it") }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理资源，避免内存泄漏
        mViewModel.unregisterReceiver()
        BluetoothRepository.unRegisterViewCallback(this)
        mWaitDialog?.dismiss()
        mMessageDialog?.dismiss()
    }

    companion object {
        private const val TAG = "BluetoothDmsActivity"

        // IP/GW/MASK 正则表达式
        val IP_GW_MASK_REGEX = "\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b".toRegex()
        // DNS 正则表达式
        val DNS_REGEX = "\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}(?:,\\s*(?:[0-9]{1,3}\\.){3}[0-9]{1,3})*\\b".toRegex()

        /**
         * 启动当前页面
         */
        fun activityStart(context: Context) {
            Intent(context, BluetoothDmsActivity::class.java).apply {
                context.startActivity(this)
            }
        }
    }
}