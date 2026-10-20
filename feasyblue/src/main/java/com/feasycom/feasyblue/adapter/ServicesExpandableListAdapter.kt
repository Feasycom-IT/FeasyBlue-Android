package com.feasycom.feasyblue.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.blankj.utilcode.util.LogUtils
import com.feasycom.ble.bean.BleNamesResolver
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.bean.CharacteristicList
import com.feasycom.feasyblue.utils.bytesToHex
import java.util.*

class ServicesExpandableListAdapter(
    activity: Activity,
    private var bluetoothGattServiceList: List<BluetoothGattService>?
) : BaseExpandableListAdapter() {

    companion object {
        const val TAG: String = "ServicesExpandable"
    }

    var characteristicListList: MutableList<CharacteristicList> = ArrayList()
    private val mInflater: LayoutInflater
    override fun getGroupCount(): Int {
        return bluetoothGattServiceList!!.size
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return characteristicListList[groupPosition].getCount()
    }

    override fun getGroup(groupPosition: Int): Any {
        return bluetoothGattServiceList!![groupPosition]
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return characteristicListList[groupPosition].getItem(childPosition)
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    @SuppressLint("SetTextI18n", "InflateParams", "LongLogTag")
    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val serviceViewHolder: ServiceViewHolder
        val view: View
        if (null == convertView) {
            view = mInflater.inflate(R.layout.peripheral_list_services_item, null)
            serviceViewHolder = ServiceViewHolder(view)
            view.tag = serviceViewHolder
        } else {
            view = convertView
            serviceViewHolder = view.tag as ServiceViewHolder
        }
        val bluetoothGattService = bluetoothGattServiceList!![groupPosition]
        val uuid = bluetoothGattService.uuid.toString().lowercase(Locale.getDefault())
        val name = BleNamesResolver.resolveServiceName(uuid)
        if ("Unknown" == name) {
            LogUtils.d(TAG, "getGroupView UUID -> ${bluetoothGattService.uuid}")
            serviceViewHolder.serviceTitle?.text = "UUID:${bluetoothGattService.uuid}"
        } else {
            LogUtils.d(TAG, "getGroupView name -> $name")
            serviceViewHolder.serviceTitle?.text = name
        }
        return view
    }

    @SuppressLint("SetTextI18n", "InflateParams")
    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val view: View
        val chViewHolder: ChViewHolder
        if (convertView == null) {
            view = mInflater.inflate(R.layout.peripheral_details_characteristic_item, null)
            chViewHolder = ChViewHolder(view)
            view.tag = chViewHolder
        } else {
            view = convertView
            chViewHolder = view.tag as ChViewHolder
        }
        val bluetoothGattCharacteristic =
            characteristicListList[groupPosition].getItem(childPosition)
        val name =
            BleNamesResolver.resolveCharacteristicName(bluetoothGattCharacteristic.uuid.toString())
        if ("Unknown" == name) {
            LogUtils.d(TAG, "getChildView UUID => ${bluetoothGattCharacteristic.uuid}")
            chViewHolder.characteristicUUID!!.text =
                "0x${bluetoothGattCharacteristic.uuid}"
        } else {
            LogUtils.d(TAG, "getChildView name => $name")
            chViewHolder.characteristicUUID!!.text = name
        }
        val properties = bluetoothGattCharacteristic.properties
        val propertiesString = StringBuffer()
        if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {
            propertiesString.append(" WriteWithoutResponse")
        }
        if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
            propertiesString.append(" Write")
        }
        if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
            propertiesString.append(" Notify")
        }
        if (properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
            propertiesString.append(" Indicate")
        }
        if (properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
            propertiesString.append(" Read")
        }
        if (bluetoothGattCharacteristic.service.uuid.toString().substring(4, 8)
                .lowercase(Locale.getDefault()) == "180a"
        ) {
            if (name != null && name.isNotEmpty() && name.contains("String")) {
                try {
                    LogUtils.d(TAG, "getChildView characteristicProperties1 -> ${String(bluetoothGattCharacteristic.value)}")
                    chViewHolder.characteristicProperties!!.text =
                        String(bluetoothGattCharacteristic.value)
                } catch (e: Exception) {
                    e.printStackTrace()
                    chViewHolder.characteristicUUID!!.visibility = View.GONE
                    chViewHolder.characteristicProperties!!.visibility = View.GONE
                    chViewHolder.characteristicImgLL!!.visibility = View.GONE
                }
            } else {
                val temp = bluetoothGattCharacteristic.value
                if (temp != null) {
                    LogUtils.d(TAG, "getChildView characteristicProperties2 -> ${temp.bytesToHex()}")
                    chViewHolder.characteristicProperties!!.text = "<${temp.bytesToHex()}>"
                }
            }
        } else {
            LogUtils.d(TAG, "getChildView properties -> $propertiesString")
            chViewHolder.characteristicProperties!!.text = "Properties: $propertiesString"
        }
        return view
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    internal class ServiceViewHolder(view: View?) {
        var serviceTitle: TextView? = view?.findViewById(R.id.serviceTitle)
    }

    internal class ChViewHolder(view: View?) {
        var characteristicUUID: TextView? = view?.findViewById(R.id.characteristicUUID)

        var characteristicProperties: TextView? = view?.findViewById(R.id.characteristicProperties)

        var characteristicImgLL: LinearLayout? = view?.findViewById(R.id.characteristicImgLL)
    }

    init {
        characteristicListList.clear()
        if (bluetoothGattServiceList != null) {
            for (service in bluetoothGattServiceList!!) {
                val characteristicList = CharacteristicList(service.characteristics)
                characteristicListList.add(characteristicList)
            }
        }
        mInflater = activity.layoutInflater
    }
}


