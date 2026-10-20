package com.feasycom.feasyblue.fragment

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.blankj.utilcode.util.ToastUtils
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.activity.*
import com.feasycom.feasyblue.databinding.FragmentAboutBinding
import com.feasycom.common.utils.GlobalVariable

/**
 * 关于页面 Fragment
 * 基于 ViewBinding 重构，保留所有原有业务逻辑
 */
class AboutFragment : BaseFragment<FragmentAboutBinding>(), View.OnClickListener {

    private var clickCount = 0
    private var clickTime = 0L

    /**
     * 实现基类的抽象方法，创建 ViewBinding 实例
     */
    override fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAboutBinding {
        return FragmentAboutBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        // 替换 findViewById/kotlinx 方式，改用 ViewBinding 访问控件
        binding.toolbarFeedback.setOnClickListener(this)
        binding.privacyText.setOnClickListener(this)
        binding.userText.setOnClickListener(this)
        binding.iconIV.setOnClickListener(this)
        binding.toEmail.setOnClickListener(this)
        binding.toWeb.setOnClickListener(this)
        binding.phoneCall.setOnClickListener(this)

        // 设置版本信息
        binding.appVersion.text = packageCode(requireContext())
        binding.sdkVersion.text = GlobalVariable.sdkVersion
    }

    private fun packageCode(context: Context): String? {
        val manager = context.packageManager
        var code: String? = null
        try {
            val info = manager.getPackageInfo(context.packageName, 0)
            code = info.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return code
    }

    /**
     * 格式化电话号码
     */
    private fun formatPhoneNumber(phoneNumber: String): String {
        // 移除所有非数字字符，保留 + 号
        return phoneNumber.replace("[^+0-9]".toRegex(), "")
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.to_web ->{
                try {
                    var url: String = "https://www.feasycom.cn/"
                    // 确保URL有协议前缀
                    val formattedUrl = if (!url.startsWith("http")) {
                        "https://$url"
                    } else {
                        url
                    }

                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(formattedUrl)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }

                    activity?.let {
                        if (intent.resolveActivity(it.packageManager) != null) {
                            startActivity(intent)
                        } else {
                            ToastUtils.showLong("无法打开浏览器")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            R.id.phone_call ->{
                try {
                    // 清理电话号码格式
                    val formattedNumber = formatPhoneNumber("17744976310")

                    // 创建拨号Intent
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$formattedNumber")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }

                    // 验证是否有应用能处理
                    activity?.let {
                        if (intent.resolveActivity(it.packageManager) != null) {
                            startActivity(intent)
                        } else {
                            ToastUtils.showLong("无法打开拨号应用")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    ToastUtils.showLong("拨号失败")
                }
            }

            R.id.to_email ->{
                try {
                    // 使用 ACTION_SENDTO 而非 ACTION_SEND
                    // ACTION_SENDTO 专门用于mailto:协议，通常不会弹出选择器
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        // 构建mailto URI，只填写收件人
                        data = Uri.parse("mailto:support@feasycom.com")

                        // 如果需要填写主题和正文，可以这样：
                        // data = Uri.parse("mailto:support@feasycom.com" +
                        //     "?subject=Support Request" +
                        //     "&body=Please describe your issue here")

                        // 确保不会显示应用选择器
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }

                    // 验证是否有应用能处理
                    activity?.let {
                        if (emailIntent.resolveActivity(it.packageManager) != null) {
                            startActivity(emailIntent)
                        } else {
                            // 没有邮件应用，提示用户
                            ToastUtils.showLong("未找到邮箱应用")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    ToastUtils.showLong("打开邮箱失败")
                }
            }

            R.id.toolbarFeedback -> {
                FeedbackActivity.activityStart(requireContext())
            }

            R.id.privacyText -> {
                val intent = Intent(requireContext(), AgreementActivity::class.java)
                intent.putExtra("type", 2)
                requireContext().startActivity(intent)
            }

            R.id.userText -> {
                val intent = Intent(requireContext(), AgreementActivity::class.java)
                intent.putExtra("type", 1)
                requireContext().startActivity(intent)
            }

            R.id.iconIV -> {
                if(clickTime == 0L){
                    clickTime = System.currentTimeMillis()
                }
                if(System.currentTimeMillis() - clickTime > 500){
                    clickCount = 1;
                } else {
                    clickCount ++;
                }
                clickTime = System.currentTimeMillis();
                if(clickCount == 5){
                    clickCount = 0
                    DeveloperDebugActivity.activityStart(requireContext())
                }
            }
        }
    }

    companion object {
        const val TAG: String = "AboutFragment"
        fun newInstance(): AboutFragment {
            return AboutFragment()
        }
    }
}