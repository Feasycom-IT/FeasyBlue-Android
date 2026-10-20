package com.feasycom.feasyblue.activity

import android.content.Context
import android.content.Intent
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivitySearchDeviceBinding
import com.feasycom.feasyblue.databinding.ActivityWifiSearchDeviceBinding

/**
 * WiFi 设备搜索页面
 * 适配 ViewBinding 模式，遵循 BaseActivity 规范
 */
class WiFiSearchDeviceActivity : BaseActivity<ActivitySearchDeviceBinding>() {
//ActivityWifiSearchDeviceBinding
    // 实现 BaseActivity 的抽象方法，提供 ViewBinding 实例
    override fun getViewBinding(): ActivitySearchDeviceBinding {
        return ActivitySearchDeviceBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // 后续可在这里添加页面初始化逻辑，示例：
        // 1. 设置标题：binding.toolbar.title = "WiFi设备搜索"
        // 2. 初始化列表：binding.recyclerViewDevice.adapter = DeviceAdapter()
        // 3. 绑定点击事件：binding.btnSearch.setOnClickListener { ... }
    }

    companion object {
        /**
         * 启动当前页面
         */
        fun activityStart(context: Context) {
            val intent = Intent(context, WiFiSearchDeviceActivity::class.java)
            context.startActivity(intent)
        }
    }
}