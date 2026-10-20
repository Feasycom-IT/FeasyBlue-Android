package com.feasycom.feasyblue.fragment


import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.LinearLayout
import androidx.core.content.FileProvider
import com.blankj.utilcode.util.LogUtils
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.FragmentStoreBinding
import com.feasycom.feasyblue.permission.PermissionDescription
import com.feasycom.feasyblue.permission.PermissionInterceptor
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class StoreFragment : BaseFragment<FragmentStoreBinding>(), View.OnClickListener {

    private var mUploadMessage: ValueCallback<Uri>? = null
    private var mUploadCallbackAboveL: ValueCallback<Array<Uri>>? = null
    private var imageUri: Uri? = null // 图片地址

    /**
     * 实现基类抽象方法，创建 ViewBinding 实例
     */
    override fun createViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentStoreBinding {
        return FragmentStoreBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        binding.toolbarTitle.text = getString(R.string.product)
        initWebView()
        binding.toolbarBack.setOnClickListener(this)
        binding.toolbarRefresh.setOnClickListener(this)
        binding.toolbarBack.visibility = View.VISIBLE
    }

    private fun initWebView() {

        // 启用硬件加速，提升视频/网页性能
        binding.myWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        binding.myWebView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                LogUtils.e(TAG, "onPageStarted url => $url")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                LogUtils.e(TAG, "onPageFinished url => $url")
                safeUiAction {
                    binding.progressBar.progress = 100
                }
            }

            // 关键修复：拦截跳转，让所有链接都在当前 WebView 打开
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url == null || view == null) return true

                // 只处理 http / https，其他协议（如抖音app跳转、电话、邮件）交给系统
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    view.loadUrl(url)
                    return true
                }

                // 非网页协议（douyin://、tel://、mailto:// 等），跳系统打开
                return try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    requireContext().startActivity(intent)
                    true
                } catch (e: Exception) {
                    // 没有安装对应APP，忽略
                    true
                }
            }
        }

        binding.myWebView.webChromeClient = object : WebChromeClient() {
            // 相机、相册
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                mUploadCallbackAboveL = filePathCallback
                requestPermission()
                return true
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                LogUtils.d(
                    TAG,
                    "APP STORE onConsoleMessage => ${consoleMessage?.messageLevel()}  ${consoleMessage?.message()}"
                )
                return super.onConsoleMessage(consoleMessage)
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request?.grant(request.resources)
                }
            }

            override fun onJsAlert(
                view: WebView?, url: String?, message: String?, result: JsResult?
            ): Boolean {
                result?.confirm()
                return true
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                safeUiAction {
                    try {
                        binding.progressBar.progress = newProgress
                        binding.progressBar.visibility =
                            if (newProgress == 100) View.GONE else View.VISIBLE
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        with(binding.myWebView.settings) {
            // 基础支持
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            allowFileAccess = true

            // 关键修复：允许混合内容（https 加载 http 资源）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            // 关键修复：支持视频/音频/图片
            mediaPlaybackRequiresUserGesture = false
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false

            // 关键修复：DOM & 存储（大型网站必须开）
            domStorageEnabled = true
            databaseEnabled = true
            allowContentAccess = true

            // 关键修复：适配手机屏幕
            useWideViewPort = true
            loadWithOverviewMode = true

            // 关键修复：修改 UA，伪装成浏览器，避免被拦截
            //userAgentString = userAgentString.replace("; wv", "")

            // 缓存模式改为正常模式
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        binding.myWebView.loadUrl(mUrl)
    }

    /**
     * 调用相机/相册选择窗
     */
    private fun takePhoto() {
        val fileName = "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(Date())}.jpg"
        val imagePath =
            File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName)

        imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                requireContext(), "${requireContext().packageName}.fileprovider", imagePath
            )
        } else {
            Uri.fromFile(imagePath)
        }
        // 相册、相机选择窗
        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)

        val photoPickerIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val chooserIntent = Intent.createChooser(photoPickerIntent, "选择上传方式")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(captureIntent))
        startActivityForResult(chooserIntent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            chooseAbove(resultCode, data)
        } else {
            chooseBelow(resultCode, data)
        }
    }

    /**
     * Android API < 21(Android 5.0)版本的回调处理
     */
    private fun chooseBelow(resultCode: Int, data: Intent?) {
        if (Activity.RESULT_OK == resultCode) {
            updatePhotos()

            if (data != null) {
                // 这里是针对文件路径处理
                val uri = data.data
                if (uri != null) {
                    LogUtils.d(TAG, "APP STORE chooseBelow 系统里取到的图片 => $uri")
                    mUploadMessage?.onReceiveValue(uri)
                } else {
                    mUploadMessage?.onReceiveValue(null)
                }
            } else {
                // 以指定图像存储路径的方式调超相机，成功后返回data为空
                LogUtils.d(TAG, "APP STORE chooseBelow 自已命名的图片 => ${imageUri.toString()}")
                mUploadMessage?.onReceiveValue(imageUri)
            }
        } else {
            mUploadMessage?.onReceiveValue(null)
        }
        mUploadMessage = null
    }

    /**
     * Android API >= 21(Android 5.0) 版本的回调处理
     */
    private fun chooseAbove(resultCode: Int, data: Intent?) {
        LogUtils.d(TAG, "APP STORE chooseAbove 调用方法 chooseAbove => $data")
        if (Activity.RESULT_OK == resultCode) {
            updatePhotos()

            if (data != null) {
                // 这里是针对从文件中选图片的处理
                val results: Array<Uri>?
                val uriData: Uri? = data.data
                results = if (uriData != null) {
                    arrayOf(uriData)
                } else {
                    null
                }

                results?.let {
                    for (uri in it) {
                        LogUtils.d(TAG, "APP STORE chooseAbove 系统里取到的图片 => $uri")
                    }
                    mUploadCallbackAboveL?.onReceiveValue(it)
                } ?: run {
                    mUploadCallbackAboveL?.onReceiveValue(null)
                }
            } else {
                LogUtils.d(TAG, "APP STORE chooseAbove 自己命名的图片 => ${imageUri.toString()}")
                mUploadCallbackAboveL?.onReceiveValue(arrayOf(imageUri!!))
            }
        } else {
            mUploadCallbackAboveL?.onReceiveValue(null)
        }
        mUploadCallbackAboveL = null
    }

    // 发送广播进行更新相册
    private fun updatePhotos() {
        // 该广播即使多发（即选取照片成功时也发送）也没有关系，只是唤醒系统刷新媒体文件
        val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        intent.data = imageUri
        requireContext().sendBroadcast(intent)
    }

    private fun requestPermission() {


        XXPermissions.with(this)
            .permission(PermissionLists.getCameraPermission())
            .permission(PermissionLists.getRecordAudioPermission())
            .interceptor(PermissionInterceptor())
            .description(PermissionDescription())
            .request(OnPermissionCallback { grantedList, deniedList ->
                val allGranted = deniedList.isEmpty()
                if (!allGranted) {
                    return@OnPermissionCallback
                }
                takePhoto()
            })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.toolbarBack -> {
                if (binding.myWebView.url?.indexOf("https://www.douyin.com/home") != -1) {
                    binding.myWebView.loadUrl(mUrl)
                    binding.myWebView.clearHistory()
                }else if (binding.myWebView.url?.indexOf("https://www.feasycom.cn/bluetooth-modules") != -1){

                } else if (binding.myWebView.canGoBack()){
                    binding.myWebView.goBack()
                }

            }

            R.id.toolbarRefresh -> {
                binding.myWebView.reload()
            }
        }
    }

    companion object {
        const val TAG = "StoreFragment"
        const val mUrl = "https://www.feasycom.cn/bluetooth-modules"
        fun newInstance(): StoreFragment {
            return StoreFragment()
        }
    }
}