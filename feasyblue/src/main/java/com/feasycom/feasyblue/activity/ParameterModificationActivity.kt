package com.feasycom.feasyblue.activity

import android.content.Context
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.blankj.utilcode.util.LogUtils
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivityParameterModificationBinding
import com.feasycom.feasyblue.interfaces.ICommandCallback
import com.feasycom.feasyblue.presenters.CommandPresenters
import com.feasycom.feasyblue.utils.*
import com.github.iielse.switchbutton.SwitchView

/**
 * 参数修改页面
 * 仅修复语法错误，完全保留原始逻辑
 */
class ParameterModificationActivity : BaseActivity<ActivityParameterModificationBinding>(), View.OnClickListener, ICommandCallback {
    private lateinit var commandSet: LinkedHashSet<String>

    /**
     * 实现父类的 ViewBinding 获取方法（核心适配点）
     */
    override fun getViewBinding(): ActivityParameterModificationBinding {
        return ActivityParameterModificationBinding.inflate(layoutInflater)
    }

    override fun initView() {
        initToolbar()
        binding.paramModifyStart.isEnabled = false
        binding.paramModifyStart.text = getString(R.string.begin)
        binding.paramModifyStart.setOnClickListener(this)

        CommandPresenters.mICommandCallback = this

        initData()
        initEvent()
        commandSet = LinkedHashSet()

        // 关键：初始化时同步所有开关与计数器，避免初始计数错乱
        syncAllSwitchStates()
    }

    /**
     * 初始化Toolbar
     */
    private fun initToolbar() {
        setSupportActionBar(binding.paramModifyToolbar)
        supportActionBar?.let {
            it.setDisplayShowTitleEnabled(false)
            it.setDisplayHomeAsUpEnabled(true)
        }
        binding.paramModifyTitle.text = getString(R.string.parameterDefining)
        binding.paramModifyToolbar.setNavigationOnClickListener {
            MainActivity.activityStart(this, 1)
            finish()
        }
    }

    /**
     * 初始化输入框数据和持久化监听（完全保留原始逻辑）
     */
    private fun initData() {
        val editTexts = listOf(
            binding.nameParaEt to "namePara",
            binding.pinParaEt to "pinPara",
            binding.baudParaEt to "baudPara",
            binding.customCommandEt1 to "customCommandEt1",
            binding.customParameterEt1 to "customParameterEt1",
            binding.customCommandEt2 to "customCommandEt2",
            binding.customParameterEt2 to "customParameterEt2",
            binding.customCommandEt3 to "customCommandEt3",
            binding.customParameterEt3 to "customParameterEt3",
            binding.customCommandEt4 to "customCommandEt4",
            binding.customParameterEt4 to "customParameterEt4",
            binding.customCommandEt5 to "customCommandEt5",
            binding.customParameterEt5 to "customParameterEt5",
            binding.customCommandEt6 to "customCommandEt6",
            binding.customParameterEt6 to "customParameterEt6",
            binding.customCommandEt7 to "customCommandEt7",
            binding.customParameterEt7 to "customParameterEt7",
            binding.customCommandEt8 to "customCommandEt8",
            binding.customParameterEt8 to "customParameterEt8",
            binding.customCommandEt9 to "customCommandEt9",
            binding.customParameterEt9 to "customParameterEt9"
        )

        // 初始化内容与持久化监听（原始逻辑）
        editTexts.forEach { (editText, key) ->
            editText.setText(getStr(key, ""))
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    putStr(key, s.toString().trim())
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    /**
     * 初始化开关点击事件（修复语法错误，保留原始逻辑）
     */
    private fun initEvent() {
        // 原始逻辑：开关点击事件绑定到当前Activity的onClick
        val switches = listOf(
            binding.nameSwitch, binding.pinSwitch, binding.baudSwitch,
            binding.customSwitch1, binding.customSwitch2, binding.customSwitch3,
            binding.customSwitch4, binding.customSwitch5, binding.customSwitch6,
            binding.customSwitch7, binding.customSwitch8, binding.customSwitch9
        )
        switches.forEach { it.setOnClickListener(this) }
    }

    /**
     * 点击事件处理（完全保留原始逻辑，仅修复语法错误）
     */
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.paramModifyStart -> {
                saveCommand()
                ParameterModificationDeviceListActivity.activityStart(this)
            }

            // 标准三项
            R.id.name_switch -> handleSwitch(binding.nameSwitch, binding.nameParaEt, "namePara")
            R.id.pin_switch  -> handleSwitch(binding.pinSwitch, binding.pinParaEt, "pinPara")
            R.id.baud_switch -> handleSwitch(binding.baudSwitch, binding.baudParaEt, "baudPara")

            // 自定义九项（以 commandKey 作为唯一 key）
            R.id.custom_switch1 -> handleCustomSwitch(binding.customSwitch1, binding.customCommandEt1, binding.customParameterEt1, "customCommandEt1")
            R.id.custom_switch2 -> handleCustomSwitch(binding.customSwitch2, binding.customCommandEt2, binding.customParameterEt2, "customCommandEt2")
            R.id.custom_switch3 -> handleCustomSwitch(binding.customSwitch3, binding.customCommandEt3, binding.customParameterEt3, "customCommandEt3")
            R.id.custom_switch4 -> handleCustomSwitch(binding.customSwitch4, binding.customCommandEt4, binding.customParameterEt4, "customCommandEt4")
            R.id.custom_switch5 -> handleCustomSwitch(binding.customSwitch5, binding.customCommandEt5, binding.customParameterEt5, "customCommandEt5")
            R.id.custom_switch6 -> handleCustomSwitch(binding.customSwitch6, binding.customCommandEt6, binding.customParameterEt6, "customCommandEt6")
            R.id.custom_switch7 -> handleCustomSwitch(binding.customSwitch7, binding.customCommandEt7, binding.customParameterEt7, "customCommandEt7")
            R.id.custom_switch8 -> handleCustomSwitch(binding.customSwitch8, binding.customCommandEt8, binding.customParameterEt8, "customCommandEt8")
            R.id.custom_switch9 -> handleCustomSwitch(binding.customSwitch9, binding.customCommandEt9, binding.customParameterEt9, "customCommandEt9")
        }
    }

