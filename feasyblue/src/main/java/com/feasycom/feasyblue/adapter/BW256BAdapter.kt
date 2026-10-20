package com.feasycom.feasyblue.adapter

import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.feasycom.feasyblue.R

class BW256BAdapter(
    private val deviceList: MutableList<WifiP2pDevice>, private val listener: OnDeviceClickListener
) : RecyclerView.Adapter<BW256BAdapter.DeviceViewHolder>() {

    interface OnDeviceClickListener {
        fun onDeviceClick(device: WifiP2pDevice)
    }

    class DeviceViewHolder(
        itemView: View,
        private val deviceList: MutableList<WifiP2pDevice>,
        private val listener: OnDeviceClickListener
    ) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val deviceName: TextView = itemView.findViewById(R.id.device_name)
        val deviceMac: TextView = itemView.findViewById(R.id.device_mac)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val device = deviceList[position]
                listener.onDeviceClick(device)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.bw256b_item, parent, false)
        return DeviceViewHolder(view, deviceList, listener)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = deviceList[position]
        holder.deviceName.text = device.deviceName
        holder.deviceMac.text = device.deviceAddress
    }

    override fun getItemCount(): Int {
        return deviceList.size
    }

    fun updateDeviceList(newList: Collection<WifiP2pDevice>) {
        deviceList.clear()
        deviceList.addAll(newList)
        notifyDataSetChanged()
    }

}