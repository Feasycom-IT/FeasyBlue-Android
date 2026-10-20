package com.feasycom.feasyblue.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivityDeveloperDebugBinding
import com.feasycom.feasyblue.logcat.LogcatService
import com.feasycom.feasyblue.utils.getBoolean
import com.feasycom.feasyblue.utils.putBoolean

class DeveloperDebugActivity : BaseActivity<ActivityDeveloperDebugBinding>(), View.OnClickListener {

    /**
     * 实现父类的 ViewBinding 获取方法（核心适配点）
     */
    override fun getViewBinding(): ActivityDeveloperDebugBinding {
        return ActivityDeveloperDebugBinding.inflate(layoutInflater)
    }

    /**
     * 初始化View（父类抽象方法）
     */
    override fun initView() {
        // 初始化Toolbar
        setupToolbar()

        // 初始化开关状态
        initSwitchStates()

        // 初始化点击事件
        initEvent()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    /**
     * 配置Toolbar
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }

        // 设置标题和返回按钮
        binding.toolbarTitle.text = getString(R.string.developer_debug)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    /**
     * 初始化开关状态
     */
    private fun initSwitchStates() {
        // 从偏好设置读取开关状态
        binding.facpSwitchView.isOpened = getBoolean("facpSwitchOpen", false)
        binding.debugLogSwitchView.isOpened = getBoolean("debugLogSwitchOpen", false)
    }

    /**
     * 初始化点击事件
     */
    private fun initEvent() {
        binding.facpSwitchView.setOnClickListener(this)
        binding.debugLogSwitchView.setOnClickListener(this)
    }

    /**
     * 点击事件处理
     */
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.facp_switch_view -> {
                // 保存FACP开关状态
                putBoolean("facpSwitchOpen", binding.facpSwitchView.isOpened)
            }
            R.id.debug_log_switch_view -> {
                val isOpened = binding.debugLogSwitchView.isOpened
                // 保存日志开关状态
                putBoolean("debugLogSwitchOpen", isOpened)

                // 根据开关状态启动/停止日志服务
                if (isOpened) {
                    openLog()
                } else {
                    closeLog()
                }
            }
        }
    }

    /**
     * 启动日志服务
     */
    private fun openLog() {
        // 使用ContextCompat确保兼容性
        ContextCompat.startForegroundService(this, Intent(this, LogcatService::class.java))
    }

    /**
     * 停止日志服务
     */
    private fun closeLog() {
        stopService(Intent(this, LogcatService::class.java))
    }

    /**
     * 页面销毁时停止日志服务
     */
    override fun onDestroy() {
        closeLog()
        super.onDestroy()
    }

    companion object {
        const val TAG: String = "DeveloperDebugActivity"

        /**
         * 启动开发者调试页面
         */
        fun activityStart(context: Context) {
            val intent = Intent(context, DeveloperDebugActivity::class.java)
            context.startActivity(intent)
        }
    }
}