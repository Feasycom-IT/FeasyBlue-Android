package com.feasycom.feasyblue.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.graphics.Rect
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.feasycom.ble.controler.FscBleCentralApi
import com.feasycom.ble.controler.FscBleCentralApiImp
import com.feasycom.ble.controler.FscBleCentralCallbacksImp
import com.feasycom.common.bean.ConnectType
import com.feasycom.common.bean.FscDevice
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.activity.*
import com.feasycom.feasyblue.adapter.DeviceAdapter
import com.feasycom.feasyblue.databinding.FragmentCommunicationBinding
import com.feasycom.feasyblue.dialog.LoadDialogFragment
import com.feasycom.feasyblue.dialog.MessageDialog
import com.feasycom.feasyblue.dialog.WFBaseDialog
import com.feasycom.feasyblue.interfaces.FeasyWiFiCallbacks
import com.feasycom.feasyblue.logic.BluetoothRepository
import com.feasycom.feasyblue.permission.PermissionDescription
import com.feasycom.feasyblue.permission.PermissionInterceptor
import com.feasycom.feasyblue.utils.*
import com.feasycom.spp.controler.FscSppCentralApi
import com.feasycom.spp.controler.FscSppCentralApiImp
import com.feasycom.spp.controler.FscSppCentralCallbacksImp
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

/**
 * 通讯界面Fragment，负责展示设备列表和操作界面
 * 基于 ViewBinding 重构，保留所有原有业务逻辑
 */
