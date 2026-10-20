package com.feasycom.feasyblue.customview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.adapter.AtCommandAdapter
import com.feasycom.feasyblue.adapter.ForceAtCommandAdapter
import com.feasycom.feasyblue.databinding.CustomizeAtCommand1Binding

/**
 * 自定义 AT 命令视图（ViewBinding 版本）
 */
class CustomizeAtCommandView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var atCommandCount = 0
    var atCommandNoParam = 0

    // 声明 ViewBinding 实例
    private lateinit var binding: CustomizeAtCommand1Binding

    init {
        getAttrs(context, attrs)
        initView(context)
        setEvent()
    }

    private fun setEvent() {
        // 使用 ViewBinding 访问按钮控件
        binding.customizeCommandButton.setOnClickListener {
            binding.customizeAtCommandList.visibility =
                if (binding.customizeAtCommandList.visibility == GONE) VISIBLE else GONE
        }
    }

    private fun initView(context: Context) {
        // 初始化 ViewBinding 并绑定布局
        binding = CustomizeAtCommand1Binding.inflate(
            LayoutInflater.from(context),
            this,
            true // 自动添加到当前 ViewGroup
        )

        val listOf = listOf<String>(
            context.getString(R.string.deviceName),
            context.getString(R.string.pin),
            context.getString(R.string.baud)
        )

        // 初始化 ForceAtCommandAdapter
        with(binding.recyclerView) {
            layoutManager = LinearLayoutManager(context)
            adapter = ForceAtCommandAdapter(listOf)
        }

        // 初始化 AtCommandAdapter
        with(binding.customizeAtCommandList) {
            layoutManager = LinearLayoutManager(context)
            adapter = AtCommandAdapter(atCommandCount, atCommandNoParam).apply {
                countChangeCallback = { count ->
                    // 使用 ViewBinding 访问计数文本控件
                    binding.customizeSelectCount.text = count.toString()
                }
            }
        }
    }

    private fun getAttrs(context: Context, attrs: AttributeSet?) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.CustomizeAtCommandView)
        atCommandCount = a.getInt(R.styleable.CustomizeAtCommandView_atCommandCount, 0)
        atCommandNoParam = a.getInt(R.styleable.CustomizeAtCommandView_atCommandNoParam, 0)
        a.recycle()
    }

    companion object {
        private const val TAG = "CustomizeAtCommandView1"
    }
}