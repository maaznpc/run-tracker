package com.stride.form

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import kotlin.math.*

class SessionStore(private val context: Context) {
    val root = File(context.filesDir, "sessions").apply { mkdirs() }
    fun directory(id: String) = File(root, id).apply { mkdirs() }
    fun video(id: String) = File(directory(id), "run.mp4")
    fun thumbnail(id: String) = File(directory(id), "cover.jpg")
    fun newId() = UUID.randomUUID().toString()
    fun save(report: RunReport) {
        require(!report.demo)
        val destination = File(directory(report.id), "report.json")
        val temporary = File(directory(report.id), "report.tmp")
        temporary.writeText(toJson(report).toString())
        check(temporary.renameTo(destination)) { "Could not save the report. Check free storage." }
    }
    fun list(): List<RunReport> = root.listFiles().orEmpty().filter { it.isDirectory }.mapNotNull { dir ->
        val f = File(dir, "report.json")
        if (!f.exists()) null else runCatching { fromJson(JSONObject(f.readText()), lightweight = true) }.getOrNull()
    }.sortedByDescending { it.createdAt }
    fun load(id: String): RunReport = fromJson(JSONObject(File(directory(id), "report.json").readText()))
    fun delete(id: String) { check(File(root, id).deleteRecursively()) { "Could not remove the session." } }
    fun share(report: RunReport, withVideo: Boolean) {
        val exports = File(context.cacheDir, "exports").apply { mkdirs() }
        exports.listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - 86400000 }?.forEach { it.delete() }
        val file = if (withVideo) video(report.id) else File(exports, "stride-${report.id.take(8)}.json").apply {
            writeText(toJson(report).toString(2))
        }
        check(file.exists()) { "The video file is no longer available." }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (withVideo) "video/mp4" else "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newRawUri("Stride export", uri)
        }
        context.startActivity(Intent.createChooser(intent, if (withVideo) "Share original video" else "Export analysis" ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    companion object {
        fun toJson(r: RunReport): JSONObject = JSONObject().apply {
            put("schemaVersion", 1); put("id", r.id); put("createdAt", r.createdAt)
            put("view", r.view.name); put("facing", r.facing.name); put("durationMs", r.durationMs)
            put("score", r.score ?: JSONObject.NULL); put("coverage", r.coverage); put("confidence", r.confidence)
            put("cadence", r.cadence ?: JSONObject.NULL); put("status", r.status); put("demo", r.demo)
            put("method", "Unvalidated 2D coaching heuristics v1. Percent of usable sampled frames without sustained threshold flags; not a medical or performance score.")
            put("frames", JSONArray().apply { r.frames.forEach { f -> put(JSONObject().apply {
                put("t", f.timeMs); put("w", f.width); put("h", f.height)
                put("p", JSONArray().apply { f.joints.forEach { p -> put(JSONArray(listOf(p.x, p.y, p.visibility))) } })
            }) } })
            put("metrics", JSONArray().apply { r.metrics.forEach { m -> put(JSONObject().apply {
                put("t", m.timeMs); put("valid", m.valid); put("confidence", m.confidence)
                putNullable("lean", m.lean); putNullable("reach", m.reach); putNullable("pelvis", m.pelvicTilt)
                putNullable("knee", m.kneeDeviation); putNullable("leftKnee", m.leftKnee); putNullable("rightKnee", m.rightKnee)
                putNullable("hipY", m.hipY); putNullable("torso", m.torso); putNullable("ankle", m.ankleDifference)
                put("flags", JSONArray(m.flags))
            }) } })
            put("moments", JSONArray().apply { r.moments.forEach { put(JSONObject().apply {
                put("t", it.timeMs); put("end", it.endMs); put("key", it.key); put("peak", it.peakValue)
            }) } })
            put("summaries", JSONArray().apply { r.summaries.forEach { put(JSONObject().apply {
                put("label", it.label); put("value", it.value); put("note", it.note)
            }) } })
        }
        private fun JSONObject.putNullable(key: String, value: Float?) { put(key, value ?: JSONObject.NULL) }
        private fun JSONObject.floatOrNull(key: String): Float? = if (isNull(key)) null else getDouble(key).toFloat()
        private fun <T> JSONArray.mapItems(f: (Int) -> T) = (0 until length()).map(f)
        fun fromJson(j: JSONObject, lightweight: Boolean = false): RunReport {
            require(j.getInt("schemaVersion") == 1)
            val frames = if (lightweight) emptyList() else j.getJSONArray("frames").let { a -> a.mapItems { i -> a.getJSONObject(i).let { f ->
                PoseFrame(f.getLong("t"), f.getJSONArray("p").let { ps -> ps.mapItems { k -> ps.getJSONArray(k).let {
                    Joint(it.getDouble(0).toFloat(), it.getDouble(1).toFloat(), it.getDouble(2).toFloat())
                } } }, f.getInt("w"), f.getInt("h"))
            } } }
            val metrics = if (lightweight) emptyList() else j.getJSONArray("metrics").let { a -> a.mapItems { i -> a.getJSONObject(i).let { m ->
                FrameMetrics(m.getLong("t"), m.getBoolean("valid"), m.getDouble("confidence").toFloat(),
                    m.floatOrNull("lean"), m.floatOrNull("reach"), m.floatOrNull("pelvis"), m.floatOrNull("knee"),
                    m.floatOrNull("leftKnee"), m.floatOrNull("rightKnee"), m.floatOrNull("hipY"), m.floatOrNull("torso"),
                    m.floatOrNull("ankle"), m.getJSONArray("flags").let { fs -> fs.mapItems { fs.getString(it) } })
            } } }
            return RunReport(j.getString("id"), j.getLong("createdAt"), ViewAngle.valueOf(j.getString("view")),
                Facing.valueOf(j.getString("facing")), j.getLong("durationMs"), frames, metrics,
                if (j.isNull("score")) null else j.getInt("score"), j.getInt("coverage"), j.getInt("confidence"),
                if (j.isNull("cadence")) null else j.getInt("cadence"),
                j.getJSONArray("moments").let { a -> a.mapItems { i -> a.getJSONObject(i).let {
                    Moment(it.getLong("t"), it.getLong("end"), it.getString("key"), it.getDouble("peak").toFloat())
                } } }, j.getJSONArray("summaries").let { a -> a.mapItems { i -> a.getJSONObject(i).let {
                    MetricSummary(it.getString("label"), it.getString("value"), it.getString("note"))
                } } }, j.getString("status"), j.optBoolean("demo"))
        }
    }
}

object ExampleSession {
    fun create(): RunReport {
        val engine = GaitEngine(ViewAngle.SIDE, Facing.RIGHT)
        for (i in 0 until 200) {
            val t = i / 10.0
            val phase = t * 2 * PI * 1.4
            val p = MutableList(33) { Joint(.5f, .2f) }
            val hipY = .48f + .012f * sin(phase * 2).toFloat()
            val leaning = t in 5.0..7.6 || t in 14.0..15.8
            val shoulderX = if (leaning) .67f else .53f
            p[0] = Joint(shoulderX + .02f, .14f)
            p[11] = Joint(shoulderX - .015f, .27f); p[12] = Joint(shoulderX + .015f, .27f)
            p[23] = Joint(.49f, hipY); p[24] = Joint(.51f, hipY)
            for (side in 0..1) {
                val wave = sin(phase + side * PI).toFloat()
                val h = p[23 + side]
                p[25 + side] = Joint(h.x + .10f * wave, .66f - .035f * max(0f, wave))
                p[27 + side] = Joint(h.x + .17f * wave, .84f - .13f * max(0f, wave))
                p[29 + side] = Joint(p[27 + side].x - .015f, p[27 + side].y + .02f)
                p[31 + side] = Joint(p[27 + side].x + .05f, p[27 + side].y + .025f)
                p[13 + side] = Joint(shoulderX - .06f * wave, .39f)
                p[15 + side] = Joint(shoulderX + .09f * wave, .35f)
            }
            engine.add(PoseFrame(i * 100L, p, 720, 960))
        }
        return engine.finish("example", 20000, demo = true)
    }
}
