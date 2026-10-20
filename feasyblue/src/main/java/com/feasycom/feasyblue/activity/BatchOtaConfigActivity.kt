package com.feasycom.feasyblue.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.SeekBar
import androidx.core.widget.addTextChangedListener
import com.blankj.utilcode.util.LogUtils
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivityOtaConfigBinding
import com.feasycom.feasyblue.utils.getBoolean
import com.feasycom.feasyblue.utils.getInt
import com.feasycom.feasyblue.utils.getStr
import com.feasycom.feasyblue.utils.putBoolean
import com.feasycom.feasyblue.utils.putInt
import com.feasycom.feasyblue.utils.putStr

class BatchOtaConfigActivity : BaseActivity<ActivityOtaConfigBinding>() {

    /**
     * 实现父类的 ViewBinding 获取方法
     */
    override fun getViewBinding(): ActivityOtaConfigBinding {
        return ActivityOtaConfigBinding.inflate(layoutInflater)
    }

    /**
     * 初始化View（父类抽象方法）
     */
    @SuppressLint("SetTextI18n")
    override fun initView() {
        // 初始化Toolbar
        setupToolbar()

        // 初始化RSSI相关控件
        initRssiConfig()

        // 初始化设备名称相关控件
        initNameConfig()

        // 初始化确认按钮
        initConfirmButton()
    }

    /**
     * 设置Toolbar
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }

        // 设置标题和返回按钮
        binding.toolbarTitle.text = getString(R.string.filter)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    /**
     * 初始化RSSI过滤配置
     */
    private fun initRssiConfig() {
        // 获取并设置RSSI开关状态
        val rssiSwitchState = getBoolean("otaRssiSwitch", false)
        binding.rssiSwitch.isChecked = rssiSwitchState
        binding.rssiGroup.visibility = if (rssiSwitchState) View.VISIBLE else View.GONE

        // 获取并设置RSSI初始值（-70 ~ 0 转换为 0 ~ 70）
        val defaultRssiValue = -70
        val savedRssiValue = getInt("otaRssiValue", defaultRssiValue)
        val seekBarProgress = savedRssiValue - defaultRssiValue // 转换为SeekBar进度值

        // 设置SeekBar进度和显示文本
        binding.rssiSeekBar.progress = seekBarProgress
        updateRssiValueText(savedRssiValue)

        // SeekBar进度变化监听
        binding.rssiSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val rssiValue = progress + defaultRssiValue // 转换回实际RSSI值
                updateRssiValueText(rssiValue)
                LogUtils.d(TAG, "APP onProgressChanged => $progress (RSSI: $rssiValue dB)")
                putInt("otaRssiValue", rssiValue)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                val rssiValue = seekBar.progress + defaultRssiValue
                updateRssiValueText(rssiValue)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val rssiValue = seekBar.progress + defaultRssiValue
                updateRssiValueText(rssiValue)
            }
        })

        // RSSI开关状态变化监听
        binding.rssiSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.rssiGroup.visibility = if (isChecked) View.VISIBLE else View.GONE
            putBoolean("otaRssiSwitch", isChecked)
        }
    }

    /**
     * 更新RSSI值显示文本
     */
    private fun updateRssiValueText(rssiValue: Int) {
        binding.rssiValueTextView.text = ":  $rssiValue dB"
    }

    /**
     * 初始化设备名称过滤配置
     */
    private fun initNameConfig() {
        // 获取并设置名称开关状态
        val nameSwitchState = getBoolean("otaNameSwitch", false)
        binding.nameSwitch.isChecked = nameSwitchState
        binding.nameEditText.visibility = if (nameSwitchState) View.VISIBLE else View.GONE

        // 设置保存的名称值
        val savedNameValue = getStr("otaNameValue", "")
        binding.nameEditText.setText(savedNameValue)

        // 名称输入框文本变化监听
        binding.nameEditText.addTextChangedListener { editable ->
            val text = editable?.toString() ?: ""
            putStr("otaNameValue", text)
        }

        // 名称开关状态变化监听
        binding.nameSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.nameEditText.visibility = if (isChecked) View.VISIBLE else View.GONE
            putBoolean("otaNameSwitch", isChecked)
        }
    }

    /**
     * 初始化确认按钮
     */
    private fun initConfirmButton() {
        binding.button.setOnClickListener {
            finish()
        }
    }

    companion object {
        const val TAG = "BatchOtaConfigActivity"

        /**
         * 启动OTA配置页面
         */
        fun activityStart(context: Context) {
            val intent = Intent(context, BatchOtaConfigActivity::class.java)
            context.startActivity(intent)
        }
    }
}