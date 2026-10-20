package com.feasycom.feasyblue.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException

class FileUtils {

    companion object {
        private const val TAG = "FileUtils"

        /**
         * 保存位图到指定路径（静态方法）
         * @param context 上下文
         * @param bitmap 位图对象
         * @param bitName 位图名称
         * @param isSuccess 保存结果回调
         */
        fun saveBitmap(
            context: Context,
            bitmap: Bitmap,
            bitName: String,
            isSuccess: (Boolean) -> Unit
        ) {
            val directory = when (Build.BRAND) {
                "xiaomi", "HUAWEI" -> "/DCIM/Camera/"
                else -> "/DCIM/"
            }

            val fileName = Environment.getExternalStorageDirectory().path + directory + bitName
            val file = File(fileName)

            if (file.exists()) {
                isSuccess.invoke(true)
            } else {
                try {
                    FileOutputStream(file).use { out ->
                        if (bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)) {
                            out.flush()
                            isSuccess(true)
                            context.sendBroadcast(
                                Intent(
                                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                    Uri.parse("file://$fileName")
                                )
                            )
                        } else {
                            isSuccess(false)
                        }
                    }
                } catch (e: IOException) {
                    isSuccess.invoke(false)
                    e.printStackTrace()
                }
            }
        }

        /**
         * 导出数据到文本文件（静态方法，核心修复）
         * @param data 数据列表
         * @param file 保存文件
         * @return 成功返回true, 否则返回false
         */
        fun exportDataToTxt(data: List<String>, file: File): Boolean {
            return try {
                FileWriter(file).use { writer ->
                    data.forEach { line ->
                        writer.append(line).append("\n")
                    }
                    writer.flush()
                }
                true
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        }

        /**
         * 创建测试数据（静态挂起方法）
         */
        suspend fun createTestData1(bufferSize: Int): ByteArray {
            if (bufferSize % 10 != 0 || bufferSize == 0) return ByteArray(0)
            val buffer = ByteArray(bufferSize)
            withContext(Dispatchers.Default) {
                var index = 0
                val hex = "0123456789ABCDEF"
                var i = 0
                while (i < bufferSize) {
                    buffer[i] = '['.code.toByte()
                    buffer[i + 8] = hex[(index.shr(0)).and(0x0000000F)].code.toByte()
                    buffer[i + 7] = hex[(index.shr(4)).and(0x0000000F)].code.toByte()
                    buffer[i + 6] = hex[(index.shr(8)).and(0x0000000F)].code.toByte()
                    buffer[i + 5] = hex[(index.shr(12)).and(0x0000000F)].code.toByte()
                    buffer[i + 4] = hex[(index.shr(16)).and(0x0000000F)].code.toByte()
                    buffer[i + 3] = hex[(index.shr(20)).and(0x0000000F)].code.toByte()
                    buffer[i + 2] = hex[(index.shr(24)).and(0x0000000F)].code.toByte()
                    buffer[i + 1] = hex[(index.shr(28)).and(0x0000000F)].code.toByte()
                    buffer[i + 9] = ']'.code.toByte()
                    index += 1
                    i += 10
                }
            }
            return buffer
        }
    }
}