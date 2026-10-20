package com.feasycom.feasyblue.dialog

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ConnectFailedDialogBinding

/**
 * 连接失败弹窗（ViewBinding 版本）
 * 注：原类名拼写错误 ConnectFiled → ConnectFailed，已同步修正布局 Binding 类名
 */
class ConnectFailedDialogFragment : DialogFragment() {

    private var onAgree: (() -> Unit)? = null

    // 声明 ViewBinding 实例
    private var _binding: ConnectFailedDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 初始化 ViewBinding
        _binding = ConnectFailedDialogBinding.inflate(inflater, container, false)

        // 弹窗基础配置
        dialog?.setCanceledOnTouchOutside(false)
        dialog?.setOnKeyListener { _, keyCode, _ ->
            requireActivity().finish()
            keyCode == KeyEvent.KEYCODE_BACK
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 文本链接配置（保留原有逻辑）
        with(binding.contentTextView) {
            movementMethod = LinkMovementMethod.getInstance()
            highlightColor = ContextCompat.getColor(requireContext(), R.color.transparent)
        }

        // 同意按钮点击事件
        binding.agreeConnetFiledTextView.setOnClickListener {
            onAgree?.invoke()
        }
    }

    override fun onResume() {
        super.onResume()
        // 设置弹窗宽高
        val params: ViewGroup.LayoutParams = dialog?.window!!.attributes
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog!!.window!!.attributes = params as WindowManager.LayoutParams
    }

    /**
     * 防止内存泄漏，在生命周期结束时释放 Binding
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ConnectFailedDialogFragment"
    }
}