package com.feasycom.feasyblue.activity

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.blankj.utilcode.util.RegexUtils
import com.blankj.utilcode.util.ToastUtils
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivityFeedbackBinding
import com.feasycom.feasyblue.dialog.InfoDialog
import com.feasycom.feasyblue.dialog.MessageDialog
import com.feasycom.feasyblue.dialog.WFBaseDialog
import com.feasycom.feasyblue.utils.FeasyCallbackSDK
import com.feasycom.feasyblue.utils.FileShareHelper
import com.feasycom.feasyblue.utils.getStr
import com.feasycom.feasyblue.utils.putStr
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class FeedbackActivity : BaseActivity<ActivityFeedbackBinding>() {

    // 对话框相关
    private var connectDialog: InfoDialog? = null
    private var mMessageDialog: WFBaseDialog? = null
    private var mResultDialog: AlertDialog? = null

    // 网络监听相关
    private lateinit var connectivityManager: ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            handleNetworkState(true)
        }

        override fun onLost(network: Network) {
            handleNetworkState(false)
        }
    }

    // 其他变量
    private val handler = Handler(Looper.getMainLooper())
    private var mPreferenceKey = "FEEDBACK_ADVICE"
    var feedType = 0

    /**
     * 实现父类的 ViewBinding 获取方法（核心适配点）
     */
    override fun getViewBinding(): ActivityFeedbackBinding {
        return ActivityFeedbackBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // 初始化Toolbar
        initToolbar()

        // 初始化网络监听
        initNetworkListener()

        // 初始化反馈内容
        initFeedbackContent()

        // 初始化事件监听
        initEvent()
    }

    /**
     * 初始化Toolbar
     */
    private fun initToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, android.R.color.black))
        binding.toolbarTitle.text = getString(R.string.feedback_title)
    }

    /**
     * 初始化网络监听
     */
    private fun initNetworkListener() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        registerNetworkCallback()
    }

    /**
     * 初始化反馈内容
     */
    private fun initFeedbackContent() {
        // 设置保存的反馈内容
        binding.deviceData.setText(getStr(mPreferenceKey))
        // 将光标定位到文本末尾
        binding.deviceData.setSelection(binding.deviceData.text.toString().length)
    }

    /**
     * 初始化所有事件监听
     */
    private fun initEvent() {
        // 返回按钮事件
        binding.toolbar.setNavigationOnClickListener { finish() }

        // 提交按钮事件
        binding.submit.setOnClickListener {
            validateAndSubmitFeedback()
        }

        // 反馈类型选择事件
        initFeedbackTypeSelection()

        // 日志导出按钮事件
        initLogButtonEvents()

        // 文本变化监听
        binding.deviceData.addTextChangedListener {
            putStr(mPreferenceKey, it.toString())
        }
    }

    /**
     * 初始化反馈类型选择事件
     */
    private fun initFeedbackTypeSelection() {
        // 建议
        binding.proposal.setOnClickListener {
            feedType = 0
            mPreferenceKey = "FEEDBACK_ADVICE"
            resetFeedbackTypeSelection()
            binding.proposal.isChecked = true
        }

        // 功能异常
        binding.abnormalFunction.setOnClickListener {
            feedType = 1
            mPreferenceKey = "FEEDBACK_BUG"
            resetFeedbackTypeSelection()
            binding.abnormalFunction.isChecked = true
        }

        // 界面异常
        binding.interfaceAbnormality.setOnClickListener {
            feedType = 2
            mPreferenceKey = "FEEDBACK_UI"
            resetFeedbackTypeSelection()
            binding.interfaceAbnormality.isChecked = true
        }

        // 合作
        binding.cooperation.setOnClickListener {
            feedType = 3
            mPreferenceKey = "FEEDBACK_COOPERATION"
            resetFeedbackTypeSelection()
            binding.cooperation.isChecked = true
        }
    }

    /**
     * 重置反馈类型选择状态
     */
    private fun resetFeedbackTypeSelection() {
        binding.proposal.isChecked = false
        binding.abnormalFunction.isChecked = false
        binding.interfaceAbnormality.isChecked = false
        binding.cooperation.isChecked = false
    }



    private val CLICK_INTERVAL = 800
    private var lastExportClick = 0L

    /**
     * 初始化日志按钮事件
     */
    private fun initLogButtonEvents() {
        // 导出日志（单击）
        binding.toolbarLog.setOnClickListener {
            if (System.currentTimeMillis() - lastExportClick < CLICK_INTERVAL) return@setOnClickListener
            lastExportClick = System.currentTimeMillis()
            exportLogFile()
        }

        // 删除日志（长按）
        binding.toolbarLog.setOnLongClickListener {
            showDeleteLogConfirmDialog()
            true
        }
    }

    /**
     * 验证并提交反馈
     */
    private fun validateAndSubmitFeedback() {
        val feedbackContent = binding.deviceData.text.toString().trim()
        val email = binding.editEmail.text.toString().trim()

        // 验证反馈内容
        if (feedbackContent.isBlank()) {
            ToastUtils.showLong(R.string.feedback_data_error)
            return
        }

        // 验证邮箱
        if (email.isBlank()) {
            ToastUtils.showLong(R.string.feedback_email)
            return
        }

        // 验证邮箱格式
        if (!RegexUtils.isEmail(email)) {
            ToastUtils.showLong(R.string.feedback_email_erro)
            return
        }

        // 显示提交中对话框
        showSubmittingDialog()

        // 设置回调
        FeasyCallbackSDK.complete = { isSuccess ->
            runOnUiThread {
                MainScope().launch {
                    // 更新对话框提示
                    connectDialog?.info = getString(
                        if (isSuccess) R.string.feedback_success else R.string.feedback_failure
                    )
                    delay(1000)
                    // 关闭对话框
                    connectDialog?.dismiss()
                    // 清空输入框
                    putStr(mPreferenceKey, "")
                    binding.deviceData.setText("")
                    binding.editEmail.setText("")
                }
            }
        }

        // 提交反馈
        FeasyCallbackSDK.feedback(this, feedbackContent, feedType, email)
    }

    /**
     * 显示提交中对话框
     */
    private fun showSubmittingDialog() {
        connectDialog = InfoDialog(this, getString(R.string.feedback_status))
        connectDialog?.show()

        // 设置超时关闭
        handler.postDelayed({
            connectDialog?.takeIf { it.isShowing }?.dismiss()
        }, 10000)
    }

    /**
     * 导出日志文件
     */
    private fun exportLogFile() {
        val directory: File = getExternalFilesDir("Logcat") ?: run {
            Toast.makeText(this, getString(R.string.no_logcat_file), Toast.LENGTH_SHORT).show()
            return
        }

        if (!directory.isDirectory) {
            directory.delete()
        }

        if (!directory.exists()) {
            Toast.makeText(this, getString(R.string.no_logcat_file), Toast.LENGTH_SHORT).show()
            return
        }

        val logcatFile = File(directory, "Logcat.txt")
        if (!logcatFile.exists()) {
            Toast.makeText(this, getString(R.string.no_log), Toast.LENGTH_SHORT).show()
            return
        }

        // 分享日志文件
        val logcatFileUri = FileShareHelper.getFileUri(this, logcatFile)
        FileShareHelper.shareFile(this, logcatFileUri, "text/plain")
    }

    /**
     * 显示删除日志确认对话框
     */
    private fun showDeleteLogConfirmDialog() {
        mMessageDialog = MessageDialog.Builder(this@FeedbackActivity)
            .setMessage(getString(R.string.is_delete_all_log))
            .setConfirm(getString(R.string.ok))
            .setCancel(getString(R.string.cancel))
            .setListener(object : MessageDialog.OnListener {
                override fun onConfirm(dialog: WFBaseDialog?) {
                    deleteAllLogs()
                }

                override fun onCancel(dialog: WFBaseDialog?) {}
            }).show()
    }

    /**
     * 删除所有日志文件
     */
    private fun deleteAllLogs() {
        val directory: File = getExternalFilesDir("Logcat") ?: return

        if (!directory.isDirectory) {
            directory.delete()
        }

        if (directory.exists()) {
            deleteFolder(directory)
        }

        // 重新创建空目录
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }

    /**
     * 递归删除文件夹
     */
    private fun deleteFolder(folder: File): Boolean {
        if (folder.isFile) {
            return folder.delete()
        }

        val files = folder.listFiles() ?: return false
        for (file in files) {
            if (file.isDirectory) {
                deleteFolder(file)
            } else {
                file.delete()
            }
        }
        return folder.delete()
    }

    /**
     * 注册网络监听回调
     */
    private fun registerNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        }
    }

    /**
     * 处理网络状态变化
     */
    private fun handleNetworkState(isConnected: Boolean) {
        runOnUiThread {
            if (!isConnected) {
                // 关闭之前的对话框
                mResultDialog?.dismiss()

                // 显示无网络提示
                mResultDialog = AlertDialog.Builder(this@FeedbackActivity)
                    .setMessage(getString(R.string.no_network))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                mResultDialog?.setCanceledOnTouchOutside(false)
            } else {
                // 有网络时关闭提示框
                mResultDialog?.dismiss()
            }
        }
    }

    /**
     * 页面销毁时清理资源
     */
    override fun onDestroy() {
        super.onDestroy()

        // 取消所有Handler回调
        handler.removeCallbacksAndMessages(null)

        // 注销网络监听
        unregisterNetworkCallback()

        // 关闭所有对话框
        connectDialog?.dismiss()
        mMessageDialog?.dismiss()
        mResultDialog?.dismiss()

        // 清空引用
        connectDialog = null
        mMessageDialog = null
        mResultDialog = null
    }

    /**
     * 注销网络监听回调
     */
    private fun unregisterNetworkCallback() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val TAG: String = "FeedbackActivity"

        /**
         * 启动反馈页面
         */
        fun activityStart(context: Context) {
            val intent = Intent(context, FeedbackActivity::class.java)
            context.startActivity(intent)
        }
    }
}