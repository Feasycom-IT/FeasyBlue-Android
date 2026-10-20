package com.feasycom.feasyblue.activity

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.LogUtils
import com.feasycom.common.bean.ConnectType
import com.feasycom.common.bean.FscDevice
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivityBatchUpdateBinding
import com.feasycom.feasyblue.dialog.DfuNameDialogFragment
import com.feasycom.feasyblue.dialog.ProgressBarDialogFragment
import com.feasycom.feasyblue.utils.*
import com.feasycom.network.DeviceNetwork
import com.feasycom.ota.utils.XmodemUtils
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

class BatchOtaActivity : BaseActivity<ActivityBatchUpdateBinding>(), View.OnClickListener {

    private lateinit var mSppCentralApi: FscSppCentralApi
    private var filterRssi: Int = 0
    private var filterNameSwitch: Boolean = false
    private var filterName: String = ""
    private val fileNameLiveData = MutableLiveData<String>()
    private val fileUriLiveData = MutableLiveData<Uri>()
    private val fileByteArrayLiveData = MutableLiveData<ByteArray>()
    private val mFailedDevices: HashSet<String> = HashSet()
    private val mSuccessDevices: HashSet<String> = HashSet()
    private lateinit var progress: ProgressBarDialogFragment
    private var mFscDevice: FscDevice? = null
    private val mMessage = SpannableStringBuilder()
    private var isStop = true
    private var isUpdate = false
    private var isAllowUpdate = false
    private var mUpdateSuccessTotal = 0
    private var mUpdateSuccessNumber = 0
    private var dialog: AlertDialog? = null

    private val handler = Handler(Looper.getMainLooper()) { msg ->
        when (msg.what) {
            START_DOWNLOAD -> {
                progress.show(supportFragmentManager, "batch_ota")
            }
            DOWNLOAD_FINISH -> {
                progress.dismiss()
            }
            SHOW_DIALOG -> {
                progress.dismiss()
                showDialog(this, msg.data.getString("message", ""))
            }
        }
        false
    }

    override fun getViewBinding(): ActivityBatchUpdateBinding {
        return ActivityBatchUpdateBinding.inflate(layoutInflater)
    }

    override fun initView() {
        progress = ProgressBarDialogFragment()
        initToolbar()
        initConfigParams()
        initLogDisplay()
        initSDK()
        initEvent()
        initObserve()
        restoreFileUri()
    }

    private fun initConfigParams() {
        isAllowUpdate = getBoolean("isAllowUpdate", false)
        val devices = getStrSet("devices", HashSet())
        mSuccessDevices.addAll(devices)
        mUpdateSuccessTotal = getInt("update_success_number")
    }

    private fun initLogDisplay() {
        mMessage.clear()
        val log = getStr("log", "")
        if (log.isNotBlank()) {
            mMessage.append(log)
        }
        binding.upgradeLogTv.text = mMessage
    }

    private fun restoreFileUri() {
        val savedUri = getStr("fileUri", "")
        if (savedUri.isNotBlank()) {
            fileUriLiveData.value = Uri.parse(savedUri)
        }
    }

    private fun initObserve() {
        fileUriLiveData.observe(this) { uri ->
            binding.otaState.text = getString(R.string.waitingForUpdate)
            putStr("fileUri", uri.toString())
            getUriInfo(uri)
        }

        fileNameLiveData.observe(this) { fileNames ->
            binding.fileName.text = fileNames
        }

        fileByteArrayLiveData.observe(this) {
            binding.startOtaButton.isEnabled = true
        }
    }

