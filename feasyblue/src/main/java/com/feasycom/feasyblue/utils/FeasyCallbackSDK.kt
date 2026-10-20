package com.feasycom.feasyblue.utils

import android.content.Context
import android.util.Log
import com.blankj.utilcode.util.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class FeasyCallbackSDK {

    companion object {
    //    private const val COMMON_URL = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key="

        // 应用包名和对应的key
        private val APP_KEYS = mapOf(
            "com.feasycom.feasyblue" to "033dbc3c-dfde-4184-9536-9ff707cd0e4e",
            "com.feasycom.feasybeacon" to "903ed4b4-4aff-4aeb-b4e7-d5128eb95714",
            "com.feasycom.feasywifi" to "24de55ae-9201-460d-a19d-65ace7d4d385",
            "com.feasycom.feasysens" to "0386cf14-b0d9-47ae-a1fc-d2e622607631",
            "com.feasycom.feasymesh" to "0e821f50-45ca-43ef-91f3-2d8e2d538ea4"
        )

        // 反馈类型
        private val FEEDBACK_TYPES = mapOf(
            0 to "建议",
            1 to "功能异常",
            2 to "界面异常",
            3 to "合作"
        )

        var complete: ((success: Boolean) -> Unit)? = null

        fun createJsonObject(context: Context, content: String, feedbackType: Int,email: String): JSONObject {

            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val appName = packageInfo.applicationInfo?.let { context.getString(it.labelRes) }
            val appVersion = packageInfo.versionName
            val systemVersion = android.os.Build.VERSION.RELEASE
            val phoneType = "${android.os.Build.BRAND}(${android.os.Build.MODEL})"
            val fbType = FEEDBACK_TYPES[feedbackType] ?: "建议"
            val contentJson = JSONObject().apply {
                put("platform", "Android")
                put("appName", appName)
                put("appBoundId", "com.feasycom.feasyblue")
                put("appVersion", appVersion)
                put("systemVersion", systemVersion)
                put("deviceModel", phoneType)
                put("type", fbType)
                put("content", content)
                //put("tel", "0755-1234567")
                //put("phone", "13123456789")
                put("email", email)
                //put("wechat", "wx12345")
                //put("QQ", "123456")
            }

            val rootJson = JSONObject().apply {
                put("content", contentJson)
            }

            return rootJson
        }

        /**
         * 反馈
         * @param context 当前Context
         * @param content 反馈内容
         * @param feedbackType 反馈类型
         */
        fun feedback(context: Context, content: String, feedbackType: Int,email: String) {
            val fbType = FEEDBACK_TYPES[feedbackType] ?: "建议"
        //    val urlString = "$COMMON_URL${APP_KEYS[context.packageName] ?: APP_KEYS["com.feasycom.feasyblue"]}"


       /*     {
                "content": {
                "platform": "Android",
                "appName": "FeasyBlue",
                "appBoundId": "com.feasycom.feasyblue",
                "appVersion": "1.0",
                "systemVersion": "1.2.3",
                "deviceModel": "iphone2,4",
                "type": "建议",
                "content": "建议加快反应速度",
                "tel": "0755-1234567",
                "phone": "13123456789",
                "email": "text@text.com",
                "wechat": "wx12345",
                "QQ": "123456"
            }
            }*/



            val urlString = "https://feasycomm.com/api/feedback/info/add"
            postJson(urlString,createJsonObject(context,content,feedbackType,email).toString()){
                    success, response ->
                if (success) {
                    complete?.invoke(success)
                    Log.e("vvv","请求成功：$response")
                } else {
                    complete?.invoke(success)
                    Log.e("vvv","请求失败：$response")
                }
            }


   /*         GlobalScope.launch(Dispatchers.IO) {
                try {
                    val url = URL(urlString)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    conn.doOutput = true
                    conn.outputStream.use { os ->
                        os.write(params(context, content, fbType,email))
                    }
                    complete?.invoke(conn.responseCode == HttpURLConnection.HTTP_OK)
                } catch (e: Exception) {
                    complete?.invoke(false)
                    e.printStackTrace()
                }
            }*/
        }



        /**
         * 简化的 JSON POST 请求方法
         * @param urlString 请求地址
         * @param jsonBody JSON 字符串参数
         * @param headers 自定义请求头（可选）
         * @param onResult 结果回调：Boolean 表示是否成功，String? 表示响应内容或错误信息
         */
        fun postJson(
            urlString: String,
            jsonBody: String,
            headers: Map<String, String> = emptyMap(),
            onResult: (Boolean, String?) -> Unit
        ) {
            GlobalScope.launch(Dispatchers.IO) {
                var connection: HttpURLConnection? = null
                try {
                    val url = URL(urlString)
                    connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000

                    // 设置请求头
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    headers.forEach { (key, value) ->
                        connection.setRequestProperty(key, value)
                    }

                    // 发送 JSON 数据
                    connection.outputStream.use { outputStream ->
                        BufferedWriter(OutputStreamWriter(outputStream, "UTF-8")).use { writer ->
                            writer.write(jsonBody)
                            writer.flush()
                        }
                    }

                    // 获取响应
                    val responseCode = connection.responseCode
                    val inputStream = if (responseCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }

                    val response = inputStream?.bufferedReader(Charsets.UTF_8)?.readText() ?: ""

                    // 返回结果
                    withContext(Dispatchers.Main) {
                        if (responseCode in 200..299) {
                            onResult(true, response)
                        } else {
                            onResult(false, "HTTP $responseCode: $response")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        onResult(false, e.message)
                    }
                    e.printStackTrace()
                } finally {
                    connection?.disconnect()
                }
            }
        }


        /**
         * 创建请求参数
         * @param context 当前Context
         * @param content 反馈内容
         * @param fbType 反馈类型
         */
        private fun params(context: Context, content: String, fbType: String, email: String): ByteArray {
            return try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val appName = packageInfo.applicationInfo?.let { context.getString(it.labelRes) }
                val appVersion = packageInfo.versionName
                val systemVersion = android.os.Build.VERSION.RELEASE
                val phoneType = "${android.os.Build.BRAND}(${android.os.Build.MODEL})"

                val markdownContent = """
                    <font color="warning">Android </font>《$appName》用户反馈
                    >类型：<font color="comment">$fbType</font>
                    >APP版本：<font color="comment">$appVersion</font>
                    >系统版本：<font color="comment">$systemVersion</font>
                    >手机型号：<font color="comment">$phoneType</font>
                    >反馈内容：<font color="comment">$content</font>
                    >邮箱：<font color="comment">$email</font>
                """.trimIndent()

                val map = JSONObject().apply {
                    put("msgtype", "markdown")
                    put("markdown", JSONObject().apply {
                        put("content", markdownContent)
                    })
                }
                LogUtils.d("TAG", "map.toString() => ${map.toString()}")
                map.toString().toByteArray()
            } catch (e: Exception) {
                e.printStackTrace()
                ByteArray(0)
            }
        }
    }
}