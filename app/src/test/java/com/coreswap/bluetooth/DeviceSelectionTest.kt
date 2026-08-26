package com.coreswap.bluetooth

import com.coreswap.lib.wrapper.PairedDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceSelectionTest {
    private val macA = "AA:BB:CC:DD:EE:01"
    private val macB = "AA:BB:CC:DD:EE:02"
    private val deviceA = PairedDevice(macAddress = macA, model = "SoundcoreA3040", isDemo = false)
    private val deviceB = PairedDevice(macAddress = macB, model = "SoundcoreA3959", isDemo = false)
    private val paired = listOf(deviceA, deviceB)

    @Test
    fun `no connected device yields null`() {
        assertNull(
            selectTarget(
                paired = paired,
                connectedMacs = emptySet(),
                activeOutputMacs = emptySet(),
                priority = listOf(macA, macB),
            ),
        )
    }

    @Test
    fun `single connected device is chosen even when it is last in priority`() {
        assertEquals(
            deviceB,
            selectTarget(
                paired = paired,
                connectedMacs = setOf(macB),
                activeOutputMacs = emptySet(),
                priority = listOf(macA, macB),
            ),
        )
    }

    @Test
    fun `active audio output wins over priority order`() {
        assertEquals(
            deviceB,
            selectTarget(
                paired = paired,
                connectedMacs = setOf(macA, macB),
                activeOutputMacs = setOf(macB),
                priority = listOf(macA, macB),
            ),
        )
    }

    @Test
    fun `priority order breaks the tie when neither device is the active output`() {
        assertEquals(
            deviceB,
            selectTarget(
                paired = paired,
                connectedMacs = setOf(macA, macB),
                activeOutputMacs = emptySet(),
                priority = listOf(macB, macA),
            ),
        )
    }

    @Test
    fun `mac comparisons ignore case`() {
        assertEquals(
            deviceA,
            selectTarget(
                paired = paired,
                connectedMacs = setOf(macA.lowercase()),
                activeOutputMacs = emptySet(),
                priority = emptyList(),
            ),
        )
    }

    @Test
    fun `devices absent from priority keep paired order and sort after listed ones`() {
        assertEquals(
            deviceB,
            selectTarget(
                paired = paired,
                connectedMacs = setOf(macA, macB),
                activeOutputMacs = emptySet(),
                priority = listOf(macB),
            ),
        )
    }
}