    private fun getUriInfo(uri: Uri) {
        try {
            when (uri.scheme) {
                "content" -> {
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            contentResolver.openInputStream(uri)?.use { inputStream ->
                                fileByteArrayLiveData.postValue(inputStream.readBytes())
                            } ?: LogUtils.e(TAG, "Content URI input stream is null")

                            val fileNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                            val fileName = cursor.getString(fileNameColumn)
                            fileNameLiveData.postValue(fileName)
                        }
                    }
                }
                "file" -> {
                    val file = uri.toFile()
                    fileNameLiveData.postValue(file.name)
                    fileByteArrayLiveData.postValue(file.readBytes())
                }
                else -> {
                    LogUtils.e(TAG, "Unsupported URI scheme: ${uri.scheme}")
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "Get URI info failed: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun initSDK() {
        mSppCentralApi = FscSppCentralApiImp.getInstance()
        mSppCentralApi.setCallbacks(object : FscSppCentralCallbacksImp() {
            @SuppressLint("SetTextI18n", "MissingPermission")
            override fun otaProgressUpdate(address: String?, percentage: Float, status: Int) {
                runOnUiThread {
                    updateOtaProgress(percentage)
                    handleOtaStatus(status)
                    binding.scrollView.post {
                        binding.scrollView.scrollTo(0, binding.upgradeLogTv.bottom)
                    }
                }
            }

            override fun packetReceived(
                address: String, strValue: String, hexString: String, data: ByteArray
            ) {}

            override fun sppPeripheralDisconnected(address: String?) {
                LogUtils.e(TAG, "APP SPP sppPeripheralDisconnected => 断开连接.")
                isStop = false
            }

            override fun sppPeripheralConnected(device: BluetoothDevice?, type: ConnectType?) {
                super.sppPeripheralConnected(device, type)
                LogUtils.d(TAG, "APP SPP sppPeripheralDisconnected => 连接成功.")
            }

            @SuppressLint("MissingPermission")
            override fun sppPeripheralFound(sppDevice: FscDevice, rssi: Int) {
                super.sppPeripheralFound(sppDevice, rssi)

                if (!checkDeviceFilterConditions(sppDevice, rssi)) {
                    return
                }

                connectAndUpdateDevice(sppDevice)
            }

            override fun startScan() {
                binding.otaState.text = getString(R.string.searching)
            }

            override fun stopScan() {
                if (!isStop) {
                    startSppScan()
                }
            }
        })
    }

    private fun checkDeviceFilterConditions(device: FscDevice, rssi: Int): Boolean {
        if (isStop) return false

        if (device.device == null || device.device.name == null) return false

        if (rssi < filterRssi || rssi > -30) {
            LogUtils.d(TAG, "APP SPP RSSI过滤: rssi=$rssi, filterRssi=$filterRssi")
            return false
        }

        if (mFailedDevices.contains(device.address)) {
            runOnUiThread {
                Toast.makeText(this, getString(R.string.ota_scan_failed_device, device.address),
                    Toast.LENGTH_SHORT).show()
            }
            return false
        }

        if (mSuccessDevices.contains(device.address) && !isAllowUpdate) {
            runOnUiThread {
                Toast.makeText(this, getString(R.string.ota_scan_upgraded_device, device.address),
                    Toast.LENGTH_SHORT).show()
            }
            return false
        }

        val filterText = if (filterName.isBlank()) "Feasycom" else filterName
        if (!device.device.name.lowercase().contains(filterText.lowercase())) {
            return false
        }

        return true
    }

    private fun connectAndUpdateDevice(device: FscDevice) {
        LogUtils.d(TAG, "APP SPP 找到符合条件的设备: ${device.device.name} (${device.address}) RSSI: ${device.rssi}")

        mFscDevice = device
        fileByteArrayLiveData.value?.let { firmwareData ->
            binding.otaState.text = getString(R.string.connecting)
            startTimer()

            isStop = true
            isAllowUpdate = false

            mSppCentralApi.connectToVerifyOTAWithFactory(
                device.address,
                firmwareData,
                binding.resetFlag.isChecked,
                binding.checkModuleVersion.isChecked
            )
        }
    }

    private fun updateOtaProgress(percentage: Float) {
        if (percentage > 0) {
            binding.otaProgress.progress = percentage.toInt()
            val df = if (percentage < 1) DecimalFormat("0.00") else DecimalFormat("#.00")
            binding.progressCount.text = df.format(percentage)
        }
    }

    private fun handleOtaStatus(status: Int) {
        when (status) {
            XmodemUtils.OTA_STATUS_FINISH -> handleOtaSuccess()
            XmodemUtils.OTA_STATUS_FAILED -> handleOtaFailed(getString(R.string.updateFailed))
            XmodemUtils.OTA_STATUS_AT_FAILED -> handleOtaFailed(getString(R.string.updateFailed), "无法查询到模块信息")
            XmodemUtils.OTA_STATUS_VERIFY_FAILED -> handleOtaFailed(getString(R.string.updateFailed), "模块版本不符")
            XmodemUtils.OTA_STATUS_BEGIN -> handleOtaBegin()
            XmodemUtils.OTA_STATUS_PROCESSING -> handleOtaProcessing()
            0x0FFF -> handleOtaFailed(getString(R.string.updateFailed), "升级失败，模组需要重新上电")
            XmodemUtils.OTA_STATUS_VERIFY_MODEL_FAILED -> handleOtaFailed(getString(R.string.updateFailed), getString(R.string.nameNotMatch))
            XmodemUtils.OTA_STATUS_VERIFY_APP_VERSION_FAILED -> handleOtaFailed(getString(R.string.updateFailed), getString(R.string.versionIsLow))
        }
    }

    private fun handleOtaBegin() {
        isUpdate = true
        binding.otaState.text = getString(R.string.waitingForUpdate)
        binding.otaProgress.progress = 0
        binding.group.visibility = View.VISIBLE
        startTimer()
    }

    private fun handleOtaProcessing() {
        binding.otaState.text = getString(R.string.updating)
    }

    @SuppressLint("MissingPermission")
    private fun handleOtaSuccess() {
        LogUtils.d(TAG, "APP SPP OTA升级成功")
        isUpdate = false
        mUpdateSuccessTotal++
        mUpdateSuccessNumber++

        putInt("update_success_number", mUpdateSuccessTotal)
        binding.toolbarSubtitle.text = getString(R.string.number_upgrades, mUpdateSuccessTotal)

        binding.otaState.text = getString(R.string.updateSuccess)
        binding.timeCount.stop()

        if (binding.clear.isChecked) {
            mSppCentralApi.clearDevice(mFscDevice?.address)
        }

        disconnect()

        addOtaLog(true, "${mFscDevice?.device?.name}  -  ${mFscDevice?.address}\n升级成功\n")

        binding.group.visibility = View.INVISIBLE
        mFscDevice?.address?.let {
            mSuccessDevices.add(it)
            mFscDevice = null
        }

        isStop = false
        startSppScan()
    }

    @SuppressLint("MissingPermission")
    private fun handleOtaFailed(stateText: String, errorMsg: String = "") {
        LogUtils.e(TAG, "APP SPP OTA升级失败: $errorMsg")
        isUpdate = false

        binding.otaState.text = stateText
        binding.timeCount.stop()

        val logMsg = if (errorMsg.isBlank()) {
            "${mFscDevice?.device?.name}  -  ${mFscDevice?.address}\n升级失败\n"
        } else {
            "${mFscDevice?.device?.name}  -  ${mFscDevice?.address}\n升级失败\n$errorMsg\n"
        }

        addOtaLog(false, logMsg)

        binding.group.visibility = View.INVISIBLE
        mFscDevice?.address?.let {
            mFailedDevices.add(it)
            mFscDevice = null
        }

        isStop = false
        startSppScan()
    }

    private fun addOtaLog(isSuccess: Boolean, message: String) {
        val color = if (isSuccess) Color.BLACK else Color.RED
        mMessage.append(SpannableString(message).apply {
            setSpan(ForegroundColorSpan(color), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        })
        binding.upgradeLogTv.text = mMessage
    }

    private fun initToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbarTitle.setText(R.string.OTABatchUpgrade)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }

        binding.toolbar.setNavigationOnClickListener {
            if (PermissionUtil.hasRequiredPermissions(this)) {
                mSppCentralApi.stopScan()
            }
            disconnect()
            finish()
        }

        binding.toolbarSubtitle.text = getString(R.string.number_upgrades, mUpdateSuccessTotal)
    }

