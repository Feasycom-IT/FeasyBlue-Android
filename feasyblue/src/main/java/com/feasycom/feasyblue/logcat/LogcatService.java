package com.feasycom.feasyblue.logcat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.feasycom.feasyblue.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public final class LogcatService extends Service {

    private static final int NOTIFICATION_ID = 0x1001;
    private static final String CHANNEL_ID = "logcat_service_channel";
    private static final String TAG = "LogcatService";

    private Process logcatProcess;
    private FileOutputStream fileOutputStream;
    private boolean isCapturing = true;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notify = createNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notify, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notify);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(this::captureLogToFile).start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopCapture();
    }

    private void captureLogToFile() {
        File directory = getExternalFilesDir("Logcat");
        if (directory != null && !directory.exists()) {
            directory.mkdirs();
        }

        File logFile = new File(directory, "Logcat.txt");
        try {
            if (logFile.exists()) logFile.delete();
            logFile.createNewFile();
        } catch (Exception e) {
            Log.e(TAG, "创建文件失败", e);
            return;
        }

        try {
            fileOutputStream = new FileOutputStream(logFile);
            ProcessBuilder processBuilder = new ProcessBuilder("logcat", "-v", "time");
            processBuilder.redirectErrorStream(true);
            logcatProcess = processBuilder.start();

            InputStream inputStream = logcatProcess.getInputStream();
            byte[] buffer = new byte[1024];
            int length;

            while (isCapturing && (length = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, length);
                fileOutputStream.flush();
            }

        } catch (Exception e) {
            Log.e(TAG, "日志捕获异常", e);
        } finally {
            stopCapture();
        }
    }

    private void stopCapture() {

        try {
            isCapturing = false;
            if (logcatProcess != null) {
                logcatProcess.destroy();
                logcatProcess = null;
            }
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        } catch (Exception e) {

        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("日志服务运行中")
                .setContentText("正在采集APP日志")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSound(null)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "日志后台服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("用于记录APP运行日志");
            channel.enableVibration(false);
            channel.enableLights(false);
            channel.setSound(null, null);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}