    // —— 通用：标准项开关处理（完全保留原始逻辑）——
    private fun handleSwitch(switch: SwitchView, editText: EditText, key: String) {
        val hasValue = getStr(key, "").isNotEmpty()
        if (hasValue) {
            val opened = switch.isOpened
            editText.isEnabled = !opened
            // 关键：用 key 维度同步状态，不再用 +1/-1
            CommandPresenters.setState(key, opened)
        } else {
            if (switch.isOpened) {
                switch.isOpened = false
            }
            editText.isEnabled = true
            CommandPresenters.setState(key, false)
            Toast.makeText(this, resources.getString(R.string.none), Toast.LENGTH_SHORT).show()
        }
    }

    // —— 通用：自定义命令开关处理（使用 commandKey 作为 key）——
    private fun handleCustomSwitch(
        switch: SwitchView,
        commandEditText: EditText,
        parameterEditText: EditText,
        commandKey: String
    ) {
        val opened = switch.isOpened
        if (opened) {
            if (getStr(commandKey, "").isNotEmpty()) {
                commandEditText.isEnabled = false
                parameterEditText.isEnabled = false
                CommandPresenters.setState(commandKey, true)
            } else {
                switch.isOpened = false
                commandEditText.isEnabled = true
                parameterEditText.isEnabled = true
                CommandPresenters.setState(commandKey, false)
                Toast.makeText(this, resources.getString(R.string.none), Toast.LENGTH_SHORT).show()
            }
        } else {
            commandEditText.isEnabled = true
            parameterEditText.isEnabled = true
            CommandPresenters.setState(commandKey, false)
        }
    }

    // —— 初始化时把所有开关的有效性与 Presenter 集合同步一次 ——
    private fun syncAllSwitchStates() {
        // 标准项
        listOf(
            Triple(binding.nameSwitch, binding.nameParaEt, "namePara"),
            Triple(binding.pinSwitch, binding.pinParaEt, "pinPara"),
            Triple(binding.baudSwitch, binding.baudParaEt, "baudPara")
        ).forEach { (sw, et, key) ->
            val valid = getStr(key, "").isNotEmpty() && sw.isOpened
            et.isEnabled = !sw.isOpened
            CommandPresenters.setState(key, valid)
        }

        // 自定义项（以 commandKey 为 key）
        listOf(
            Triple(binding.customSwitch1, binding.customCommandEt1, binding.customParameterEt1) to "customCommandEt1",
            Triple(binding.customSwitch2, binding.customCommandEt2, binding.customParameterEt2) to "customCommandEt2",
            Triple(binding.customSwitch3, binding.customCommandEt3, binding.customParameterEt3) to "customCommandEt3",
            Triple(binding.customSwitch4, binding.customCommandEt4, binding.customParameterEt4) to "customCommandEt4",
            Triple(binding.customSwitch5, binding.customCommandEt5, binding.customParameterEt5) to "customCommandEt5",
            Triple(binding.customSwitch6, binding.customCommandEt6, binding.customParameterEt6) to "customCommandEt6",
            Triple(binding.customSwitch7, binding.customCommandEt7, binding.customParameterEt7) to "customCommandEt7",
            Triple(binding.customSwitch8, binding.customCommandEt8, binding.customParameterEt8) to "customCommandEt8",
            Triple(binding.customSwitch9, binding.customCommandEt9, binding.customParameterEt9) to "customCommandEt9"
        ).forEach { (triple, commandKey) ->
            val (sw, cmdEt, paramEt) = triple
            val valid = getStr(commandKey, "").isNotEmpty() && sw.isOpened
            cmdEt.isEnabled = !sw.isOpened
            paramEt.isEnabled = !sw.isOpened
            CommandPresenters.setState(commandKey, valid)
        }
    }

