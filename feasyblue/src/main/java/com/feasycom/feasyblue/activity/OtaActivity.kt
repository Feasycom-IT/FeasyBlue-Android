package com.feasycom.feasyblue.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.LogUtils
import com.feasycom.ble.controler.FscBleCentralApi
import com.feasycom.ble.controler.FscBleCentralApiImp
import com.feasycom.ble.controler.FscBleCentralCallbacksImp
import com.feasycom.common.bean.FscDevice
import com.feasycom.common.utils.Constant
import com.feasycom.common.utils.MsgLogger
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.dao.DeviceDatabase
import com.feasycom.feasyblue.databinding.ActivityOtaBinding
import com.feasycom.feasyblue.dialog.DfuNameDialogFragment
import com.feasycom.feasyblue.dialog.LoadDialogFragment
import com.feasycom.feasyblue.dialog.ProgressBarDialogFragment
import com.feasycom.feasyblue.utils.*
import com.feasycom.network.DeviceNetwork
import com.feasycom.ota.utils.FileUtil
import com.feasycom.ota.utils.OtaUtils
import com.feasycom.ota.utils.XmodemUtils.Companion.OTA_STATUS_BEGIN
import com.feasycom.ota.utils.XmodemUtils.Companion.OTA_STATUS_FAILED
import com.feasycom.ota.utils.XmodemUtils.Companion.OTA_STATUS_FINISH
import com.feasycom.ota.utils.XmodemUtils.Companion.OTA_STATUS_PROCESSING
import com.feasycom.ota.utils.XmodemUtils.Companion.OTA_STATUS_VERIFY_APP_VERSION_FAILED
import com.feasycom.ota.utils.XmodemUtils.Companion.OTA_STATUS_VERIFY_FAILED
import com.feasycom.ota.utils.XmodemUtils.Companion.OTA_STATUS_VERIFY_MODEL_FAILED
import com.feasycom.spp.controler.FscSppCentralApi
import com.feasycom.spp.controler.FscSppCentralApiImp
import com.feasycom.spp.controler.FscSppCentralCallbacksImp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.*
import java.net.UnknownHostException
import java.text.DecimalFormat

/**
 * OTA升级页面
 * 适配优化后的泛型 ViewBinding BaseActivity
 */
class OtaActivity : BaseActivity<ActivityOtaBinding>(), View.OnClickListener {

    // 设备和SDK相关变量
    private lateinit var mFscDevice: FscDevice
    private lateinit var mSppCentralApi: FscSppCentralApi
    private lateinit var mBleCentralApi: FscBleCentralApi

    // LiveData 用于观察文件相关数据变化
    private val mFileNameLiveData = MutableLiveData<String>()
    private val mFileUriLiveData = MutableLiveData<Uri>()
    private val mFileByteArrayLiveData = MutableLiveData<ByteArray>()

    // 对话框相关
    private val mProgress = ProgressBarDialogFragment()
    private var dialog: androidx.appcompat.app.AlertDialog? = null
    private var mWaitDialog: LoadDialogFragment? = null
    private var isUpgrading = false

    // 类型模式
    private var typeMode = -1

    // Handler 用于主线程更新UI
    private val mHandler = Handler(Looper.getMainLooper()) { msg ->
        when (msg.what) {
            START_DOWNLOAD -> mProgress.show(supportFragmentManager, "ota")
            DOWNLOAD_FINISH -> mProgress.dismiss()
            SHOW_DIALOG -> {
                mProgress.dismiss()
                showDialog(this, msg.data.getString("message", ""))
            }

            UPDATE_FAILED -> {
                binding.otaState.text = getString(R.string.updateFailed)
                binding.timeCount.stop()
            }
        }
        false
    }

    // 数据库DAO
    private val deviceDao by lazy { DeviceDatabase.getDataBase(this).deviceDao() }

    /**
     * 实现父类的 ViewBinding 获取方法（核心适配点）
     */
    override fun getViewBinding(): ActivityOtaBinding {
        return ActivityOtaBinding.inflate(layoutInflater)
    }

    @SuppressLint("StringFormatInvalid")
    override fun initView() {


        // 初始化设备信息
        initDeviceInfo()

        // 初始化Toolbar
        initToolbar()

        // 初始化SDK
        initSDK()

        // 设置事件监听
        setEvent()

        // 设置LiveData观察
        setObserve()
    }

