package com.feasycom.feasyblue.activity

import android.Manifest
import android.app.AlertDialog
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.view.View
import android.view.View.OnClickListener
import androidx.annotation.RequiresApi
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.LogUtils
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivityBw256commOtaBinding
import com.feasycom.feasyblue.dialog.DfuNameDialogFragment
import com.feasycom.feasyblue.dialog.ProgressBarDialogFragment
import com.feasycom.feasyblue.permission.PermissionDescription
import com.feasycom.feasyblue.permission.PermissionInterceptor
import com.feasycom.feasyblue.utils.getBoolean
import com.feasycom.feasyblue.utils.putBoolean
import com.feasycom.network.DeviceNetwork
import com.feasycom.wifi.tcp.FirmwareHttpSdk
import com.feasycom.wifi.tcp.FscClientBwBwCallback
import com.feasycom.wifi.tcp.TCPClientManager
import com.feasycom.wifi.utils.BwNetUtils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.*
import java.lang.ref.WeakReference
import java.net.UnknownHostException

// 1. 补充 ViewBinding 泛型参数
class BW256BOtaActivity : BaseActivity<ActivityBw256commOtaBinding>(), OnClickListener, FscClientBwBwCallback {

    private lateinit var mHandler: Handler
    private var mProgress = ProgressBarDialogFragment()
    private val mFileNameLiveData = MutableLiveData<String>()
    private val mFileUriLiveData = MutableLiveData<Uri>()
    private val mFileByteArrayLiveData = MutableLiveData<ByteArray>()

    private var mFileName: String? = null

    class MyHandler(activity: BW256BOtaActivity) : Handler(Looper.getMainLooper()) {
        private val mWeakReference: WeakReference<BW256BOtaActivity> = WeakReference(activity)
        override fun handleMessage(msg: Message) {
            mWeakReference.get()?.run {
                when (msg.what) {
                    START_DOWNLOAD -> {
                        mProgress.show(supportFragmentManager, "ota")
                    }
                    DOWNLOAD_FINISH -> {
                        mProgress.dismiss()
                    }
                    SHOW_DIALOG -> {
                        mProgress.dismiss()
                        showDialog(this, msg.data.getString("message", ""))
                    }
                }
                false
            }
        }
    }

    // 2. 实现基类要求的 getViewBinding 方法
    override fun getViewBinding(): ActivityBw256commOtaBinding {
        return ActivityBw256commOtaBinding.inflate(layoutInflater)
    }

    override fun initView() {
        host = intent?.getStringExtra("tcpServer")
        port = intent?.getIntExtra("tcpPort", -1)
        type = intent?.getIntExtra("tcpType", -1)
        putBoolean("haveUpgrade", false)
        initToolbar()
        setEvent()
        setObserver()
    }

    // 3. 移除过时的 getLayout 方法

    private fun initToolbar() {
        // 4. 替换所有控件引用为 binding 调用
        setSupportActionBar(binding.otaToolbar)
        binding.otaTitle.text = getString(R.string.air_upgrade)
        binding.otaToolbar.setNavigationOnClickListener {
            if (type == 1) {
                GatewayActivity.activityStart(this)
                finish()
            } else if (type == 2) {
                BW256BScanActivity.activityStart(this, if (getBoolean("haveUpgrade")) 0 else 1)
                finish()
            }
        }

        supportActionBar?.let {
            it.setDisplayShowTitleEnabled(false)
            it.setDisplayHomeAsUpEnabled(true)
        }
        mHandler = MyHandler(this)
        TCPClientManager.initClient(this)
        FirmwareHttpSdk.init(this)
    }

    override fun packetReceived(
        strValue: String,
        rawValue: ByteArray,
    ) {
        handleFotaResponse(strValue)
    }

    private fun handleFotaResponse(response: String) {
        LogUtils.d(TAG, "APP WIFI TCP handleFotaResponse response => $response")
        if (response.contains("\$PROGRESS")) {
            runOnUiThread {
                binding.otaState?.text = getString(R.string.updating)
            }
            val upgradeProgress = response.split("=")[1].split("\r\n")[0]
            val upgradeProgressInt = getStringBeforeProgress(upgradeProgress).toInt()
            runOnUiThread {
                if (upgradeProgressInt > 0) {
                    binding.otaProgress?.progress = upgradeProgressInt
                    binding.progressCount?.text = getStringBeforeProgress(upgradeProgress)
                }
            }
        } else if (response.contains("\$OTA=0")) {
            runOnUiThread {
                binding.timeCount?.stop()
                binding.otaState?.text = getString(R.string.updateFailed)
                putBoolean("haveUpgrade", true)
            }
        } else if (response.contains("\$OTA=1")) {
            runOnUiThread {
                binding.timeCount?.stop()
                binding.otaState?.text = getString(R.string.updateSuccess)
                putBoolean("haveUpgrade", true)
            }
        }
    }