    private fun initEvent() {
        binding.selectFileButton.setOnClickListener(this)
        binding.downloadButton.setOnClickListener(this)
        binding.startOtaButton.setOnClickListener(this)

        binding.copyLogBtn.setOnClickListener {
            Toast.makeText(this, getString(R.string.copy_all), Toast.LENGTH_SHORT).show()
            copyText(binding.upgradeLogTv.text.toString())
        }

        binding.batchFilterTv.setOnClickListener {
            BatchFilterActivity.activityStart(this)
        }

        binding.clearCacheBtn.setOnClickListener {
            DataCleanManagerUtil.clearIntExtCache(this)
            Toast.makeText(this, getString(R.string.clear_cache), Toast.LENGTH_SHORT).show()

            mUpdateSuccessTotal = 0
            putInt("update_success_number", mUpdateSuccessTotal)
            binding.toolbarSubtitle.text = getString(R.string.number_upgrades, mUpdateSuccessTotal)

            isAllowUpdate = true
            putBoolean("isAllowUpdate", true)

            mMessage.clear()
            binding.upgradeLogTv.text = mMessage
        }

        binding.deleteHistoryBtn.setOnClickListener {
            Toast.makeText(this, getString(R.string.clear_device_record), Toast.LENGTH_SHORT).show()
            putStrSet("devices", HashSet())
            mSuccessDevices.clear()
            mFailedDevices.clear()
        }
    }

    override fun onResume() {
        super.onResume()
        updateFilterConditions()
        updateFilterIcon()
    }

