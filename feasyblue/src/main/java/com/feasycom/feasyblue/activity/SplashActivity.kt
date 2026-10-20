package com.feasycom.feasyblue.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivitySplashBinding
import com.feasycom.feasyblue.dialog.FirstDialogFragment
import com.feasycom.feasyblue.utils.*
import com.feasycom.network.DeviceNetwork
import com.feasycom.network.bean.AdParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

@SuppressLint("SetTextI18n")
class SplashActivity : BaseActivity<ActivitySplashBinding>(), CoroutineScope by MainScope() {

    private var mFirstDialogFragment: FirstDialogFragment? = null

    /**
     * 实现父类的 ViewBinding 获取方法（核心适配点）
     */
    override fun getViewBinding(): ActivitySplashBinding {
        return ActivitySplashBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 🚀 在 super.onCreate() 之后立即隐藏系统UI


        // 初始化Toast样式
        initToastStyle()

        // 清理偏好设置
        clearPreference()

        // 处理首次进入逻辑
        handleFirstInLogic()
    }

    /**
     * 初始化View（父类抽象方法实现）
     */
    override fun initView() {
        // 此处无需额外初始化，核心逻辑已在onCreate中处理
    }


    /**
     * 初始化Toast样式
     */
    private fun initToastStyle() {
        ToastUtils.getDefaultMaker().setGravity(Gravity.CENTER, 0, 0)
    }


    /**
     * 清理所有偏好设置
     */
    private fun clearPreference() {
        clearFilter()
        clearBatchFilter()
        clearFeed()
        clearTcp()
        clearPassword()
        clearDeveloperDebug()
    }

    /**
     * 清理过滤相关设置
     */
    private fun clearFilter() {
        putBoolean("rssiSwitch", false)
        putBoolean("nameSwitch", false)
        putInt("rssiValue", -100)
        putStr("nameValue", "")
    }

    /**
     * 清理批量过滤相关设置
     */
    private fun clearBatchFilter() {
        putBoolean("otaRssiSwitch", false)
        putBoolean("otaNameSwitch", false)
        putInt("otaRssiValue", -70)
        putStr("otaNameValue", "")
    }

    /**
     * 清理反馈相关设置
     */
    private fun clearFeed() {
        putStr("FEEDBACK_ADVICE", "")
        putStr("FEEDBACK_BUG", "")
        putStr("FEEDBACK_UI", "")
        putStr("FEEDBACK_COOPERATION", "")
    }

    /**
     * 清理TCP相关设置
     */
    private fun clearTcp() {
        putStr("tcpServer", "")
        putStr("tcpPort", "")
        putInt("selection", 0)
    }

    /**
     * 清理密码相关设置
     */
    private fun clearPassword() {
        putBoolean("isPasswordPara", false)
        putBoolean("isPasswordEAdvance", false)
    }

    /**
     * 清理开发者调试相关设置
     */
    private fun clearDeveloperDebug() {
        putBoolean("facpSwitchOpen", true)
        putStr("customUuid", "")
        putBoolean("sdpServiceSwitchOpen", false)
    }

    /**
     * 处理首次进入应用逻辑
     */
    private fun handleFirstInLogic() {
        val isFirstTime = getBoolean("firstTime", false)

        if (!isFirstTime) {
            // 首次进入，显示用户协议对话框
            showFirstDialog()
        } else {
            // 非首次进入，预加载广告图片
            preloadAdImage()
        }
    }

    /**
     * 显示首次进入对话框
     */
    private fun showFirstDialog() {
        mFirstDialogFragment = FirstDialogFragment().apply {
            onAgree = {
                putBoolean("firstTime", true)
                dismiss()
                navigateToMainActivity()
            }
            onRefuse = { finish() }
        }

        // 确保对话框未添加时才显示
        if (mFirstDialogFragment?.isAdded == false) {
            mFirstDialogFragment?.show(supportFragmentManager, "firstTime")
        }
    }

