package com.stride.form

import kotlin.math.*

enum class ViewAngle(val label: String) { SIDE("Side view"), REAR("Rear view") }
enum class Facing(val label: String) { RIGHT("Running right"), LEFT("Running left") }
data class Joint(val x: Float, val y: Float, val visibility: Float = 1f)
data class PoseFrame(val timeMs: Long, val joints: List<Joint>, val width: Int, val height: Int)
data class FrameMetrics(
    val timeMs: Long, val valid: Boolean, val confidence: Float,
    val lean: Float? = null, val reach: Float? = null, val pelvicTilt: Float? = null,
    val kneeDeviation: Float? = null, val leftKnee: Float? = null, val rightKnee: Float? = null,
    val hipY: Float? = null, val torso: Float? = null, val ankleDifference: Float? = null,
    val flags: List<String> = emptyList()
)
data class Finding(val key: String, val title: String, val explanation: String, val cue: String, val joints: List<Int>)
data class Moment(val timeMs: Long, val endMs: Long, val key: String, val peakValue: Float)
data class MetricSummary(val label: String, val value: String, val note: String)
data class RunReport(
    val id: String, val createdAt: Long, val view: ViewAngle, val facing: Facing,
    val durationMs: Long, val frames: List<PoseFrame>, val metrics: List<FrameMetrics>,
    val score: Int?, val coverage: Int, val confidence: Int, val cadence: Int?,
    val moments: List<Moment>, val summaries: List<MetricSummary>,
    val status: String, val demo: Boolean = false
)

/** Screen-plane coaching heuristics. Not a diagnostic or a learned running-quality classifier. */
class GaitEngine(private val view: ViewAngle, private val facing: Facing) {
    private val frames = mutableListOf<PoseFrame>()
    private val metrics = mutableListOf<FrameMetrics>()
    private var origin: Long? = null

    fun add(raw: PoseFrame): FrameMetrics {
        if (origin == null) origin = raw.timeMs
        val frame = raw.copy(timeMs = raw.timeMs - origin!!, joints = raw.joints.takeIf { points -> points.all { it.x.isFinite() && it.y.isFinite() && it.visibility.isFinite() } } ?: emptyList())
        if (frames.isNotEmpty() && frame.timeMs <= frames.last().timeMs) return metrics.last()
        frames += frame
        val m = measure(frame, view, facing)
        metrics += m
        return m
    }

    fun finish(id: String, durationMs: Long, createdAt: Long = System.currentTimeMillis(), demo: Boolean = false): RunReport {
        val good = metrics.filter { it.valid }
        val coverage = if (metrics.isEmpty()) 0 else (100f * good.size / metrics.size).roundToInt()
        val events = stepEvents(good)
        val cadence = if (view == ViewAngle.SIDE) GaitMath.cadence(events.toLongArray()).takeIf { it >= 0 } else null
        val moving = events.size >= 6
        val observedDuration = if (metrics.size > 1) metrics.last().timeMs - metrics.first().timeMs else 0L
        val sustained = sustainedKeys(metrics)
        val clean = metrics.indices.count { metrics[it].valid && sustained[it].isEmpty() }
        val score = GaitMath.score(metrics.size, good.size, clean, observedDuration, events.size).takeIf { it >= 0 }
        val scorable = score != null
        val moments = buildMoments(metrics, sustained)
        val summaries = if (view == ViewAngle.SIDE) listOf(
            MetricSummary("Trunk angle", formatAverage(good.mapNotNull { it.lean }, "°"), "Screen-plane angle from vertical; flag above 20°."),
            MetricSummary("Forward reach", formatAverage(good.mapNotNull { it.reach }, "× leg"), "At estimated contact only. Camera-dependent proxy, not measured braking."),
            MetricSummary("Hip excursion", excursion(good), "Vertical hip range ÷ torso length. Fixed camera required; descriptive only."),
            MetricSummary("Step rate", cadence?.let { "$it spm" } ?: "—", "Estimated from alternating ankle motion. No universal cadence target.")
        ) else listOf(
            MetricSummary("Pelvic tilt", formatAverage(good.mapNotNull { it.pelvicTilt }, "°"), "Screen-plane hip-line tilt; flag above 8°. Not 3D pelvic drop."),
            MetricSummary("Knee alignment", formatAverage(good.mapNotNull { it.kneeDeviation }, "× hip"), "Inward knee offset from hip–ankle line; flag above 0.20 hip widths."),
            MetricSummary("Hip excursion", excursion(good), "Vertical hip range ÷ torso length. Descriptive only."),
            MetricSummary("Tracking coverage", "$coverage%", "Share of sampled frames with all required joints visible.")
        )
        val status = when {
            observedDuration < 4000 -> "Record at least 5 seconds of running."
            good.size < 30 || good.size * 2 < metrics.size -> "Tracking is incomplete. Keep your whole body in frame, with brighter light and a fixed camera."
            !moving -> "Not enough alternating leg motion to score. Record 10–20 seconds of continuous running."
            else -> "${score}% of usable frames stayed within this app’s coaching thresholds."
        }
        return RunReport(id, createdAt, view, facing, durationMs, frames.toList(), metrics.toList(), score,
            coverage, (100 * (good.map { it.confidence }.average().takeIf { !it.isNaN() } ?: 0.0)).roundToInt(),
            cadence, if (scorable) moments else emptyList(), summaries, status, demo)
    }

