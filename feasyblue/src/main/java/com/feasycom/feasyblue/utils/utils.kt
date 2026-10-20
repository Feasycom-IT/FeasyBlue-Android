package com.feasycom.feasyblue.utils

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.LogUtils
import com.feasycom.feasyblue.R
import org.mozilla.universalchardet.UniversalDetector
import java.io.*
import java.nio.charset.Charset
import java.util.*
import kotlin.experimental.and

private const val PREFS_READ_STORAGE_PERMISSION_REQUESTED = "read_storage_permission_requested"
private const val PREFS_WRITE_STORAGE_PERMISSION_REQUESTED = "write_storage_permission_requested"
private const val HEX = "0123456789ABCDEF"

/**
 * 判断系统版本是否是 KitKat 及以上
 */
fun isKitkatOrAbove(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

fun isQOrAbove(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

fun isUpsideDownCakeOrAbove(): Boolean = Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE

/**
 * 判断是否是安卓 S 及以上版本，并且外部存储是否是传统模式
 */
fun isExternalStorageLegacy(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !Environment.isExternalStorageLegacy()

/**
 * 判断是否已获得写入外部存储权限
 */
fun isWriteExternalStoragePermissionsGranted(context: Context?): Boolean =
    ContextCompat.checkSelfPermission(
        context!!,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED

/**
 * 判断是否永久拒绝了写入外部存储权限
 */
fun isWriteExternalStoragePermissionDeniedForever(activity: Activity?): Boolean {
    val preferences = PreferenceManager.getDefaultSharedPreferences(activity)
    return (!isWriteExternalStoragePermissionsGranted(activity) // 权限未授予
            && preferences.getBoolean(PREFS_WRITE_STORAGE_PERMISSION_REQUESTED, false) // 权限曾被请求
            && !ActivityCompat.shouldShowRequestPermissionRationale(
        activity!!,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )) // 不再提示请求权限
}

/**
 * 字符串转小端 int，字符串必须是 4 个数字，不足补 0，超过截取前 4 个
 */
fun stringToInt(string: String): Int {
    val str1 = string.substring(0, 2)
    val str2 = string.substring(2, 4)
    val int1: Int = formattingHexToInt(str1)
    val int2: Int = formattingHexToInt(str2)
    val byte1 = int1.toByte()
    val byte2 = int2.toByte()
    return byteToInt(byte1, byte2)
}

/**
 * 格式化十六进制字符串为 int
 */
fun formattingHexToInt(a: String): Int {
    val a1 = a.substring(0, 1)
    val a2 = a.substring(1, 2)
    return formattingOneHexToInt(a1) * 16 + formattingOneHexToInt(a2)
}

/**
 * 转换字节为 int
 */
fun byteToInt(a: Byte, b: Byte): Int {
    val intB: Int = (b and 0xff.toByte()).toInt()
    val intA: Int = (a and 0xff.toByte()).toInt()
    return intA + (intB shl 8)
}

/**
 * 将十六进制中的字母转为对应的数字
 */
fun formattingOneHexToInt(a: String?): Int =
    a?.takeIf { it.length == 1 }?.let { HEX.indexOf(it.uppercase(Locale.getDefault())) } ?: 0

/**
 * 判断系统版本是否需要打开定位
 */
fun isMarshmallowOrAbove(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

/**
 * 检查定位服务是否开启
 */
fun isLocationEnabled(context: Context): Boolean {
    if (isMarshmallowOrAbove()) {
        val locationMode = try {
            Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE)
        } catch (e: Settings.SettingNotFoundException) {
            Settings.Secure.LOCATION_MODE_OFF
        }
        return locationMode != Settings.Secure.LOCATION_MODE_OFF
    }
    return true
}

/**
 * 检查蓝牙是否开启
 */
fun isBleEnabled(): Boolean {
    val adapter = BluetoothAdapter.getDefaultAdapter()
    return adapter?.isEnabled == true
}

/**
 * 复制文本到剪贴板
 */
fun Context.copyText(copiedText: String) {
    val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboardManager.setPrimaryClip(ClipData.newPlainText(null, copiedText))
}

/**
 * 从 uri 获取文件路径
 */
fun getFilePathFromContentUri(context: Context, uri: Uri?): String? {
    uri ?: return null
    val filePathColumn = arrayOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME)
    context.contentResolver.query(uri, filePathColumn, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
            return if (index > -1) cursor.getString(index) else {
                val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val fileName = cursor.getString(nameIndex)
                getPathFromInputStreamUri(context, uri, fileName)
            }
        }
    }
    return null
}

/**
 * 用流拷贝文件到应用私有目录
 */
private fun getPathFromInputStreamUri(context: Context, uri: Uri, fileName: String): String? {
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        return createTemporalFileFrom(context, inputStream, fileName).path
    }
    return null
}

@Throws(IOException::class)
private fun createTemporalFileFrom(
    context: Context,
    inputStream: InputStream,
    fileName: String
): File {
    val targetFile = File(context.externalCacheDir, fileName).apply {
        if (exists()) delete()
    }
    FileOutputStream(targetFile).use { outputStream ->
        val buffer = ByteArray(8 * 1024)
        var read: Int
        while (inputStream.read(buffer).also { read = it } != -1) {
            outputStream.write(buffer, 0, read)
        }
        outputStream.flush()
    }
    return targetFile
}

/**
 * 获取文件编码类型
 */
fun ByteArray.getEncoding(): String {
    val defaultEncoding = "UTF-8"
    val detector = UniversalDetector { charset ->
        LogUtils.d("Utils", "APP UTIL report: $charset")
    }
    detector.handleData(this, 0, size)
    detector.dataEnd()
    val encoding = detector.detectedCharset
    detector.reset()
    return if (encoding == defaultEncoding || this[0] in 0..127) {
        String(this, Charset.forName(encoding ?: defaultEncoding))
    } else {
        ""
    }
}

fun showDialog(context: Context, message: String) {
    val alertDialog =
        AlertDialog.Builder(context).setTitle(R.string.error).setInverseBackgroundForced(false)
            .setMessage(message).setIcon(R.mipmap.ic_launcher)
            .setNegativeButton(R.string.ok, null).create()
    alertDialog.show()
}