    private fun getStringBeforeProgress(input: String): String {
        val index = input.indexOf("\$PROGRESS")
        return if (index != -1) input.substring(0, index) else input
    }

    private fun setEvent() {
        binding.selectFileButton.setOnClickListener(this)
        binding.downloadButton.setOnClickListener(this)
        binding.startOtaButton.setOnClickListener(this)
    }

    private fun setObserver() {
        mFileUriLiveData.observe(this) { uri ->
            FirmwareHttpSdk?.setFirmwareUri(uri)
            getUriInfo(uri)
        }
        mFileNameLiveData.observe(this) { fileName ->
            binding.fileNameTv.text = fileName
            mFileName = fileName
            binding.startOtaButton.isEnabled = true
        }
    }

    private fun getUriInfo(uri: Uri) {
        when (uri.scheme) {
            "content" -> {
                try {
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            contentResolver.openInputStream(uri)?.use {
                                mFileByteArrayLiveData.postValue(it.readBytes())
                            }
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

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.selectFileButton -> {
                performFileSearch()
                binding.group?.visibility = View.INVISIBLE
            }
            R.id.downloadButton -> {
                if (BwNetUtils.isExternalStorageLegacy()) {
                    createBinDialog()
                } else {
                    checkPermission()
                }
                binding.group?.visibility = View.INVISIBLE
            }
            R.id.startOtaButton -> {
                binding.group?.visibility = View.VISIBLE
                binding.startOtaButton.isEnabled = false
                binding.timeCount?.apply {
                    base = SystemClock.elapsedRealtime()
                    format = "0${(SystemClock.elapsedRealtime() - base) / 1000 / 60}:%s"
                    start()
                }

                binding.otaState.text = getString(R.string.waitingForUpdate)
                binding.otaProgress?.progress = 0
                binding.progressCount?.text = "0"

                val fullAddress =
                    FirmwareHttpSdk?.startServer(mFileName!!, type!!)

                val fotaCommand = "AT+FOTA=$fullAddress\r\n"
                LogUtils.d(TAG, "APP WIFI TCP handleFotaResponse fotaCommand => $fotaCommand")
                TCPClientManager.sendMessage(fotaCommand)
            }
        }
    }

    private fun performFileSearch() {
        val intent: Intent = if (BwNetUtils.isKitkatOrAbove()) {
            Intent(Intent.ACTION_OPEN_DOCUMENT)
        } else {
            Intent(Intent.ACTION_GET_CONTENT)
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/octet-stream"

        startActivityForResult(intent, READ_FILE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != READ_FILE_REQUEST_CODE || resultCode != RESULT_OK) {
            return
        }
        data?.data?.let { uri ->
            mFileUriLiveData.postValue(uri)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createBinDialog() {
        val dfuNameDialogFragment = DfuNameDialogFragment()
        dfuNameDialogFragment.onClickComplete = { dialog, parameter ->
            dialog.dismiss()
            if (parameter.isNotEmpty() && parameter.isNotBlank()) {
                mHandler.sendEmptyMessage(START_DOWNLOAD)
                lifecycleScope.launch(Dispatchers.IO) {
                    downloadBIN(parameter)
                }
            }
        }
        dfuNameDialogFragment.show(supportFragmentManager, "bin")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkPermission() {
        XXPermissions.with(this)
            .permission(PermissionLists.getReadExternalStoragePermission())
            .permission(PermissionLists.getWriteExternalStoragePermission())
            .interceptor(PermissionInterceptor())
            .description(PermissionDescription())
            .request(OnPermissionCallback { grantedList, deniedList ->
                val allGranted = deniedList.isEmpty()
                if (!allGranted) {
                    return@OnPermissionCallback
                }
                createBinDialog()
            })
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun downloadBIN(parameter: String) {
        coroutineScope {
            deleteExistsFile(parameter)
            try {
                DeviceNetwork.downloadBIN(parameter).apply {
                    if (BwNetUtils.isExternalStorageLegacy()) {
                        val contentUri = MediaStore.Downloads.getContentUri("external")
                        val contentValues = ContentValues()
                        contentValues.put(
                            MediaStore.Downloads.RELATIVE_PATH,
                            Environment.DIRECTORY_DOWNLOADS + "/bin"
                        )
                        contentValues.put(
                            MediaStore.Downloads.DISPLAY_NAME, "${parameter}.bin"
                        )
                        launch(Dispatchers.IO) {
                            contentResolver.insert(contentUri, contentValues)?.let {
                                contentResolver.openOutputStream(it)?.use { out ->
                                    val byteArray = ByteArray(1024)
                                    var len: Int
                                    while (byteStream().read(byteArray).also { len = it } != -1) {
                                        out.write(byteArray, 0, len)
                                    }
                                }
                                mFileUriLiveData.postValue(it)
                            }
                            runOnUiThread {
                                BwNetUtils.showToast(
                                    this@BW256BOtaActivity,
                                    getString(R.string.down_file_finish)
                                )
                            }
                            mHandler.sendEmptyMessage(DOWNLOAD_FINISH)
                        }
                        return@coroutineScope
                    } else {
                        val directoryPath =
                            "${File.separator}sdcard${File.separator}Download${File.separator}bin${File.separator}"
                        val directory = File(directoryPath)
                        if (!directory.exists()) {
                            directory.mkdirs()
                        }
                        val replaceParameter = parameter.replace("/", "_")
                        val path = "${directoryPath}${replaceParameter}.bin"
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
                                BwNetUtils.showToast(this@BW256BOtaActivity, "下载成功")
                            }
                            mHandler.sendEmptyMessage(DOWNLOAD_FINISH)
                            mFileUriLiveData.postValue(file.toUri())
                        }
                    }
                }
            } catch (e: HttpException) {
                val message = Message()
                message.what = SHOW_DIALOG
                message.data.putString(
                    "message", when (e.code()) {
                        404 -> {
                            getString(R.string.down_file_error)
                        }
                        else -> {
                            getString(R.string.network_error)
                        }
                    }
                )
                mHandler.sendMessage(message)
            } catch (e: IOException) {
                val message = Message()
                message.what = SHOW_DIALOG
                message.data.putString("message", getString(R.string.io_error))
                mHandler.sendMessage(message)
            } catch (e: UnknownHostException) {
                val message = Message()
                message.what = SHOW_DIALOG
                message.data.putString("message", getString(R.string.network_error))
                mHandler.sendMessage(message)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun deleteExistsFile(parameter: String) {
        val replaceParameter = parameter.replace("/", "_")
        if (BwNetUtils.isExternalStorageLegacy()) {
            val externalContentUri = MediaStore.Downloads.getContentUri("external")
            val selection = MediaStore.Downloads.DISPLAY_NAME + "=?"
            val arg = arrayOf("$replaceParameter.bin")
            val cursor = contentResolver.query(
                externalContentUri, null, selection, arg, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndexOrThrow = it.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                    val queryId = it.getLong(columnIndexOrThrow)
                    val uri = ContentUris.withAppendedId(externalContentUri, queryId)
                    contentResolver.delete(uri, null, null)
                }
            }
        } else {
            // 不是分区存储
            val path =
                "${File.separator}sdcard${File.separator}Download${File.separator}bin${File.separator}${replaceParameter}"
            val file = File(path)
            val exists = file.exists()
            if (exists) {
                file.delete()
            }
        }
    }

    fun showDialog(context: Context, message: String) {
        val alertDialog =
            AlertDialog.Builder(context).setTitle(R.string.error).setInverseBackgroundForced(false)
                .setMessage(message).setIcon(R.mipmap.ic_launcher)
                .setNegativeButton(R.string.ok, null).create()
        alertDialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        FirmwareHttpSdk?.stopServer()
        mHandler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val TAG = "BW256BOtaActivity"
        private const val START_DOWNLOAD = 0x0001
        private const val DOWNLOAD_FINISH = 0x0002
        private const val SHOW_DIALOG = 0x0003
        private const val READ_FILE_REQUEST_CODE = 42
        private var host: String? = null
        private var port: Int? = -1
        private var type: Int? = -1

        fun activityStart(context: Context, tcpServer: String, tcpPort: Int, type: Int) {
            Intent(context, BW256BOtaActivity::class.java).apply {
                putExtra("tcpServer", tcpServer)
                putExtra("tcpPort", tcpPort)
                putExtra("tcpType", type)
                context.startActivity(this)
            }
        }
    }
}