    companion object {
        val findings = mapOf(
            "lean" to Finding("lean", "Excessive trunk lean", "The shoulder-to-hip line exceeds 20° from vertical in this view. Perspective and acceleration can change this angle.", "Try a relaxed, tall posture. Compare clips at a similar steady pace.", listOf(11, 12, 23, 24)),
            "reach" to Finding("reach", "Foot reaching ahead", "At an estimated contact frame, the ankle is more than 0.35 leg lengths ahead of the hip with a relatively straight knee. This is a visual overstride proxy.", "Try slightly shorter, comfortable steps. Avoid forcing a particular foot strike.", listOf(23, 24, 25, 26, 27, 28, 29, 30)),
            "pelvis" to Finding("pelvis", "Hip-line tilt", "The visible line between the hips tilts more than 8°. Camera roll, clothing and perspective can affect this estimate.", "Keep the camera level. Review both sides and discuss a repeated pattern with a running professional.", listOf(23, 24)),
            "knee" to Finding("knee", "Inward knee movement", "A knee moves inward from the hip-to-ankle line by more than 0.20 hip widths. This is a 2D alignment cue, not a diagnosis.", "Review the highlighted stance. Practice comfortable control with a coach if this pattern persists.", listOf(23, 24, 25, 26, 27, 28))
        )
        val connections = listOf(11 to 12, 11 to 13, 13 to 15, 12 to 14, 14 to 16,
            11 to 23, 12 to 24, 23 to 24, 23 to 25, 25 to 27, 27 to 29, 29 to 31,
            27 to 31, 24 to 26, 26 to 28, 28 to 30, 30 to 32, 28 to 32)
        fun measure(frame: PoseFrame, view: ViewAngle, facing: Facing): FrameMetrics {
            val p = frame.joints
            val required = listOf(11, 12, 23, 24, 25, 26, 27, 28)
            if (p.size < 33 || frame.width <= 0 || frame.height <= 0)
                return FrameMetrics(frame.timeMs, false, 0f)
            if (p.any { !it.x.isFinite() || !it.y.isFinite() || !it.visibility.isFinite() })
                return FrameMetrics(frame.timeMs, false, 0f)
            val confidence = required.map { p[it].visibility }.average().toFloat()
            val visible = required.all { p[it].visibility >= .55f && p[it].x in .015f.. .985f && p[it].y in .015f.. .985f }
            // Convert to isotropic image coordinates before angles and length ratios.
            val aspect = frame.width.toFloat() / frame.height
            fun pt(i: Int) = Joint(p[i].x * aspect, p[i].y, p[i].visibility)
            fun dist(a: Joint, b: Joint) = hypot(a.x - b.x, a.y - b.y)
            val lh = pt(23); val rh = pt(24)
            val hip = Joint((lh.x + rh.x) / 2, (lh.y + rh.y) / 2)
            val shoulder = Joint((pt(11).x + pt(12).x) / 2, (pt(11).y + pt(12).y) / 2)
            val torso = dist(hip, shoulder)
            val leg = (dist(lh, pt(25)) + dist(pt(25), pt(27)) + dist(rh, pt(26)) + dist(pt(26), pt(28))) / 2
            if (!visible || torso < .08f || leg < .12f || shoulder.y >= hip.y)
                return FrameMetrics(frame.timeMs, false, confidence)
            val leftKnee = angle(lh, pt(25), pt(27))
            val rightKnee = angle(rh, pt(26), pt(28))
            val ankleDiff = (pt(27).y - pt(28).y) / leg
            val flags = mutableListOf<String>()
            if (view == ViewAngle.SIDE) {
                val lean = Math.toDegrees(atan2(abs(shoulder.x - hip.x), hip.y - shoulder.y).toDouble()).toFloat()
                val stanceLeft = pt(27).y >= pt(28).y
                val stanceHip = if (stanceLeft) lh else rh
                val ankle = if (stanceLeft) pt(27) else pt(28)
                val knee = if (stanceLeft) leftKnee else rightKnee
                val direction = if (facing == Facing.RIGHT) 1 else -1
                val reach = if (knee > 150 && abs(ankleDiff) > .06f) max(0f, (ankle.x - stanceHip.x) * direction / leg) else null
                if (lean > 20) flags += "lean"
                if (reach != null && reach > .35f) flags += "reach"
                return FrameMetrics(frame.timeMs, true, confidence, lean = lean, reach = reach,
                    leftKnee = leftKnee, rightKnee = rightKnee, hipY = hip.y, torso = torso,
                    ankleDifference = ankleDiff, flags = flags)
            }
            val hipWidth = abs(lh.x - rh.x)
            // Near-overlapping hips cannot support rear-plane measurements.
            if (hipWidth < .35f * torso) return FrameMetrics(frame.timeMs, false, confidence)
            val tilt = Math.toDegrees(atan2(abs(lh.y - rh.y), hipWidth).toDouble()).toFloat()
            fun inward(h: Joint, k: Joint, a: Joint): Float {
                if (abs(a.y - h.y) < .03) return 0f
                val t = ((k.y - h.y) / (a.y - h.y)).coerceIn(0f, 1f)
                val expected = h.x + (a.x - h.x) * t
                val towardCenter = if (h.x < hip.x) 1f else -1f
                return max(0f, (k.x - expected) * towardCenter / hipWidth)
            }
            val deviation = max(inward(lh, pt(25), pt(27)), inward(rh, pt(26), pt(28)))
            if (tilt > 8) flags += "pelvis"
            if (deviation > .20f) flags += "knee"
            return FrameMetrics(frame.timeMs, true, confidence, pelvicTilt = tilt, kneeDeviation = deviation,
                leftKnee = leftKnee, rightKnee = rightKnee, hipY = hip.y, torso = torso, ankleDifference = ankleDiff, flags = flags)
        }
        fun angle(a: Joint, b: Joint, c: Joint): Float = GaitMath.angle(a.x, a.y, b.x, b.y, c.x, c.y)
        private fun stepEvents(metrics: List<FrameMetrics>): List<Long> =
            GaitMath.stepEvents(metrics.map { it.timeMs }.toLongArray(),
                metrics.map { it.ankleDifference ?: Float.NaN }.toFloatArray()).toList()
        internal fun sustainedKeys(metrics: List<FrameMetrics>): List<Set<String>> {
            val result = List(metrics.size) { mutableSetOf<String>() }
            val times = metrics.map { it.timeMs }.toLongArray()
            val valid = metrics.map { it.valid }.toBooleanArray()
            findings.keys.forEach { key ->
                val sustained = GaitMath.sustained(times, valid, metrics.map { key in it.flags }.toBooleanArray())
                sustained.forEachIndexed { i, flagged -> if (flagged) result[i] += key }
            }
            return result
        }
        private fun buildMoments(metrics: List<FrameMetrics>, flags: List<Set<String>>): List<Moment> {
            val result = mutableListOf<Moment>()
            findings.keys.forEach { key ->
                var start: Long? = null; var end = 0L; var peak = 0f
                fun flush() { start?.let { result += Moment(it, end, key, peak) }; start = null; peak = 0f }
                metrics.forEachIndexed { i, m ->
                    if (key in flags[i]) {
                        if (start != null && m.timeMs - end > 300) flush()
                        if (start == null) start = m.timeMs
                        end = m.timeMs
                        val value = when (key) { "lean" -> m.lean; "reach" -> m.reach; "pelvis" -> m.pelvicTilt; else -> m.kneeDeviation } ?: 0f
                        peak = max(peak, value)
                    } else flush()
                }
                flush()
            }
            return result.sortedBy { it.timeMs }
        }
        private fun formatAverage(v: List<Float>, unit: String): String = if (v.isEmpty()) "—" else
            if (unit == "°") "%.1f°".format(java.util.Locale.US, v.average()) else "%.2f $unit".format(java.util.Locale.US, v.average())
        private fun excursion(good: List<FrameMetrics>): String {
            val ys = good.mapNotNull { it.hipY }.sorted(); val torsos = good.mapNotNull { it.torso }
            if (ys.size < 10 || torsos.isEmpty()) return "—"
            val range = ys[(ys.size * .95).toInt().coerceAtMost(ys.lastIndex)] - ys[(ys.size * .05).toInt()]
            return "%.1f%%".format(java.util.Locale.US, 100 * range / torsos.average())
        }
    }
}
