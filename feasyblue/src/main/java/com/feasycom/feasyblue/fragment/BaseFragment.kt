package com.feasycom.feasyblue.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null

    // 仅在确定视图存活时使用，视图销毁访问直接崩溃
    protected val binding: VB
        get() = checkNotNull(_binding) {
            "Fragment ${this::class.java.simpleName}：ViewBinding 只能在 onCreateView -> onDestroyView 之间使用"
        }

    private val fragmentJob = SupervisorJob()
    protected val uiScope = CoroutineScope(Dispatchers.Main + fragmentJob)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = createViewBinding(inflater, container)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    override fun onDestroyView() {
        fragmentJob.cancel()
        // 解除父布局避免部分复用异常
        _binding?.root?.parent?.let {
            (it as ViewGroup).removeView(_binding?.root)
        }
        _binding = null
        super.onDestroyView()
    }

    /**
     * 安全UI操作，视图销毁自动跳过
     */
    protected fun safeUiAction(action: VB.() -> Unit) {
        val bind = _binding ?: return
        if (!isAdded || isDetached || isRemoving) return
        action.invoke(bind)
    }

    protected fun safeRunOnUiThread(action: VB.() -> Unit) {
        if (!isSafeState()) return
        activity?.runOnUiThread {
            if (isSafeState()) {
                _binding?.let(action)
            }
        }
    }

    protected fun getNullableBinding(): VB? = _binding

    private fun isSafeState(): Boolean {
        return _binding != null
                && isAdded
                && !isDetached
                && !isRemoving
    }

    abstract fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB
    abstract fun initView()
}