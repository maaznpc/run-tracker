# APK verification record

Verified on 2026-10-21 in [GitHub Actions run 39545663142](https://github.com/maaznpc/run-tracker/actions/runs/39545663142). Overall result: **success**.

- Tested app-source commit: `d052a74bc40123d125af44b0532076d4900cab68`.
- [Signed debug APK ZIP](https://github.com/maaznpc/run-tracker/actions/runs/39545663142/artifacts/12320629355), containing `app-debug.apk`.
- [Reports and screenshots](https://github.com/maaznpc/run-tracker/actions/runs/39545663142/artifacts/12320778986).

## Passed

| Check | Evidence and scope |
| --- | --- |
| Production signal processing | 30 Java checks: angles, score gating, temporal persistence, lost tracking, cadence and timestamp handling. |
| Android build | `assembleDebug` and `assembleDebugAndroidTest` succeeded. |
| Unit tests | All 12 JUnit tests passed, including full-engine geometry/confidence/aspect-ratio checks, synthetic report behavior and JSON persistence. |
| Android lint | `lintDebug` passed. API 26 video decoding has a compatible fallback; light navigation-bar styling is qualified for API 27+. |
| Emulator installation/runtime | All 4 instrumentation tests passed on Android 15 / API 35, x86_64, Pixel 6 profile. |
| UI navigation | Home, illustrated report, side/rear setup, history and Learn opened successfully. Four actual emulator screenshots were collected. |
| Actual pose inference | The bundled model detected 33 body landmarks on Google's known-person pose fixture; both hip visibility assertions passed. |
| Actual video inference | A decoded 5-second blank H.264 clip produced no form score, no review moments and zero usable tracking coverage. |
| APK signature | `apksigner verify --verbose --print-certs` reported `Verifies` and APK Signature Scheme v2 verification `true`. Debug certificate, not a store-release signing key. |
| APK identity | Package `com.stride.form`, minimum SDK 26, target SDK 35, exactly one launcher pointing to MainActivity. |
| Permissions | Actual merged APK contains camera permission and excludes Internet and microphone permissions. Transitive dependency requests are explicitly removed. |
| Packaged assets/native code | APK ZIP integrity, bundled pose task ZIP integrity, both TFLite models and an arm64-v8a native library passed verification. |
| Integrity record | The APK SHA-256, merged manifest, permission list and signature output are included in the reports artifact. |

The local workspace did not have an Android SDK/network access. The Android build and runtime results above are remote CI evidence. The later documentation/checker-only commit does not change the verified app source. The signature-check pipeline was also hardened with `set -euo pipefail`; local positive/negative shell checks confirm that a failed verifier cannot be hidden by `tee`.

## Still requires physical-device validation

A passing build and emulator run do not validate running biomechanics or certify every phone.

1. Grant/deny camera permission and record a real runner for 10–20 seconds. Check that CameraX supports simultaneous recording and tracking on the target phone.
2. Confirm live and replay joint alignment, hip trails, camera rotation, video aspect ratio and flagged timestamps against manual frame inspection.
3. Test steady side/rear running clips, varying lighting, occlusion, body types and pace. Compare the same camera setup across sessions.
4. Check real-gallery imports, cloud document providers, unsupported codecs, the 2-minute limit, low storage, cancellation, export/share, deletion and backgrounding during recording.
5. Test Android 8.0 fallback decoding on a real API 26 device if that version is a distribution target. Only API 35 was used for emulator runtime verification.
6. Confirm native-library performance and 16KB-page compatibility on the intended arm64 phones before any store release.

The form percentage remains an explicit, unvalidated 2D coaching heuristic. No injury prediction, clinical diagnosis, calibrated 3D joint motion or running-economy measurement is claimed. Reference-video and specialist validation is still needed for stronger running-quality claims.