    /**
     * 初始化设备信息
     */
    private fun initDeviceInfo() {
        mFscDevice = intent?.getParcelableExtra("FscDevice") ?: run {
            finish()
            return
        }

        if (!::mFscDevice.isInitialized) finish()
    }

    /**
     * 初始化Toolbar
     */
    private fun initToolbar() {
        setSupportActionBar(binding.otaToolbar)
        binding.otaTitle.setText(R.string.OTAUpgrade)

        supportActionBar?.let {
            it.setDisplayShowTitleEnabled(false)
            it.setDisplayHomeAsUpEnabled(true)
        }

        binding.otaToolbar.setNavigationOnClickListener {
            disconnect()
            finish()
        }
    }

    /**
     * 初始化SDK（BLE/SPP）
     */
    private fun initSDK() {
        if (mFscDevice.mode == Constant.BLE_MODE) {
            // BLE模式初始化
            mBleCentralApi = FscBleCentralApiImp.getInstance()
            mBleCentralApi.setCallbacks(object : FscBleCentralCallbacksImp() {
                override fun packetSend(address: String?, strValue: String?, data: ByteArray?) {
                    super.packetSend(address, strValue, data)
                }

                override fun otaProgressUpdate(address: String, percentage: Float, status: Int) {
                    uiOTAProgressUpdate(percentage, status)
                }

                override fun packetReceived(
                    address: String,
                    strValue: String,
                    dataHexString: String,
                    data: ByteArray
                ) {
                    LogUtils.e(
                        TAG, "FeasyBlue APP BLE OTA packetReceived: address => $address \n" +
                                " strValue => $strValue \n" +
                                " data => ${data.bytesToHex()}."
                    )
                    uiReceived(strValue)
                }
            })
            binding.clear.visibility = View.GONE
        } else {
            // SPP模式初始化
            mSppCentralApi = FscSppCentralApiImp.getInstance()
            mSppCentralApi.setCallbacks(object : FscSppCentralCallbacksImp() {
                override fun packetSend(
                    address: String?,
                    strValue: String?,
                    packetReceived: String?,
                    data: ByteArray?
                ) {
                    super.packetSend(address, strValue, packetReceived, data)
                    LogUtils.e(
                        TAG, "FeasyBlue APP SPP OTA packetSend: address => $address \n" +
                                " strValue => $strValue \n" +
                                " data => ${data?.bytesToHex()}."
                    )
                }

                override fun otaProgressUpdate(address: String?, percentage: Float, status: Int) {
                    uiOTAProgressUpdate(percentage, status)
                }

                override fun packetReceived(
                    address: String,
                    strValue: String,
                    hexString: String,
                    data: ByteArray
                ) {
                    LogUtils.e(
                        TAG, "FeasyBlue APP SPP OTA packetReceived: address => $address \n" +
                                " strValue => $strValue \n" +
                                " data => ${data.bytesToHex()}."
                    )
                    uiReceived(strValue)
                }

                override fun bondIng() {
                    binding.startOtaButton.isEnabled = false
                }
            })
        }
    }

    /**
     * 设置点击事件
     */
    private fun setEvent() {
        binding.selectFileButton.setOnClickListener(this)
        binding.downloadButton.setOnClickListener(this)
        binding.startOtaButton.setOnClickListener(this)
    }

