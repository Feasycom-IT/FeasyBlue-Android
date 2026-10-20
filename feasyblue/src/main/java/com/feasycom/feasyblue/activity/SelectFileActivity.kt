package com.feasycom.feasyblue.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import android.text.Editable
import android.text.TextWatcher
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.LogUtils
import com.feasycom.feasyblue.R
import com.feasycom.feasyblue.adapter.SelectFileSizeAdapter
import com.feasycom.feasyblue.databinding.ActivitySelectFileBinding
import com.feasycom.feasyblue.decoration.DividerGridItemDecoration
import com.feasycom.feasyblue.utils.isKitkatOrAbove
import java.io.IOException

const val FILE_SIZE = 2000
const val FILE_PATH = 2001

/**
 * 文件选择页面
 * 修复 delete 方法未解析 + addTextChangedListener 语法错误 + 适配 ViewBinding
 */
class SelectFileActivity : BaseActivity<ActivitySelectFileBinding>() {

    private lateinit var mFileAdapter: SelectFileSizeAdapter

    override fun getViewBinding(): ActivitySelectFileBinding {
        return ActivitySelectFileBinding.inflate(layoutInflater)
    }

    @SuppressLint("SetTextI18n")
    override fun initView() {
        // 配置Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.let {
            it.setDisplayShowTitleEnabled(false)
            it.setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbarTitle.text = getString(R.string.selectFile)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // 初始化RecyclerView
        binding.recyclerView?.let { recyclerView ->
            recyclerView.layoutManager = GridLayoutManager(this, 2, RecyclerView.VERTICAL, false)
            recyclerView.adapter = SelectFileSizeAdapter().apply {
                mFileAdapter = this
                onClickListener = { size, data ->
                    returnFileSize(size, data)
                }
            }
            recyclerView.addItemDecoration(DividerGridItemDecoration(this))
        } ?: let {
            LogUtils.e(TAG, "APP FILE null")
        }

        // 自定义按钮点击事件
        binding.customizeButton.setOnClickListener {
            if (binding.editTv.text.isNullOrEmpty()|| binding.editTv.text.toString() == "0") {
                Toast.makeText(this, resources.getString(R.string.size_cannot_zero), Toast.LENGTH_SHORT).show()
            } else {
                returnFileSize(getTestFileSize(), getTestFileSizeStr())
                val inputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(binding.editTv.windowToken, 0)
            }
        }

        // 返回按钮
        binding.backBtn.setOnClickListener { finish() }
        // 清空按钮
        binding.clear.setOnClickListener {
            binding.editTv.text.clear()

        }
        // 选择文件按钮
        binding.photoBtn.setOnClickListener { performFileSearch() }

        // ========== 核心修复：delete 方法未解析 + 文本校验逻辑 ==========
        binding.editTv.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let { charSequence ->
                    if (charSequence.isNotBlank()) {
                        val maxValue = when (binding.myGroup.getSelectRadioButton()) {
                            0 -> 50000000
                            1 -> 50000
                            2 -> 50
                            else -> 50
                        }

                        try {
                            val inputValue = charSequence.toString().toInt()
                            if (inputValue > maxValue) {
                                // 修复：先转为 Editable，再删除最后一个字符（避免直接操作 CharSequence）
                                val editable = binding.editTv.text
                                if (editable.isNotEmpty()) {
                                    // 删除最后一个字符（正确的 Editable.delete 用法）
                                    editable.delete(editable.length - 1, editable.length)
                                    // 保持光标在最后
                                    binding.editTv.setSelection(editable.length)
                                }
                            }
                        } catch (e: NumberFormatException) {
                            // 非数字输入，清空
                            binding.editTv.text.clear()
                        }
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // 单选组点击事件
        binding.myGroup.onClickListener = { radioIndex ->
            if (binding.editTv.text.isNotBlank()) {
                when (radioIndex) {
                    2 -> {
                        val inputValue = try {
                            binding.editTv.text.toString().toInt()
                        } catch (e: NumberFormatException) {
                            0
                        }
                        if (inputValue > 50) {
                            binding.editTv.setText("50")
                            binding.editTv.setSelection(binding.editTv.text.toString().length)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    // 移除原 getLayout() 方法
    // override fun getLayout() = R.layout.activity_select_file

    /**
     * 选择文件逻辑（保留原始）
     */
    private fun performFileSearch() {
        val intent = if (isKitkatOrAbove()) {
            Intent(Intent.ACTION_OPEN_DOCUMENT)
        } else {
            Intent(Intent.ACTION_GET_CONTENT)
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        startActivityForResult(intent, FILE_PATH)
    }

    /**
     * 返回文件大小逻辑（保留原始）
     */
    private fun returnFileSize(size: Int, data: String) {
        val intent = Intent()
        intent.putExtra("size", size)
        intent.putExtra("sizeStr", data)
        setResult(FILE_SIZE, intent)
        finish()
    }

    /**
     * 计算测试文件大小（保留原始）
     */
    private fun getTestFileSize(): Int {
        return if (binding.editTv.text.isNotBlank()) {
            val inputValue = try {
                binding.editTv.text.toString().toInt()
            } catch (e: NumberFormatException) {
                0
            }
            when (binding.myGroup.getSelectRadioButton()) {
                0 -> inputValue * 10
                1 -> inputValue * 1000
                2 -> inputValue * 1000000
                else -> 0
            }
        } else {
            when (binding.myGroup.getSelectRadioButton()) {
                0 -> 50
                1 -> 50 * 1000
                2 -> 50 * 1000 * 1000
                else -> 0
            }
        }
    }

    /**
     * 获取文件大小字符串（保留原始）
     */
    private fun getTestFileSizeStr(): String {
        return if (binding.editTv.text.isNotBlank()) {
            val inputValue = binding.editTv.text.toString()
            when (binding.myGroup.getSelectRadioButton()) {
                0 -> "${inputValue}0 B"
                1 -> "${inputValue} KB"
                2 -> "${inputValue} MB"
                else -> "0 B"
            }
        } else {
            when (binding.myGroup.getSelectRadioButton()) {
                0 -> "50 B"
                1 -> "50 KB"
                2 -> "50 MB"
                else -> "0 B"
            }
        }
    }

    /**
     * 选择文件结果处理（保留原始）
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                FILE_PATH -> {
                    data?.data?.let { uri ->
                        if (isFileValid(uri)) {
                            setResult(FILE_PATH, data)
                            finish()
                        } else {
                            Toast.makeText(this, resources.getString(R.string.file_valid), Toast.LENGTH_SHORT).show()
                        }
                    } ?: run {
                        Toast.makeText(this, resources.getString(R.string.select_file_fail), Toast.LENGTH_SHORT).show()
                    }
                }
                else -> {
                    setResult(FILE_PATH, data)
                    finish()
                }
            }
        }
    }

    /**
     * 校验文件有效性（保留原始）
     */
    private fun isFileValid(uri: Uri): Boolean {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val available = inputStream.available()
                LogUtils.d(TAG, "Selected file size: $available bytes")
                available > 0
            } ?: false
        } catch (e: SecurityException) {
            LogUtils.e(TAG, "Security exception when checking file: ${e.message}")
            Toast.makeText(this, "没有权限访问该文件", Toast.LENGTH_SHORT).show()
            false
        } catch (e: IOException) {
            LogUtils.e(TAG, "IO exception when checking file: ${e.message}")
            Toast.makeText(this, "无法读取文件", Toast.LENGTH_SHORT).show()
            false
        } catch (e: Exception) {
            LogUtils.e(TAG, "Unexpected error when checking file: ${e.message}")
            Toast.makeText(this, "文件检查失败", Toast.LENGTH_SHORT).show()
            false
        }
    }

    companion object {
        const val TAG = "SelectFileActivity"
    }
}