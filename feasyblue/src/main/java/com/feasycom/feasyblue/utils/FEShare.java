package com.feasycom.feasyblue.utils;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.UUID;

public class FEShare implements Serializable {

    public Intent intent = new Intent();
    public Calendar c_start = Calendar.getInstance();
    public Context context;

    public BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public BluetoothSocket socket;
    public BluetoothDevice device;
    public InputStream inputStream;
    public OutputStream outputStream;

    private static class FEShareHolder {
        static final FEShare INSTANCE = new FEShare();
    }

    public static FEShare getInstance() {
        return FEShareHolder.INSTANCE;
    }

    private FEShare() {

    }

    //readResolve方法应对单例对象被序列化时候
    private Object readResolve() {
        return getInstance();
    }

    @SuppressLint("MissingPermission")
    private void initSocket() throws IOException {
        final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";

        UUID uuid = UUID.fromString(SPP_UUID);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
            socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
        } else {
            socket = device.createRfcommSocketToServiceRecord(uuid);
        }
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
    }

    synchronized public boolean connect(BluetoothDevice device) {
        this.device = device;
        return SPPConnect();
    }

    /**
     * SPP连接该地址设备
     *
     * @throws IOException
     */
    @SuppressLint("MissingPermission")
    private boolean SPPConnect() {
        try {
            initSocket();
            sleep(100);
            try {
                socket.connect();
                return true;
            } catch (IOException e) {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 断开连接
     */
    synchronized public void disConnect() {
        new Thread(() -> {
            if (socket != null) {
                try {
                    outputStream.close();
                    inputStream.close();
                    socket.close();
                    socket = null;
                    c_start = Calendar.getInstance();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 发送数据
     *
     * @param
     * @return 返回包数
     */
    public int writes(final byte packgeSend[]) {
        int packets = 0;
        int length_bytes = packgeSend.length;
        // 分包
        final int perPacketLength = 2000;
        if (length_bytes > perPacketLength) {
            int startPoint = 0;
            byte[] bytes = new byte[perPacketLength];
            while (length_bytes > perPacketLength) {
                System.arraycopy(packgeSend, startPoint, bytes, 0, perPacketLength);
                try {
                    outputStream.write(bytes);
                    startPoint += perPacketLength;
                    length_bytes -= perPacketLength;

                    packets++;
                    Thread.sleep(30);
                } catch (Exception e) {
                    e.printStackTrace();
                    return packets;
                }
            }
            if (length_bytes != perPacketLength) {
                length_bytes = packgeSend.length % perPacketLength;
            }
            if (length_bytes > 0) {
                byte[] bytes_last = new byte[length_bytes];
                System.arraycopy(packgeSend, startPoint, bytes_last, 0, length_bytes);
                try {
                    outputStream.write(bytes_last);
                    packets++;
                } catch (IOException e) {
                    e.printStackTrace();
                    return packets;
                }
            }
            return packets;
        } else {
            try {
                outputStream.write(packgeSend);
                return 1;
            } catch (IOException e) {
                return 0;
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

}
