package com.feasycom.feasyblue.activity

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.LogUtils
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivityWifiOtaBinding
import com.feasycom.feasyblue.dialog.LoadDialogFragment
import com.feasycom.feasyblue.dialog.MessageDialog
import com.feasycom.feasyblue.dialog.WFBaseDialog
import com.feasycom.feasyblue.interfaces.FeasyWiFiCallbacks
import com.feasycom.feasyblue.logic.BluetoothRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * WiFi OTA升级页面
 * 适配优化后的泛型 ViewBinding BaseActivity
 */
class WFOtaActivity : BaseActivity<ActivityWifiOtaBinding>(), FeasyWiFiCallbacks {

    private var mWaitDialog: LoadDialogFragment? = null
    private var mMessageDialog: WFBaseDialog? = null

    /**
     * 实现父类的 ViewBinding 获取方法（核心适配点）
     */
    override fun getViewBinding(): ActivityWifiOtaBinding {
        return ActivityWifiOtaBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // 注册WiFi回调
        BluetoothRepository.registerViewCallback(this)

        // 初始化标题栏
        initToolbar()

        // 初始化点击事件
        initEvent()
    }

    /**
     * 初始化标题栏
     */
    private fun initToolbar() {
        binding.header.titleText.text = getString(R.string.title_ota)
        binding.header.backGroup.visibility = View.VISIBLE
    }

    /**
     * 初始化所有点击事件
     */
    private fun initEvent() {
        // 返回按钮事件
        binding.header.backImg.setOnClickListener { finish() }
        binding.header.backTv.setOnClickListener { finish() }

        // 查询IP按钮
        binding.queryIp.setOnClickListener {
            clearFocusAndHideKeyboard()
            BluetoothRepository.getIp()
        }

        // 查询版本按钮
        binding.queryVer.setOnClickListener {
            clearFocusAndHideKeyboard()
            BluetoothRepository.getVersion()
        }

        // 升级按钮
        binding.upgrade.setOnClickListener {
            val otaUrl = binding.ota.text.toString().trim()
            if (otaUrl.isNotBlank()) {
                clearFocusAndHideKeyboard()
                BluetoothRepository.startOTA(otaUrl)
            }
        }

        // 恢复出厂设置按钮
        binding.reset.setOnClickListener {
            clearFocusAndHideKeyboard()
            showResetConfirmDialog()
        }
    }

    /**
     * 显示恢复出厂设置确认弹窗
     */
    private fun showResetConfirmDialog() {
        mMessageDialog = MessageDialog.Builder(this)
            .setMessage(getString(R.string.reset_tips))
            .setConfirm(getString(R.string.pin_cancel))
            .setCancel(getString(R.string.reset))
            .setListener(object : MessageDialog.OnListener {
                override fun onConfirm(dialog: WFBaseDialog?) {
                    dialog?.dismiss()
                }

                override fun onCancel(dialog: WFBaseDialog?) {
                    super.onCancel(dialog)
                    dialog?.dismiss()
                    // 显示重置加载弹窗
                    mWaitDialog = LoadDialogFragment.newInstance(getString(R.string.reset_wait))
                    mWaitDialog?.show(supportFragmentManager, "reset")
                    BluetoothRepository.reset()
                }
            })
            .show()
    }

    /**
     * 清除所有输入框焦点并隐藏软键盘
     */
    private fun clearFocusAndHideKeyboard() {
        // 清除焦点
        binding.ota.clearFocus()
        binding.ip.clearFocus()
        binding.ver.clearFocus()

        // 隐藏软键盘
        hideKeyboard(binding.ota)
        hideKeyboard(binding.ip)
        hideKeyboard(binding.ver)
    }

    /**
     * 隐藏软键盘
     */
    private fun hideKeyboard(v: View) {
        val imm = v.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        if (imm.isActive) {
            imm.hideSoftInputFromWindow(v.applicationWindowToken, 0)
        }
    }