    /**
     * 设置LiveData观察
     */
    @SuppressLint("SuspiciousIndentation")
    private fun setObserve() {
        // 观察文件URI变化
        mFileUriLiveData.observe(this) { uri ->
            putStr("fileUri", uri.toString())
            getUriInfo(uri)
        }

        // 观察文件名变化
        mFileNameLiveData.observe(this) { fileName ->
            binding.fileNameTv.text = fileName
        }

        // 观察文件字节数组变化
        mFileByteArrayLiveData.observe(this) { byteArray ->
            // 更新文件大小
            binding.fileSizeTv.text = getString(R.string.file_size, byteArray.size)

            // 解析DFU文件信息
            getDfuFileInformation(byteArray)?.let { dfuFileInfo ->
                // 更新DFU信息
                typeMode = dfuFileInfo.type_model

                binding.dfuBootloader.text = dfuFileInfo.bootloader.toString()
                binding.dfuVersion.text =
                    "${dfuFileInfo.versionStart}-${dfuFileInfo.version_soft_end}"

                Log.e(
                    TAG,
                    "FeasyBlue APP OTA setObserve typeMode => $typeMode    ${dfuFileInfo.bootloader}"
                )

                // 查询设备信息
                lifecycleScope.launch(Dispatchers.IO) {
                        val deviceInfo = deviceDao.queryDeviceByNumber(typeMode)
                    withContext(Dispatchers.Main) {
                        binding.dfuModelName.text = deviceInfo?.name ?: "unknown"
                        if (!isUpgrading) {
                            isUpgrading = true
                            binding.startOtaButton.isEnabled = true
                            binding.otaState.text = getString(R.string.please_start_the_upgrade)
                        }
                    }
                }
            } ?: let {
                // DFU文件解析失败
                binding.otaState.text = getString(R.string.dfu_error)
                binding.startOtaButton.isEnabled = false
                binding.dfuModelName.text = "unknown"
                binding.dfuVersion.text = "unknown"
                binding.dfuBootloader.text = "unknown"
            }
        }
    }

