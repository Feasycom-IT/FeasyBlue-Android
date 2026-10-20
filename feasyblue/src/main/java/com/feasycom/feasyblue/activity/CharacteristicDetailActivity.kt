package com.feasycom.feasyblue.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.adapter.CharacteristicsDetailAdapter
import com.feasycom.feasyblue.databinding.ActivityCharacteristicDetailBinding

/**
 * 特征详情页面（ViewBinding 优化版）
 * 移除 kotlinx.android.synthetic 依赖，适配 BaseActivity<VB> 架构
 */
class CharacteristicDetailActivity : BaseActivity<ActivityCharacteristicDetailBinding>() {

    private lateinit var mAddress: String

    /**
     * 实现 BaseActivity 抽象方法 - 获取 ViewBinding 实例
     */
    override fun getViewBinding(): ActivityCharacteristicDetailBinding {
        return ActivityCharacteristicDetailBinding.inflate(layoutInflater)
    }

    /**
     * 初始化视图（替换原 initView，移除 synthetic 依赖）
     */
    @SuppressLint("SetTextI18n")
    override fun initView() {
        // 1. 获取地址参数，无参数则关闭页面
        mAddress = intent?.getStringExtra("address") ?: run {
            finish()
            return
        }

        // 2. 初始化 Toolbar（替换 synthetic 引用）
        setupToolbar()

        // 3. 初始化特征信息和列表
        setupCharacteristicInfo()
    }

    /**
     * 初始化 Toolbar
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbarTitle.text = getString(R.string.characteristicInfo)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    /**
     * 初始化特征信息和 RecyclerView
     */
    private fun setupCharacteristicInfo() {
        // 获取蓝牙特征数据
        val characteristic = ServiceSelectActivity.bluetoothGattCharacteristic ?: return

        // 设置 UUID 显示（替换 synthetic 引用）
        binding.uuid.text = "${characteristic.uuid}"

        // 初始化 RecyclerView（替换 synthetic 引用）
        with(binding.recyclerView) {
            layoutManager = LinearLayoutManager(this@CharacteristicDetailActivity)
            adapter = CharacteristicsDetailAdapter(mAddress, characteristic)
        }
    }

    /**
     * 移除原 getLayout 方法（由 ViewBinding 替代）
     */
    // 注意：删除原有的 override fun getLayout() = R.layout.activity_characteristic_detail

    /**
     * 静态启动方法（保留原始逻辑）
     */
    companion object {
        @RequiresApi(Build.VERSION_CODES.N)
        fun activityStart(context: Context, address: String) {
            val intent = Intent(context, CharacteristicDetailActivity::class.java)
            intent.putExtra("address", address)
            context.startActivity(intent)
        }
    }
}