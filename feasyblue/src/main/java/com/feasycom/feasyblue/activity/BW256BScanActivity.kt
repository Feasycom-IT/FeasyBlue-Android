package com.feasycom.feasyblue.activity

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.net.wifi.p2p.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.LogUtils
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.adapter.BW256BAdapter
import com.feasycom.feasyblue.databinding.ActivityScanBw256bBinding
import com.feasycom.feasyblue.dialog.LoadDialogFragment
import com.feasycom.feasyblue.interfaces.OnDataReceivedListener
import com.feasycom.feasyblue.utils.*
import com.feasycom.wifi.utils.BwNetUtils
import com.scwang.smartrefresh.layout.SmartRefreshLayout

class BW256BScanActivity : BaseActivity<ActivityScanBw256bBinding>(), View.OnClickListener {

    private lateinit var bW256BAdapter: BW256BAdapter
    private lateinit var wifiP2pManager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private var receiver: BroadcastReceiver? = null
    private lateinit var mWaitDialog: LoadDialogFragment
    private val handler = Handler(Looper.getMainLooper())
    private val mWifiStateLiveData = MutableLiveData<Boolean>()
    private val mConnectStateLiveData = MutableLiveData<Boolean>()

    // ViewBinding 替代 findViewById/lazy
    private val cancelP2pConnect: Button get() = binding.cancelP2pConnect
    private val tcpConnectBtn: Button get() = binding.tcpConnectBtn
    private val refreshableView: SmartRefreshLayout get() = binding.refreshableView
    private val deviceRecycler: RecyclerView get() = binding.deviceRecycler

    // ========== 核心修复：直接通过根布局获取Header子View ==========
    // 不再使用HeaderBinding，直接从Activity的binding中获取Header的子控件
    private val headerTitleText get() = binding.header.titleText
    private val headerBackGroup get() = binding.header.backGroup
    private val headerBackTv get() = binding.header.backTv
    private val headerBackImg get() = binding.header.backImg

    val mConnectTimeout = Runnable {
        if (::mWaitDialog.isInitialized && mWaitDialog.isVisible) {
            mWaitDialog.dismiss()
        }
    }

    /**
     * 实现父类的 ViewBinding 获取方法（核心适配点）
     */
    override fun getViewBinding(): ActivityScanBw256bBinding {
        return ActivityScanBw256bBinding.inflate(layoutInflater)
    }

    /**
     * 初始化View（父类抽象方法）
     */
    override fun initView() {
        // 检查Wi-Fi Direct支持
        checkWifiP2PSupport()

        // 初始化Wifi P2P
        initWifiP2P()

        // 设置视图
        setupViews()

        // 自动刷新
        refreshableView.autoRefresh()
    }

