package com.feasycom.feasyblue.activity

import android.app.Activity
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewbinding.ViewBinding
import com.blankj.utilcode.util.BarUtils

/**
 * 基础活动类，所有活动类应继承此类
 * 适配 ViewBinding，自动处理布局加载和字体缩放固定
 */
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    // 缓存Resources实例，避免重复创建
    private var fixedResources: Resources? = null

    // ViewBinding 实例，由子类提供
    protected lateinit var binding: VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 沉浸式状态栏
        setImmersiveStatusBar(this, true)
    //    BarUtils.transparentStatusBar(this@BaseActivity)
    //    BarUtils.setStatusBarLightMode(this@BaseActivity, true)

        // 1. 初始化 ViewBinding
        binding = getViewBinding()
        // 2. 设置布局（使用 ViewBinding 的 root 视图）
        setContentView(binding.root)
        // 3. 初始化视图
        initView()
    }

    override fun getResources(): Resources {
        // 使用缓存，避免频繁创建新的Resources实例
        if (fixedResources == null) {
            val resources = super.getResources()
            val configuration = resources.configuration
            configuration.fontScale = 1.0f // 固定字体缩放为1.0，不受系统字体大小影响
            fixedResources = createConfigurationContext(configuration).resources
        }
        return fixedResources ?: super.getResources()
    }


    private fun setImmersiveStatusBar(activity: Activity, isLightStatusBar: Boolean) {
        val window = activity.getWindow()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setStatusBarColor(Color.TRANSPARENT)
        val controller =
            WindowCompat.getInsetsController(window, window.getDecorView())
        controller.setAppearanceLightStatusBars(isLightStatusBar)

        val contentView = findViewById<View?>(Window.ID_ANDROID_CONTENT)
        ViewCompat.setOnApplyWindowInsetsListener(
            contentView,
            OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                // 只获取底部导航栏高度
                val bottom = insets!!.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                // 只设置底部padding，顶部0，不遮挡沉浸式
                v!!.setPadding(0, 0, 0, bottom)
                insets.consumeSystemWindowInsets()
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理资源引用，避免内存泄漏
        fixedResources = null
    }

    /**
     * 获取 ViewBinding 实例，子类必须实现
     * 示例：ActivityAgreementBinding.inflate(layoutInflater)
     */
    abstract fun getViewBinding(): VB

    /**
     * 初始化视图，子类必须实现
     */
    abstract fun initView()
}