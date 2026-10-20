package com.feasycom.feasyblue.utils

object ClickUtil {
    private var lastClickTime: Long = 0 // 上次点击时间

    @Synchronized
    fun isFastClick(): Boolean {
        val currentTime = System.currentTimeMillis()
        return if (currentTime - lastClickTime < 1000) {
            true // 如果两次点击时间间隔小于1000毫秒，返回true
        } else {
            lastClickTime = currentTime
            false // 更新上次点击时间并返回false
        }
    }
}