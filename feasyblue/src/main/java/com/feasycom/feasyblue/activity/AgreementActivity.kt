package com.feasycom.feasyblue.activity

import android.os.Build
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.LogUtils
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivityAgreementBinding
import com.feasycom.network.DeviceNetwork
import com.feasycom.network.bean.ProtocolParams
import com.feasycom.network.bean.ProtocolResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 协议展示页面（用户协议/隐私协议）
 * 适配 ViewBinding + 优化后的 BaseActivity
 */
class AgreementActivity : BaseActivity<ActivityAgreementBinding>() {
    private var type: Int = 0

    // 协议地址常量
    private val USER_AGREEMENT_CN =
        "https://document.feasycom.com/file/agreement/blue/userAgreement.html"
    private val USER_AGREEMENT_EN =
        "https://document.feasycom.com/file/agreement/blue/userAgreement_en.html"
    private val PRIVACY_AGREEMENT_CN =
        "https://document.feasycom.com/file/agreement/blue/privacyAgreement.html"
    private val PRIVACY_AGREEMENT_EN =
        "https://document.feasycom.com/file/agreement/blue/privacyAgreement_en.html"

    /**
     * 实现父类的 ViewBinding 获取方法
     */
    override fun getViewBinding(): ActivityAgreementBinding {
        return ActivityAgreementBinding.inflate(layoutInflater)
    }

    /**
     * 初始化视图
     */
    @RequiresApi(Build.VERSION_CODES.N)
    override fun initView() {
        // 设置Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }

        // 获取协议类型并设置标题
        type = intent.getIntExtra("type", 0)
        binding.toolbarTitle.text = when (type) {
            1 -> getString(R.string.userAgreement)
            else -> getString(R.string.privacyAgreement)
        }

        // 设置返回按钮事件
        binding.toolbar.setNavigationOnClickListener { finish() }

        // 请求协议内容
        request(type)
    }

    /**
     * 根据类型和语言加载对应协议
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private fun request(type: Int) {
        if (type == 1) {
            // 用户协议
            val localeLanguage = getLocaleLanguage()
            if (localeLanguage == "cn") {
                configureWebView(USER_AGREEMENT_CN)
            } else {
                configureWebView(USER_AGREEMENT_EN)
            }
        } else {
            // 隐私协议
            val localeLanguage = getLocaleLanguage()
            if (localeLanguage == "cn") {
                configureWebView(PRIVACY_AGREEMENT_CN)
            } else {
                configureWebView(PRIVACY_AGREEMENT_EN)
            }
        }

//        // TODO 隐私协议开发者：深圳市飞易通科技有限公司
//         val protocolParams = ProtocolParams("blue", type )
//        // TODO 隐私协议开发者：长沙市飞易通科技有限公司
////        val protocolParams = ProtocolParams("blue", type,"changsha")
//
//        // 获取当前系统语言，中文用 "cn"，其他用 "en"
//        val languageCode = if (resources.configuration.locales[0].language.endsWith("zh")) "cn" else "en"
//
//        lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                var protocol = getProtocolWithFallback(protocolParams, languageCode)
//                if (protocol?.code == 200) {
//                    displayProtocol(protocol.data.url)
//                } else {
//                    showErrorDialog()
//                }
//            } catch (e: Exception) {
//                showErrorDialog()
//            }
//        }
    }

    /**
     * 获取当前系统语言（兼容高低版本）
     * @return cn / en
     */
    private fun getLocaleLanguage(): String {
        return try {
            if (resources.configuration.locales[0].language.endsWith("zh")) "cn" else "en"
        } catch (e: NoSuchMethodError) {
            if (resources.configuration.locale.language.endsWith("zh")) "cn" else "en"
        }
    }

    /**
     * 配置WebView并加载协议网页
     */
    private fun configureWebView(url: String) {
        // 设置进度条监听
     /*   binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                // 更新进度条的进度
                binding.progressBar.progress = newProgress
                // 根据进度条状态显示或隐藏进度条
                binding.progressBar.visibility = if (newProgress == 100) View.GONE else View.VISIBLE
            }
        }*/
        // 加载网页
        binding.myWebView.setUrl(url)
    }

    /**
     * 获取协议（失败时降级为英文）
     */
    private suspend fun getProtocolWithFallback(params: ProtocolParams, languageCode: String): ProtocolResponse? {
        return try {
            DeviceNetwork.getProtocol(params, languageCode)
        } catch (e: Exception) {
            DeviceNetwork.getProtocol(params, "en")
        }
    }

    /**
     * 显示协议内容
     */
    private fun displayProtocol(url: String) {
        // 在主线程中显示协议内容
        lifecycleScope.launch(Dispatchers.Main) {
            LogUtils.d(TAG, "APP displayProtocol url => $url")
            binding.myWebView.setUrl(url)
        }
    }

    /**
     * 显示网络错误弹窗
     */
    private fun showErrorDialog() {
        // 在主线程中显示错误对话框
        lifecycleScope.launch(Dispatchers.Main) {
            AlertDialog.Builder(this@AgreementActivity)
                .setTitle(R.string.error)
                .setMessage(getString(R.string.network_error))
                .setIcon(R.mipmap.ic_launcher)
                .setNegativeButton(R.string.close, null)
                .create()
                .show()
        }
    }

    companion object {
        const val TAG = "AgreementActivity"
    }
}