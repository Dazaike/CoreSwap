# CoreSwap

Version **0.4.1**

Switches the ambient sound mode of Anker Soundcore headphones and earbuds from the app or, more
usefully, from separately launchable activities that MacroDroid (or any launcher/automation tool)
can start directly.

On launch a mode activity looks at Bluetooth, finds which of your configured Soundcore devices is
actually connected, connects to it over RFCOMM, sets the mode, and disconnects. It shows no UI
beyond a toast.

## Modes and component names

These component names are the automation contract and will not change:

| Mode | Component | Intent action |
| --- | --- | --- |
| Noise Canceling | `com.coreswap.app/com.coreswap.mode.SetNoiseCancelingActivity` | `com.coreswap.action.SET_ANC` |
| Transparency | `com.coreswap.app/com.coreswap.mode.SetTransparencyActivity` | `com.coreswap.action.SET_TRANSPARENCY` |
| Normal | `com.coreswap.app/com.coreswap.mode.SetNormalActivity` | `com.coreswap.action.SET_NORMAL` |

Debug builds use the applicationId `com.coreswap.app.debug`, so the debug component is
`com.coreswap.app.debug/com.coreswap.mode.SetTransparencyActivity`.

The mode activities are exported and also answer `android.intent.action.VIEW`, so they can be
started by component name or by their own action.

## App shortcuts

CoreSwap has one launcher icon. Long-press it and the launcher offers **ANC**, **Transparency**,
and **Normal**; any of them can be dragged out to become a home-screen icon of its own. The
shortcuts are declared statically in `res/xml/shortcuts.xml` and also published as dynamic
shortcuts at startup (`AppShortcuts`), so launchers that only read one of the two still show them.

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
activities, add an **Intent** action instead: intent type *Activity*, action set to the mode's
`com.coreswap.action.SET_*` value from the table above.

## Toasts

Failures always toast. Success toasts are controlled by the **Show confirmation toast** switch in
the app (default on).

## Keeping switches instant

A cold start has to load the 22 MB engine before it can talk to the earbuds, which makes the first
switch after a while noticeably slow. The **Keep running in background** row in the app opens
accessibility settings, where enabling **CoreSwap keep-alive** lets Android keep the process bound
and restart it if it dies. On connect, the service opens the engine session so the mode-switch path
pays for neither the cold start nor the first database read.

The service is a deliberate no-op: it requests no accessibility event types and declares
`canRetrieveWindowContent="false"`, so it is never told what is on screen. It is optional — mode
switching works with it off, just slower. Only the user can enable it; the app cannot.

## Building

Requires the Android SDK (platform 37, build-tools 37.0.0), NDK `29.0.14206865`, JDK 21, a Rust
toolchain that can build the pinned engine (1.98.0 or newer), the `aarch64-linux-android` Rust
target, and `cargo-ndk` 4.1.2. `local.properties` must point `sdk.dir` at the SDK.

```sh
git submodule update --init --recursive
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease      # signed if local.properties has signing.* keys, else unsigned
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

### Auto-Transparency on pause

An optional setting switches to Transparency when playback pauses and restores the previous mode
when it resumes. It watches media sessions through `PlaybackWatcherService`, which needs notification
access (it only reads play/pause state, never notifications). Buffering and track changes count as
playing; the pause delay is adjustable from 0 to 10 seconds. Optionally, Shizuku can grant that
access only while the setting is on. A debug switch holds the connection open and polls the mode
every 0.5 s.

## Changelog

### 0.4.1

- Added a **Modes** app shortcut (long-press the launcher icon) that opens the mode menu.
- Restyled the main screen and the mode menu with the Prism design language: liquid-glass
  controls, Outfit font, dark theme. The device and model pickers are now sheets instead of
  dialogs, and messages use in-app toasts. The mode menu and the main screen share the same
  glass mode grid.
- Release APKs are signed from keys in `local.properties` (`4ad12ac`).

### 0.4.0

- Added **Transparency when playback pauses**: switches to Transparency after a configurable delay
  (0–10 s) and restores the previous mode on resume, unless the mode was changed in between.
  Driven by media-session state via the new `PlaybackWatcherService` (notification access).
- Added optional **Shizuku** management of that notification access, granted only while the
  feature is on.
- Added a **Current mode** card on the main screen.
- Added a debug switch that keeps the device connected and polls the mode every 0.5 s; mode switches
  reuse that connection while it runs.
- Added `ModeMenuActivity`, an overlay mode picker, with an optional home-screen icon.
- Updated the shortcut icons.

### 0.3.0

- Added launcher app shortcuts for the three modes, declared both statically
  (`res/xml/shortcuts.xml`) and dynamically at startup, so long-pressing the CoreSwap icon switches
  modes and each shortcut can be dragged onto the home screen.
- The mode activities no longer register `MAIN`/`LAUNCHER`; CoreSwap now installs a single launcher
  icon instead of four. They stay exported and gained `VIEW` plus a per-mode
  `com.coreswap.action.SET_*` action, so component-name automation keeps working and action-based
  automation is now possible.

### 0.2.0

- Added the optional **CoreSwap keep-alive** accessibility service, which keeps the process warm
  and pre-opens the engine session so mode switches respond immediately. Toggle discoverable from
  the main screen; enabling it is up to the user.
- Mode activities use a dedicated `Theme.CoreSwap.Headless` with an empty `taskAffinity` and
  `finishOnTaskLaunch`, so triggering a mode never leaves a stray task or steals the recents entry.
- The app is dark only: the window background is pinned and Compose uses `darkColorScheme()`, which
  removes the white flash on launch. Dropped the API-29 theme override.
- A2DP and HEADSET connection state is queried concurrently instead of sequentially, halving the
  worst-case profile-proxy wait on the mode-switch path.
- Successful switches log the device and resulting mode.

### 0.1.0

- Initial release: mode switching from the app and from the three directly launchable activities.

## License

GPL-3.0-or-later. `openscq30-lib` is GPL-3.0-or-later and is linked into the APK, so CoreSwap is
too. See `LICENSE.txt`.
