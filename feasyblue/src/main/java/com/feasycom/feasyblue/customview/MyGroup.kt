package com.feasycom.feasyblue.customview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.RadioGroupViewBinding

/**
 * 自定义单选按钮组视图（ViewBinding 版本）
 */
class MyGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    // 声明并初始化 ViewBinding 实例
    private val binding: RadioGroupViewBinding = RadioGroupViewBinding.inflate(
        LayoutInflater.from(context),
        this,
        true // 自动将布局添加到当前 LinearLayout
    )

    // 点击回调接口
    var onClickListener: ((position: Int) -> Unit)? = null

    init {
        initView()
        initListener()
    }

    /**
     * 初始化视图状态
     */
    private fun initView() {
        // 默认选中第一个单选按钮
        binding.radioBnt1.isChecked = true
    }

    /**
     * 初始化单选组监听
     */
    private fun initListener() {
        binding.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            onClickListener?.invoke(
                when (checkedId) {
                    R.id.radio_bnt1 -> 0
                    R.id.radio_bnt2 -> 1
                    R.id.radio_bnt3 -> 2
                    else -> 0
                }
            )
        }
    }

    /**
     * 获取当前选中的单选按钮位置
     */
    fun getSelectRadioButton(): Int {
        return when (binding.radioGroup.checkedRadioButtonId) {
            R.id.radio_bnt1 -> 0
            R.id.radio_bnt2 -> 1
            R.id.radio_bnt3 -> 2
            else -> -1
        }
    }
}