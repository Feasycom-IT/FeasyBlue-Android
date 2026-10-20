package com.feasycom.feasyblue.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.feasycom.feasyblue.databinding.DialogLoadBinding

class LoadDialogFragment : DialogFragment {

    private lateinit var mBinding: DialogLoadBinding
    var mMessage: String = ""

    // 必须添加的无参构造函数
    constructor() : super()

    // 保留原有的带参构造函数，但改为内部使用
    private constructor(message: String) : this() {
        mMessage = message
    }

    companion object {
        // 推荐使用 newInstance 静态方法来创建 Fragment
        @JvmStatic
        fun newInstance(message: String): LoadDialogFragment {
            val fragment = LoadDialogFragment()
            val args = Bundle()
            args.putString("message", message)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 从 Arguments 中获取参数
        arguments?.getString("message")?.let {
            mMessage = it
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog?.setCancelable(false);
        dialog?.setCanceledOnTouchOutside(false)
        mBinding = DialogLoadBinding.inflate(inflater)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 在这里设置文本内容
        mBinding.messageTv.text = mMessage
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.setCanceledOnTouchOutside(false)
    }

    // 添加一个setter方法，以便在需要时动态更新消息
    fun setMessage(message: String) {
        mMessage = message
        if (::mBinding.isInitialized) {
            mBinding.messageTv.text = message
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (manager.isDestroyed) return
        try {
            // 在每个add事务前增加一个remove事务，防止连续的add
            manager.beginTransaction().remove(this).commit()
            super.show(manager, tag)
        } catch (e: Exception) {
            // 同一实例使用不同的tag会异常，这里捕获一下
            e.printStackTrace()
        }
    }
}