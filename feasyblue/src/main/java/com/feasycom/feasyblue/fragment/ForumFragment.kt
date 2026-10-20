package com.feasycom.feasyblue.fragment

import android.graphics.Bitmap
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.LinearLayout
import com.blankj.utilcode.util.LogUtils
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.FragmentForumBinding

class ForumFragment : BaseFragment<FragmentForumBinding>(), View.OnClickListener {

    /**
     * 实现基类抽象方法，创建 ViewBinding 实例
     */
    override fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentForumBinding {
        return FragmentForumBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        binding.toolbarTitle.text = getString(R.string.forum)
        initWebView()
        binding.toolbarBack.setOnClickListener(this)
        binding.toolbarRefresh.setOnClickListener(this)
    }

    private fun initWebView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            binding.myWebView.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    LogUtils.d(TAG, "initWebView url => $url")
                    try {
                        if (url != mUrl) {
                            binding.toolbarBack?.visibility = View.VISIBLE
                        } else {
                            binding.toolbarBack?.visibility = View.GONE
                        }
                    } catch (e: IllegalStateException) {
                        e.printStackTrace()
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        CookieManager.getInstance().flush()
                    }
                }
            }

            binding.myWebView.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    LogUtils.d(StoreFragment.TAG, "APP STORE onConsoleMessage => ${consoleMessage?.messageLevel()}  ${consoleMessage?.message()}")
                    return super.onConsoleMessage(consoleMessage)
                }

                override fun onPermissionRequest(request: PermissionRequest?) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        for (str in request?.resources!!) {
                            LogUtils.d(StoreFragment.TAG, "APP STORE onPermissionRequest str => $str")
                        }
                        request?.grant(request.resources)
                    }
                }

                override fun onJsAlert(
                    view: WebView?, url: String?, message: String?, result: JsResult?
                ): Boolean {
                    LogUtils.d(StoreFragment.TAG, "APP STORE onJsAlert message => $message")
                    result?.confirm()
                    return true
                }

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    try {
                        binding.progressBar.progress = newProgress
                        if (newProgress == 100) {
                            binding.progressBar?.visibility = LinearLayout.GONE
                        } else {
                            if (binding.progressBar?.visibility == LinearLayout.GONE) {
                                binding.progressBar?.visibility = LinearLayout.VISIBLE
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else {
            binding.progressBar?.visibility = View.GONE
        }

        with(binding.myWebView.settings) {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            allowFileAccess = true // 设置允许访问文件数据
            setSupportZoom(true)
            builtInZoomControls = true
            cacheMode = WebSettings.LOAD_DEFAULT
            domStorageEnabled = true
            databaseEnabled = true

            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            loadsImagesAutomatically = true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(binding.myWebView, true)

        binding.myWebView.loadUrl(mUrl)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.toolbarBack -> {
                binding.myWebView.goBack()
            }
            R.id.toolbarRefresh -> {
                binding.myWebView.reload()
            }
        }
    }

    companion object {
        const val TAG = "ForumFragment"
        const val mUrl = "https://forum.feasycom.com/"
        fun newInstance(): ForumFragment {
            return ForumFragment()
        }
    }
}