package com.feasycom.feasyblue.customview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.MtuViewBinding

/**
 * MTU 调节自定义视图（ViewBinding 版本）
 */
class MtuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    var setMtu: ((mtu: Int) -> Unit)? = null

    // 声明并初始化 ViewBinding 实例
    private val binding: MtuViewBinding = MtuViewBinding.inflate(
        LayoutInflater.from(context),
        this,
        true // 自动将布局添加到当前 ConstraintLayout
    )

    init {
        initEvent()
    }

    private fun initEvent() {
        // SeekBar 进度变化监听（保留原有逻辑）
        binding.rangeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.mtuTextView.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 减号按钮点击事件
        binding.minusButton.setOnClickListener {
            binding.rangeSeekBar.progress = binding.rangeSeekBar.progress - 1
        }

        // 加号按钮点击事件
        binding.plusButton.setOnClickListener {
            binding.rangeSeekBar.progress = binding.rangeSeekBar.progress + 1
        }

        // 确认按钮点击事件（回调 MTU 值）
        binding.okButton.setOnClickListener {
            setMtu?.invoke(binding.rangeSeekBar.progress)
        }
    }

    /**
     * 设置 SeekBar 进度
     */
    fun setProgress(progress: Int) {
        binding.rangeSeekBar.progress = progress
    }
}