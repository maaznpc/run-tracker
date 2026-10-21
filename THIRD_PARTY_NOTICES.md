# Third-party notices

Stride uses external software and an external pose model. Dependencies are fetched through Gradle; the pose task is downloaded from Google's official model repository during setup and bundled for offline inference.

- **MediaPipe / TensorFlow Authors** — [source](https://github.com/google-ai-edge/mediapipe), [Apache License 2.0](https://github.com/google-ai-edge/mediapipe/blob/master/LICENSE). Runtime `com.google.mediapipe:tasks-vision:1.0.0` and Pose Landmarker Lite model. Review the current model documentation and terms for your distribution. The Android reference sample's 2023 TensorFlow Authors copyright and Apache 2.0 notice apply to that sample. Stride's implementation was informed by the official APIs; see `docs/RESEARCH.md`.
- **AndroidX / Jetpack Compose / CameraX / Media3** — Android Open Source Project contributors; [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).
- **Kotlin / kotlinx.coroutines** — JetBrains and contributors; [Apache License 2.0](https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt).
- **Gradle wrapper** — Gradle contributors; [Apache License 2.0](https://github.com/gradle/gradle/blob/v8.13.0/LICENSE). The official wrapper JAR is included unchanged. Launcher scripts are minimal original launchers. The Gradle distribution is downloaded separately by the wrapper.
- **JUnit 4** (test only) — [Eclipse Public License 1.0](https://github.com/junit-team/junit4/blob/main/LICENSE-junit.txt).

No code from PoseRun Analyzer is included. Review transitive dependency licenses and native-library requirements before publishing a store release. This file identifies third-party components; it does not grant additional rights over them.
