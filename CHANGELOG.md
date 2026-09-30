# Changelog

Releases before 0.4.0 are recorded in the Changelog section of `README.md`.

## [v0.4.0] - 2026-09-30

### Added
- Transparency when playback pauses: switches to Transparency after a configurable delay (0-10 s)
  and restores the previous mode on resume, unless the mode was changed in between. Driven by media
  session state through the new `PlaybackWatcherService` (notification access).
- Optional Shizuku management of that notification access, granted only while the feature is on.
- "Current mode" card on the main screen.
- Debug switch that keeps the device connected and polls the mode every 0.5 s; mode switches reuse
  that connection while it runs.
- `ModeMenuActivity`, an overlay mode picker, with an optional home-screen icon.

### Changed
- Shortcut icons redrawn.
- `ModeSwitcher` can read the current mode without changing it and reports the previous mode.
