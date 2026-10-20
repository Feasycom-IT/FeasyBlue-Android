package com.feasycom.feasyblue.activity

import android.bluetooth.BluetoothDevice
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.feasycom.common.bean.ConnectType
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivityAdvancedSettingsListBinding
import com.feasycom.feasyblue.utils.*
import com.feasycom.spp.controler.FscSppCentralApi
import com.feasycom.spp.controler.FscSppCentralApiImp
import com.feasycom.spp.controler.FscSppCentralCallbacksImp

/**
 * 高级设置列表页面
 * 适配优化后的泛型 ViewBinding BaseActivity
 */
class AdvancedSettingsListActivity : BaseActivity<ActivityAdvancedSettingsListBinding>(), View.OnClickListener {

    private val sFscSppCentralApi: FscSppCentralApi by lazy {
        FscSppCentralApiImp.getInstance(this)
    }

    /**
     * 实现父类的 ViewBinding 获取方法（核心适配点）
     */
    override fun getViewBinding(): ActivityAdvancedSettingsListBinding {
        return ActivityAdvancedSettingsListBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // 直接使用父类的 binding 变量访问视图（无需自己声明 _binding）
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbarTitle.text = getString(R.string.advanced_setting_text)

        binding.toolbar.setNavigationOnClickListener {
            MainActivity.activityStart(this, 1)
            finish()
        }

        // 初始化开关状态
        binding.sdpServiceSwitchView.isOpened = getBoolean("sdpServiceSwitchOpen", false)
        if (binding.sdpServiceSwitchView.isOpened) disableCustomUuidInputs()

        // 初始化 UUID 输入框
        val customUuid = getStr("customUuid", "")
        if (customUuid.isNotEmpty()) fillCustomUuidInputs(customUuid)

        // 初始化事件绑定
        initEvent()

        // 设置 SPP 回调
        sFscSppCentralApi.setCallbacks(object : FscSppCentralCallbacksImp() {
            override fun sppPeripheralConnected(device: BluetoothDevice, type: ConnectType) {
                ThroughputActivity.activityStart(this@AdvancedSettingsListActivity, device)
                finish()
            }
        })
    }

    private fun initEvent() {
        // 绑定点击事件
        binding.sppAutoConnectTestButton.setOnClickListener(this)
        binding.bleAutoConnectTestButton.setOnClickListener(this)
        binding.sdpServiceSwitchView.setOnClickListener(this)
        binding.restoreUuidAcb.setOnClickListener(this)

        // 绑定长按事件（复制粘贴 UUID）
        listOf(
            binding.customUuid8Et,
            binding.customUuid4Et1,
            binding.customUuid4Et2,
            binding.customUuid4Et3,
            binding.customUuid12Et
        ).forEach { setOnLongClickListener(it) }
    }

    private fun setOnLongClickListener(editText: EditText) {
        editText.setOnLongClickListener {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboardManager.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val copiedText = clipData.getItemAt(0).text
                Log.e("TAG", "copiedText => $copiedText")
                // 处理 32 位 UUID 文本自动拆分填充
                if (copiedText.length == 32) {
                    val editTexts = listOf(
                        binding.customUuid8Et,
                        binding.customUuid4Et1,
                        binding.customUuid4Et2,
                        binding.customUuid4Et3,
                        binding.customUuid12Et
                    )
                    formatAndFillEditText(copiedText.toString(), editTexts)
                }
            }
            true
        }
    }

    /**
     * 处理 UUID 文本拆分填充（补充实现，避免编译错误）
     */
    private fun formatAndFillEditText(fullText: String, editTexts: List<EditText>) {
        val ranges = listOf(0..7, 8..11, 12..15, 16..19, 20..31)
        editTexts.zip(ranges).forEach { (et, range) ->
            et.setText(fullText.substring(range))
        }
    }

    /**
     * 移除旧的 getLayout() 方法（由父类 ViewBinding 接管）
     */
    // override fun getLayout(): Int = R.layout.activity_advanced_settings_list

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            MainActivity.activityStart(this, 1)
            finish()
            false
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.sppAutoConnectTestButton -> {
                SppAutoConnectTestActivity.activityStart(this)
            }
            R.id.bleAutoConnectTestButton -> {
                BleAutoConnectTestActivity.activityStart(this)
            }
            R.id.sdp_service_switch_view -> handleSdpServiceSwitch()
            R.id.restore_uuid_acb -> clearCustomUuidInputs()
        }
    }

    private fun handleSdpServiceSwitch() {
        if (binding.sdpServiceSwitchView.isOpened) {
            val customUuid = getCustomUuid()
            if (isValidCustomUuid()) {
                putStr("customUuid", customUuid)
                sFscSppCentralApi.openSdpService(customUuid)
                putBoolean("sdpServiceSwitchOpen", true)
                disableCustomUuidInputs()
            } else {
                showToastAndDisableSdpService()
            }
        } else {
            putBoolean("sdpServiceSwitchOpen", false)
            binding.sdpServiceSwitchView.isOpened = false
            sFscSppCentralApi.closeSdpService()
            enableCustomUuidInputs()
        }
    }

    private fun getCustomUuid(): String {
        // 拼接 UUID 输入框内容
        return "${binding.customUuid8Et.text}-${binding.customUuid4Et1.text}-${binding.customUuid4Et2.text}-${binding.customUuid4Et3.text}-${binding.customUuid12Et.text}"
    }

    private fun isValidCustomUuid(): Boolean {
        val editTexts = listOf(
            binding.customUuid8Et,
            binding.customUuid4Et1,
            binding.customUuid4Et2,
            binding.customUuid4Et3,
            binding.customUuid12Et
        )
        val lengths = listOf(8, 4, 4, 4, 12)
        return editTexts.zip(lengths).all { (et, len) ->
            et.text.length == len && et.text.matches(hexRegex.toRegex())
        }
    }

    private fun showToastAndDisableSdpService() {
        putBoolean("sdpServiceSwitchOpen", false)
        binding.sdpServiceSwitchView.isOpened = false
        Toast.makeText(this, getString(R.string.none), Toast.LENGTH_SHORT).show()
    }

    private fun disableCustomUuidInputs() {
        listOf(
            binding.customUuid8Et,
            binding.customUuid4Et1,
            binding.customUuid4Et2,
            binding.customUuid4Et3,
            binding.customUuid12Et
        ).forEach { it.isEnabled = false }
    }

    private fun enableCustomUuidInputs() {
        listOf(
            binding.customUuid8Et,
            binding.customUuid4Et1,
            binding.customUuid4Et2,
            binding.customUuid4Et3,
            binding.customUuid12Et
        ).forEach { it.isEnabled = true }
    }

    private fun fillCustomUuidInputs(customUuid: String) {
        val hexArray = customUuid.split("-")
        binding.customUuid8Et.setText(hexArray[0])
        binding.customUuid4Et1.setText(hexArray[1])
        binding.customUuid4Et2.setText(hexArray[2])
        binding.customUuid4Et3.setText(hexArray[3])
        binding.customUuid12Et.setText(hexArray[4])
    }

    private fun clearCustomUuidInputs() {
        listOf(
            binding.customUuid8Et,
            binding.customUuid4Et1,
            binding.customUuid4Et2,
            binding.customUuid4Et3,
            binding.customUuid12Et
        ).forEach { it.text.clear() }
    }

    companion object {
        const val hexRegex = "^[0-9A-Fa-f]+$"

        fun activityStart(context: Context) {
            val intent = Intent(context, AdvancedSettingsListActivity::class.java)
            context.startActivity(intent)
        }
    }
}