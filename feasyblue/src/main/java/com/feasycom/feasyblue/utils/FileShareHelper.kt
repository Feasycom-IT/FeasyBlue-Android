package com.feasycom.feasyblue.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.feasycom.feasyblue.R
import java.io.File

object FileShareHelper {

    fun shareFile(context: Context, fileUri: Uri, mimeType: String) {
        try {

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, fileUri)

                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }


            val chooser = Intent.createChooser(
                shareIntent,
                context.getString(R.string.logcat_options_share)
            )


            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                context.getString(R.string.unable_to_share_file),
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    fun shareFile(context: Context, file: File, mimeType: String) {
        try {

            if (!file.exists() || !file.isFile) {
                Toast.makeText(context, "文件不存在或已删除", Toast.LENGTH_SHORT).show()
                return
            }

            val fileUri = getFileUri(context, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                this.type = mimeType
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.grantUriPermission(
                "com.tencent.mm",
                fileUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            val chooser = Intent.createChooser(
                shareIntent,
                context.getString(R.string.logcat_options_share)
            )

            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "分享失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun getFileUri(context: Context, file: File): Uri {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } else {
            Uri.fromFile(file)
        }
    }
}