    /**
     * 检查设备是否支持Wi-Fi Direct
     */
    private fun checkWifiP2PSupport() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            LogUtils.d(TAG, "APP WIFI P2P Device does not support Wi-Fi Direct")
        } else {
            LogUtils.d(TAG, "APP WIFI P2P Device does support Wi-Fi Direct")
        }
    }

    /**
     * 初始化Wifi P2P
     */
    private fun initWifiP2P() {
        wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = wifiP2pManager.initialize(this, Looper.getMainLooper(), null)
    }

    /**
     * 设置视图和事件
     */
    private fun setupViews() {
        val isUpgrade = intent.getIntExtra("isUpgrade", -1)

        // 设置Header (核心修复：直接使用binding.header的子控件)
        setupHeader(isUpgrade)

        // 设置刷新控件
        setupRefreshLayout()

        // 设置RecyclerView
        setupRecyclerView()
    }

    /**
     * 设置Header (核心修复：移除HeaderBinding，直接操作View)
     */
    private fun setupHeader(isUpgrade: Int) {
        headerTitleText.text = getString(R.string.wlan_direct_connection)
        headerBackGroup.visibility = View.VISIBLE
        LogUtils.d(TAG, "APP WIFI P2P isUpgrade => $isUpgrade")

        if (isUpgrade != -1) {
            disconnectP2PGroup()
        }

        // 返回按钮事件 (直接绑定View的点击事件)
        headerBackTv.setOnClickListener(this)
        headerBackImg.setOnClickListener(this)
    }

    /**
     * 设置刷新布局
     */
    private fun setupRefreshLayout() {
        refreshableView.setOnRefreshListener {
            initializeWifiP2p()
            it.closeHeaderOrFooter()
        }
    }

    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        bW256BAdapter = BW256BAdapter(mutableListOf(), object : BW256BAdapter.OnDeviceClickListener {
            override fun onDeviceClick(device: WifiP2pDevice) {
                val macAddress = device.deviceAddress
                LogUtils.d(TAG, "APP WIFI P2P setupRecyclerView macAddress => $macAddress")
                putStr("macAddress", macAddress)

                // 处理设备点击事件
                handler.postDelayed(mConnectTimeout, 5000)
                mWaitDialog = LoadDialogFragment.newInstance(getString(R.string.start_connect))
                mWaitDialog.show(supportFragmentManager, "connect")
                BwNetUtils.showToast(this@BW256BScanActivity, getString(R.string.click_device, device.deviceName))
                connectToDevice(device)
            }
        })

        deviceRecycler.layoutManager = LinearLayoutManager(this)
        deviceRecycler.adapter = bW256BAdapter
    }

    /**
     * 连接到指定设备
     */
    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }

        wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // 连接请求已成功发送到对方设备
                BwNetUtils.showToast(
                    this@BW256BScanActivity, getString(R.string.connect_to, device.deviceName)
                )
            }

            override fun onFailure(reason: Int) {
                // 连接请求失败
                if (::mWaitDialog.isInitialized && mWaitDialog.isVisible) {
                    mWaitDialog.dismiss()
                }
                BwNetUtils.showToast(
                    this@BW256BScanActivity,
                    getString(R.string.p2p_connect_fail, getFailureReason(reason))
                )
            }
        })
    }

    /**
     * 断开P2P组连接
     */
    private fun disconnectP2PGroup() {
        wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                BwNetUtils.showToast(this@BW256BScanActivity, getString(R.string.removed_group))
                updateHeaderText(getString(R.string.wlan_direct_connection))
                refreshableView.autoRefresh()
                cancelP2pConnect.isEnabled = false
                tcpConnectBtn.isEnabled = false
            }

            override fun onFailure(reason: Int) {
                BwNetUtils.showToast(
                    this@BW256BScanActivity, getString(R.string.removed_group_failed, "$reason")
                )
            }
        })
    }

    /**
     * 初始化Wifi P2P并设置广播接收器
     */
    private fun initializeWifiP2p() {
        setupReceiver()
        if (!isWifiEnabled()) {
            openWifiSettings()
        } else {
            findAndConnectToServices()
        }
    }

    /**
     * 设置广播接收器
     */
    private fun setupReceiver() {
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }

        // 先注销旧的接收器
        if (receiver != null) {
            try {
                unregisterReceiver(receiver)
            } catch (e: Exception) {
                LogUtils.e(TAG, "注销广播接收器失败: ${e.message}")
            }
        }

        // 创建新的广播接收器
        receiver = WifiDirectBroadcastReceiver(
            wifiP2pManager,
            channel,
            { peerList -> onPeersAvailable(peerList) },
            object : OnDataReceivedListener {
                override fun onDataReceived(host: String, port: Int) {
                    if (::mWaitDialog.isInitialized && mWaitDialog.isVisible) {
                        mWaitDialog.dismiss()
                        runOnUiThread {
                            LogUtils.d(TAG, "APP WIFI P2P setupReceiver host => $host  port => $port")
                            putStr("host", host)
                            putInt("port", port)
                            if (host.isNotEmpty() && port != -1) {
                                BW256BCommTCPActivity.activityStart(this@BW256BScanActivity, host, port)
                                finish()
                            }
                        }
                    }
                }
            },
            mWifiStateLiveData,
            mConnectStateLiveData
        )

        // 注册新的接收器
        registerReceiver(receiver, intentFilter)
    }

    /**
     * Wifi Direct广播接收器
     */
    inner class WifiDirectBroadcastReceiver(
        private val wifiP2pManager: WifiP2pManager,
        private val channel: WifiP2pManager.Channel,
        private val onPeersAvailableCallback: (WifiP2pDeviceList) -> Unit,
        private val onDataReceivedListener: OnDataReceivedListener,
        private val mWifiStateLiveData: MutableLiveData<Boolean>,
        private val mConnectStateLiveData: MutableLiveData<Boolean>
    ) : BroadcastReceiver() {

        @SuppressLint("MissingPermission")
        @RequiresApi(Build.VERSION_CODES.R)
        override fun onReceive(context: Context, intent: Intent) {
            var serviceAddress = ""
            LogUtils.d(TAG, "APP WIFI P2P intent.action => ${intent.action}")

            when (intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                        // Wi-Fi P2P已启用
                        LogUtils.d(TAG, "APP WIFI P2P onReceive Wi-Fi P2P is enabled state => $state")
                        mWifiStateLiveData.postValue(true)
                    } else if (state == WifiP2pManager.WIFI_P2P_STATE_DISABLED) {
                        // Wi-Fi P2P已禁用
                        LogUtils.d(TAG, "APP WIFI P2P onReceive Wi-Fi P2P is disabled state => $state")
                        mWifiStateLiveData.postValue(false)
                    }
                }

                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    // 从wifiP2pManager请求可用对等体
                    wifiP2pManager.requestPeers(channel) { peerList ->
                        onPeersAvailableCallback(peerList)
                    }
                }

                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo: NetworkInfo? = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)

                    if (networkInfo != null && networkInfo.isConnected) {
                        // 已连接到对等体
                        wifiP2pManager.requestConnectionInfo(channel) { info ->
                            if (info != null && info.groupOwnerAddress != null) {
                                serviceAddress = info.groupOwnerAddress.hostAddress ?: ""
                                LogUtils.d(TAG, "APP WIFI P2P onReceive Group Owner IP - $serviceAddress")
                                onDataReceivedListener.onDataReceived(serviceAddress, 9100)
                                mConnectStateLiveData.postValue(true)
                            }
                        }
                    } else {
                        // 与对等体断开连接
                        LogUtils.d(TAG, "APP WIFI P2P onReceive p2p Disconnected.")
                        mConnectStateLiveData.postValue(false)
                    }
                }
            }
        }
    }

    /**
     * 发现可用的P2P设备
     */
    @SuppressLint("MissingPermission")
    private fun findAndConnectToServices() {
        wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                BwNetUtils.showToast(this@BW256BScanActivity, getString(R.string.discovery_success))
            }

            override fun onFailure(reasonCode: Int) {
                val message = when (reasonCode) {
                    WifiP2pManager.P2P_UNSUPPORTED -> getString(R.string.p2p_unsupported, "$reasonCode")
                    WifiP2pManager.BUSY -> getString(R.string.busy, "$reasonCode")
                    WifiP2pManager.ERROR -> getString(R.string.p2p_error, "$reasonCode")
                    else -> getString(R.string.busy, "$reasonCode")
                }
                BwNetUtils.showToast(this@BW256BScanActivity, message)
            }
        })
    }

    /**
     * 检查Wi-Fi是否启用
     */
    @SuppressLint("WifiManagerLeak")
    private fun isWifiEnabled(): Boolean {
        return try {
            val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.isWifiEnabled
        } catch (e: Exception) {
            LogUtils.e(TAG, "检查Wi-Fi状态失败: ${e.message}")
            false
        }
    }

    /**
     * 打开Wi-Fi设置页面
     */
    private fun openWifiSettings() {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        startActivityForResult(intent, REQUEST_ENABLE_WIFI)
    }

    /**
     * 处理可用设备列表更新
     */
    private fun onPeersAvailable(peerList: WifiP2pDeviceList) {
        runOnUiThread {
            bW256BAdapter.updateDeviceList(peerList.deviceList)
        }
    }

    /**
     * 获取连接失败原因
     */
    private fun getFailureReason(reason: Int): String {
        return when (reason) {
            WifiP2pManager.P2P_UNSUPPORTED -> getString(R.string.p2p_not_supported)
            else -> getString(R.string.unknown_error, "$reason")
        }
    }

    /**
     * 更新Header标题文本
     */
    private fun updateHeaderText(text: String) {
        runOnUiThread {
            headerTitleText.text = text
        }
    }

    /**
     * 点击事件处理
     */
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.back_tv, R.id.backImg -> {
                disconnectP2PGroup()
                finish()
            }
        }
    }

    /**
     * 页面恢复时更新UI状态
     */
    override fun onResume() {
        super.onResume()
        val isP2pConnectStatus = getBoolean("isP2pConnectStatus", false)
        if (isP2pConnectStatus) {
            headerTitleText.text = getString(R.string.p2p_connect_success)
            cancelP2pConnect.isEnabled = true
            tcpConnectBtn.isEnabled = true
        } else {
            headerTitleText.text = getString(R.string.wlan_direct_connection)
            cancelP2pConnect.isEnabled = false
            tcpConnectBtn.isEnabled = false
        }
    }

    /**
     * 页面销毁时清理资源
     */
    override fun onDestroy() {
        super.onDestroy()

        // 移除Handler回调
        handler.removeCallbacks(mConnectTimeout)

        // 注销广播接收器
        if (receiver != null) {
            try {
                unregisterReceiver(receiver)
                receiver = null
            } catch (e: Exception) {
                LogUtils.e(TAG, "注销广播接收器失败: ${e.message}")
            }
        }

        // 关闭等待对话框
        if (::mWaitDialog.isInitialized && mWaitDialog.isVisible) {
            mWaitDialog.dismiss()
        }
    }

    companion object {
        const val TAG: String = "DeviceFragment"
        const val REQUEST_ENABLE_WIFI = 1

        /**
         * 启动BW256B扫描页面
         */
        fun activityStart(context: Context, isUpgrade: Int) {
            val intent = Intent(context, BW256BScanActivity::class.java)
            intent.putExtra("isUpgrade", isUpgrade)
            context.startActivity(intent)
        }
    }
}