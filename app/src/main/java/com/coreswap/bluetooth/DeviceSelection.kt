package com.coreswap.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.util.Log
import com.coreswap.lib.wrapper.PairedDevice
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "DeviceSelection"
private const val PROFILE_PROXY_TIMEOUT_MS = 3_000L

/**
 * Picks which configured device to talk to. Pure so the precedence rules are unit testable:
 * only connected devices are candidates, the device currently receiving audio wins, and user
 * priority order breaks any remaining tie.
 */
fun selectTarget(
    paired: List<PairedDevice>,
    connectedMacs: Set<String>,
    activeOutputMacs: Set<String>,
    priority: List<String>,
): PairedDevice? {
    val connected = connectedMacs.mapTo(HashSet()) { it.uppercase() }
    val activeOutputs = activeOutputMacs.mapTo(HashSet()) { it.uppercase() }
    val priorityIndex = priority.withIndex().associate { (index, mac) -> mac.uppercase() to index }

    val candidates = paired.filter { it.macAddress.uppercase() in connected }
    if (candidates.isEmpty()) return null

    val active = candidates.filter { it.macAddress.uppercase() in activeOutputs }
    val preferred = active.ifEmpty { candidates }

    // sortedBy is stable, so devices missing from the priority list keep their original order.
    return preferred.sortedBy { priorityIndex[it.macAddress.uppercase()] ?: Int.MAX_VALUE }.first()
}

/** Macs of devices connected over A2DP or HFP. Empty when bluetooth is unavailable or denied. */
suspend fun connectedMacs(context: Context): Set<String> {
    if (!hasBluetoothConnectPermission(context)) {
        Log.w(TAG, "missing bluetooth permission, cannot list connected devices")
        return emptySet()
    }
    val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
    if (adapter == null) {
        Log.w(TAG, "no bluetooth adapter")
        return emptySet()
    }
    return buildSet {
        for (profile in intArrayOf(BluetoothProfile.A2DP, BluetoothProfile.HEADSET)) {
            addAll(connectedMacsForProfile(context, adapter, profile))
        }
    }
}

private suspend fun connectedMacsForProfile(
    context: Context,
    adapter: BluetoothAdapter,
    profile: Int,
): Set<String> = withTimeoutOrNull(PROFILE_PROXY_TIMEOUT_MS) {
    suspendCancellableCoroutine { continuation ->
        val listener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(serviceProfile: Int, proxy: BluetoothProfile) {
                val macs = try {
                    proxy.connectedDevices.map { it.address }.toSet()
                } catch (ex: SecurityException) {
                    Log.w(TAG, "denied reading connected devices for profile $serviceProfile", ex)
                    emptySet()
                } finally {
                    adapter.closeProfileProxy(serviceProfile, proxy)
                }
                if (continuation.isActive) continuation.resume(macs)
            }

            override fun onServiceDisconnected(serviceProfile: Int) {
                if (continuation.isActive) continuation.resume(emptySet())
            }
        }
        if (!adapter.getProfileProxy(context, listener, profile)) {
            Log.w(TAG, "could not get profile proxy for $profile")
            if (continuation.isActive) continuation.resume(emptySet())
        }
    }
} ?: run {
    Log.w(TAG, "timed out getting profile proxy for $profile")
    emptySet()
}

/** Macs of bluetooth outputs the system is currently routing audio to. */
fun activeOutputMacs(context: Context): Set<String> {
    val audioManager = context.getSystemService(AudioManager::class.java) ?: return emptySet()
    return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        .filter { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
        .map { it.address.orEmpty() }
        .filter { it.isNotBlank() }
        .toSet()
}