    /**
     * 蓝牙断开连接回调
     */
    override fun blePeripheralDisconnected() {
        lifecycleScope.launch(Dispatchers.Main) {
            BluetoothRepository.isDisconnected = true
            LogUtils.d(TAG, "FeasyBlue APP blePeripheralDisconnected 111111")

            // 安全检查：Activity不可用时直接返回
            if (isFinishing || isDestroyed) {
                LogUtils.d(TAG, "Activity is finishing or destroyed, abort.")
                return@launch
            }

            LogUtils.d(TAG, "FeasyBlue APP blePeripheralDisconnected 222222")

            // 关闭消息弹窗
            mMessageDialog?.takeIf { it.isShowing }?.dismiss()

            LogUtils.d(TAG, "FeasyBlue APP blePeripheralDisconnected 333333")

            // 关闭加载弹窗（允许状态丢失）
            mWaitDialog?.takeIf { it.isVisible }?.let { dialog ->
                dialog.dismissAllowingStateLoss()
                // 非重置弹窗才关闭页面
                if (dialog.tag != "reset" && !isFinishing && !isDestroyed) {
                    finish()
                    return@launch
                }
            }

            LogUtils.d(TAG, "FeasyBlue APP blePeripheralDisconnected 444444")

            // 最终安全检查后关闭页面
            if (!isFinishing && !isDestroyed) {
                finish()
            }

            LogUtils.d(TAG, "FeasyBlue APP blePeripheralDisconnected 555555")
        }
    }

    /**
     * IP信息回调
     */
    override fun ipInformation(ips: String?) {
        super.ipInformation(ips)
        lifecycleScope.launch(Dispatchers.Main) {
            binding.ip.text = ips ?: ""
        }
    }

    /**
     * 版本信息回调
     */
    override fun versionInformation(version: String?) {
        super.versionInformation(version)
        try {
            lifecycleScope.launch(Dispatchers.Main) {
                binding.ver.text = version ?: ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * OTA升级成功回调
     */
    override fun onOtaSuccess() {
        super.onOtaSuccess()
        LogUtils.d(TAG, "APP WIFI onOtaSuccess => 升级成功.")
        lifecycleScope.launch(Dispatchers.Main) {
            // 关闭加载弹窗
            mWaitDialog?.dismissAllowingStateLoss()

            // 显示升级成功提示
            mMessageDialog = MessageDialog.Builder(this@WFOtaActivity)
                .setMessage(getString(R.string.update_success))
                .setConfirm(getString(R.string.ok))
                .setCancel(null)
                .show()
        }
    }

    /**
     * OTA升级失败回调
     */
    override fun onOtaFailure() {
        super.onOtaFailure()
        LogUtils.d(TAG, "APP WIFI onOtaFailure => 升级失败.")
        lifecycleScope.launch(Dispatchers.Main) {
            // 关闭加载弹窗
            mWaitDialog?.dismissAllowingStateLoss()

            // 显示升级失败提示
            mMessageDialog = MessageDialog.Builder(this@WFOtaActivity)
                .setMessage(getString(R.string.update_fail))
                .setConfirm(getString(R.string.ok))
                .setCancel(null)
                .show()
        }
    }

    /**
     * OTA升级进度回调
     */
    override fun onOtaProgress(progress: String?) {
        super.onOtaProgress(progress)
        LogUtils.d(TAG, "APP WIFI onOtaProgress => $progress")
        lifecycleScope.launch(Dispatchers.Main) {
            mWaitDialog?.setMessage("${getString(R.string.upgrade_message)} ( ${progress ?: 0}%)")
        }
    }

    /**
     * OTA网络未配置回调
     */
    override fun onOtaNetworkIsNotConfigured() {
        super.onOtaNetworkIsNotConfigured()
        lifecycleScope.launch(Dispatchers.Main) {
            mMessageDialog = MessageDialog.Builder(this@WFOtaActivity)
                .setMessage(getString(R.string.ota_tips_dialog))
                .setConfirm(android.R.string.ok)
                .setCancel(null)
                .show()
            mMessageDialog?.setCanceledOnTouchOutside(false)
        }
    }

    /**
     * OTA开始配置回调
     */
    override fun onOtaStartConfig() {
        super.onOtaStartConfig()
        lifecycleScope.launch(Dispatchers.Main) {
            mWaitDialog = LoadDialogFragment.newInstance(getString(R.string.upgrade_message))
            mWaitDialog?.show(this@WFOtaActivity.supportFragmentManager, "ota")
        }
    }

    /**
     * 页面销毁时清理资源
     */
    override fun onDestroy() {
        super.onDestroy()
        // 注销回调
        BluetoothRepository.unRegisterViewCallback(this)

        // 清理对话框资源
        mMessageDialog?.dismiss()
        mWaitDialog?.dismissAllowingStateLoss()
    }

    companion object {
        const val TAG: String = "WFOtaActivity"

        /**
         * 启动WiFi OTA升级页面
         */
        fun activityStart(context: Context) {
            Intent(context, WFOtaActivity::class.java).apply {
                context.startActivity(this)
            }
        }
    }
}