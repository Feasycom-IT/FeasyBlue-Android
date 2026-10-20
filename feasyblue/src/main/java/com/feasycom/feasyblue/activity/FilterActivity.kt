package com.feasycom.feasyblue.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.SeekBar
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivityFilterBinding
import com.feasycom.feasyblue.utils.*

/**
 * 过滤页面
 * 修复 addTextChangedListener 语法错误 + 适配 ViewBinding + 保留所有原始逻辑
 */
class FilterActivity : BaseActivity<ActivityFilterBinding>(), View.OnClickListener {

    private lateinit var handler: Handler
    private val nameFilterClickRunnable = Runnable {
        binding.nameFilterSwitch.isEnabled = true
    }

    /**
     * 实现 ViewBinding 获取方法（替换原 getLayout() 方式）
     */
    override fun getViewBinding(): ActivityFilterBinding {
        return ActivityFilterBinding.inflate(layoutInflater)
    }

    @SuppressLint("SetTextI18n")
    override fun initView() {
        // 初始化Handler（避免Looper为空）
        handler = Handler(Looper.myLooper() ?: Looper.getMainLooper())

        // 设置工具栏（替换 findViewById 为 binding）
        setSupportActionBar(binding.toolbar)
        supportActionBar?.let {
            it.setDisplayShowTitleEnabled(false)
            it.setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbarTitle.text = getString(R.string.filter)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // 设置过滤开关
        binding.nameFilterSwitch.setOnClickListener(this)

        // 初始化RSSI进度条
        val defaultRssi = getInt("rssiValue", -100)
        binding.rssiSeekBar.progress = defaultRssi + 100
        binding.rssiFilterThresholdAtv.text = " $defaultRssi dB"

        // 初始化名称过滤开关和编辑框
        val nameSwitch = getBoolean("nameSwitch", false)
        binding.nameFilterSwitch.isOpened = nameSwitch
        binding.nameFilterAet.setText(getStr("nameValue", ""))

        // ========== 修复核心语法错误：addTextChangedListener 标准实现 ==========
        binding.nameFilterAet.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 替换原错误的 it 为 s，实现文本持久化
                putStr("nameValue", s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // 设置RSSI进度条监听器（保留原始逻辑）
        binding.rssiSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.rssiFilterThresholdAtv.text = " ${progress - 100} dB"
                putInt("rssiValue", progress - 100)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                val start = seekBar.progress
                binding.rssiFilterThresholdAtv.text = "${start - 100} dB"
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val end = seekBar.progress
                binding.rssiFilterThresholdAtv.text = "${end - 100} dB"
            }
        })
    }

    /**
     * 移除原 getLayout() 方法（ViewBinding 无需返回布局ID）
     */
    // override fun getLayout() = R.layout.activity_filter

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.name_filter_switch -> {
                // 保留原始防抖逻辑
                binding.nameFilterSwitch.isEnabled = false
                handler.postDelayed(nameFilterClickRunnable, 300)
                putBoolean("nameSwitch", binding.nameFilterSwitch.isOpened)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 保留原始回调清理逻辑 + 优化内存泄漏
        handler.removeCallbacks(nameFilterClickRunnable)
        if (::handler.isInitialized) {
            handler.removeCallbacksAndMessages(null)
        }
    }

    companion object {
        /**
         * 启动方法（保留原始逻辑）
         */
        fun activityStart(context: Context) {
            val intent = Intent(context, FilterActivity::class.java)
            context.startActivity(intent)
        }
    }
}