package com.coreswap.ui

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.coreswap.bluetooth.connectedMacs
import com.coreswap.bluetooth.hasBluetoothConnectPermission
import com.coreswap.core.KeepAliveService
import com.coreswap.core.Prefs
import com.coreswap.core.SessionHolder
import com.coreswap.lib.bindings.deviceModels
import com.coreswap.lib.bindings.translateDeviceModel
import com.coreswap.lib.wrapper.PairedDevice
import com.coreswap.mode.ModeSwitcher
import kotlinx.coroutines.launch

private data class BondedDevice(val name: String, val macAddress: String)

class MainActivity : ComponentActivity() {
    private var paired by mutableStateOf<List<PairedDevice>>(emptyList())
    private var connected by mutableStateOf<Set<String>>(emptySet())
    private var busy by mutableStateOf(false)
    private var keepAliveEnabled by mutableStateOf(false)
    private var toastOnSuccess by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toastOnSuccess = Prefs.toastOnSuccess(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { refresh() }
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        !hasBluetoothConnectPermission(this@MainActivity)
                    ) {
                        permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                }
                MainScreen()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        refresh()
    }

    private fun refresh() {
        // onStart also runs when returning from accessibility settings, so this reflects the switch.
        keepAliveEnabled = KeepAliveService.isEnabled(this)
        lifecycleScope.launch {
            paired = runCatching { SessionHolder.get(applicationContext).pairedDevices() }
                .getOrElse {
                    toast(it.message ?: "Could not read configured devices")
                    emptyList()
                }
            connected = connectedMacs(applicationContext)
        }
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            toast("Enable \"CoreSwap keep-alive\" in this list")
        } catch (_: ActivityNotFoundException) {
            toast("Could not open accessibility settings")
        }
    }

    private fun toast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun switchMode(mode: String) {
        if (busy) return
        busy = true
        lifecycleScope.launch {
            try {
                val modelName = ModeSwitcher.apply(applicationContext, lifecycleScope, mode)
                if (toastOnSuccess) toast("$modelName: ${ModeSwitcher.label(mode)}")
            } catch (t: Throwable) {
                toast(t.message ?: "Mode switch failed")
            } finally {
                busy = false
            }
        }
    }

    private fun addDevice(macAddress: String, model: String) {
        lifecycleScope.launch {
            try {
                SessionHolder.get(applicationContext)
                    .pair(PairedDevice(macAddress = macAddress, model = model, isDemo = false))
                Prefs.addToPriority(applicationContext, macAddress)
            } catch (t: Throwable) {
                toast(t.message ?: "Could not add device")
            }
            refresh()
        }
    }

    private fun removeDevice(macAddress: String) {
        lifecycleScope.launch {
            try {
                SessionHolder.get(applicationContext).unpair(macAddress)
                Prefs.removeFromPriority(applicationContext, macAddress)
            } catch (t: Throwable) {
                toast(t.message ?: "Could not remove device")
            }
            refresh()
        }
    }

    private fun bondedDevices(): List<BondedDevice> {
        if (!hasBluetoothConnectPermission(this)) return emptyList()
        val adapter = getSystemService(BluetoothManager::class.java)?.adapter ?: return emptyList()
        val alreadyPaired = paired.map { it.macAddress.uppercase() }.toSet()
        return adapter.bondedDevices.orEmpty()
            .filterNot { it.address.uppercase() in alreadyPaired }
            .map { BondedDevice(name = it.name ?: "Unknown", macAddress = it.address) }
            .sortedBy { it.name }
    }

    @Composable
    private fun MainScreen() {
        var pendingDevice by remember { mutableStateOf<BondedDevice?>(null) }
        var bondedChoices by remember { mutableStateOf<List<BondedDevice>?>(null) }

        Scaffold { insets ->
            Column(
                modifier = Modifier
                    .padding(insets)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Devices", style = MaterialTheme.typography.titleMedium)
                if (paired.isEmpty()) {
                    Text(
                        "No devices configured yet. Add the Soundcore device you want to control.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                paired.forEach { device ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(translateDeviceModel(device.model))
                                Text(device.macAddress, style = MaterialTheme.typography.bodySmall)
                                if (device.macAddress.uppercase() in connected.map { it.uppercase() }) {
                                    Text("connected", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            TextButton(onClick = { removeDevice(device.macAddress) }) { Text("Remove") }
                        }
                    }
                }
                Button(onClick = { bondedChoices = bondedDevices() }) { Text("Add device") }

                HorizontalDivider()

                Text("Switch mode", style = MaterialTheme.typography.titleMedium)
                Button(
                    onClick = { switchMode(ModeSwitcher.MODE_NOISE_CANCELING) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Noise Canceling") }
                Button(
                    onClick = { switchMode(ModeSwitcher.MODE_TRANSPARENCY) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Transparency") }
                Button(
                    onClick = { switchMode(ModeSwitcher.MODE_NORMAL) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Normal") }
                if (busy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        Text("Switching…")
                    }
                }

                HorizontalDivider()

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show confirmation toast")
                    }
                    Switch(
                        checked = toastOnSuccess,
                        onCheckedChange = {
                            toastOnSuccess = it
                            Prefs.setToastOnSuccess(applicationContext, it)
                        },
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Keep running in background")
                        Text(
                            if (keepAliveEnabled) {
                                "On. Switches respond immediately."
                            } else {
                                "Off. Switches cold start and take longer. Turn on the " +
                                    "\"CoreSwap keep-alive\" accessibility service to fix that."
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = keepAliveEnabled,
                        // Only the user can enable an accessibility service, so this opens settings.
                        onCheckedChange = { openAccessibilitySettings() },
                    )
                }

                HorizontalDivider()

                Text("MacroDroid", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Launch these activities to switch modes without opening the app:\n" +
                        "com.coreswap.mode.SetNoiseCancelingActivity\n" +
                        "com.coreswap.mode.SetTransparencyActivity\n" +
                        "com.coreswap.mode.SetNormalActivity",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        bondedChoices?.let { choices ->
            BondedDevicePickerDialog(
                devices = choices,
                onDismiss = { bondedChoices = null },
                onPick = {
                    bondedChoices = null
                    pendingDevice = it
                },
            )
        }

        pendingDevice?.let { device ->
            ModelPickerDialog(
                device = device,
                onDismiss = { pendingDevice = null },
                onPick = { model ->
                    pendingDevice = null
                    addDevice(device.macAddress, model)
                },
            )
        }
    }
}

@Composable
private fun BondedDevicePickerDialog(
    devices: List<BondedDevice>,
    onDismiss: () -> Unit,
    onPick: (BondedDevice) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick a paired bluetooth device") },
        text = {
            if (devices.isEmpty()) {
                Text("No bluetooth devices are paired with this phone, or they are all already configured.")
            } else {
                LazyColumn {
                    items(devices) { device ->
                        TextButton(
                            onClick = { onPick(device) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(device.name)
                                Text(device.macAddress, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ModelPickerDialog(device: BondedDevice, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    var filter by remember { mutableStateOf("") }
    // The engine cannot detect the model over RFCOMM, so the user has to say which one this is.
    val models = remember { deviceModels().map { it to translateDeviceModel(it) }.sortedBy { it.second } }
    val shown = models.filter { (_, translated) -> translated.contains(filter, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Which model is ${device.name}?") },
        text = {
            Column {
                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    label = { Text("Search models") },
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn {
                    items(shown) { (model, translated) ->
                        TextButton(
                            onClick = { onPick(model) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                translated,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
