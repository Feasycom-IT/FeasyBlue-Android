package com.feasycom.feasyblue.dialog

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.activity.AgreementActivity
import com.feasycom.feasyblue.databinding.PrivacyDialogBinding

/**
 * 隐私协议弹窗（ViewBinding 版本）
 */
class FirstDialogFragment : DialogFragment() {

    var onAgree: (() -> Unit)? = null
    var onRefuse: (() -> Unit)? = null

    // 声明 ViewBinding 实例，避免内存泄漏
    private var _binding: PrivacyDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 初始化 ViewBinding 加载布局
        _binding = PrivacyDialogBinding.inflate(inflater, container, false)

        // 弹窗基础配置（保留原有逻辑）
        dialog?.setCanceledOnTouchOutside(false)
        dialog?.setOnKeyListener { _, keyCode, _ ->
            requireActivity().finish()
            keyCode == KeyEvent.KEYCODE_BACK
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 滚动条配置
        binding.scrollView.isVerticalScrollBarEnabled = true
        binding.scrollView.isScrollbarFadingEnabled = false

        // 构建用户协议可点击富文本
        val userSpan = SpannableString(getString(R.string.user_agreement)).apply {
            setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(requireContext(), AgreementActivity::class.java)
                    intent.putExtra("type", 1)
                    requireContext().startActivity(intent)
                }
            }, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            setSpan(
                ForegroundColorSpan(Color.parseColor("#D1894A")),
                0,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // 构建隐私协议可点击富文本
        val privacySpan = SpannableString(getString(R.string.privacy_agreement)).apply {
            setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(requireContext(), AgreementActivity::class.java)
                    intent.putExtra("type", 2)
                    requireContext().startActivity(intent)
                }
            }, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            setSpan(
                ForegroundColorSpan(Color.parseColor("#D1894A")),
                0,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // 拼接富文本内容
        val stringBuilder = SpannableStringBuilder(getString(R.string.userAndPrivacyAgreementPrefix)).apply {
            append("《").append(userSpan).append("》")
            append(getString(R.string.and))
            append("《").append(privacySpan).append("》")
            append(getString(R.string.userAndPrivacyAgreementSuffix))
        }

        // 设置富文本显示（保留原有逻辑）
        with(binding.contentTextView) {
            movementMethod = LinkMovementMethod.getInstance()
            text = stringBuilder
            highlightColor = ContextCompat.getColor(requireContext(), R.color.transparent)
        }

        // 注释掉的代码保留结构（如需启用可直接放开）
        // binding.confirmButton.isEnabled = false

        // binding.isCheckedCB.setOnCheckedChangeListener { _, isChecked ->
        //     if (isChecked) {
        //         binding.agreeTextView.isEnabled = true
        //         binding.agreeTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
        //     } else {
        //         binding.agreeTextView.isEnabled = false
        //         binding.agreeTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey))
        //     }
        // }

        // 同意按钮点击事件
        binding.confirmButton.setOnClickListener {
            onAgree?.invoke()
        }

        // 拒绝按钮点击事件
        binding.disagreeButton.setOnClickListener {
            onRefuse?.invoke()
        }
    }

    override fun onResume() {
        super.onResume()
        // 设置弹窗宽高（保留原有逻辑）
        val params: ViewGroup.LayoutParams = dialog?.window!!.attributes
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog!!.window!!.attributes = params as WindowManager.LayoutParams
    }

    /**
     * 生命周期结束时释放 Binding，防止内存泄漏
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "FirstDialogFragment"
    }
}