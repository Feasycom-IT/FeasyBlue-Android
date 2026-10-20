package com.feasycom.feasyblue.utils

import android.content.Context
import android.os.Build
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists

object PermissionUtil {

    fun hasRequiredPermissions(context: Context): Boolean {
        val locationPermissionGranted =
        XXPermissions.isGrantedPermission(context, PermissionLists.getAccessFineLocationPermission())
        val bluetoothScanGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || XXPermissions.isGrantedPermission(
                context, PermissionLists.getBluetoothScanPermission()
            )
        val bluetoothConnectGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || XXPermissions.isGrantedPermission(
                context, PermissionLists.getBluetoothConnectPermission()
            )

        return locationPermissionGranted && bluetoothScanGranted && bluetoothConnectGranted
    }

}