package com.feasycom.feasyblue.dialog

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.UpgradeCompleteDialogBinding

/**
 * 升级完成弹窗（ViewBinding 版本）
 */
class UpgradeCompleteDialogFragment : DialogFragment() {

    private var onAgree: (() -> Unit)? = null

    // 声明 ViewBinding 实例，避免内存泄漏
    private var _binding: UpgradeCompleteDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 初始化 ViewBinding 加载布局
        _binding = UpgradeCompleteDialogBinding.inflate(inflater, container, false)

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

        // 链接文本配置（保留原有逻辑）
        with(binding.contentTextView) {
            movementMethod = LinkMovementMethod.getInstance()
            highlightColor = ContextCompat.getColor(requireContext(), R.color.transparent)
        }

        // 同意按钮点击事件
        binding.agreeCompleteTextView.setOnClickListener {
            onAgree?.invoke()
        }
    }

    override fun onResume() {
        super.onResume()
        // 设置弹窗宽高（保留原有逻辑）
        val params: ViewGroup.LayoutParams = dialog?.window!!.attributes
        params.width = WindowManager.LayoutParams.MATCH_PARENT
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
        const val TAG = "UpgradeCompleteDialogFragment"
    }
}