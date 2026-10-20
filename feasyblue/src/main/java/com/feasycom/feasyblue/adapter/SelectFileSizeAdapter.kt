package com.feasycom.feasyblue.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.FileSizeItemBinding

/**
 * 文件大小选择适配器（ViewBinding 版本）
 */
class SelectFileSizeAdapter : RecyclerView.Adapter<SelectFileSizeAdapter.ViewHolder>() {

    private val items = listOf("10K", "1M", "50K", "2MB", "100K", "5MB", "200KB", "10MB", "500KB", "20MB")
    var onClickListener: ((size: Int, data: String) -> Unit)? = null

    /**
     * ViewHolder 重构为持有 ViewBinding 实例
     */
    inner class ViewHolder(val binding: FileSizeItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // 使用 ViewBinding 加载布局
        val binding = FileSizeItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // 使用 ViewBinding 访问控件，设置文本
        holder.binding.text.text = items[position]

        // 保留原有点击事件和大小转换逻辑
        holder.itemView.setOnClickListener {
            onClickListener?.invoke(
                when (position) {
                    0 -> 10000
                    1 -> 1000000
                    2 -> 50000
                    3 -> 2000000
                    4 -> 100000
                    5 -> 5000000
                    6 -> 200000
                    7 -> 10000000
                    8 -> 500000
                    9 -> 20000000
                    else -> 0
                },
                items[position]
            )
        }
    }

    override fun getItemCount() = items.size

    companion object {
        private const val TAG = "SelectFileSizeAdapter"
    }
}