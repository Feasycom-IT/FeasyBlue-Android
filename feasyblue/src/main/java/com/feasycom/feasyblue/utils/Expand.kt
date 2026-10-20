package com.feasycom.feasyblue.utils

import android.widget.EditText
import androidx.annotation.Keep

@Keep
private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()

/**
 * 将字节数组转换为十六进制字符串
 */
@Keep
fun ByteArray.bytesToHex(): String {
    if (isEmpty()) return ""
    val hexChars = StringBuilder()
    for (byte in this) {
        val v = byte.toInt() and 0xFF
        hexChars.append(HEX_ARRAY[v ushr 4]).append(HEX_ARRAY[v and 0x0F]).append(" ")
    }
    return hexChars.toString().trim()
}

/**
 * 将十六进制字符串转换为普通字符串
 */
@Keep
fun hexToString(hex: String): String {
    val result = StringBuilder()
    for (i in hex.indices step 2) {
        val hexPair = hex.substring(i, i + 2)
        val decimal = hexPair.toInt(16)
        result.append(decimal.toChar())
    }
    return result.toString()
}

/**
 * 将字符串转换为十六进制字符串
 */
@Keep
fun String.toHexString() = toByteArray(Charsets.ISO_8859_1).bytesToHex()

/**
 * 将十六进制字符串转换为字节数组并转换为字符串
 */
@Keep
fun String.hexToByte(): String{
    if (isEmpty()) return ""
    var hexString = replace(" ", "")
    //如果是奇位数，最后一个数前面补0
    if (hexString.length % 2 == 1) {
       hexString = "0$hexString"
    }
    return hexString.chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
        .toString(Charsets.ISO_8859_1)
}

/**
 * 获取字符串中的参数部分
 */
@Keep
fun String.getParam() = try {
    substringAfter("=").replace("\r\n", "").replace("OK","")
} catch (e: java.lang.Exception) {
    e.printStackTrace()
    null
}

/**
 * 解析版本号
 */
@Keep
fun String.parsingVersion(): String {
    return split(",")
        .firstOrNull { it.contains(".") }
        ?.replace(Regex("[a-zA-Z.]"), "")
        ?: ""
}

/**
 * 解析型号
 */
@Keep
fun String.parsingModel(): String{
    return split(",")
        .firstOrNull { it.contains(Regex("(FSC|-|BP|BT)")) }
        ?.replace("FSC-", "")
        ?: ""
}

/**
 * 将字节数组转换为十六进制字符串
 */
@Keep
fun ByteArray.toHexString(): String{
    return joinToString(" ") { String.format("%02x",it) }
}

/**
 * 格式化输入并填充 EditText 列表
 */
@Keep
fun formatAndFillEditText(input: String, editTexts: List<EditText>) {
    val formattedString = StringBuilder(input)
    val lengths = intArrayOf(8, 4, 4, 4, 12)
    var currentLength = 0

    for (length in lengths) {
        if (currentLength + length <= formattedString.length) {
            formattedString.insert(currentLength + length, "-")
            currentLength += length + 1
        } else {
            break
        }
    }

    val parts = formattedString.split("-")
    for ((index, editText) in editTexts.withIndex()) {
        if (index < parts.size) {
            editText.setText(parts[index])
        } else {
            editText.setText("")
        }
    }
}