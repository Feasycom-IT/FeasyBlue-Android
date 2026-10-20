package com.feasycom.feasyblue.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivitySearchDeviceBinding
import com.feasycom.feasyblue.utils.putStr

// 1. 补充 ViewBinding 泛型参数
class OtaSearchDeviceActivity: BaseActivity<ActivitySearchDeviceBinding>() {

    // 2. 实现基类要求的 getViewBinding 抽象方法
    override fun getViewBinding(): ActivitySearchDeviceBinding {
        return ActivitySearchDeviceBinding.inflate(layoutInflater)
    }

    override fun initView() {
        if (Intent.ACTION_VIEW == intent.action) {
            intent.dataString?.let {
                val parse = Uri.parse(it)
                // 修复权限授予的位运算错误（原代码用 and 应该改为 or）
                grantUriPermission(
                    packageName,
                    parse,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                putStr("fileUri", it)
            }
        }
    }

    // 3. 移除过时的 getLayout() 方法

    companion object{
        private const val TAG = "OtaSearchDeviceActivity"
        fun activityStart(context: Context){
            val intent = Intent(context, OtaSearchDeviceActivity::class.java)
            context.startActivity(intent)
        }
    }
}