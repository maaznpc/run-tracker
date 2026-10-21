# Analysis specification — version 1

## Inputs and coordinate system

One runner, fixed level camera, whole body visible, well-lit scene. Side or rear view is explicitly selected. Side running direction is explicit. All inference is local using MediaPipe Pose Landmarker Lite. Saved-video sampling is 100ms (10Hz). Camera inference requests at most 12.5Hz; actual performance depends on the device. The live display is preliminary per-frame feedback; the final video analysis applies temporal persistence.

MediaPipe image coordinates are normalized. Before geometric operations, x is multiplied by image width / image height, so x/y have equal scale. Model depth/world coordinates are deliberately not presented as calibrated 3D biomechanics.

A frame is usable when landmarks 11,12,23,24,25,26,27,28 have visibility ≥0.55, coordinates between 0.015 and 0.985, a torso ≥0.08 image-heights, average leg length ≥0.12 image-heights, and shoulders above the hips. Rear view additionally needs a horizontal hip separation ≥0.35 torso lengths. This avoids interpreting overlapping hips as a rear view. The runner must still select the correct view; this is not automatic camera-angle classification.

The detector chooses one pose. Multiple people in frame can cause subject switching. This release does not implement cross-person identity tracking; the capture instructions require a single runner.

## Side measures

- **Trunk angle:** unsigned angle between mid-hip → mid-shoulder and image vertical. Flag above 20°. It cannot distinguish a useful whole-body ankle lean from flexion at the waist and does not account for slope or acceleration.
- **Contact reach proxy:** choose the lower ankle, require its knee angle >150° and a left/right ankle height difference >0.06 leg lengths. Measure positive forward ankle-minus-same-side-hip displacement in the selected running direction, divided by mean thigh+shank length. Flag above 0.35. This does not detect true foot contact or measure braking force. The UI calls it an estimated contact proxy.
- **Knee angles:** image-plane hip–knee–ankle included angles; stored per frame. No universal knee-angle target is applied.
- **Hip excursion:** 95th minus 5th percentile of mid-hip y, divided by average torso length, shown as percent. Descriptive only, not a body-height measurement or efficiency rating.
- **Step rate:** ankle-height difference divided by leg length; hysteresis ±0.07; accepted alternate phases at least 180ms apart. Missing samples or gaps >300ms reset phase. Phase age >1200ms resets. At least 6 events and 5 plausible 180–1000ms intervals, coefficient of variation <0.4 and result 90–240 steps/min. A hidden/irregular signal yields no cadence, not a default value. Both feet contribute. No 180-spm target is used.

## Rear measures

- **Hip-line tilt:** absolute atan2(vertical hip difference, horizontal hip separation), in screen-plane degrees. Flag above 8°. This is not a 3D pelvic-drop assessment and is sensitive to camera roll.
- **Knee offset:** interpolate the hip-to-ankle line at knee y. Measure the inward horizontal knee displacement toward the midpoint of the hips, divided by hip width. Flag the larger side above 0.20. Perspective and swing phase affect it. This is an image-plane alignment cue, not a diagnosis of valgus or structural alignment.
- Hip excursion as above. Cadence is withheld in rear view.

## Temporal flags and score

For each flag type, at least 3 consecutive valid samples with inter-sample gaps ≤300ms are required. Invalid samples and gaps break a segment. The same sustained flags drive scoring and the clickable review timeline. A short spike is not deducted from the score. A segment stores start/end, kind and peak numeric value. Replay overlays highlight corresponding joints; low-visibility landmarks are omitted.

A score requires 30 usable samples, ≥4000ms observed time, at least 50% usable samples, and 6 alternating ankle-motion events. Other alternating motions can pass this gate: there is no trained running-vs-walking classifier. Capture instructions must be followed.

`score = round(100 × usable frames with no sustained flag / usable frames)`

Coverage is usable samples / all attempted samples. Missing decoded frames are counted as unusable. Visibility is mean model visibility across usable required joints, not an externally calibrated probability of measurement accuracy. A high visibility number is not a claim that angle measurements are correct.

No clinical reference population, labelled running-quality dataset, model calibration or prospective validation supports these thresholds. They are explicit app heuristics, documented so a coach can inspect their effect. There is no injury prediction, diagnosis, force measurement, energy-economy percentage, distance or speed estimation.

## Playback and storage

Raw landmarks, per-frame measurements and report method are saved with the original video in app-private storage. Imported video is copied. Reports are written to a temporary file then renamed. The history reads lightweight report summaries; detailed landmark arrays are loaded when a report is opened. JSON export includes the entire analysis. Sharing a video shares the original, without burned-in overlays. The example uses generated 2D landmarks and is clearly labelled synthetic; it is never saved as a real run.
