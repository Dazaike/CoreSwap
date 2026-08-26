# CoreSwap

Version **0.1.0**

Switches the ambient sound mode of Anker Soundcore headphones and earbuds from the app or, more
usefully, from separately launchable activities that MacroDroid (or any launcher/automation tool)
can start directly.

On launch a mode activity looks at Bluetooth, finds which of your configured Soundcore devices is
actually connected, connects to it over RFCOMM, sets the mode, and disconnects. It shows no UI
beyond a toast.

## Modes and component names

These component names are the automation contract and will not change:

| Mode | Component |
| --- | --- |
| Noise Canceling | `com.coreswap.app/com.coreswap.mode.SetNoiseCancelingActivity` |
| Transparency | `com.coreswap.app/com.coreswap.mode.SetTransparencyActivity` |
| Normal | `com.coreswap.app/com.coreswap.mode.SetNormalActivity` |

Debug builds use the applicationId `com.coreswap.app.debug`, so the debug component is
`com.coreswap.app.debug/com.coreswap.mode.SetTransparencyActivity`.

Each mode also gets its own launcher icon, so it can be triggered from the home screen or via
MacroDroid's "Launch Application" picker as well as "Launch Activity".

## First run

1. Open CoreSwap and grant the Bluetooth permission (Android 12+ only).
2. Tap **Add device**, pick the Bluetooth-bonded earbuds, then pick the matching model
   (for example `SoundcoreA3040` for the Q45). The model cannot be detected automatically —
   the engine requires it, so you have to say which device this is.
3. Repeat for any other Soundcore device. The order devices are added is the tie-break order used
   when more than one is connected and none is actively playing audio.

Devices actively receiving audio win over that order, so with two devices connected the one you are
listening to is the one that gets switched.

## MacroDroid setup

Add an action → **Launch Activity** → CoreSwap → the mode you want. If the picker does not list the
activities, use **Launch Application** instead; both intent filters are present for that reason.

## Toasts

Failures always toast. Success toasts are controlled by the **Show confirmation toast** switch in
the app (default on).

## Building

Requires the Android SDK (platform 37, build-tools 37.0.0), NDK `29.0.14206865`, JDK 21, a Rust
toolchain that can build the pinned engine (1.98.0 or newer), the `aarch64-linux-android` Rust
target, and `cargo-ndk` 4.1.2. `local.properties` must point `sdk.dir` at the SDK.

```sh
git submodule update --init --recursive
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease      # unsigned
./gradlew testDebugUnitTest    # device-selection precedence tests
```

The build is arm64-only (`arm64-v8a` / `aarch64-linux-android`). To target another ABI, change
`abiAndroid`/`abiRust` in `app/build.gradle.kts` and add the matching Rust target.

## How it works

CoreSwap does not fork the OpenSCQ30 app. It vendors the upstream repository as a submodule at
`external/OpenSCQ30`, pinned to commit `dbf496e4e1a9c7e8dae0e22a38fa2c0622da91db` (v2.11.0), and
reuses only its engine: the Rust `openscq30-lib` core exposed to Kotlin through the
`openscq30-android` uniffi cdylib. The Kotlin app, UI, and mode activities are ours.

`cargo ndk` builds `libopenscq30_android.so`, `uniffi-bindgen` generates Kotlin bindings into the
`com.coreswap.lib.bindings` package (see `app/uniffi.toml`), and both are wired into the Android
variants by the tasks in `buildSrc`. RFCOMM socket work happens in Kotlin
(`app/src/main/java/com/coreswap/bluetooth/ConnectionBackends.kt`) because the Rust side delegates
it to the platform.

Mode switching writes the `ambientSoundMode` setting with one of the values `NoiseCanceling`,
`Transparency`, or `Normal`. Devices that do not expose that setting report an error toast instead.

## License

GPL-3.0-or-later. `openscq30-lib` is GPL-3.0-or-later and is linked into the APK, so CoreSwap is
too. See `LICENSE.txt`.
