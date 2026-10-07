# Zazen

A zazen timer for the Light Phone III.

1. Press START.
2. Settle timer runs (1 min, 2 min or custom; can be skipped).
3. Three bells, 5s apart.
4. Zazen timer runs (20 min, 40 min or custom).
5. One bell.

Bells can be replaced by, or combined with, vibration. Everything keeps working while the phone is locked: the whole session plays as one detached audio queue (silence and bells), so playback keeps time.

## Bell

`src/main/assets/audio/bell.wav` must be a PCM WAV no longer than 5 seconds.

## Vibration

Uses `LightVibrator` from `:sdk:client`, which is not yet part of the upstream Light SDK.

## Build and install (LP3)

```bash
./gradlew :tool:testDebugUnitTest :tool:assembleDebug
adb install -r tool/build/outputs/apk/debug/tool-debug.apk
adb shell monkey -p net.hitnail.zazen -c android.intent.category.LAUNCHER 1
```
