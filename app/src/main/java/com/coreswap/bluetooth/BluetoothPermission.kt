package com.coreswap.bluetooth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * BLUETOOTH_CONNECT only exists on API 31+. On older releases the legacy BLUETOOTH permission is
 * declared in the manifest and granted at install time, and querying BLUETOOTH_CONNECT there always
 * reports DENIED, so the check must be version gated or nothing works on pre-31 devices.
 */
fun hasBluetoothConnectPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