class CommunicationFragment : BaseFragment<FragmentCommunicationBinding>(), View.OnClickListener,
    RadioGroup.OnCheckedChangeListener, FeasyWiFiCallbacks {
    private lateinit var mWaitDialog: LoadDialogFragment
    private lateinit var mMessageDialog: WFBaseDialog
    private var filterRssi: Int = -100
    private var filterNameSwitch: Boolean = false
    private var filterName: String = ""
    private val mHandler = Handler(Looper.getMainLooper())
    private val devices = mutableListOf<FscDevice>()
    private var mFscDevice: FscDevice? = null
    private var isWiFiParamSetting = false
    private var isSppTest = false
    private var isBleTest = false

    private val sFscSppCentralApi: FscSppCentralApi by lazy {
        FscSppCentralApiImp.getInstance(requireContext())
    }

    private val mFscBleCentralApi: FscBleCentralApi by lazy {
        FscBleCentralApiImp.getInstance(requireContext())
    }

    /**
     * 实现基类抽象方法，创建 ViewBinding 实例
     */
    override fun createViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCommunicationBinding {
        return FragmentCommunicationBinding.inflate(inflater, container, false)
    }

    private var mDeviceAdapter: DeviceAdapter? = null

    @SuppressLint("NotifyDataSetChanged")
    override fun initView() {
        activity?.let {
            // 根据Activity类型设置标题和显示内容
            when (it) {
                is MainActivity -> {
                    binding.toolbarTitle.text = getString(R.string.communication)
                }

                is ParameterModificationDeviceListActivity -> {
                    binding.toolbarTitle.text = getString(R.string.parameterDefining)
                }

                is OtaSearchDeviceActivity -> {
                    binding.toolbarTitle.text = getString(R.string.OTAUpgrade)
                }

                is WiFiSearchDeviceActivity -> {
                    binding.toolbarTitle.text = getString(R.string.wifi_param_setting)
                    isWiFiParamSetting = true
                }

                is SppAutoConnectTestActivity -> {
                    binding.toolbarTitle.text = getString(R.string.spp_auto_connect_test)
                    isSppTest = true
                }

                is BleAutoConnectTestActivity -> {
                    binding.toolbarTitle.text = getString(R.string.ble_auto_connect_test)
                    isBleTest = true
                }

                else -> binding.toolbarTitle.text = getString(R.string.communication)
            }
            // 根据Activity类型设置是否显示返回按钮
            binding.toolbarImageButton.visibility =
                if (it is MainActivity) View.GONE else View.VISIBLE
        }
        BluetoothRepository.registerViewCallback(this)
        mWaitDialog = LoadDialogFragment.newInstance(getString(R.string.connectings))
        // 根据不同的场景设置默认选择的通信方式
        when {
            isWiFiParamSetting -> {
                binding.bleCheck.isChecked = true
                binding.sppCheck.visibility = View.GONE
            }

            isSppTest -> {
                binding.sppCheck.isChecked = true
                binding.bleCheck.visibility = View.GONE
            }

            isBleTest -> {
                binding.bleCheck.isChecked = true
                binding.sppCheck.visibility = View.GONE
            }

            else -> binding.sppCheck.isChecked = true
        }

        mDeviceAdapter = DeviceAdapter(devices)
        mDeviceAdapter?.mOnClickListener = { position ->
            stopScan()
            activity?.let {
                when (it) {
                    is MainActivity -> {
                        if (devices.isNotEmpty()) {
                            ThroughputActivity.activityStart(requireContext(), devices[position])
                        }
                    }

                    is ParameterModificationDeviceListActivity -> {
                        ParameterModifyInformationActivity.activityStart(
                            requireContext(), devices[position]
                        )
                    }

                    is OtaSearchDeviceActivity -> {
                        OtaActivity.activityStart(requireContext(), devices[position])
                    }

                    is WiFiSearchDeviceActivity -> {
                        if (BluetoothRepository.isBtEnabled()) {
                            mFscDevice = devices[position]
                            if (mFscDevice!!.name != null /*&& mFscDevice!!.name.contains("BW")*/) {
                                mWaitDialog.show(
                                    requireActivity().supportFragmentManager,
                                    "connect"
                                )
                                BluetoothRepository.connect(devices[position].address)
                            } else {
                                mMessageDialog = MessageDialog.Builder(requireContext())
                                    .setMessage(getString(R.string.not_wifi_device))
                                    .setConfirm(getString(R.string.ok))
                                    .show()
                            }
                        } else {
                            mMessageDialog = MessageDialog.Builder(requireContext())
                                .setMessage(getString(R.string.open_bluetooth_tips)) // 确定按钮文本
                                .setConfirm(getString(R.string.ok)) // 设置点击按钮后不关闭对话框
                                .show()
                        }
                    }

                    is SppAutoConnectTestActivity -> {
                        SppConnectTestActivity.activityStart(requireContext(), devices[position])
                    }

                    is BleAutoConnectTestActivity -> {
                        BleConnectTestActivity.activityStart(requireContext(), devices[position])
                    }
                }
            }
        }

        with(binding.deviceRecyclerView) {
            itemAnimator = null // 解决局部刷新导致页面闪烁的问题
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mDeviceAdapter

            // 设置RecyclerView的ItemDecoration
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
                ) {
                    super.getItemOffsets(outRect, view, parent, state)
                    with(UIUtil.dip2px(context, 1.0)) {
                        outRect.top = this
                        outRect.bottom = this
                        outRect.left = this
                        outRect.right = this
                    }
                }
            })
        }

        // 设置下拉刷新监听
        binding.smartRefreshLayout.setOnRefreshListener {
            if (checkEnvironment() == 0) {
                devices.clear()
                mDeviceAdapter?.notifyDataSetChanged()
                startScan()
                it.closeHeaderOrFooter()
            } else if (checkEnvironment() == -3) {
                it.closeHeaderOrFooter()
                ToastUtils.showShort(getString(R.string.insufficient_permissions))
            }
        }
        // 根据蓝牙和GPS状态设置对应的可见性
        binding.bluetooth.visibility = if (mFscBleCentralApi.isEnabled) View.GONE else View.VISIBLE
        val enabled: Boolean = isLocationEnabled(requireContext())
        binding.gps.visibility = if (enabled) View.GONE else View.VISIBLE
        // 初始化事件监听
        initEvent()
        // 注册广播接收器
        registerBroadcastReceivers()
    }

    override fun blePeripheralConnected() {
        BluetoothRepository.isDisconnected = false
        lifecycleScope.launch(Dispatchers.Main) {
            if (::mWaitDialog.isInitialized && mWaitDialog.isVisible) {
                mWaitDialog.dismiss()
                WiFiSettingActivity.activityStart(requireContext(), mFscDevice!!)
            }
        }
    }

    override fun blePeripheralDisconnected() {
        super.blePeripheralDisconnected()
        BluetoothRepository.isDisconnected = true
        lifecycleScope.launch(Dispatchers.Main) {
            if (::mWaitDialog.isInitialized && mWaitDialog.isVisible) {
                mWaitDialog.dismiss()
                mMessageDialog = MessageDialog.Builder(requireContext())
                    .setMessage(getString(R.string.connection_failed)) // 确定按钮文本
                    .setConfirm(getString(R.string.ok)) // 设置点击按钮后不关闭对话框
                    .show()
            }
        }
    }

    override fun onConnectTimeout() {// 30秒钟连接超时的处理
        super.onConnectTimeout()
        if (::mWaitDialog.isInitialized && mWaitDialog.isVisible) {
            mWaitDialog.dismiss()
            mMessageDialog = MessageDialog.Builder(requireContext())
                .setMessage(getString(R.string.connection_time_out)) // 确定按钮文本
                .setConfirm(getString(R.string.ok)) // 设置点击按钮后不关闭对话框
                .show()
        }
    }

    private fun initEvent() {
        binding.sortLinearLayout.setOnClickListener(this)
        binding.filterLinearLayout.setOnClickListener(this)
        binding.headerRightLL.setOnCheckedChangeListener(this)
        binding.toolbarImageButton.setOnClickListener(this)
        binding.applyTv.setOnClickListener(this)
        binding.bluetoothEnable.setOnClickListener(this)
        binding.gpsEnable.setOnClickListener(this)
        binding.gpsMore.setOnClickListener(this)
    }


    private var bleCallbackInited = true
    private var sppCallbackInited = true

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()

        if (checkEnvironment() == 0) {
            binding.tipsRl.visibility = View.GONE
            devices.clear()
            mDeviceAdapter?.notifyDataSetChanged()
            with(requireContext()) {
                filterRssi = getInt("rssiValue", -100)
                filterNameSwitch = getBoolean("nameSwitch", false)
                filterName = if (filterNameSwitch) {
                    getStr("nameValue", "")
                } else {
                    ""
                }
                if (filterRssi > -100 || filterNameSwitch) {
                    binding.filterIv.setImageResource(R.drawable.icon_filtter_ing)
                } else {
                    binding.filterIv.setImageResource(R.drawable.icon_filtter_none)
                }
            }

            if (bleCallbackInited) {
                bleCallbackInited = true
                // 修复BLE回调：使用可空Binding + 状态检查
                mFscBleCentralApi.setCallbacks(object : FscBleCentralCallbacksImp() {
                    override fun blePeripheralFound(
                        bleDevice: FscDevice,
                        rssi: Int,
                        record: ByteArray?
                    ) {
                        super.blePeripheralFound(bleDevice, rssi, record)
                        safeUiAction {
                            // 核心修复2：使用可空Binding判断选中状态
                            if (binding.bleCheck?.isChecked == true) {

                                uiFoundDevice(bleDevice)

                            }
                        }

                    }

                    override fun startScan() {
                        super.startScan()

                    }

                    override fun stopScan() {
                        super.stopScan()
                        safeUiAction {
                            val nullableBinding = getNullableBinding()
                            if (nullableBinding != null && nullableBinding.bleCheck?.isChecked == true) {
                                uiStopScan()
                            }
                        }
                    }
                })
            }


            if (sppCallbackInited) {
                sppCallbackInited = true
                // 修复SPP回调：同样使用可空Binding + 状态检查
                sFscSppCentralApi.setCallbacks(object : FscSppCentralCallbacksImp() {
                    override fun sppPeripheralFound(sppDevice: FscDevice, rssi: Int) {
                        super.sppPeripheralFound(sppDevice, rssi)

                        safeUiAction {
                            if (binding.sppCheck?.isChecked == true) {
                                uiFoundDevice(sppDevice)
                            }
                        }
                    }

                    override fun startScan() {
                        super.startScan()

                    }

                    override fun stopScan() {
                        super.stopScan()
                        safeUiAction {
                            val nullableBinding = getNullableBinding()
                            if (nullableBinding != null && nullableBinding.sppCheck?.isChecked == true) {
                                uiStopScan()
                            }
                        }

                    }

                    override fun sppPeripheralConnected(
                        device: BluetoothDevice,
                        connectType: ConnectType
                    ) {
                        safeUiAction {
                            stopScan()
                            // 修复：使用requireContext()而非activity，避免空指针
                            ThroughputActivity.activityStart(requireContext(), device)
                        }
                    }
                })
            }

            binding.smartRefreshLayout.autoRefresh()

        } else if (checkEnvironment() == -3) {
            binding.tipsRl.visibility = View.VISIBLE
        }
    }

    private fun requestPermissions() {
        XXPermissions.with(this)
            .permission(PermissionLists.getAccessFineLocationPermission())
            .permission(PermissionLists.getBluetoothConnectPermission())
            .permission(PermissionLists.getBluetoothScanPermission())
            .permission(PermissionLists.getNearbyWifiDevicesPermission())
            .interceptor(PermissionInterceptor())
            .description(PermissionDescription())
            .request(OnPermissionCallback { grantedList, deniedList ->
                val allGranted = deniedList.isEmpty()
                if (!allGranted) {
                    return@OnPermissionCallback
                }
                binding.smartRefreshLayout.autoRefresh()
            })
    }

    @SuppressLint("MissingPermission", "NotifyDataSetChanged")
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.sortLinearLayout -> {
                if (!ClickUtil.isFastClick()) {
                    with(devices) {
                        val pairedDevices = filter {
                            it.device.bondState == BluetoothDevice.BOND_BONDED
                        }
                        val unpairedDevice = filterNot {
                            it.device.bondState == BluetoothDevice.BOND_BONDED
                        }.sortedByDescending {
                            it.rssi
                        }
                        clear()
                        addAll(pairedDevices)
                        addAll(unpairedDevice)
                    }
                    mDeviceAdapter?.notifyDataSetChanged()
                }
            }

            R.id.filterLinearLayout -> {
                FilterActivity.activityStart(requireContext())
            }

            R.id.toolbarImageButton -> {
                MainActivity.activityStart(requireContext(), 1)
                requireActivity().finish()
            }

            R.id.applyTv -> {
                if (checkEnvironment() == -3) {
                    requestPermissions()
                }
            }

            R.id.bluetooth_enable -> {
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(intent, 1)
            }

            R.id.gps_enable -> {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(intent, 2)
            }

            R.id.gps_more -> {
                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.gps_more_title)) //设置对话框标题
                    .setMessage(getString(R.string.gps_more_text_bluetooth)) //设置显示的内容
                    .setPositiveButton("OK") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                    .show() //显示此对话框
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        if (checkEnvironment() == 0) {
            devices.clear()
            mDeviceAdapter?.notifyDataSetChanged()
            binding.smartRefreshLayout.autoRefresh()
        } else if (checkEnvironment() == -3) {
            ToastUtils.showShort(getString(R.string.insufficient_permissions))
        }
    }

    private fun checkEnvironment(): Int {
        if (!PermissionUtil.hasRequiredPermissions(requireContext())) {
            binding.tipsRl.visibility = View.VISIBLE
            return -3
        }
        binding.tipsRl.visibility = View.GONE
        return 0
    }

    private fun startScan() {
        if (!PermissionUtil.hasRequiredPermissions(requireContext())) {
            return
        }
        try {
            if (binding.bluetooth.visibility == View.VISIBLE || binding.gps.visibility == View.VISIBLE) {
                return
            }
            uiStartScan()

            sFscSppCentralApi.stopScan()
            mFscBleCentralApi.stopScan()
            mHandler.removeCallbacks(mScanSppRunnable)
            mHandler.removeCallbacks(mScanBleRunnable)
            if (binding.headerRightLL.checkedRadioButtonId == R.id.bleCheck) {
                LogUtils.e(TAG, "APP BLE startScan: ble扫描.")
                mHandler.postDelayed(mScanBleRunnable, 200)

            } else {
                LogUtils.e(TAG, "APP SPP startScan: spp扫描.")
                mHandler.postDelayed(mScanSppRunnable, 200)

            }

        } catch (e: Exception) {
            e.printStackTrace()
            LogUtils.e(TAG, "扫描. erro: ${e.toString()}")
        }
    }

    private val mScanBleRunnable = Runnable {
        mFscBleCentralApi.startScan()
    }

    private val mScanSppRunnable = Runnable {
        sFscSppCentralApi.startScan()
    }

    private fun stopScan() {
        try {
            if (binding.bluetooth.visibility == View.VISIBLE || binding.gps.visibility == View.VISIBLE) {
                return
            }
            sFscSppCentralApi.stopScan()
            mFscBleCentralApi.stopScan()
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtils.e(TAG, "stopScan. erro: ${e.toString()}")
        }
    }

    // 全局定义一个原生进度圈
    private var mProgressDialog: android.app.ProgressDialog? = null

    private fun uiStartScan() {
        if (isAdded) {
            binding.toolbarSubtitle.text = resources.getString(R.string.searching)

            // ========== 原生进度圈（替换你原来的 mWaitDialog）==========
            activity?.let {
                if (mProgressDialog == null) {
                    mProgressDialog = android.app.ProgressDialog(it)
                    mProgressDialog?.setCancelable(false) // 不可取消
                }
                mProgressDialog?.setMessage("Loading...")
                if (!mProgressDialog!!.isShowing) {
                    mProgressDialog?.show()
                }
            }

            // 2秒后自动关闭
            mHandler.postDelayed({
                if (isAdded) {
                    mProgressDialog?.dismiss()
                }
            }, 2000)
        }
    }

    private fun uiStopScan() {
        if (isAdded) {
            binding.toolbarSubtitle?.text = resources.getString(R.string.searched)
        }
    }

    @SuppressLint("MissingPermission", "SuspiciousIndentation")
    private fun uiFoundDevice(fscDevice: FscDevice) {
        try {
            Log.e("vvvv","==========${fscDevice.name}")
            // 1. RSSI 过滤
            if (fscDevice.rssi > 0 || fscDevice.rssi < -100) {
                return
            }

            // 2. 名称过滤（安全判空，修复 NPE）
            if (filterNameSwitch && filterName.isNotEmpty()) {
                // 安全获取设备名称，为空则返回空字符串
                val deviceName = fscDevice.device.name?.uppercase() ?: "unknown"
                val targetName = filterName.uppercase()

                // 不包含则跳过
                if (!deviceName.contains(targetName)) {
                    return
                }
            }

            // 3. RSSI 强度过滤 + 更新列表
            if (fscDevice.rssi > filterRssi) {
                val index = devices.indexOf(fscDevice)
                if (index == -1) {
                    devices.add(fscDevice)
                    binding.deviceRecyclerView?.adapter?.notifyItemInserted(devices.size - 1)
                } else {
                    devices[index] = fscDevice
                    binding.deviceRecyclerView?.adapter?.notifyItemChanged(index)
                }
            }

        } catch (e: Exception) {

        }

    }

    private val mLocationProviderChangedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val enabled: Boolean = isLocationEnabled(requireContext())
            if (enabled) {
                stopScan()
            }
            binding.gps.visibility = if (enabled) View.GONE else View.VISIBLE
        }
    }

    private val mBluetoothStateBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
            val previousState = intent.getIntExtra(
                BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF
            )
            when (state) {
                BluetoothAdapter.STATE_ON -> {
                    // 记录一下，这里添加了stopScan()代码是因为关闭了蓝牙刷新界面后，再重新打开蓝牙没有数据
                    stopScan()
                    binding.bluetooth.visibility = View.GONE
                }

                BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> if (previousState != BluetoothAdapter.STATE_TURNING_OFF && previousState != BluetoothAdapter.STATE_OFF) {
                    stopScan()
                    binding.bluetooth.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun registerBroadcastReceivers() {
        requireContext().registerReceiver(
            mBluetoothStateBroadcastReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
        if (isMarshmallowOrAbove()) {
            requireContext().registerReceiver(
                mLocationProviderChangedReceiver, IntentFilter(LocationManager.MODE_CHANGED_ACTION)
            )
        }
    }

    private fun unregisterBroadcastReceivers() {
        requireContext().unregisterReceiver(mBluetoothStateBroadcastReceiver)
        if (isMarshmallowOrAbove()) {
            requireContext().unregisterReceiver(mLocationProviderChangedReceiver)
        }
    }


    override fun onPause() {
        super.onPause()
        if (mProgressDialog?.isShowing == true) {
            mProgressDialog?.dismiss() // 关闭原生进度圈
        }
        stopScan()
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterBroadcastReceivers()
        BluetoothRepository.unRegisterViewCallback(this)
        mHandler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val TAG = "CommunicationFragment"

        fun newInstance(): CommunicationFragment {
            return CommunicationFragment()
        }
    }
}