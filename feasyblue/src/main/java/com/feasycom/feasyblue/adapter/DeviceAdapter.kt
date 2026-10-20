package com.feasycom.feasyblue.adapter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.feasycom.common.bean.FscDevice
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.DeviceItemBinding

/**
 * 蓝牙设备列表适配器（ViewBinding 版本）
 */
class DeviceAdapter(private val devices: List<FscDevice>) :
    RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    var mOnClickListener: ((position: Int) -> Unit)? = null

    /**
     * ViewHolder 重构为持有 ViewBinding 实例
     */
    class ViewHolder(val binding: DeviceItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // 使用 ViewBinding 加载布局
        val binding = DeviceItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    @SuppressLint("SetTextI18n", "MissingPermission")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]
        val context = holder.itemView.context

        // 使用 ViewBinding 访问控件，保留原有业务逻辑
        holder.binding.deviceName.text =
            if (device.device.bondState == BluetoothDevice.BOND_BONDED
            /** && device.mode == Constant.SPP_MODE **/
            ) {
                "${context.resources.getString(R.string.paired)} ${
                    if (device.device.name != null) {

                        device.device.name

                    } else {
                        "unknown"
                    }
                }"
            } else {
                if (device.device.name != null) {

                    device.device.name

                } else {
                    "unknown"
                }
            }

        // 设备地址
        holder.binding.deviceAddress.text = device.address
        // 设备模式
        holder.binding.deviceMode.text = device.mode
        // RSSI 数值
        holder.binding.deviceRssi.text =
            "${context.resources.getString(R.string.rssi)} (${device.rssi})"
        // RSSI 进度条
        holder.binding.deviceRssiProgressBar.progress = 100 + device.rssi

        // 点击事件（保留原有回调逻辑）
        holder.itemView.setOnClickListener {
            mOnClickListener?.invoke(position)
        }
    }

    override fun getItemCount(): Int {
        return devices.size
    }

    override fun getItemViewType(position: Int): Int {
        return 0
    }

    companion object {
        private const val TAG = "DeviceAdapter"
    }
}