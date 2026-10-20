package com.feasycom.feasyblue.utils

import android.content.Context
import android.os.Environment
import android.text.TextUtils
import java.io.File
import java.lang.Exception
import java.math.BigDecimal

/**
 * @ClassName: DataCleanManagerUtil
 * @Author: Xiao JinLai
 * @Date: 2015-10-22
 * @Description: 清除数据管理类
 */
object DataCleanManagerUtil {

    /**
     * 清除应用内部缓存 (/data/data/com.xxx.xxx/cache)
     *
     * @param context
     */
    private fun cleanInternalCache(context: Context) {
        deleteFilesByDirectory(context.cacheDir)
    }

    /**
     * 清除应用所有数据库 (/data/data/com.xxx.xxx/databases)
     *
     * @param context
     */
    private fun cleanDatabases(context: Context) {
        deleteFilesByDirectory(File("/data/data/${context.packageName}/databases"))
    }

    /**
     * 清除应用 SharedPreference (/data/data/com.xxx.xxx/shared_prefs)
     *
     * @param context
     */
    private fun cleanSharedPreference(context: Context) {
        deleteFilesByDirectory(File("/data/data/${context.packageName}/shared_prefs"))
    }

    /**
     * 按名字清除应用数据库
     *
     * @param context
     * @param dbName
     */
    fun cleanDatabaseByName(context: Context, dbName: String) {
        context.deleteDatabase(dbName)
    }

    /**
     * 清除应用 files 目录下的内容 (/data/data/com.xxx.xxx/files)
     *
     * @param context
     */
    private fun cleanFiles(context: Context) {
        deleteFilesByDirectory(context.filesDir)
    }

    /**
     * 清除外部 cache 目录下的内容 (/mnt/sdcard/android/data/com.xxx.xxx/cache)
     *
     * @param context
     */
    private fun cleanExternalCache(context: Context) {
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            deleteFilesByDirectory(context.externalCacheDir!!)
        }
    }

    /**
     * 清除自定义路径下的文件，使用需小心
     *
     * @param filePath
     */
    private fun cleanCustomCache(filePath: String?) {
        deleteFilesByDirectory(File(filePath))
    }

    /**
     * 清除应用所有的数据
     *
     * @param context
     * @param filepaths
     */
    fun cleanApplicationData(context: Context, vararg filePaths: String) {
        cleanInternalCache(context)
        cleanExternalCache(context)
        cleanDatabases(context)
        cleanSharedPreference(context)
        cleanFiles(context)
        filePaths.forEach { cleanCustomCache(it) }
    }

    /**
     * 删除某个文件夹下的文件，只删除目录下的文件
     *
     * @param dir
     */
    private fun deleteFilesByDirectory(dir: File): Boolean {
        dir.let {
            if (it.isDirectory) {
                it.listFiles()?.forEach { file ->
                    deleteFilesByDirectory(file)
                }
            }
            return it.delete()
        }
        return false
    }

    /**
     * 获取文件夹大小
     *
     * @param file
     */
    private fun getFolderSize(file: File): Long {
        var size: Long = 0
        try {
            file.listFiles()?.forEach {
                size += if (it.isDirectory) getFolderSize(it) else it.length()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return size
    }

    /**
     * 删除指定目录下文件及目录
     *
     * @param filePath
     * @param deleteThisPath
     */
    private fun deleteFolderFile(filePath: String?, deleteThisPath: Boolean) {
        if (!TextUtils.isEmpty(filePath)) {
            try {
                val file = File(filePath)
                if (file.isDirectory) { // 如果下面还有文件
                    file.listFiles()?.forEach {
                        deleteFolderFile(it.absolutePath, true)
                    }
                }
                if (deleteThisPath) {
                   if (!file.isDirectory || file.listFiles().isNullOrEmpty()) {
                       file.delete()
                   }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 获取缓存大小，返回 String 型
     *
     * @param context
     */
    fun getCacheSize(context: Context): String {
        var cacheSize = getFolderSize(context.cacheDir) // 内部缓存大小
        // 判断SD卡是否存在，并且是否具有读写权限
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            cacheSize += getFolderSize(context.externalCacheDir!!) // 外部缓存大小
        }
        return getFormatSize(cacheSize.toDouble())
    }

    /**
     * 获取缓存大小，返回 int 型
     *
     * @param context
     */
    fun getCacheSizeInt(context: Context): Long {
        var cacheSize = getFolderSize(context.cacheDir) // 内部缓存大小
        // 判断SD卡是否存在，并且是否具有读写权限
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            cacheSize += getFolderSize(context.externalCacheDir!!) // 外部缓存大小
        }
        return cacheSize
    }

    /**
     * 清除内外缓存
     *
     * @param context
     */
    fun clearIntExtCache(context: Context) {
        cleanInternalCache(context)
        cleanExternalCache(context)
    }

    /**
     * 格式化单位
     *
     * @param size
     */
    private fun getFormatSize(size: Double): String {
        val kiloByte = size / 1024
        if (kiloByte < 1) return "$size Byte"

        val megaByte = kiloByte / 1024
        if (megaByte < 1) return BigDecimal(kiloByte).setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + " KB"

        val gigaByte = megaByte / 1024
        if (gigaByte < 1) return BigDecimal(megaByte).setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + " MB"

        val teraBytes = gigaByte / 1024
        if (teraBytes < 1) return BigDecimal(gigaByte).setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + " GB"

        return BigDecimal(teraBytes).setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + " TB"
    }
}