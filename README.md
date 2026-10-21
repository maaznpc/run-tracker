# Stride — running form, in focus

Native Android app written in Kotlin, Jetpack Compose, CameraX and MediaPipe. Ivory, charcoal, olive, lime and coral. No blue, account, backend, API key or runtime internet access.

**Delivery status:** source implementation provided. The Java signal-processing checks run successfully in the delivery environment. The Android SDK and network access were unavailable there, so **no APK, Android compilation result, lint result or device-test result is claimed**. Fetching the model and running the Android build below are required before installation. Camera behavior, model inference and overlay alignment still need physical-device verification.

## Run in Android Studio

1. Install a current Android Studio with JDK 17+ and Android SDK 35.
2. Open this `stride` directory as a project.
3. From its terminal, bundle Google's official model:

   ```sh
   python3 scripts/fetch_model.py
   ```

   On Windows use `python scripts/fetch_model.py`. The script downloads Pose Landmarker Lite **float16 version 1** from Google, verifies the task archive and records its SHA-256. The model is packaged into the APK, so it is not downloaded at runtime. The build deliberately fails if the model is missing.
4. Allow Gradle sync to finish and choose a physical Android 8.0+ device. Run the `app` configuration. Use the rear camera. An emulator can exercise navigation and imports; real camera performance needs a phone.

To produce a debug APK:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Windows: `gradlew.bat testDebugUnitTest lintDebug assembleDebug`.

APK: `app/build/outputs/apk/debug/app-debug.apk`.
Install with Android Studio, or `adb install -r app/build/outputs/apk/debug/app-debug.apk`. For distribution, configure your own release signing; do not distribute an unsigned release or expose keystores.

For a Linux machine without Android Studio:

```sh
./scripts/setup.sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Setup prompts for the Android SDK licenses. `--accept-sdk-licenses` is available for callers who have reviewed and accept them. Downloads need `dl.google.com`, `storage.googleapis.com`, Google's Maven repository, Maven Central and Gradle distribution hosts. Existing proxies and CA trust are preserved.

## What the app implements

- Home dashboard and an explicit, synthetic example report; no fabricated personal history.
- Guided side/rear capture setup and side-view running direction selection.
- Camera permission flow, live 33-landmark body overlay, hip trail, per-frame coaching cues and recording timer. Records silent video, with a 2-minute limit.
- Imported video selection through Android's document picker. Copies clips into app-private storage, so reports survive gallery permission changes.
- On-device video analysis at 10 samples/sec on a background dispatcher, progress and cancellation. Clips must be 4 seconds to 2 minutes and imports at most 600 MB; 10–20 seconds of steady running is recommended.
- Side: trunk angle, estimated contact reach, knee angles, hip excursion and estimated step rate. Rear: hip-line tilt, inward knee offset and hip excursion.
- Form consistency percentage, tracking coverage and model landmark visibility. Insufficient observations yield an **unavailable** score, never an invented percentage.
- Video replay with synchronized joint overlays, red/orange highlighted review segments, a hip trail, scrubbing, half-speed playback and clickable issue timestamps.
- Hip-position chart, issue explanations and contextual coaching cues.
- Persistent session history, recent score chart, local deletion confirmation, JSON export (landmarks, metrics, timestamps, method) and original-video sharing through Android's share sheet. Exported video is the original recording; the overlay is rendered during in-app replay.
- Research and privacy explanations, error states and lifecycle cleanup.

## What the percentage means

`100 × usable frames without a sustained threshold flag / all usable frames`.

A usable frame has all 8 required shoulder, hip, knee and ankle landmarks visible at confidence ≥0.55, in frame, with reasonable torso/leg dimensions. Flags require ≥3 consecutive usable samples, with gaps ≤300ms. A score also requires ≥30 usable samples, ≥4 seconds of observations, ≥50% coverage and ≥6 alternating leg-motion events. Stationary, occluded and poorly captured clips are withheld. This is a conservative motion check, not a trained running/activity classifier: other repetitive alternating movements can still pass, so recordings must actually show running.

**This percentage is an unvalidated coaching heuristic, not “percent correct running,” a medical measurement, a measure of running economy or an injury-risk estimate.** Thresholds are product choices for review, not clinical standards. 2D video is sensitive to perspective, camera roll, occlusion and model errors. There is no one ideal form or 180-spm requirement. See [the analysis specification](docs/ANALYSIS.md) and [research notes](docs/RESEARCH.md).

## Verify

```sh
# Runs the actual production Java signal class with no Android dependency.
bash scripts/test-core.sh

# Requires the model, Gradle and Android SDK.
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Thirty offline checks cover joint geometry, score gating, temporal persistence, dropped tracking, hysteresis and cadence. Twelve Kotlin JUnit tests cover the full engine, including pose confidence, aspect ratio, stationary clips, the synthetic example and JSON persistence; those require the Android/Gradle toolchain and were not run in the delivery environment. Device cases are listed in [verification](docs/VERIFICATION.md).

An optional GitHub Actions workflow builds, tests and uploads a debug APK when this project is added to your repository. It is included locally and has **not** been run or published on your behalf.

## Structure

- `MainActivity.kt` — design system, home, setup, history and app navigation.
- `CameraScreen.kt` — CameraX recording, frame conversion, live landmarks and overlay.
- `PoseProcessor.kt` — model initialization and sampled video inference.
- `GaitEngine.kt` / `GaitMath.java` — geometry, temporal flags, scores and summaries.
- `ReportScreen.kt` — replay, flags, charts and report actions.
- `SessionStore.kt` / `StrideViewModel.kt` — local persistence and cancellable processing.
- `LearnScreen.kt` — research, limitations and privacy.

App data is private to this installation. Uninstalling removes it; export anything you need to retain. Reports are stored atomically. Raw videos are not modified. No audio is captured. No Internet permission is requested. Background processing is not a foreground service; keep the app open during analysis. If Android kills the process, that in-progress analysis must be restarted; saved reports remain.

See [third-party notices](THIRD_PARTY_NOTICES.md).
