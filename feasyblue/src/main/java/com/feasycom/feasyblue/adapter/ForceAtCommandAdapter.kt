package com.feasycom.feasyblue.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ForceAtCommandItemBinding

/**
 * 强制 AT 命令适配器（ViewBinding 版本）
 */
class ForceAtCommandAdapter(private val labelList: List<String>) : RecyclerView.Adapter<ForceAtCommandAdapter.ViewHolder>() {

    /**
     * ViewHolder 重构为持有 ViewBinding 实例
     */
    inner class ViewHolder(val binding: ForceAtCommandItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // 使用 ViewBinding 加载布局
        val binding = ForceAtCommandItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context = holder.itemView.context
        val binding = holder.binding // 获取当前 ViewHolder 的 Binding 对象

        // 使用 ViewBinding 访问控件，设置标签文本
        binding.label.text = labelList[position]

        // 保留原有复选框监听逻辑
        binding.checkFlag.setOnCheckedChangeListener { buttonView, isChecked ->
            if (binding.contextEditText.text.toString().isNotEmpty()) {
                binding.contextEditText.isEnabled = !isChecked
            } else {
                binding.checkFlag.isChecked = false
                Toast.makeText(
                    context,
                    context.resources.getString(R.string.none),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun getItemCount() = labelList.size
}