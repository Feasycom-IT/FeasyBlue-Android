//package com.feasycom.feasyblue.viewmodel
//
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import androidx.work.OneTimeWorkRequest
//import androidx.work.WorkManager
//import com.feasycom.feasyblue.App
//import com.feasycom.feasyblue.R
//import com.feasycom.feasyblue.worker.DeviceWorker
//import com.feasycom.feasyblue.worker.SplashWorker
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//
///**
// * Splash 页面的 ViewModel，负责处理初始化数据和后台任务调度
// */
//class SplashViewModel : ViewModel() {
//
//    // LiveData 用于通知 URL 或资源数据变化
//    var mUrlLiveData = MutableLiveData<Any>()
//
//    // 存储文件列表
//    var mFiles: MutableList<String> = mutableListOf()
//
//    // WorkManager 实例
//    private val workManager by lazy {
//        WorkManager.getInstance(App.context!!)
//    }
//
//    // SplashWorker 后台任务请求
//    private val splashWorker by lazy {
//        OneTimeWorkRequest.Builder(SplashWorker::class.java)
//            .build()
//    }
//
//    // DeviceWorker 后台任务请求
//    private val deviceWorker by lazy {
//        OneTimeWorkRequest.Builder(DeviceWorker::class.java)
//            .build()
//    }
//
//    init {
//        // 在 ViewModel 初始化时加载文件并启动后台任务
//        viewModelScope.launch(Dispatchers.IO) {
//            // 获取应用文件列表
//            mFiles = getFile()
//            // 根据文件列表大小决定加载的数据
//            if (mFiles.isEmpty()) {
////                mUrlLiveData.postValue(R.drawable.load) // 如果没有文件，显示默认加载图像
//            } else {
//                // 如果存在图片文件，显示最后一个图片文件，否则显示默认加载图像
//                mFiles.filter {
//                    it.contains(".png") || it.contains(".jpg")
//                }.let {
//                    if (it.isNotEmpty()) {
//                        mUrlLiveData.postValue(it[it.lastIndex])
//                    } else {
////                        mUrlLiveData.postValue(R.drawable.load)
//                    }
//                }
//            }
//            // 启动后台任务链
//            workManager.beginWith(listOf(deviceWorker, splashWorker)).enqueue()
//        }
//    }
//
//    // 获取应用文件列表
//    private fun getFile(): MutableList<String> {
//        return App.context!!.fileList().toMutableList()
//    }
//
//    companion object {
//        private const val TAG = "SplashViewModel"
//    }
//
//}