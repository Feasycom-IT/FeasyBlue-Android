package com.feasycom.feasyblue.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.blankj.utilcode.util.LogUtils
import com.feasycom.network.bean.DeviceInfo

@Database(version = 1, entities = [DeviceInfo::class], exportSchema = false)
abstract class DeviceDatabase : RoomDatabase() {

    abstract fun deviceDao(): DeviceDao

    companion object {
        const val TAG: String = "DeviceDatabase"
        private var instance: DeviceDatabase? = null


        fun getDataBase(context: Context): DeviceDatabase {
            LogUtils.d(TAG, "APP DATABASE 数据库包名 => ${context.packageName}")
            instance?.let {
                return it
            }
            return Room.databaseBuilder(
                context.applicationContext,
                DeviceDatabase::class.java,
                context.packageName + "_app_database"
            )
                .build()
                .apply { instance = this }
        }
    }

}