# Verification record

## Executed in the delivery environment

- `bash scripts/test-core.sh`: **PASS, 30 checks**. Compiles and runs the production `GaitMath.java` class using the installed Java compiler module. Covers angles, confidence-independent score eligibility, percentages, low coverage, stationary clips, short clips, temporal persistence, dropped observations, hysteresis, plausible cadence, bilateral counting and timestamp validation.
- `bash -n scripts/setup.sh scripts/test-core.sh`: passed shell syntax checks.
- `sh -n gradlew`: passed launcher syntax check.
- `python -m py_compile scripts/fetch_model.py`: passed Python syntax check.
- Android manifest, theme, vector icon and provider-path XML: parsed successfully.
- Gradle wrapper JAR: fetched from the official Gradle v8.13.0 GitHub source and checked as a valid ZIP/JAR.

## Not executed; required before shipping

The environment has no Android SDK or Kotlin/Gradle toolchain. Its network proxy refused connections, and the network-enabled download requests were cancelled. No APK was produced. The official pose model has not been downloaded into the source tree. Source/API compatibility, model loading, Android lint, Android unit tests and device behavior therefore remain unverified. Do not interpret the offline checks as an Android build result.

After fetching the model:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Twelve JUnit tests cover full-engine geometry, pose visibility, stationary clips, aspect-ratio invariance, rear-plane validity, synthetic example behaviour, timestamp normalization and JSON persistence. The included GitHub Actions workflow runs these checks and uploads the debug APK when used in a repository. It has not been triggered here.

## Physical-device acceptance checklist

1. Open app offline after installation. Navigate Home, Analyze, My runs and Learn. Confirm no blue app accents and readable layouts at large system font sizes.
2. Deny camera permission; confirm explanatory screen and document-picker alternative. Grant permission and reopen capture.
3. Record 10–20 seconds of one runner from a fixed side camera, whole body in view. Verify skeleton aligns at portrait/landscape input resolutions. Verify knees/hips remain aligned after device-specific frame rotation and video playback. Capture explicitly uses the rear camera.
4. Test recording + analysis streams on a low-end camera. A device that cannot support concurrent streams should show the import alternative instead of crashing.
5. Tap stop, wait for analysis, inspect score formula, metrics, hip chart and flagged moments. Flag timestamps must match the video, and playback overlays must follow aspect-fit letterboxing.
6. Tap each flagged timestamp; replay should scroll into view, seek and pause at the moment. Play at half speed, scrub, toggle tracing and verify highlighted joints.
7. Record a stationary person, a blank scene, a partial body and a short clip. Confirm scores are unavailable. No person must clear the live overlay.
8. Test a tilted/moving camera and a wrong-view clip. The app states these capture limitations; do not expect it to correct camera motion or classify angle automatically.
9. Test rear view with whole-body visibility. Check hip-line tilt and knee cue overlays against manual frame inspection. Confirm cadence is not shown as a rear-view measurement.
10. Import MP4 from both local gallery and a cloud document provider. Imported clips are copied locally. Reject unreadable, <4s, >120s and >600MB files with useful messages. Check 90°/270° rotation metadata on multiple devices; calibration is not assumed.
11. Cancel an import and cancel a mid-analysis clip. No report should be saved, and partial files should be deleted. Try low storage and unsupported codec input.
12. Background the app during recording. Camera recording stops; a finalized valid clip is analyzed. During replay, playback should pause on background. Keep the app open during analysis; process-death recovery of an in-progress job is not implemented.
13. Relaunch the app after a saved report, reopen it from history and verify it is unchanged. Delete a report and verify both video and JSON disappear. Export JSON and share the original video to a compatible Android app; overlays are in-app, not burned into exports.
14. At the recording limit, confirm recording stops early enough that the finalized file remains inside the 120s import limit.
15. Validate pose model runtime and native 16KB page-size compatibility on your target Android devices before any Play Store release. No physical-device certification is claimed.

## Analysis validation still needed

A coach/biomechanics specialist should review varied speeds, camera views and body types against manually labelled reference clips. Measure landmark and angle errors, false flags, cadence error and invalid-tracking rates. This release has no clinically validated “correct running” label or threshold dataset. Its visible score is explicitly a heuristic summary of observed 2D patterns.
