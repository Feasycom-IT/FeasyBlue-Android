package com.feasycom.feasyblue.activity

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivityMainBinding
import com.feasycom.feasyblue.fragment.AboutFragment
import com.feasycom.feasyblue.fragment.CommunicationFragment
import com.feasycom.feasyblue.fragment.ForumFragment
import com.feasycom.feasyblue.fragment.SettingFragment
import com.feasycom.feasyblue.fragment.StoreFragment

/**
 * 主Activity，负责展示ViewPager和底部导航栏
 */
class MainActivity : BaseActivity<ActivityMainBinding>() {

    /**
     * 实现 ViewBinding 获取方法
     */
    override fun getViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    /**
     * 移除原 getLayout() 方法（ViewBinding 无需返回布局ID）
     */
    // override fun getLayout() = R.layout.activity_main

    override fun initView() {
        // 设置ViewPager的适配器
        with(binding.viewPager2) {
            adapter = object : FragmentStateAdapter(this@MainActivity) {
                override fun getItemCount() = 5
                override fun createFragment(position: Int): Fragment {
                    return when (position) {
                        0 -> CommunicationFragment.newInstance()
                        1 -> SettingFragment.newInstance()
                        2 -> StoreFragment.newInstance()
                        3 -> ForumFragment.newInstance()
                        else -> AboutFragment.newInstance()
                    }
                }
            }
            offscreenPageLimit = 1
            isUserInputEnabled = false
        }

        // 设置底部导航栏的选择监听器
        binding.navigationBar.setOnNavigationItemSelectedListener {
            binding.viewPager2.setCurrentItem(
                when (it.itemId) {
                    R.id.communication -> 0
                    R.id.setting -> 1
                    R.id.store -> 2
                    R.id.forum -> 3
                    else -> 4
                }, false
            )
            true
        }

        // 根据传入的位置参数设置初始选中项
        binding.navigationBar.selectedItemId = when (intent.getIntExtra("position", 0)) {
            0 -> R.id.communication
            1 -> R.id.setting
            2 -> R.id.store
            3 -> R.id.forum
            else -> R.id.about
        }
    }

    companion object {
        /**
         * 启动MainActivity，并传入指定的位置参数
         */
        fun activityStart(context: Context, position: Int) {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra("position", position)
            context.startActivity(intent)
        }
    }
}