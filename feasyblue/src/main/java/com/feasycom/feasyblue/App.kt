package com.feasycom.feasyblue

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.blankj.utilcode.util.LogUtils
import com.feasycom.ble.controler.FscBleCentralApi
import com.feasycom.ble.controler.FscBleCentralApiImp
import com.feasycom.feasyblue.dao.DeviceDao
import com.feasycom.feasyblue.dao.DeviceDatabase
import com.feasycom.network.bean.Devices
import com.feasycom.spp.controler.FscSppCentralApi
import com.feasycom.spp.controler.FscSppCentralApiImp
import com.google.gson.Gson
import com.tencent.bugly.crashreport.CrashReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class App : Application() {

    private lateinit var sFscBleCentralApi: FscBleCentralApi
    private lateinit var sFscSppCentralApi: FscSppCentralApi

    private val deviceDao: DeviceDao by lazy {
        DeviceDatabase.getDataBase(applicationContext).deviceDao()
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        CrashReport.initCrashReport(getApplicationContext(), "7fce3273e0", true)



        // 多 dex 支持
        MultiDex.install(this)

        // 初始化 BLE 和 SPP API
        sFscBleCentralApi = FscBleCentralApiImp.getInstance(this).apply { initialize() }
        sFscSppCentralApi = FscSppCentralApiImp.getInstance(this).apply { initialize() }

        // 控制是否打印sdk日志，true打印false不打印
        sFscBleCentralApi.isShowLog(true)

        // 控制是否打印app日志，true打印false不打印
        LogUtils.getConfig().isLogSwitch = true

        val json = assetsToJsonString("Device.json")
        val gson = Gson()
        val devices = gson.fromJson(json, Devices::class.java)
        MainScope().launch(Dispatchers.IO) {
            deviceDao.deleteAllBeacons()
            deviceDao.addDevices(devices.data!!.list)
        }
    }

    // 将 assets 文件转换为 JSON 字符串
    private fun Context.assetsToJsonString(fileName: String): String {
        val stringBuilder = StringBuilder()
        try {
            val bf = BufferedReader(InputStreamReader(assets.open(fileName)))
            var line: String?
            while (bf.readLine().also { line = it } != null) {
                stringBuilder.append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return stringBuilder.toString()
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        var context: Context? = null
    }
}