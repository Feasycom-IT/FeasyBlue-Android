package com.feasycom.feasyblue.activity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.LogUtils
import com.feasycom.ble.controler.FscBleCentralApi
import com.feasycom.ble.controler.FscBleCentralApiImp
import com.feasycom.common.bean.FscDevice
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.adapter.ServicesExpandableListAdapter
import com.feasycom.feasyblue.databinding.ActivityServiceSelectBinding
import com.feasycom.feasyblue.databinding.PeripheralListServicesHeaderBinding
import com.feasycom.feasyblue.utils.bytesToHex

/**
 * 服务选择页面
 * 适配 ViewBinding + 移除 kotlinx.android.synthetic + 保留所有原始逻辑
 */
class ServiceSelectActivity : BaseActivity<ActivityServiceSelectBinding>() {

    private lateinit var mFscDevice: FscDevice
    private lateinit var mBleApi: FscBleCentralApi
    private lateinit var mServiceList: MutableList<BluetoothGattService>
    private lateinit var mServicesExpandableListAdapter: ServicesExpandableListAdapter

    // 头部View的Binding（替代findViewById）
    private lateinit var headerBinding: PeripheralListServicesHeaderBinding

    /**
     * 实现 ViewBinding 获取方法
     */
    override fun getViewBinding(): ActivityServiceSelectBinding {
        return ActivityServiceSelectBinding.inflate(layoutInflater)
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.N)
    override fun initView() {
        // 1. 获取并校验FscDevice参数（保留原始逻辑）
        val fscDevice = intent.getParcelableExtra<FscDevice>("FscDevice")
        if (fscDevice == null) {
            finish()
            return
        }
        mFscDevice = fscDevice

        // 2. 配置Toolbar（替换findViewById为binding）
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbarTitle.text = getString(R.string.transmissionConfiguration) // 保留原始逻辑（覆盖设备名称）
        binding.toolbar.setNavigationOnClickListener { finish() }

        // 3. 初始化BLE API和MTU（保留原始逻辑）
        mBleApi = FscBleCentralApiImp.getInstance()
        binding.mtuView.setProgress(mBleApi.getMtu(mFscDevice.address))
        binding.mtuView.setMtu = {
            mBleApi.requestMtu(mFscDevice.address, it)
        }

        // 4. 获取服务列表（保留原始异常处理）
        mServiceList = try {
            mBleApi.getBluetoothGattServices(mFscDevice.address)
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf() // 避免空指针
        }

        // 5. 初始化ExpandableListAdapter（保留原始逻辑）
        mServicesExpandableListAdapter = ServicesExpandableListAdapter(this, mServiceList)

        // 6. 初始化列表头部View（使用ViewBinding替代findViewById）
        headerBinding = PeripheralListServicesHeaderBinding.inflate(layoutInflater)
        val mListViewHeader = headerBinding.root
        mListViewHeader.setOnClickListener { } // 保留原始空点击逻辑

        // 7. 配置广播数据（保留原始日志和显示逻辑）
        LogUtils.d(TAG, "APP BLE initView =>  ${mFscDevice.scanRecord?.bytesToHex()}")
        headerBinding.advData.text = mFscDevice.scanRecord?.bytesToHex()

        // 8. 设置ExpandableListView（保留原始逻辑）
        binding.expandableListView.addHeaderView(mListViewHeader)
        binding.expandableListView.setAdapter(mServicesExpandableListAdapter)
        // 展开所有分组
        for (i in 0 until mServicesExpandableListAdapter.groupCount) {
            binding.expandableListView.expandGroup(i)
        }
        mServicesExpandableListAdapter.notifyDataSetChanged()

        // 9. 配置头部View的广播数据显示（保留原始显隐逻辑）
        // Flag
        if (mFscDevice.flag.isNullOrEmpty()) {
            headerBinding.advFlagLL.visibility = View.GONE
        } else {
            headerBinding.advFlag.text = mFscDevice.flag
        }
        // 16位不完整服务UUID
        if (mFscDevice.incompleteServiceUUIDs_16bit.isNullOrEmpty()) {
            headerBinding.incompleteServiceUUIDs16bitLL.visibility = View.GONE
        } else {
            headerBinding.incompleteServiceUUIDs16bit.text = mFscDevice.incompleteServiceUUIDs_16bit
        }
        // 128位不完整服务UUID
        if (mFscDevice.incompleteServiceUUIDs_128bit.isNullOrEmpty()) {
            headerBinding.incompleteServiceUUIDs128bitLL.visibility = View.GONE
        } else {
            headerBinding.incompleteServiceUUIDs128bit.text = mFscDevice.incompleteServiceUUIDs_128bit
        }
        // 完整本地名称
        headerBinding.completeLocalName.text = mFscDevice.name
        // 服务数据
        if (mFscDevice.serviceData.isNullOrEmpty()) {
            headerBinding.serviceDataLL.visibility = View.GONE
        } else {
            headerBinding.serviceData.text = mFscDevice.serviceData
        }
        // 发射功率
        if (mFscDevice.txPowerLevel.isNullOrEmpty()) {
            headerBinding.txPowerLevelLL.visibility = View.GONE
        } else {
            headerBinding.txPowerLevel.text = mFscDevice.txPowerLevel
        }
        // 厂商特定数据
        if (mFscDevice.manufacturerSpecificData.isNullOrEmpty()) {
            headerBinding.manufacturerSpecificDataLL.visibility = View.GONE
        } else {
            headerBinding.manufacturerSpecificData.text = mFscDevice.manufacturerSpecificData
        }

        // 10. 设置子项点击事件（保留原始逻辑）
        binding.expandableListView.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
            if (groupPosition < mServiceList.size && childPosition < mServiceList[groupPosition].characteristics.size) {
                bluetoothGattCharacteristic = mServiceList[groupPosition].characteristics[childPosition]
                CharacteristicDetailActivity.activityStart(this, mFscDevice.address)
            }
            false
        }
    }

    /**
     * 移除原 getLayout() 方法（ViewBinding 无需返回布局ID）
     */
    // override fun getLayout() = R.layout.activity_service_select

    companion object {
        const val TAG = "ServiceSelectActivity"
        var bluetoothGattCharacteristic: BluetoothGattCharacteristic? = null

        /**
         * 启动方法（保留原始逻辑）
         */
        fun activityStart(context: Context, fscDevice: FscDevice) {
            val intent = Intent(context, ServiceSelectActivity::class.java)
            intent.putExtra("FscDevice", fscDevice)
            context.startActivity(intent)
        }
    }
}