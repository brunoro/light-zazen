# Zazen

A zazen timer for the Light Phone III, built with the [Light SDK](https://github.com/lightphone/light-sdk).

## Sitting session

1. Choose a settle period: skip, 2 minutes, or custom.
2. Choose a zazen duration: 20 minutes, 40 minutes, or custom.
3. Choose sound, vibration, or both, then press START.
4. After settling, three opening strikes mark the start of zazen. Each interval varies independently from 4–6 seconds; strikes progress from medium to full to soft.
5. One full strike marks the end of zazen.

Custom durations display a `custom:` label above the value. Sessions continue while the phone is locked.

## Build and run

Open the project in Android Studio with the Android SDK configured. From the repository root:

```bash
./gradlew :tool:assembleDebug
adb install -r tool/build/outputs/apk/debug/tool-debug.apk
adb shell monkey -p net.hitnail.zazen -c android.intent.category.LAUNCHER 1
```

Run unit tests:

```bash
./gradlew :tool:testDebugUnitTest
```

## Project layout

- [`tool/`](tool): Zazen application, audio assets, and session logic.
- [`sdk/`](sdk): Light SDK modules included in this repository.
- [`docs/`](docs): SDK documentation and project tasks.

See the [tool README](tool/README.md) for audio and vibration details and the [sideloading documentation](docs/sideloading) for device setup.

## Planned work

[Explore synthesized zazen bell sounds](docs/tasks/001-synthesize-zazen-bell/TASK.md): prototype and audition reproducible bell synthesis before integrating a replacement sound.