    /**
     * 恢复时获取文件URI
     */
    override fun onResume() {
        super.onResume()
        getFileUri()
        // 保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    }

    /**
     * 从SharedPreferences获取文件URI
     */
    private fun getFileUri() {
        getStr("fileUri", "").takeIf { it.isNotEmpty() }?.let {
            mFileUriLiveData.value = Uri.parse(it)
        }
    }


    /**
     * OTA进度更新UI处理
     */
    @SuppressLint("StringFormatInvalid")
    fun uiOTAProgressUpdate(percentage: Float, status: Int) {
        runOnUiThread {
            if (percentage > 0) {
                // 更新进度条和百分比
                binding.otaProgress.progress = percentage.toInt()
                val df = if (percentage < 1) DecimalFormat("0.00") else DecimalFormat("#.00")
                binding.progressCount.text = "${df.format(percentage)}"
            }

            // 根据状态更新UI
            when (status) {
                OTA_STATUS_FINISH -> {
                    binding.otaState.text = getString(R.string.updateSuccess)
                    if (binding.clear.isChecked) {
                        mSppCentralApi.clearDevice(mFscDevice.address)
                    }
                    binding.timeCount.stop()
                    if (typeMode != 99) disconnect()
                    updateClickStatus()
                }

                OTA_STATUS_FAILED -> {
                    LogUtils.e(TAG, "APP OTA 升级失败.")
                    binding.otaState.text = getString(R.string.updateFailed)
                    binding.timeCount.stop()
                    updateClickStatus()
                }

                OTA_STATUS_BEGIN -> {
                    binding.otaState.text = getString(R.string.waitingForUpdate)
                    isUpgrading = true
                }

                OTA_STATUS_PROCESSING -> {
                    binding.otaState.text = getString(R.string.updating)
                }

                OTA_STATUS_VERIFY_MODEL_FAILED -> {
                    binding.otaState.text = getString(R.string.nameNotMatch)
                    binding.timeCount.stop()
                    listOf(
                        binding.selectFileButton,
                        binding.downloadButton,
                        binding.clear,
                        binding.resetFlag
                    ).forEach {
                        it.isEnabled = true
                    }
                }

                OTA_STATUS_VERIFY_APP_VERSION_FAILED -> {
                    binding.otaState.text = getString(R.string.versionIsLow)
                    binding.timeCount.stop()
                    updateClickStatus()
                }

                OTA_STATUS_VERIFY_FAILED -> {
                    binding.otaState.text = getString(R.string.versionIsHigh)
                    binding.timeCount.stop()
                    updateClickStatus()
                }
            }
        }
    }

    /**
     * 更新控件可点击状态
     */
    private fun updateClickStatus() {
        listOf(
            binding.startOtaButton, binding.selectFileButton, binding.downloadButton,
            binding.clear, binding.resetFlag
        ).forEach { it.isEnabled = true }
    }

    /**
     * 收到数据包UI处理
     */
    fun uiReceived(strValue: String) {
        Log.e(TAG, "=======uiReceived=========$strValue")
        if (strValue.contains("OK")) {
            runOnUiThread {
                if (strValue.length >= 15) {
                    // 解析设备信息
                    val moduleVersionInt =
                        Integer.valueOf(FileUtil.stringToInt(strValue.substring(3, 7)))
                    val moduleBootloaderInt =
                        Integer.valueOf(FileUtil.stringToInt(strValue.substring(7, 11)))

                    // 更新UI
                    binding.moduleVersion.text = moduleVersionInt.toString()
                    binding.moduleBootloader.text = moduleBootloaderInt.toString()

                    // 查询设备型号
                    lifecycleScope.launch(Dispatchers.IO) {
                        val deviceInfo =
                            deviceDao.queryDeviceByNumber(stringToInt(strValue.substring(11, 15)))
                        withContext(Dispatchers.Main) {
                            binding.moduleModelName.text = deviceInfo?.name ?: "unknown"
                        }
                    }
                } else {
                    // 数据不完整
                    binding.moduleVersion.text = "unknown"
                    binding.moduleBootloader.text = "unknown"
                    binding.moduleModelName.text = "unknown"
                }
            }
        }
    }

    /**
     * 获取URI对应的文件信息
     */
    private fun getUriInfo(uri: Uri) {
        when (uri.scheme) {
            "content" -> {
                try {
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            // 读取文件字节数组
                            contentResolver.openInputStream(uri)?.use {
                                mFileByteArrayLiveData.postValue(it.readBytes())
                            }
                            // 获取文件名
                            val fileName =
                                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME))
                            mFileNameLiveData.postValue(fileName)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            "file" -> {
                try {
                    uri.toFile().let { file ->
                        mFileNameLiveData.postValue(file.name)
                        mFileByteArrayLiveData.postValue(file.readBytes())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 解析DFU文件信息
     */
    private fun getDfuFileInformation(dfuFile: ByteArray) = OtaUtils.checkDfuFile(dfuFile)

    /**
     * 处理Activity返回结果
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 关闭等待对话框
        mWaitDialog?.takeIf { it.isVisible }?.dismiss()

        if (requestCode != READ_FILE_REQUEST_CODE || resultCode != RESULT_OK) return

        LogUtils.e(
            TAG,
            "APP onActivityResult requestCode => $requestCode resultCode => $resultCode"
        )

        lifecycleScope.launch(Dispatchers.IO) {
            data?.data?.let { uri ->
                try {
                    if (isQOrAbove()) {
                        // 申请持久化URI权限
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    }
                    LogUtils.e(TAG, "APP OTA fileSearch selected file URI: $uri")
                    mFileUriLiveData.postValue(uri)
                } catch (e: Exception) {
                    LogUtils.e(TAG, "onActivityResult Error handling file url: ${e.message}")
                }
            }
        }
    }

    /**
     * 权限请求结果处理
     */
    @SuppressLint("NewApi")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    createDfuDialog()
                }
            }
        }
    }

    /**
     * 点击事件处理
     */
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.selectFileButton -> {
                // 选择固件文件
                performFileSearch()
                binding.group.visibility = View.INVISIBLE
            }

            R.id.downloadButton -> {
                // 下载固件
                if (isExternalStorageLegacy()) {
                    createDfuDialog()
                } else {
                    checkPermission()
                }
                binding.group.visibility = View.INVISIBLE
            }

            R.id.startOtaButton -> {
                // 开始升级
                isUpgrading = true
                listOf(
                    binding.startOtaButton, binding.selectFileButton, binding.downloadButton,
                    binding.clear, binding.resetFlag
                ).forEach { it.isEnabled = false }

                startOta()
                binding.group.visibility = View.VISIBLE

                // 启动计时器
                binding.timeCount.apply {
                    base = SystemClock.elapsedRealtime()
                    format = "0${(SystemClock.elapsedRealtime() - base) / 1000 / 60}:%s"
                    start()
                }

                // 重置升级状态UI
                binding.otaState.text = getString(R.string.waitingForUpdate)
                binding.otaProgress.progress = 0
                binding.progressCount.text = getString(R.string.upgrade_percentage, 0)
            }
        }
    }

