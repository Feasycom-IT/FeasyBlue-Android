package com.feasycom.feasyblue.fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.activity.*
import com.feasycom.feasyblue.databinding.FragmentSettingBinding

/**
 * 设置页面 Fragment
 * 基于 BaseFragment 重构，移除冗余的 ViewBinding 声明，保留所有业务逻辑
 */
class SettingFragment : BaseFragment<FragmentSettingBinding>(), View.OnClickListener {

    /**
     * 实现基类抽象方法，创建 ViewBinding 实例
     * 基类已处理 onCreateView/onDestroyView 生命周期，无需重复实现
     */
    override fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingBinding {
        return FragmentSettingBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        // 使用基类提供的 binding 属性访问控件（无需自己声明 _binding）
        binding.toolbarTitle.text = getString(R.string.setting)
        binding.parameterDefining.setOnClickListener(this)
        binding.otaButton.setOnClickListener(this)
        binding.otaBatchButton.setOnClickListener(this)
        binding.wifiSettingButton.setOnClickListener(this)
        binding.bw256bButton.setOnClickListener(this)
        binding.advancedSettingButton.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.parameterDefining -> {
                PasswordActivity.activityStart(requireContext(), 0)
                requireActivity().finish()
            }
            R.id.otaButton -> {
                OtaSearchDeviceActivity.activityStart(requireContext())
            }
            R.id.otaBatchButton -> {
                BatchOtaActivity.activityStart(requireContext())
            }
            R.id.wifiSettingButton -> {
                WiFiSearchDeviceActivity.activityStart(requireContext())
            }
            R.id.bw256bButton -> {
                BW256BScanActivity.activityStart(requireContext(), -1)
            }
            R.id.advancedSettingButton -> {
                AdvancedSettingsListActivity.activityStart(requireContext())
                requireActivity().finish()
            }
        }
    }

    companion object {
        fun newInstance(): SettingFragment {
            return SettingFragment()
        }
    }
}