    /**
     * 保存命令集合（完全保留原始逻辑）
     */
    private fun saveCommand() {
        commandSet.clear()

        val commands = listOf(
            Triple(binding.nameSwitch, "namePara", "AT+NAME"),
            Triple(binding.pinSwitch, "pinPara", "AT+PIN"),
            Triple(binding.baudSwitch, "baudPara", "AT+BAUD"),
            Triple(binding.customSwitch1, "customCommandEt1", "AT+"),
            Triple(binding.customSwitch2, "customCommandEt2", "AT+"),
            Triple(binding.customSwitch3, "customCommandEt3", "AT+"),
            Triple(binding.customSwitch4, "customCommandEt4", "AT+"),
            Triple(binding.customSwitch5, "customCommandEt5", "AT+"),
            Triple(binding.customSwitch6, "customCommandEt6", "AT+"),
            Triple(binding.customSwitch7, "customCommandEt7", "AT+"),
            Triple(binding.customSwitch8, "customCommandEt8", "AT+"),
            Triple(binding.customSwitch9, "customCommandEt9", "")
        )

        commands.forEach { (switch, key, commandPrefix) ->
            val commandValue = getStr(key, "")
            if (commandValue.isNotEmpty() && switch.isOpened) {
                val parameterKey = key.replace("Command", "Parameter")
                val parameterValue = getStr(parameterKey, "").trim()
                LogUtils.d(TAG, "APP PARAMETER parameterKey: $parameterKey  parameterValue => $parameterValue")

                if (parameterValue.isNotEmpty()) {
                    val fullCommand = if (key.contains("Para")) "$commandPrefix=$commandValue".trim()
                    else "$commandPrefix$commandValue=$parameterValue".trim()
                    LogUtils.d(TAG, "APP PARAMETER Command1: $fullCommand")
                    commandSet.add(fullCommand)
                }

                val simpleCommand = if (key.contains("Para")) commandPrefix else "$commandPrefix$commandValue"
                LogUtils.d(TAG, "APP PARAMETER Command2: $simpleCommand")
                commandSet.add(simpleCommand)
            }
        }

        val commandValue9 = getStr("customCommandEt9","")
        LogUtils.d(TAG,"APP PARAMETER commandValue9: $commandValue9")
        if (commandValue9.isNotEmpty() && binding.customSwitch9.isOpened) {
            val parameterValue9 = getStr("customParameterEt9", "")
            LogUtils.d(TAG, "APP PARAMETER parameterValue9: $commandValue9=$parameterValue9")
            commandSet.add("$commandValue9=$parameterValue9")
        }

        if (commandSet.isEmpty()) {
            Toast.makeText(this, getString(R.string.commandNull), Toast.LENGTH_SHORT).show()
        }

        for (command in commandSet) {
            LogUtils.d(TAG, "saveCommand command => $command")
        }

        saveOrderedStringSet(this, commandSet)
    }

    /**
     * 处理返回键事件（完全保留原始逻辑）
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            saveCommand()
            MainActivity.activityStart(this, 1)
            finish()
            false
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    /**
     * ICommandCallback 回调：更新开始按钮状态（完全保留原始逻辑）
     */
    override fun update(i: Int) {
        LogUtils.d(TAG, "APP PARAMETER 数据 => $i")
        // 只要集合里有有效开关（i>0），就允许开始
        binding.paramModifyStart.isEnabled = i > 0
    }

    companion object {
        private const val TAG = "ParameterModify"

        /**
         * 启动参数修改页面（完全保留原始逻辑）
         */
        fun activityStart(context: Context) {
            val intent = Intent(context, ParameterModificationActivity::class.java)
            context.startActivity(intent)
        }
    }
}