    /**
     * 打开文件选择器
     */
    private fun performFileSearch() {
        mWaitDialog = LoadDialogFragment.newInstance(resources.getString(R.string.wait))
        mWaitDialog?.show(supportFragmentManager, "wait")

        val intent: Intent = if (isQOrAbove()) {
            Intent(Intent.ACTION_OPEN_DOCUMENT)
        } else {
            Intent(Intent.ACTION_GET_CONTENT)
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"

        startActivityForResult(intent, READ_FILE_REQUEST_CODE)
    }

    /**
     * 检查存储权限
     */
    @SuppressLint("NewApi")
    private fun checkPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 申请权限
            dialog?.dismiss()
            dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(resources.getString(R.string.tips))
                .setMessage(resources.getString(R.string.file_access_permissions))
                .setPositiveButton(resources.getString(R.string.ok)) { dialog, _ ->
                    dialog.dismiss()
                    ActivityCompat.requestPermissions(
                        this@OtaActivity,
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE
                    )
                }.create()
            dialog?.show()
        } else {
            createDfuDialog()
        }
    }

    /**
     * 创建DFU文件名输入对话框
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createDfuDialog() {
        val dfuNameDialogFragment = DfuNameDialogFragment()
        dfuNameDialogFragment.onClickComplete = { dialog, parameter ->
            dialog.dismiss()
            if (parameter.isNotEmpty() && parameter.isNotBlank()) {
                mHandler.sendEmptyMessage(START_DOWNLOAD)
                lifecycleScope.launch(Dispatchers.IO) {
                    downloadDFU(parameter)
                }
            }
        }
        dfuNameDialogFragment.show(supportFragmentManager, "dfu")
    }

    /**
     * 下载DFU文件
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun downloadDFU(parameter: String) {
        coroutineScope {
            // 清理DFU目录
            cleanDfuDirectory()

            try {
                DeviceNetwork.downloadDFU(parameter).apply {
                    if (isExternalStorageLegacy()) {
                        // Android 10+ 分区存储
                        val contentUri = MediaStore.Downloads.getContentUri("external")
                        val contentValues = ContentValues().apply {
                            put(
                                MediaStore.Downloads.RELATIVE_PATH,
                                Environment.DIRECTORY_DOWNLOADS + "/dfu"
                            )
                            put(MediaStore.Downloads.DISPLAY_NAME, "${parameter}.dfu")
                        }

                        launch(Dispatchers.IO) {
                            contentResolver.insert(contentUri, contentValues)?.let { uri ->
                                contentResolver.openOutputStream(uri)?.use { out ->
                                    val byteArray = ByteArray(1024)
                                    var len: Int
                                    while (byteStream().read(byteArray).also { len = it } != -1) {
                                        out.write(byteArray, 0, len)
                                    }
                                }
                                mFileUriLiveData.postValue(uri)
                            }

                            runOnUiThread {
                                Toast.makeText(
                                    this@OtaActivity,
                                    getString(R.string.down_file_finish), Toast.LENGTH_LONG
                                ).show()
                            }
                            mHandler.sendEmptyMessage(DOWNLOAD_FINISH)
                        }
                        return@coroutineScope
                    } else {
                        // 传统存储方式
                        val directoryPath =
                            "${File.separator}sdcard${File.separator}Download${File.separator}dfu${File.separator}"
                        LogUtils.e(TAG, "APP OTA downloadDFU directoryPath => $directoryPath")

                        val directory = File(directoryPath)
                        if (!directory.exists()) directory.mkdirs()

                        val replaceParameter = parameter.replace("/", "_")
                        val path = "${directoryPath}${replaceParameter}.dfu"
                        LogUtils.e(TAG, "APP OTA downloadDFU path => $path")

                        val file = File(path)
                        launch(Dispatchers.IO) {
                            BufferedInputStream(byteStream()).use { input ->
                                BufferedOutputStream(FileOutputStream(file)).use { out ->
                                    var data: Int
                                    while (input.read().also { data = it } != -1) {
                                        out.write(data)
                                    }
                                }
                            }

                            runOnUiThread {
                                Toast.makeText(
                                    this@OtaActivity,
                                    getString(R.string.down_file_finish), Toast.LENGTH_LONG
                                ).show()
                            }
                            mHandler.sendEmptyMessage(DOWNLOAD_FINISH)
                            mFileUriLiveData.postValue(file.toUri())
                        }
                    }
                }
            } catch (e: HttpException) {
                val message = Message().apply {
                    what = SHOW_DIALOG
                    data.putString(
                        "message", when (e.code()) {
                            404 -> getString(R.string.down_file_error)
                            else -> getString(R.string.network_error)
                        }
                    )
                }
                mHandler.sendMessage(message)
            } catch (e: UnknownHostException) {
                val message = Message().apply {
                    what = SHOW_DIALOG
                    data.putString("message", getString(R.string.network_error))
                }
                mHandler.sendMessage(message)
            }
        }
    }

    /**
     * 清理DFU目录
     */
    private suspend fun cleanDfuDirectory() {
        withContext(Dispatchers.IO) {
            try {
                val dfuDir =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isExternalStorageLegacy()) {
                        File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            "dfu"
                        )
                    } else {
                        File(Environment.getExternalStorageDirectory(), "Download/dfu")
                    }

                if (!dfuDir.exists()) {
                    dfuDir.mkdirs()
                    return@withContext
                }

                var deletedCount = 0
                dfuDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.delete()) {
                        deletedCount++

                        // Android 10+ 从媒体库删除记录
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            try {
                                contentResolver.delete(
                                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                                    MediaStore.Downloads.DATA + "=?",
                                    arrayOf(file.absolutePath)
                                )
                            } catch (e: Exception) {
                                MsgLogger.e(
                                    TAG,
                                    "Failed to remove file from MediaStore: ${e.message}"
                                )
                            }
                        }
                    }
                }

                MsgLogger.e(TAG, "Deleted $deletedCount files from DFU directory")

            } catch (e: Exception) {
                MsgLogger.e(TAG, "Failed to clean DFU directory: ${e.message}")
            }
        }
    }

    /**
     * 开始OTA升级
     */
    private fun startOta() {
        mFileByteArrayLiveData.value?.let { byteArray ->
            try {
                val address = mFscDevice.address
                val isChecked = binding.resetFlag.isChecked

                // 根据设备模式选择对应的OTA方式
                if (mFscDevice.mode == Constant.BLE_MODE) {
                    mBleCentralApi.connectToOTAWithFactory(address, byteArray, isChecked)
                } else {
                    //先断开之前的连接（通讯页面设备中配对，然后立即进入空中升级界面升级的时候，会出现升级失败bug。因为配对后立即连接了，点击开始升级时设备还处于连接状态，目前的流程就无法继续升级）
                    mSppCentralApi.disconnect()
                    mSppCentralApi.connectToOTAWithFactory(address, byteArray, isChecked)
                }
            } catch (e: NullPointerException) {
                mHandler.sendEmptyMessage(UPDATE_FAILED)
            }
        }
    }

    /**
     * 返回键处理
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            disconnect()
            startActivity(Intent(this, OtaSearchDeviceActivity::class.java))
            finish()
            false
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    /**
     * 断开设备连接
     */
    private fun disconnect() {
        Log.e(TAG,"主动断开连接 1")
        if (mFscDevice.mode == Constant.BLE_MODE) {
            mBleCentralApi.disconnect(mFscDevice.address)
        } else {
            mSppCentralApi.disconnect(mFscDevice.address)
        }

        // 保存文件URI
        mFileUriLiveData.value?.let {
            putStr("fileUri", it.toString())
        }
    }


    override fun onPause() {
        super.onPause()
        // 取消屏幕常亮
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * 页面销毁清理资源
     */
    override fun onDestroy() {
        super.onDestroy()

        // 关闭对话框
        mWaitDialog?.takeIf { it.isVisible }?.dismiss()
        dialog?.takeIf { it.isShowing }?.dismiss()
        mProgress?.takeIf { it.isVisible }?.dismiss()

    }

    companion object {
        private const val TAG = "OtaActivity"

        // Handler消息常量
        private const val START_DOWNLOAD = 0x0001
        private const val DOWNLOAD_FINISH = 0x0002
        private const val SHOW_DIALOG = 0x0003
        private const val UPDATE_FAILED = 0x0004

        // 请求码
        private const val READ_FILE_REQUEST_CODE = 42
        private const val REQUEST_EXTERNAL_STORAGE = 1

        // 权限数组
        private val PERMISSIONS_STORAGE = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        /**
         * 启动OTA升级页面
         */
        fun activityStart(context: Context, fscDevice: FscDevice) {
            Intent(context, OtaActivity::class.java).apply {
                putExtra("FscDevice", fscDevice)
                context.startActivity(this)
            }
        }
    }
}
