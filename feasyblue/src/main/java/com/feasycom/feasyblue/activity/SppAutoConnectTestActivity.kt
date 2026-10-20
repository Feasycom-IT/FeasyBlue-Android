package com.feasycom.feasyblue.activity

import android.content.Context
import android.content.Intent
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivitySearchDeviceBinding

/**
 * SPP自动连接测试页面
 * 适配 ViewBinding 规范，无业务逻辑修改
 */
class SppAutoConnectTestActivity : BaseActivity<ActivitySearchDeviceBinding>() {

    /**
     * 实现 ViewBinding 获取方法（替换原 getLayout() 方式）
     * 自动生成的 ActivitySearchDeviceBinding 对应布局文件 activity_search_device.xml
     */
    override fun getViewBinding(): ActivitySearchDeviceBinding {
        return ActivitySearchDeviceBinding.inflate(layoutInflater)
    }

    /**
     * 保留原始 initView 方法（空实现，与原始代码一致）
     */
    override fun initView() {
        // 原始代码无业务逻辑，保持空实现
    }

    companion object {
        /**
         * 启动方法（完全保留原始逻辑，无任何修改）
         */
        fun activityStart(context: Context) {
            val intent = Intent(context, SppAutoConnectTestActivity::class.java)
            context.startActivity(intent)
        }
    }
}