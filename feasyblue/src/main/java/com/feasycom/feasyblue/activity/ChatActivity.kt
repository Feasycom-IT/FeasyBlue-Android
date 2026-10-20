package com.feasycom.feasyblue.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.webkit.*
import androidx.core.content.FileProvider
import com.blankj.utilcode.util.LogUtils
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivityChatBinding
import com.feasycom.feasyblue.permission.PermissionDescription
import com.feasycom.feasyblue.permission.PermissionInterceptor
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import com.hjq.permissions.permission.base.IPermission
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : BaseActivity<ActivityChatBinding>(), View.OnClickListener {

    // 聊天网页地址
    private val chatUrl = "https://affimvip.baidu.com/cps5/chat?siteToken=93839bd68be05c3e05dad4b8b5c8c7ed&siteId=19886600&reqParam=%7B%22from%22%3A0,%22siteId%22%3A%2219886600%22,%22tid%22%3A%22-1%22,%22userId%22%3A%2229388571%22,%22ttype%22%3A1,%22siteConfig%22%3A%7B%22eid%22%3A%2229388571%22,%22isCsOnline%22%3A1,%22siteToken%22%3A%2293839bd68be05c3e05dad4b8b5c8c7ed%22,%22userId%22%3A%2229388571%22,%22isGray%22%3A%22false%22,%22vstProto%22%3A1,%22likeVersion%22%3A%22generic%22,%22siteId%22%3A%2219886600%22,%22online%22%3A%22true%22,%22startTime%22%3A1753845574362,%22webRoot%22%3A%22https%3A%2F%2Faffimvip.baidu.com%2Fcps5%2F%22,%22bid%22%3A%22175384557518671349%22,%22isSmallFlow%22%3A0,%22isPreonline%22%3A0,%22userCommonSwitch%22%3A%7B%22isOpenTelCall%22%3Afalse,%22isOpenCallDialog%22%3Afalse,%22isOpenStateOfRead%22%3Atrue,%22isOpenVisitorLeavingClue%22%3Atrue,%22isOpenVoiceMsg%22%3Atrue,%22isOpenVoiceCom%22%3Atrue%7D,%22invited%22%3A0%7D,%22config%22%3A%7B%7D%7D&hotIssue&hotIssueid&ctype=1&reqCluster=CCE"

    // 文件选择回调
    private var mUploadMessage: ValueCallback<Uri>? = null
    private var mUploadCallbackAboveL: ValueCallback<Array<Uri>>? = null

    // 图片URI
    private var imageUri: Uri? = null

    /**
     * 实现父类的 ViewBinding 获取方法
     */
    override fun getViewBinding(): ActivityChatBinding {
        return ActivityChatBinding.inflate(layoutInflater)
    }

    /**
     * 初始化View（父类抽象方法）
     */
    override fun initView() {
        // 设置返回按钮点击事件
        binding.toolbarBack.setOnClickListener(this)

        // 初始化WebView
        initWebViewConfig()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    /**
     * 初始化WebView配置
     */
    private fun initWebViewConfig() {
        // WebViewClient配置
        val webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }
        }
        binding.chatWeb.webViewClient = webViewClient

        // WebChromeClient配置
        binding.chatWeb.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                mUploadCallbackAboveL = filePathCallback
                requestCameraAndRecordPermissions()
                return true
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                LogUtils.d(TAG, "APP STORE onConsoleMessage => ${consoleMessage?.messageLevel()}  ${consoleMessage?.message()}")
                return super.onConsoleMessage(consoleMessage)
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && request != null) {
                    request.grant(request.resources)
                }
            }

            override fun onJsAlert(
                view: WebView?, url: String?, message: String?, result: JsResult?
            ): Boolean {
                LogUtils.d(TAG, "APP STORE onJsAlert message => $message")
                result?.confirm()
                return true
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
            }
        }

        // WebSettings配置
        val webSettings = binding.chatWeb.settings
        webSettings.javaScriptEnabled = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.allowFileAccess = true
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        // 加载网页
        binding.chatWeb.loadUrl(chatUrl)
        binding.chatWeb.setInitialScale(100)
    }

    /**
     * 调用相机/相册选择界面
     */
    private fun openImageChooser() {
        try {
            val fileName = "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(Date())}.jpg"
            val imageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return
            val imagePath = File(imageDir, fileName)

            imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(this, "${packageName}.fileprovider", imagePath)
            } else {
                Uri.fromFile(imagePath)
            }

            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            captureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val photoPickerIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

            val chooserIntent = Intent.createChooser(photoPickerIntent, "选择上传方式")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(captureIntent))

            startActivityForResult(chooserIntent, 1001)
        } catch (e: Exception) {
            LogUtils.e(TAG, "调用相机/相册失败: ${e.message}", e)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001) {
            handleImageChooserResult(resultCode, data)
        }
    }

    private fun handleImageChooserResult(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            mUploadMessage?.onReceiveValue(null)
            mUploadCallbackAboveL?.onReceiveValue(null)
            mUploadMessage = null
            mUploadCallbackAboveL = null
            return
        }

        updateGallery()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val uris = when {
                    data?.data != null -> arrayOf(data.data!!)
                    imageUri != null -> arrayOf(imageUri!!)
                    else -> null
                }
                mUploadCallbackAboveL?.onReceiveValue(uris)
                mUploadCallbackAboveL = null
            } else {
                val uri = data?.data ?: imageUri
                mUploadMessage?.onReceiveValue(uri)
                mUploadMessage = null
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "处理图片选择结果失败: ${e.message}", e)
            mUploadMessage?.onReceiveValue(null)
            mUploadCallbackAboveL?.onReceiveValue(null)
            mUploadMessage = null
            mUploadCallbackAboveL = null
        }
    }

    private fun updateGallery() {
        try {
            imageUri?.let { uri ->
                val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                intent.data = uri
                sendBroadcast(intent)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "更新相册失败: ${e.message}", e)
        }
    }

    /**
     * 核心修复：适配XXPermissions的onResult抽象方法
     */
    private fun requestCameraAndRecordPermissions() {
        XXPermissions.with(this)
            .permission(PermissionLists.getRecordAudioPermission())
            .permission(PermissionLists.getCameraPermission())
            .interceptor(PermissionInterceptor())
            .description(PermissionDescription())
            // 修复：实现OnPermissionCallback接口的抽象方法onResult
            .request(object : OnPermissionCallback {
                // 必须实现的抽象方法（解决核心报错）
                override fun onResult(grantedList: MutableList<IPermission>, deniedList: MutableList<IPermission>) {
                    // 判断是否所有权限都授予
                    val allGranted = deniedList.isEmpty()
                    if (allGranted) {
                        // 权限全部授予，打开图片选择器
                        openImageChooser()
                    } else {
                        // 权限被拒绝，清空WebView回调避免卡死
                        LogUtils.e(TAG, "权限被拒绝: requestCameraAndRecordPermissions")
                        mUploadMessage?.onReceiveValue(null)
                        mUploadCallbackAboveL?.onReceiveValue(null)
                    }
                }
            })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.toolbarBack -> finish()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.chatWeb.onPause()
        binding.chatWeb.pauseTimers()
    }

    override fun onResume() {
        super.onResume()
        binding.chatWeb.onResume()
        binding.chatWeb.resumeTimers()
    }

    override fun onDestroy() {
        // 修复WebView置空问题：使用空实例而非null
        binding.chatWeb.stopLoading()
        binding.chatWeb.webViewClient = WebViewClient()
        binding.chatWeb.webChromeClient = WebChromeClient()
        binding.chatWeb.settings.javaScriptEnabled = false
        binding.chatWeb.removeAllViews()
        binding.chatWeb.destroy()

        mUploadMessage = null
        mUploadCallbackAboveL = null

        super.onDestroy()
    }

    companion object {
        private const val TAG: String = "ChatActivity"

        fun activityStart(context: Context) {
            val intent = Intent(context, ChatActivity::class.java)
            context.startActivity(intent)
        }
    }
}