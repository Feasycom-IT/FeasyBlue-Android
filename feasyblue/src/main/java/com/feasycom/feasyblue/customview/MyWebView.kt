package com.feasycom.feasyblue.customview

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.webkit.*
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import com.blankj.utilcode.util.LogUtils
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.StoreViewBinding

class MyWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(
    context,
    attrs,
    defStyleAttr
) {
    // 声明 ViewBinding 对象
    private lateinit var binding: StoreViewBinding

    companion object {
        const val TAG = "MyWebView"
        const val CAMERA_PERMISSION_REQUEST_CODE = 1
        const val FILE_CHOOSER_RESULT_CODE = 2
    }

    var onUrlChangeListener: ((b: Boolean) -> Unit)? = null
    private lateinit var mUrl: String

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.MyWebView)
        a.getString(R.styleable.MyWebView_url)?.let {
            mUrl = it
        }
        a.recycle()

        // 初始化 ViewBinding
        initView(context)

        if (::mUrl.isInitialized) {
            initWebView()
        }
    }

    private fun initView(context: Context) {
        // 使用 ViewBinding 替代 LayoutInflater.inflate
        binding = StoreViewBinding.inflate(LayoutInflater.from(context), this, true)
    }

    private fun initWebView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // 通过 binding 访问 webView
            binding.webView.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    onUrlChangeListener?.let {
                        try {
                            if (url != mUrl) {
                                it.invoke(false)
                            } else {
                                it.invoke(true)
                            }
                        } catch (e: IllegalStateException) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            binding.webView.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    LogUtils.d(TAG,"APP WEB onConsoleMessage => ${consoleMessage?.messageLevel()}  ${consoleMessage?.message()}")
                    return super.onConsoleMessage(consoleMessage)
                }

                override fun onPermissionRequest(request: PermissionRequest?) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        for (str in request?.resources!!){
                            LogUtils.d(TAG,"APP WEB onPermissionRequest str => $str")
                        }
                        request?.grant(request.resources)
                    }
                }

                override fun onJsAlert(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    result: JsResult?
                ): Boolean {
                    result?.confirm()
                    return true
                }

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    try {
                        // 通过 binding 访问 progressBar
                        binding.progressBar.progress = newProgress
                        if (newProgress == 100) {
                            binding.progressBar.visibility = GONE
                        } else {
                            if (binding.progressBar.visibility == GONE) {
                                binding.progressBar.visibility = VISIBLE
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else {
            // 低版本隐藏进度条
            binding.progressBar.visibility = GONE
        }

        // 配置 WebView 设置
        with(binding.webView.settings) {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            allowFileAccess = true // 设置允许访问文件数据
            setSupportZoom(true)
            builtInZoomControls = true
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            domStorageEnabled = true
            databaseEnabled = true
        }

        // 加载 URL
        binding.webView.loadUrl(mUrl)
    }

    // 回退方法
    fun goBack() {
        binding.webView.goBack()
    }

    // 刷新方法
    fun reload() {
        binding.webView.reload()
    }

    // 设置 URL 并重新初始化
    fun setUrl(url: String) {
        mUrl = url
        initWebView()
    }
}