    private fun updateFilterConditions() {
        filterRssi = getInt("batchRssiValue", -70)
        filterNameSwitch = getBoolean("batchNameSwitch", false)
        filterName = if (filterNameSwitch) getStr("batchNameValue", "") else ""
    }

    private fun updateFilterIcon() {
        val iconRes = if (filterRssi > -70 || filterNameSwitch) {
            R.drawable.icon_filtter_ing
        } else {
            R.drawable.icon_filtter_none
        }
        binding.batchFilterIv.setImageResource(iconRes)
    }

    private fun disconnect() {
        mSppCentralApi.disconnect()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.selectFileButton -> performFileSearch()
            R.id.downloadButton -> handleDownloadButtonClick()
            R.id.startOtaButton -> handleStartOtaButtonClick()
        }
    }

    private fun handleDownloadButtonClick() {
        if (isExternalStorageLegacy()) {
            createDfuDialog()
        } else {
            checkPermission()
        }
    }

    private fun handleStartOtaButtonClick() {
        if (!PermissionUtil.hasRequiredPermissions(this)) {
            Toast.makeText(this, getString(R.string.upgrade_prohibited), Toast.LENGTH_LONG).show()
            return
        }

        if (binding.startOtaButton.text == getString(R.string.start_batch_upgrade)) {
            binding.selectFileButton.isEnabled = false
            binding.downloadButton.isEnabled = false
            isStop = false
            binding.startOtaButton.text = getString(R.string.stop_batch_upgrade)

            if (!isUpdate) {
                startSppScan()
                binding.otaState.text = getString(R.string.waitingForUpdate)
                binding.otaProgress.progress = 0
                binding.progressCount.text = getString(R.string.upgrade_percentage, 0)
            }
        } else {
            isStop = true
            mSppCentralApi.stopScan()
            binding.selectFileButton.isEnabled = true
            binding.downloadButton.isEnabled = true
            binding.startOtaButton.text = getString(R.string.start_batch_upgrade)
        }
    }

    private fun createDfuDialog() {
        val dfuNameDialogFragment = DfuNameDialogFragment()
        dfuNameDialogFragment.onClickComplete = { dialog, parameter ->
            dialog.dismiss()
            if (parameter.isNotEmpty() && parameter.isNotBlank()) {
                handler.sendEmptyMessage(START_DOWNLOAD)
                // 修复点1：使用lifecycleScope.launch，确保协程上下文正确
                lifecycleScope.launch(Dispatchers.IO) {
                    downloadDFU(parameter)
                }
            }
        }
        dfuNameDialogFragment.show(supportFragmentManager, "dfu")
    }

    private fun checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            dialog?.dismiss()

            dialog = AlertDialog.Builder(this)
                .setTitle(getString(R.string.tips))
                .setMessage(getString(R.string.file_access_permissions))
                .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                    dialog.dismiss()
                    ActivityCompat.requestPermissions(
                        this,
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE
                    )
                }.create()
            dialog?.show()
        } else {
            createDfuDialog()
        }
    }

    private fun startTimer() {
        binding.timeCount.base = SystemClock.elapsedRealtime()
        val hour = ((SystemClock.elapsedRealtime() - binding.timeCount.base) / 1000 / 60)
        binding.timeCount.format = "0${hour}:%s"
        binding.timeCount.start()
    }

    private fun startSppScan() {
        if (PermissionUtil.hasRequiredPermissions(this) && !isStop) {
            mSppCentralApi.startScan()
        }
    }

    private fun performFileSearch() {
        val intent = if (isKitkatOrAbove()) {
            Intent(Intent.ACTION_OPEN_DOCUMENT)
        } else {
            Intent(Intent.ACTION_GET_CONTENT)
        }

        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/octet-stream"
        startActivityForResult(intent, READ_FILE_REQUEST_CODE)
    }

    private suspend fun downloadDFU(parameter: String) {
        // 修复点2：使用withContext确保在IO线程执行
        withContext(Dispatchers.IO) {
            try {
                deleteExistsFile(parameter)

                // 修复点3：先获取响应体，再处理byteStream()
                val responseBody = DeviceNetwork.downloadDFU(parameter)

                if (isExternalStorageLegacy()) {
                    handleScopedStorageDownload(parameter, responseBody.byteStream())
                } else {
                    handleLegacyStorageDownload(parameter, responseBody.byteStream())
                }
            } catch (e: HttpException) {
                handleDownloadError(e)
            } catch (e: IOException) {
                sendErrorMessage(getString(R.string.io_error))
            } catch (e: UnknownHostException) {
                sendErrorMessage(getString(R.string.network_error))
            } catch (e: Exception) {
                LogUtils.e(TAG, "Download DFU failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // 修复点4：将byteStream作为参数传入，避免跨上下文调用
    @SuppressLint("NewApi")
    private suspend fun handleScopedStorageDownload(parameter: String, inputStream: InputStream) {
        val contentUri = MediaStore.Downloads.getContentUri("external")
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/dfu")
            put(MediaStore.Downloads.DISPLAY_NAME, "${parameter}.dfu")
        }

        // 修复点5：使用withContext而非直接launch
        withContext(Dispatchers.IO) {
            contentResolver.insert(contentUri, contentValues)?.let { uri ->
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val buffer = ByteArray(1024)
                    var bytesRead: Int

                    // 修复点6：直接使用传入的inputStream，避免重复调用byteStream()
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                }

                fileUriLiveData.postValue(uri)

                runOnUiThread {
                    Toast.makeText(this@BatchOtaActivity, getString(R.string.down_file_finish), Toast.LENGTH_LONG).show()
                }
                handler.sendEmptyMessage(DOWNLOAD_FINISH)
            }
        }
    }

    // 修复点7：同理，传入inputStream参数
    private suspend fun handleLegacyStorageDownload(parameter: String, inputStream: InputStream) {
        val directoryPath = "${Environment.getExternalStorageDirectory()}/Download/dfu/"
        val directory = File(directoryPath)

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val safeParameter = parameter.replace("/", "_")
        val filePath = "${directoryPath}${safeParameter}.dfu"
        val file = File(filePath)

        // 修复点8：使用withContext
        withContext(Dispatchers.IO) {
            BufferedInputStream(inputStream).use { input ->
                BufferedOutputStream(FileOutputStream(file)).use { output ->
                    var data: Int
                    while (input.read().also { data = it } != -1) {
                        output.write(data)
                    }
                }
            }

            runOnUiThread {
                Toast.makeText(this@BatchOtaActivity, getString(R.string.down_file_finish), Toast.LENGTH_LONG).show()
            }
            handler.sendEmptyMessage(DOWNLOAD_FINISH)

            fileUriLiveData.postValue(file.toUri())
        }
    }

    private fun handleDownloadError(e: HttpException) {
        val errorMsg = when (e.code()) {
            404 -> getString(R.string.down_file_error)
            else -> getString(R.string.network_error)
        }
        sendErrorMessage(errorMsg)
    }

    private fun sendErrorMessage(message: String) {
        val msg = Message().apply {
            what = SHOW_DIALOG
            data.putString("message", message)
        }
        handler.sendMessage(msg)
    }

    private fun deleteExistsFile(parameter: String) {
        val safeParameter = parameter.replace("/", "_")

        if (isExternalStorageLegacy()) {
            val contentUri = MediaStore.Downloads.getContentUri("external")
            val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf("${safeParameter}.dfu")

            contentResolver.query(contentUri, null, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                    val uri = ContentUris.withAppendedId(contentUri, id)
                    contentResolver.delete(uri, null, null)
                    LogUtils.d(TAG, "删除分区存储文件: $uri")
                }
            }
        } else {
            val filePath = "${Environment.getExternalStorageDirectory()}/Download/dfu/${safeParameter}"
            val file = File(filePath)

            if (file.exists()) {
                file.delete()
                LogUtils.d(TAG, "删除传统存储文件: $filePath")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == READ_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                fileUriLiveData.postValue(uri)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        isStop = true
        putStrSet("devices", mSuccessDevices)
        putStr("log", binding.upgradeLogTv.text.toString())
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        dialog?.dismiss()
    }

    private fun isExternalStorageLegacy(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !Environment.isExternalStorageLegacy()
    }

    private fun isKitkatOrAbove(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
    }

    private fun showDialog(context: Context, message: String) {
        AlertDialog.Builder(context)
            .setTitle(getString(R.string.tips))
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok), null)
            .show()
    }

    companion object {
        private const val TAG = "BatchOtaActivity"
        private const val START_DOWNLOAD = 0X0001
        private const val SHOW_DIALOG = 0x0003
        private const val DOWNLOAD_FINISH = 0x000444
        private const val READ_FILE_REQUEST_CODE = 42

        private const val REQUEST_EXTERNAL_STORAGE = 1
        private val PERMISSIONS_STORAGE = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        fun activityStart(context: Context) {
            val intent = Intent(context, BatchOtaActivity::class.java)
            context.startActivity(intent)
        }
    }
}