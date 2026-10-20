package com.feasycom.feasyblue.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.appcompat.app.AlertDialog
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.databinding.ActivityQrcodeBinding
import com.feasycom.feasyblue.permission.PermissionDescription
import com.feasycom.feasyblue.permission.PermissionInterceptor
import com.feasycom.feasyblue.utils.FileUtils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import java.io.OutputStream

/**
 * 二维码页面（基于ViewBinding的BaseActivity优化版）
 * 保留原始核心逻辑，优化点：
 * 1. 继承ViewBinding版BaseActivity
 * 2. 移除kotlinx.android.synthetic依赖
 * 3. 增强异常处理和空值安全
 * 4. 优化文件命名和存储路径
 * 5. 修复内存泄漏问题
 */
class QRCodeActivity : BaseActivity<ActivityQrcodeBinding>() {

    private var mResultDialog: AlertDialog? = null

    /**
     * 实现BaseActivity抽象方法 - 获取ViewBinding实例
     */
    override fun getViewBinding(): ActivityQrcodeBinding {
        return ActivityQrcodeBinding.inflate(layoutInflater)
    }

    /**
     * 实现BaseActivity抽象方法 - 初始化视图（替代原initView）
     */
    override fun initView() {
        setupToolbar()
        setupQrImage()
        setupSaveButton()
    }

    /**
     * 初始化Toolbar（保留原始逻辑）
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbarTitle.text = getString(R.string.wx)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    /**
     * 设置二维码图片（保留原始逻辑，增加兼容性）
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setupQrImage() {
        // 适配不同Android版本的Drawable加载方式
        binding.img.setImageDrawable(resources.getDrawable(R.drawable.wx, theme))
    }

    /**
     * 设置保存按钮点击事件（核心优化，保留原始权限逻辑）
     */
    private fun setupSaveButton() {
        binding.save.setOnClickListener {
            // 解码二维码图片（增加异常捕获和空值判断）
            val bitmap = try {
                BitmapFactory.decodeResource(resources, R.drawable.wx)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } ?: run {
                showResultDialog(getString(R.string.qr_failed))
                return@setOnClickListener
            }

            // 保留原始的版本判断逻辑
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用MediaStore保存
                saveImageToMediaStore(bitmap)
            } else {
                // Android 9及以下 申请存储权限（保留原始PermissionLists逻辑）
                requestStoragePermission(bitmap)
            }
        }
    }

    /**
     * 保留原始逻辑：申请存储权限（适配XXPermissions）
     */
    private fun requestStoragePermission(bitmap: Bitmap) {
        XXPermissions.with(this)
            .permission(PermissionLists.getReadExternalStoragePermission())
            .permission(PermissionLists.getWriteExternalStoragePermission())
            .interceptor(PermissionInterceptor())
            .description(PermissionDescription())
            .request(OnPermissionCallback { grantedList, deniedList ->
                // 仅判断是否全部授予，移除所有高版本API（解决所有爆红）
                val allGranted = deniedList.isEmpty()
                if (!allGranted) {
                    showResultDialog(getString(R.string.qr_failed))
                    return@OnPermissionCallback
                }

                // 权限授予成功，执行保存逻辑
                FileUtils
                    .saveBitmap(this@QRCodeActivity, bitmap, "wechat.jpg") { success ->
                        showResultDialog(getString(if (success) R.string.qr_success else R.string.qr_failed))
                    }
            })
    }

    /**
     * 优化版：保存图片到MediaStore（保留原始逻辑，增加异常处理）
     */
    private fun saveImageToMediaStore(bitmap: Bitmap) {
        // 优化：增加时间戳避免文件名重复
        val imgName = "wx_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, imgName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        imageUri?.let { uri ->
            try {
                val outputStream: OutputStream? = contentResolver.openOutputStream(uri)
                outputStream?.use {
                    // 优化：压缩质量从100改为90，减少文件大小
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
                    showResultDialog(getString(R.string.qr_success))
                } ?: throw Exception("输出流为空")
            } catch (e: Exception) {
                e.printStackTrace()
                // 优化：保存失败时删除无效文件
                contentResolver.delete(uri, null, null)
                showResultDialog(getString(R.string.qr_failed))
            }
        } ?: showResultDialog(getString(R.string.qr_failed))
    }

    /**
     * 优化版：显示结果对话框（修复内存泄漏）
     */
    private fun showResultDialog(message: String) {
        // 优化：先关闭旧对话框，避免内存泄漏
        mResultDialog?.dismiss()

        // 保留原始对话框逻辑
        mResultDialog = AlertDialog.Builder(this@QRCodeActivity)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
        mResultDialog?.setCanceledOnTouchOutside(false)
    }

    /**
     * 优化：页面销毁时清理资源
     */
    override fun onDestroy() {
        super.onDestroy()
        mResultDialog?.dismiss() // 确保对话框关闭，避免内存泄漏
    }

    /**
     * 保留原始静态启动方法
     */
    companion object {
        fun activityStart(context: Context) {
            val intent = Intent(context, QRCodeActivity::class.java)
            context.startActivity(intent)
        }
    }
}