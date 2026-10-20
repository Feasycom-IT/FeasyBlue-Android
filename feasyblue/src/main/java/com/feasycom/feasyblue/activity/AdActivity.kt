package com.feasycom.feasyblue.activity

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.text.TextUtils
import android.view.View
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.bumptech.glide.Glide
import com.feasycom.feasyblue.databinding.ActivityAdBinding
import com.feasycom.feasyblue.utils.*

/**
 * 广告启动页
 * 适配优化后的泛型 ViewBinding BaseActivity
 */
class AdActivity : BaseActivity<ActivityAdBinding>() {

    private lateinit var countDownTimer: CountDownTimer
    private var remainingTime: Long = 5000 // 总时长5秒
    private var timerStartTime: Long = 0
    private var isTimerRunning = false

    /**
     * 实现父类的 ViewBinding 获取方法（核心适配点）
     */
    override fun getViewBinding(): ActivityAdBinding {
        return ActivityAdBinding.inflate(layoutInflater)
    }

    override fun initView() {

        // 初始化计时器和广告数据
        initCountDownTimer()
        loadCachedAdData()
    }

    override fun onResume() {
        super.onResume()
        startOrResumeTimer()
    }

    override fun onPause() {
        super.onPause()
        pauseTimer()
    }



    private fun initCountDownTimer() {
        countDownTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = millisUntilFinished
                // 直接使用父类的 binding 访问控件
                binding.toMain.text = "${millisUntilFinished / 1000} s"
            }

            override fun onFinish() {
                remainingTime = 0
                isTimerRunning = false
                navigateToMainActivity()
            }
        }

        // 绑定跳过按钮点击事件
        binding.toMain.setOnClickListener {
            navigateToMainActivity()
        }
    }

    private fun startOrResumeTimer() {
        if (remainingTime <= 0) {
            navigateToMainActivity()
            return
        }

        if (!isTimerRunning) {
            countDownTimer.cancel()
            // 重新初始化计时器，使用当前的剩余时间
            countDownTimer = object : CountDownTimer(remainingTime, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    remainingTime = millisUntilFinished
                    binding.toMain.text = "${millisUntilFinished / 1000} s"
                }

                override fun onFinish() {
                    remainingTime = 0
                    isTimerRunning = false
                    navigateToMainActivity()
                }
            }
            countDownTimer.start()
            isTimerRunning = true
            timerStartTime = System.currentTimeMillis()
        }
    }

    private fun pauseTimer() {
        if (isTimerRunning) {
            countDownTimer.cancel()
            isTimerRunning = false

            // 计算实际经过的时间
            val elapsedTime = System.currentTimeMillis() - timerStartTime
            remainingTime = maxOf(0, remainingTime - elapsedTime)
        }
    }

    private fun isChineseLanguage(): Boolean {
        return try {
            resources.configuration.locales[0].language.endsWith("zh")
        } catch (e: Exception) {
            // 兼容低版本
            @Suppress("DEPRECATION")
            resources.configuration.locale.language.endsWith("zh")
        }
    }

    private fun loadCachedAdData() {
        val isChinese = isChineseLanguage()
        val imageKey = if (isChinese) "lanch1" else "lanch2"
        val urlKeys = if (isChinese) listOf("cnurl1", "cnurl2", "cnurl3")
        else listOf("enurl1", "enurl2", "enurl3")

        val cachedImagePath = getStr(imageKey)
        val cachedUrls = urlKeys.map { getStr(it) }

        if (TextUtils.isEmpty(cachedImagePath)) {
            countDownTimer.cancel()
            navigateToMainActivity()
        } else {
            LogUtils.d(TAG, "APP AD load cached image => $cachedImagePath")
            // 加载广告图片
            Glide.with(this@AdActivity)
                .load(cachedImagePath)
                .into(binding.adImg)

            // 绑定广告按钮事件
            cachedUrls.forEachIndexed { index, url ->
                if (!TextUtils.isEmpty(url)) {
                    val button = when (index) {
                        0 -> binding.button1Area // XML中button1_area转为驼峰命名
                        1 -> binding.button2Area
                        else -> binding.button3Area
                    }
                    setupAdButton(button, url)
                }
            }

            // 启动计时器
            startOrResumeTimer()
        }
    }

    private fun setupAdButton(buttonArea: View, url: String) {
        buttonArea.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    ToastUtils.showShort("未找到浏览器应用")
                }
            } catch (e: Exception) {
                ToastUtils.showShort("打开链接失败: ${e.message}")
            } finally {
                countDownTimer.cancel()
                isTimerRunning = false
            }
        }
    }

    /**
     * 跳转到主页面
     */
    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer.cancel()
        isTimerRunning = false
    }

    companion object {
        const val TAG = "AdActivity"
    }
}