package com.feasycom.feasyblue.dialog

import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.DfuNameFragmentBinding

/**
 * DFU 命名弹窗（ViewBinding 版本）
 */
class DfuNameDialogFragment : DialogFragment() {

    var onClickComplete: ((dialog: Dialog, name: String) -> Unit)? = null

    // 声明 ViewBinding 实例，使用 ? 避免内存泄漏
    private var _binding: DfuNameFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 初始化 ViewBinding 加载布局
        _binding = DfuNameFragmentBinding.inflate(inflater, container, false)

        // 弹窗样式配置（保留原有逻辑）
        dialog?.let {
            it.window?.setBackgroundDrawable(ColorDrawable(0))
            it.requestWindowFeature(Window.FEATURE_NO_TITLE)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.let { d ->
            d.setCancelable(false)
            d.setCanceledOnTouchOutside(false)

            // 确认按钮点击事件
            binding.ok.setOnClickListener {
                onClickComplete?.invoke(d, binding.dfuName.text.toString())
            }

            // 关闭按钮点击事件
            binding.close.setOnClickListener {
                d.dismiss()
            }
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
}