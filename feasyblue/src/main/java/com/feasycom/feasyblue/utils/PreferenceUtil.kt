package com.feasycom.feasyblue.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.swallowsonny.convertextlibrary.hex2ByteArray
import com.swallowsonny.convertextlibrary.toHexString

private const val FILE_NAME = "feasycom"
private const val KEY_ORDERED_STRING_SET = "OrderedStringSet"

private fun Context.preference() =
    getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

/**
 * 获取布尔值
 */
fun Context.getBoolean(key: String, default: Boolean = false): Boolean =
    preference().getBoolean(key, default)

/**
 * 获取字符串
 */
fun Context.getStr(key: String, default: String = ""): String =
    preference().getString(key, default) ?: default

/**
 * 获取整数
 */
fun Context.getInt(key: String, default: Int = 0): Int =
    preference().getInt(key, default)

/**
 * 获取长整数
 */
fun Context.getLong(key: String, default: Long = 0L): Long =
    preference().getLong(key, default)

/**
 * 获取字符串集合
 */
fun Context.getStrSet(key: String, data: HashSet<String>): HashSet<String> =
    preference().getStringSet(key, data) as HashSet<String>

/**
 * 存储布尔值
 */
fun Context.putBoolean(key: String, value: Boolean) = preference().edit {
    putBoolean(key, value)
}

/**
 * 存储字符串
 */
fun Context.putStr(key: String, value: String) = preference().edit {
    putString(key, value)
}

/**
 * 存储整数
 */
fun Context.putInt(key: String, value: Int) = preference().edit{
    putInt(key, value)
}

/**
 * 存储长整数
 */
fun Context.putLong(key: String, value: Long) = preference().edit {
    putLong(key, value)
}

/**
 * 存储字符串集合
 */
fun Context.putStrSet(key: String, value: HashSet<String>) = preference().edit {
    putStringSet(key, value)
}

/**
 * 存储字节数组
 */
fun Context.putByteArray(key: String, value: ByteArray) = preference().edit {
    putStr(key, value.toHexString(true))
}

/**
 * 保存有序字符串集合
 */
fun saveOrderedStringSet(context: Context, orderedStringSet: LinkedHashSet<String>) {
    val prefs: SharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    val json = Gson().toJson(orderedStringSet)
    prefs.edit { putString(KEY_ORDERED_STRING_SET, json) }
}

/**
 * 获取有序字符串集合
 */
fun getOrderedStringSet(context: Context): LinkedHashSet<String> {
    val prefs: SharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(KEY_ORDERED_STRING_SET, null)
    return if (json != null) {
        val type = object : TypeToken<LinkedHashSet<String>>() {}.type
        Gson().fromJson(json, type)
    } else {
        LinkedHashSet()
    }
}

/**
 * 获取字节数组
 */
fun Context.getByteArray(key: String, value: ByteArray = byteArrayOf()): ByteArray {
    val string = preference().getString(key, "")
    return if (string.isNullOrEmpty()){
        value
    }else {
        try {
            string!!.hex2ByteArray()
        }catch (e: Exception){
            value
        }
    }
}