    /**
     * 检查是否有缓存的广告图片
     */
    private fun hasCachedAdImage(): Boolean {
        val isChinese = resources.configuration.locales[0].language.endsWith("zh")
        val cacheKey = if (isChinese) "lanch1" else "lanch2"
        val cachedImagePath = getStr(cacheKey, "")

        return cachedImagePath.isNotEmpty() && try {
            val file = File(cachedImagePath)
            file.exists() && file.length() > 0
        } catch (e: Exception) {
            LogUtils.e(TAG, "检查广告缓存失败: ${e.message}")
            false
        }
    }

    /**
     * 预加载广告图片
     */
    private fun preloadAdImage() {
        launch(Dispatchers.IO) {
            try {
                val adParams = AdParams("Android", "com.feasycom.feasyblue")
                val adData = DeviceNetwork.getAdData(adParams)
                LogUtils.e(TAG, "预加载广告----: ${adData.msg}")
                if (adData != null && adData.code == 0) {
                    // 广告数据获取成功，处理并缓存广告图片
                    val isChinese = resources.configuration.locales[0].language.endsWith("zh")
                    val imageUrl = if (isChinese) adData.data.cnimg else adData.data.enimg
                    val urls = if (isChinese) {
                        listOf(adData.data.cnurl1, adData.data.cnurl2, adData.data.cnurl3)
                    } else {
                        listOf(adData.data.enurl1, adData.data.enurl2, adData.data.enurl3)
                    }

                    // 使用Glide下载并缓存图片
                    val target = com.bumptech.glide.Glide.with(this@SplashActivity)
                        .asFile()
                        .load(imageUrl)
                        .submit()
                    val imageFile = target.get(8, TimeUnit.SECONDS)

                    // 保存广告缓存信息
                    val cacheKey = if (isChinese) "lanch1" else "lanch2"
                    putStr(cacheKey, imageFile.path)

                    // 保存广告链接
                    if (isChinese) {
                        putStr("cnurl1", urls[0])
                        putStr("cnurl2", urls[1])
                        putStr("cnurl3", urls[2])
                    } else {
                        putStr("enurl1", urls[0])
                        putStr("enurl2", urls[1])
                        putStr("enurl3", urls[2])
                    }

                    LogUtils.d(TAG, "广告数据获取成功，缓存图片并跳转到广告页面")

                    // 切换到主线程跳转页面
                    withContext(Dispatchers.Main) {
                        navigateToAdActivity()
                    }
                } else {
                    // 广告数据获取失败，清理缓存并跳转到主页面
                    clearAdCache()
                    withContext(Dispatchers.Main) {
                        navigateToMainActivity()
                    }
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "预加载广告失败: ${e.message}", e)

                // 异常处理：有缓存则跳广告页，无缓存则跳主页面
                navigateToMainActivity()
            }
        }
    }

    /**
     * 跳转到广告页面
     */
    private fun navigateToAdActivity() {
        startActivity(Intent(this, AdActivity::class.java))
        finish()
    }

    /**
     * 跳转到主页面
     */
    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    /**
     * 清除广告缓存
     */
    private fun clearAdCache() {
      /*  try {
            putStr("lanch1", "")
            putStr("lanch2", "")
            putStr("cnurl1", "")
            putStr("cnurl2", "")
            putStr("cnurl3", "")
            putStr("enurl1", "")
            putStr("enurl2", "")
            putStr("enurl3", "")
            LogUtils.e(TAG, "广告缓存已清除")
        } catch (e: Exception) {
            LogUtils.e(TAG, "清除广告缓存失败: ${e.message}", e)
        }*/
    }

    /**
     * 页面销毁时清理资源
     */
    override fun onDestroy() {
        super.onDestroy()
        // 取消所有协程
        cancel()
        // 清理对话框引用
        mFirstDialogFragment = null
    }

    companion object {
        const val TAG = "SplashScreenActivity"
    }
}