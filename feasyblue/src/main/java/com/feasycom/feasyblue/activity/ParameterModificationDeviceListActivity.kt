package com.feasycom.feasyblue.activity

import android.content.Context
import android.content.Intent
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivitySearchDeviceBinding

/**
 * 参数修改设备列表页面
 * 适配 ViewBinding 模式，遵循 BaseActivity 规范
 */
class ParameterModificationDeviceListActivity :
    BaseActivity<ActivitySearchDeviceBinding>() {

    // 实现 BaseActivity 的抽象方法，提供 ViewBinding 实例
    override fun getViewBinding(): ActivitySearchDeviceBinding {
        return ActivitySearchDeviceBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // 这里可以添加页面的初始化逻辑，比如：
        // 1. 设置标题栏
        // 2. 初始化 RecyclerView/ListView
        // 3. 绑定点击事件等
        // 示例：binding.toolbar.title = "参数修改设备列表"
    }

    companion object {
        const val TAG: String = "ParameterModify"

        fun activityStart(context: Context) {
            val intent = Intent(context, ParameterModificationDeviceListActivity::class.java)
            context.startActivity(intent)
        }
    }
}