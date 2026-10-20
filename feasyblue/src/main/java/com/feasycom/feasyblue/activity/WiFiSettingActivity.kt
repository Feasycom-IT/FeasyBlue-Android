package com.feasycom.feasyblue.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.LogUtils
import com.feasycom.common.bean.FscDevice
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivityWifiSettingBinding
import com.feasycom.feasyblue.dialog.MessageDialog
import com.feasycom.feasyblue.dialog.WFBaseDialog
import com.feasycom.feasyblue.interfaces.FeasyWiFiCallbacks
import com.feasycom.feasyblue.logic.BluetoothRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * WiFi设置页面
 * 适配优化后的泛型 ViewBinding BaseActivity
 */
class WiFiSettingActivity : BaseActivity<ActivityWifiSettingBinding>(), View.OnClickListener, FeasyWiFiCallbacks {

    private lateinit var mFscDevice: FscDevice
    private lateinit var mMessageDialog: WFBaseDialog

    /**
     * 实现父类的 ViewBinding 获取方法（核心适配点）
     */
    override fun getViewBinding(): ActivityWifiSettingBinding {
        return ActivityWifiSettingBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // 注册WiFi回调
        BluetoothRepository.registerViewCallback(this)

        // 检查蓝牙连接状态
        checkBluetoothConnectionState()

        // 初始化设备信息
        initDeviceInfo()

        // 初始化事件绑定
        initEvent()

        // 获取DNS信息
        BluetoothRepository.getDNS()
    }

    /**
     * 检查蓝牙连接状态，断开时显示提示弹窗
     */
    private fun checkBluetoothConnectionState() {
        if (BluetoothRepository.isDisconnected) {
            lifecycleScope.launch(Dispatchers.Main) {
                try {
                    showDisconnectDialog()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 初始化设备信息和标题
     */
    private fun initDeviceInfo() {
        // 获取并校验设备信息
        mFscDevice = intent?.getParcelableExtra("FscDevice") ?: run {
            finish()
            return
        }

        // 处理设备名称显示（超长截断）
        val deviceName = mFscDevice.name
        binding.header.titleText.text = if (deviceName.length > 10) {
            "${deviceName.substring(0, 10)}..."
        } else {
            deviceName
        }

        // 显示返回按钮
        binding.header.backGroup.visibility = View.VISIBLE
    }

    /**
     * 初始化点击事件
     */
    private fun initEvent() {
        // 绑定卡片点击事件
        binding.bluetoothCrv.setOnClickListener(this)
        binding.otaCrv.setOnClickListener(this)
        binding.gatewayCrv.setOnClickListener(this)

        // 绑定返回按钮事件
        binding.header.backImg.setOnClickListener(this)
        binding.header.backTv.setOnClickListener(this)
    }

    /**
     * 显示断开连接提示弹窗
     */
    private fun showDisconnectDialog() {
        mMessageDialog = MessageDialog.Builder(this@WiFiSettingActivity)
            .setMessage(getString(R.string.disconnet))
            .setConfirm(getString(R.string.ok))
            .setCancel(null)
            .setListener(
                object : MessageDialog.OnListener {
                    override fun onConfirm(dialog: WFBaseDialog?) {
                        finish()
                    }

                    override fun onCancel(dialog: WFBaseDialog?) {}
                })
            .show()
    }

    /**
     * 蓝牙断开连接回调
     */
    override fun blePeripheralDisconnected() {
        super.blePeripheralDisconnected()
        BluetoothRepository.isDisconnected = true
        lifecycleScope.launch(Dispatchers.Main) {
            showDisconnectDialog()
        }
    }

    /**
     * 点击事件处理
     */
    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.bluetooth_crv -> BluetoothDmsActivity.activityStart(this)
            R.id.ota_crv -> WFOtaActivity.activityStart(this)
            R.id.gateway_crv -> GatewayActivity.activityStart(this)
            R.id.backImg, R.id.back_tv -> finish() // 统一处理返回按钮
        }
    }

    /**
     * 接收WiFi数据包回调
     */
    override fun packetReceived(strValue: String?) {
        LogUtils.d(TAG," APP WIFI packetReceived strValue => $strValue")
        BluetoothRepository.mGoneDNS = strValue?.contains("ERR") != true
    }

    /**
     * 页面销毁时清理资源
     */
    override fun onDestroy() {
        super.onDestroy()
        // 断开蓝牙连接（如果已连接）
        if (BluetoothRepository.isConnect()) {
            BluetoothRepository.disconnect()
        }
        // 注销回调
        BluetoothRepository.unRegisterViewCallback(this)
    }

    companion object {
        private const val TAG = "WiFiSettingActivity"

        /**
         * 启动WiFi设置页面
         * @param context 上下文
         * @param fscDevice 蓝牙设备信息
         */
        @Suppress("DEPRECATION")
        fun activityStart(context: Context, fscDevice: FscDevice) {
            Intent(context, WiFiSettingActivity::class.java).apply {
                // 适配Android T+的Parcelable兼容性
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    putExtra("FscDevice", fscDevice)
                } else {
                    putExtra("FscDevice", fscDevice)
                }
                context.startActivity(this)
            }
        }
    }
}