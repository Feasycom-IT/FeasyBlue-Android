package com.feasycom.feasyblue.interfaces

import com.feasycom.wifi.bean.BluetoothDeviceWrapper

interface FeasyWiFiCallbacks {

    fun blePeripheralFound(device: BluetoothDeviceWrapper, record: ByteArray?) {
    }

    fun blePeripheralConnected() {
    }

    fun onConnectTimeout() {
    }

    fun blePeripheralDisconnected() {
    }

    fun packetReceived(strValue: String?) {
    }

    fun packetSend(strValue: String?) {
    }

    fun ipInformation(ip: String?) {
    }

    fun versionInformation(version: String?) {
    }

    fun success(ip: String) {
    }

    fun failure() {
    }

    fun onOtaSuccess() {

    }

    fun onOtaFailure() {

    }

    fun onOtaProgress(progress: String?) {
    }

    fun onOtaNetworkIsNotConfigured() {
    }

    fun onOtaStartConfig() {
    }
}