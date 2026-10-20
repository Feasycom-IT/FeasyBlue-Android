package com.feasycom.feasyblue.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.SeekBar
import android.text.Editable
import android.text.TextWatcher
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivityBatchFilterBinding
import com.feasycom.feasyblue.utils.*

/**
 * 批量过滤页面
 * 修复 addTextChangedListener 语法错误 + it 未解析引用，保留所有原始逻辑
 */
class BatchFilterActivity : BaseActivity<ActivityBatchFilterBinding>(), View.OnClickListener {

    private lateinit var handler: Handler
    private val nameFilterClickRunnable = Runnable {
        binding.batchNameFilterSwitch.isEnabled = true
    }

    /**
     * 实现 ViewBinding 获取方法
     */
    override fun getViewBinding(): ActivityBatchFilterBinding {
        return ActivityBatchFilterBinding.inflate(layoutInflater)
    }

    @SuppressLint("SetTextI18n")
    override fun initView() {
        // 初始化Handler（避免Looper为空）
        handler = Handler(Looper.myLooper() ?: Looper.getMainLooper())

        // 设置工具栏
        setSupportActionBar(binding.batchToolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }
        binding.batchToolbarTitle.text = getString(R.string.filter)
        binding.batchToolbar.setNavigationOnClickListener { finish() }

        // 设置过滤开关
        binding.batchNameFilterSwitch.setOnClickListener(this)

        // 初始化RSSI进度条
        val defaultRssi = getInt("batchRssiValue", -70)
        binding.batchRssiSeekBar.progress = defaultRssi + 70
        binding.batchRssiFilterThresholdAtv.text = " $defaultRssi dB"

        // 初始化名称过滤开关和编辑框（修复核心语法错误）
        binding.batchNameFilterSwitch.isOpened = getBoolean("batchNameSwitch", false)
        binding.batchNameFilterAet.setText(getStr("batchNameValue", ""))
        // ========== 修复点1：替换错误的 addTextChangedListener 写法 ==========
        binding.batchNameFilterAet.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // ========== 修复点2：替换未解析的 it 为 s ==========
                putStr("batchNameValue", s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // 设置RSSI进度条监听器
        binding.batchRssiSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val rssiValue = progress - 70
                binding.batchRssiFilterThresholdAtv.text = " $rssiValue dB"
                putInt("batchRssiValue", rssiValue)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                binding.batchRssiFilterThresholdAtv.text = "${seekBar.progress - 70} dB"
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                binding.batchRssiFilterThresholdAtv.text = "${seekBar.progress - 70} dB"
            }
        })
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.batch_name_filter_switch -> {
                // 解决重复快速点击导致hexCheck时 发送框内容转换出现错误的bug
                binding.batchNameFilterSwitch.isEnabled = false
                handler.postDelayed(nameFilterClickRunnable, 300)
                putBoolean("batchNameSwitch", binding.batchNameFilterSwitch.isOpened)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 移除回调，避免内存泄漏
        handler.removeCallbacks(nameFilterClickRunnable)
        if (::handler.isInitialized) {
            handler.removeCallbacksAndMessages(null)
        }
    }

    companion object {
        fun activityStart(context: Context) {
            val intent = Intent(context, BatchFilterActivity::class.java)
            context.startActivity(intent)
        }
    }
}