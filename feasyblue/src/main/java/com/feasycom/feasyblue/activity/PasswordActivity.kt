package com.feasycom.feasyblue.activity

import android.content.Context
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivityPasswordBinding
import com.feasycom.feasyblue.utils.getBoolean
import com.feasycom.feasyblue.utils.putBoolean

/**
 * 密码验证页面
 * 适配优化后的泛型 ViewBinding BaseActivity
 */
class PasswordActivity : BaseActivity<ActivityPasswordBinding>() {

    // 声明页面类型变量（参数设置/高级设置）
    private var type: Int = 0

    /**
     * 实现父类的 ViewBinding 获取方法（核心适配点）
     */
    override fun getViewBinding(): ActivityPasswordBinding {
        return ActivityPasswordBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // 初始化 Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.let {
            it.setDisplayShowTitleEnabled(false)
            it.setDisplayHomeAsUpEnabled(true)
        }

        // 获取页面类型（0=参数设置，1=高级设置）
        type = intent.getIntExtra("type", 0)

        // 根据类型初始化标题和跳转逻辑
        initPageByType()

        // 绑定返回按钮事件
        binding.toolbar.setNavigationOnClickListener {
            MainActivity.activityStart(this, 1)
            finish()
        }

        // 绑定密码输入框文本变化监听
        initPasswordEditTextListener()
    }

    /**
     * 根据页面类型初始化标题和自动跳转逻辑
     */
    private fun initPageByType() {
        if (type == 0) {
            // 参数设置页面
            binding.toolbarTitle.text = getString(R.string.parameterDefining)
            val isPasswordPara = getBoolean("isPasswordPara", false)
            if (isPasswordPara) {
                ParameterModificationActivity.activityStart(this@PasswordActivity)
                finish()
            }
        } else {
            // 高级设置页面
            binding.toolbarTitle.text = getString(R.string.advanced_setting_text)
            val isPasswordEAdvance = getBoolean("isPasswordEAdvance", false)
            if (isPasswordEAdvance) {
                AdvancedSettingsListActivity.activityStart(this@PasswordActivity)
                finish()
            }
        }
    }

    /**
     * 初始化密码输入框监听
     */
    private fun initPasswordEditTextListener() {
        binding.passwordEditView.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 密码长度达到8位时验证
                if (s?.length == 8) {
                    validatePassword(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // 密码长度不足8位时清空错误提示
                if (s?.length != 8) {
                    binding.passwordEditView.error = ""
                }
            }
        })
    }

    /**
     * 验证密码并处理跳转逻辑
     */
    private fun validatePassword(password: String) {
        when (type) {
            0 -> {
                // 参数设置密码验证
                if (password == "20138888") {
                    putBoolean("isPasswordPara", true)
                    ParameterModificationActivity.activityStart(this@PasswordActivity)
                    finish()
                } else {
                    binding.passwordEditView.error = getString(R.string.password_error)
                }
            }
            1 -> {
                // 高级设置密码验证
                if (password == "20137777") {
                    putBoolean("isPasswordEAdvance", true)
                    AdvancedSettingsListActivity.activityStart(this@PasswordActivity)
                    finish()
                } else {
                    binding.passwordEditView.error = getString(R.string.password_error)
                }
            }
        }
    }

    /**
     * 处理返回键事件
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            MainActivity.activityStart(this, 1)
            finish()
            false
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    companion object {
        private const val TAG = "PasswordActivity"

        /**
         * 启动密码验证页面
         * @param context 上下文
         * @param type 页面类型（0=参数设置，1=高级设置）
         */
        fun activityStart(context: Context, type: Int) {
            val intent = Intent(context, PasswordActivity::class.java)
            intent.putExtra("type", type)
            context.startActivity(intent)
        }
    }
}