package com.feasycom.feasyblue.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.AtCommandItemBinding

/**
 * 密封类 ViewHolder，基于 ViewBinding 重构
 */
sealed class AtCommandViewHolder(val binding: AtCommandItemBinding) :
    RecyclerView.ViewHolder(binding.root)

/**
 * 带参数的 AT 命令 ViewHolder
 */
class CustomizeAtCommandViewHolder(binding: AtCommandItemBinding) :
    AtCommandViewHolder(binding)

/**
 * 无参数的 AT 命令 ViewHolder
 */
class CustomizeAtCommandNoParamViewViewHolder(binding: AtCommandItemBinding) :
    AtCommandViewHolder(binding)

/**
 * AT 命令适配器（ViewBinding 版本）
 */
class AtCommandAdapter(
    private val atCommandCount: Int,
    private val atCommandNoParamCount: Int
) : RecyclerView.Adapter<AtCommandViewHolder>() {

    private val atCommandTotal = atCommandCount + atCommandNoParamCount
    var countChangeCallback: ((count: Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AtCommandViewHolder {
        // 初始化 ViewBinding
        val binding = AtCommandItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return if (viewType == CUSTOMIZE_AT_COMMAND) {
            CustomizeAtCommandViewHolder(binding)
        } else {
            CustomizeAtCommandNoParamViewViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: AtCommandViewHolder, position: Int) {
        val context = holder.itemView.context
        val binding = holder.binding // 获取当前 ViewHolder 的 Binding

        when (holder) {
            is CustomizeAtCommandViewHolder -> {
                // 使用 ViewBinding 访问控件
                binding.group.visibility = View.VISIBLE
                binding.checkFlag.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        if (binding.parameter.text.toString().isNotEmpty() &&
                            binding.command.text.toString().isNotEmpty()
                        ) {
                            binding.parameter.isEnabled = !isChecked
                            binding.command.isEnabled = !isChecked
                            CUSTOMIZE_COMMAND_COUNT++
                            countChangeCallback?.invoke(CUSTOMIZE_COMMAND_COUNT)
                        } else {
                            binding.checkFlag.isChecked = false
                            Toast.makeText(
                                context,
                                context.resources.getString(R.string.none),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        binding.parameter.isEnabled = !isChecked
                        binding.command.isEnabled = !isChecked
                        CUSTOMIZE_COMMAND_COUNT--
                        countChangeCallback?.invoke(CUSTOMIZE_COMMAND_COUNT)
                    }
                }
            }

            is CustomizeAtCommandNoParamViewViewHolder -> {
                // 使用 ViewBinding 访问控件
                binding.checkFlag.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        if (binding.command.text.toString().isNotEmpty()) {
                            binding.command.isEnabled = !isChecked
                            CUSTOMIZE_COMMAND_COUNT++
                            countChangeCallback?.invoke(CUSTOMIZE_COMMAND_COUNT)
                        } else {
                            binding.checkFlag.isChecked = false
                            Toast.makeText(
                                context,
                                context.resources.getString(R.string.none),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        binding.command.isEnabled = !isChecked
                        CUSTOMIZE_COMMAND_COUNT--
                        countChangeCallback?.invoke(CUSTOMIZE_COMMAND_COUNT)
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position <= atCommandCount) {
            CUSTOMIZE_AT_COMMAND
        } else {
            CUSTOMIZE_AT_COMMAND_NO_PARAM
        }
    }

    override fun getItemCount(): Int {
        return atCommandTotal
    }

    companion object {
        private const val TAG = "AtCommandAdapter"
        var CUSTOMIZE_COMMAND_COUNT = 0

        const val CUSTOMIZE_AT_COMMAND = 0
        const val CUSTOMIZE_AT_COMMAND_NO_PARAM = 1
    }
}