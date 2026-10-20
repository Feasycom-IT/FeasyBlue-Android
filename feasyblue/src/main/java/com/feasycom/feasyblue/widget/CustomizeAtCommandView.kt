package com.feasycom.feasyblue.widget

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.presenters.CommandPresenters
import com.github.iielse.switchbutton.SwitchView

class CustomizeAtCommandView(context: Context?, attrs: AttributeSet?) :
    LinearLayout(context, attrs) {
    var label: TextView
    var parameterEdit: EditText
    var commandEdit: EditText
    lateinit var flagSwitch: SwitchView

    var mHandler = Handler(Looper.myLooper() ?: Looper.getMainLooper())
    var flagSwitchClickRunnable =
        Runnable { flagSwitch.isEnabled = true }

    val commandInfo: String
        get() =
            if (flagSwitch.isOpened) {
                "AT+" + commandEdit.text.toString() + "=" + parameterEdit.text.toString()
            } else ""

    val queryCommand: String
        get() = if (flagSwitch.isOpened) {
            "AT+" + commandEdit.text.toString()
        } else ""

    var customizeCommandCountChange: ((count: Int) -> Unit)? = null

    fun setCommandInfo(comand: String?, info: String?) {
        commandEdit.setText(comand)
        parameterEdit.setText(info)
        flagSwitch.isOpened = false
    }

    init {
        val view = inflate(context, R.layout.customize_at_command, this)
        label = view.findViewById<View>(R.id.label) as TextView
        parameterEdit = view.findViewById<View>(R.id.parameter) as EditText
        commandEdit = view.findViewById<View>(R.id.command) as EditText

        flagSwitch = view.findViewById<View>(R.id.flag_switch) as SwitchView
        flagSwitch.setOnClickListener {
            flagSwitch.isEnabled = false
            mHandler.postDelayed(flagSwitchClickRunnable, 300)

            if (flagSwitch.isOpened) {
                if (commandEdit.text.toString().isNotEmpty()) {
                    parameterEdit.isEnabled = !flagSwitch.isOpened
                    commandEdit.isEnabled = !flagSwitch.isOpened
                    CommandPresenters.plus()
                } else {
                    flagSwitch.isOpened = false
                    Toast.makeText(getContext(), resources.getString(R.string.none), Toast.LENGTH_SHORT).show()
                }
            } else {
                parameterEdit.isEnabled = !flagSwitch.isOpened
                commandEdit.isEnabled = !flagSwitch.isOpened
                CommandPresenters.minus()
            }

        }
    }
}