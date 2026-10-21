# Research and implementation choices

Sources inspected during implementation on 2026-10-21. Summaries are paraphrases.

## Running biomechanics

1. Anderson et al. (2022), *What is the Effect of Changing Running Step Rate on Injury, Performance and Biomechanics? A Systematic Review and Meta-analysis.* Sports Medicine – Open 8:112. [Full paper](https://pmc.ncbi.nlm.nih.gov/articles/PMC9441414/). This review included 37 studies. Step-rate changes can change biomechanical variables, while evidence for injury/performance effects is insufficient. App implication: report cadence descriptively, avoid a universal 180-spm target and avoid injury-risk predictions.
2. Doyle et al. (2022), *The Effectiveness of Gait Retraining on Running Kinematics, Kinetics, Performance, Pain, and Injury in Distance Runners.* [PubMed](https://pubmed.ncbi.nlm.nih.gov/35128941/), DOI 10.2519/jospt.2022.10585. Nineteen trials covered selected gait-retraining outcomes. Evidence certainty varies and clinical outcomes were limited. App implication: show observable patterns and contextual cues; do not equate a visual score with health or running performance.

These reviews do not establish the app's angle/offset thresholds as clinical cutoffs. Those values are engineering heuristics for transparent visual review and require real-world validation before stronger claims.

## Pose model and GitHub research

- [Official MediaPipe Pose Landmarker Android documentation](https://ai.google.dev/edge/mediapipe/solutions/vision/pose_landmarker/android): image/video/live-stream modes, timestamps and landmark output.
- [Google AI Edge Android sample](https://github.com/google-ai-edge/mediapipe-samples/tree/main/examples/pose_landmarker/android). Inspected `PoseLandmarkerHelper.kt` (blob `84a0363fc17eaa505245100d36eb27f04597f100`) and the current app dependency configuration. This informed RGBA bitmap rotation, video timestamps and the use of the official Android pose API. Stride's UI and coaching engine are original code; the actual pretrained model/runtime is Google's reusable MediaPipe implementation.
- [PoseRun Analyzer](https://github.com/sjzno1/poserun-analyzer): inspected its README as an existing running-analysis example using Python/MediaPipe, derived measures and reports. Its application code was not copied or bundled. A native on-device pipeline avoids requiring a Python server, account or video upload.
- [Official Gradle wrapper](https://github.com/gradle/gradle/blob/v8.13.0/gradle/wrapper/gradle-wrapper.jar): the bundled wrapper JAR is from Gradle v8.13.0, Git blob `9bbc975c742b298b441bfb90dbc124400a3751b9`.

## Why this implementation

CameraX supplies native recording and analysis frames. MediaPipe identifies 33 landmarks. The app converts those to 2D measures, applies confidence and temporal checks, saves the evidence and lets the user review flagged timestamps. It reuses an established pose model rather than inventing an unsupported learned running-quality classifier. Kotlin/Compose makes the Android capture, local-file lifecycle and permission behavior direct.

No research source supports claiming complete biomechanical or medical analytics from one monocular phone clip. The product therefore makes those limits visible and gives a transparent percentage of threshold-compliant observed frames.
