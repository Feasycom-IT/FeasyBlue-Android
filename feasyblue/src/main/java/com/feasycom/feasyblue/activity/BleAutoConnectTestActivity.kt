package com.feasycom.feasyblue.activity

import android.content.Context
import android.content.Intent
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivitySearchDeviceBinding

/**
 * BLE自动连接测试页面
 * 适配 ViewBinding 规范，保留原始逻辑
 */
class BleAutoConnectTestActivity : BaseActivity<ActivitySearchDeviceBinding>() {

    /**
     * 实现 ViewBinding 获取方法（替换原 getLayout() 方式）
     */
    override fun getViewBinding(): ActivitySearchDeviceBinding {
        return ActivitySearchDeviceBinding.inflate(layoutInflater)
    }

    /**
     * 保留原始 initView 方法（无业务逻辑，空实现）
     */
    override fun initView() {
        // 原始代码无逻辑，保持空实现
    }

    companion object {
        /**
         * 启动方法（完全保留原始逻辑）
         */
        fun activityStart(context: Context) {
            val intent = Intent(context, BleAutoConnectTestActivity::class.java)
            context.startActivity(intent)
        }
    }
}