package com.feasycom.feasyblue.utils

import android.content.Context

object UIUtil {

    /**
     * 将 dp 值转换为 px 值
     * @param context 当前上下文
     * @param dpValue dp 值
     * @return 转换后的 px 值
     */
    fun dip2px(context: Context, dpValue: Double): Int {
        val density = context.resources.displayMetrics.density
        return (dpValue * density + 0.5).toInt()
    }

    /**
     * 获取屏幕宽度
     * @param context 当前上下文
     * @return 屏幕宽度（像素）
     */
    fun getScreenWidth(context: Context): Int {
        return context.resources.displayMetrics.widthPixels
    }
}
