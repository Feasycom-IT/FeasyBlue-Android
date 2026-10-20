package com.feasycom.feasyblue.presenters

import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import com.blankj.utilcode.util.LogUtils
import com.feasycom.feasyblue.interfaces.ICommandCallback
import com.feasycom.feasyblue.interfaces.ICommandPresenters
import java.util.concurrent.ConcurrentHashMap

object CommandPresenters: ICommandPresenters {

    private const val TAG = "CommandPresenters"

    // 用集合按 key 维度管理当前“有效开启”的开关（避免快切换导致 +1/-1 累计错误）
    private val activeKeys: MutableSet<String> = ConcurrentHashMap.newKeySet()

    // 兼容旧的 plus/minus 调用（如果还有其他地方在用），做下限保护
    private var legacyCount: Int = 0

    private val mainHandler = Handler(Looper.getMainLooper())

    var mICommandCallback: ICommandCallback? = null
        set(value) {
            // 绑定新的回调时清空状态，避免脏数据
            activeKeys.clear()
            legacyCount = 0
            LogUtils.d(TAG, "APP COMMAND reset: active=0, legacy=0")
            field = value
            // 立刻同步一次
            notifyUpdate()
        }

    // —— 新增：按 key 设置状态（推荐使用）——
    @MainThread
    fun setState(key: String, enabled: Boolean) {
        val changed = if (enabled) activeKeys.add(key) else activeKeys.remove(key)
        if (changed) notifyUpdate()
        else {
            // 即使集合没变化，也触发一次回调可选（避免极端时 UI 不刷新）
            notifyUpdate()
        }
    }

    // —— 兼容旧接口：简单 +1 / -1（有下限保护），但不再推荐使用 ——
    override fun plus() {
        legacyCount = (legacyCount + 1).coerceAtLeast(0)
        notifyUpdate()
    }

    override fun minus() {
        legacyCount = (legacyCount - 1).coerceAtLeast(0)
        notifyUpdate()
    }

    private fun notifyUpdate() {
        val i = activeKeys.size + legacyCount
        mainHandler.post {
            mICommandCallback?.update(i)
        }
    }
}
