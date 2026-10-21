package com.stride.form

import org.junit.Assert.*
import org.junit.Test

class GaitEngineTest {
    private fun pose(time: Long = 0, visibility: Float = .99f, width: Int = 720, height: Int = 960): PoseFrame {
        val p = MutableList(33) { Joint(.5f, .2f, visibility) }
        p[11] = Joint(.47f,.25f,visibility); p[12] = Joint(.53f,.25f,visibility)
        p[23] = Joint(.48f,.48f,visibility); p[24] = Joint(.52f,.48f,visibility)
        p[25] = Joint(.48f,.66f,visibility); p[26] = Joint(.52f,.66f,visibility)
        p[27] = Joint(.48f,.85f,visibility); p[28] = Joint(.52f,.85f,visibility)
        return PoseFrame(time,p,width,height)
    }
    @Test fun stationaryPersonNeverGetsAQualityScore() {
        val engine = GaitEngine(ViewAngle.SIDE,Facing.RIGHT)
        for(i in 0..100) engine.add(pose(i*100L))
        val result = engine.finish("stationary",10000)
        assertNull(result.score); assertNull(result.cadence); assertTrue(result.moments.isEmpty())
    }
    @Test fun occludedJointsAreUnusable() {
        val m = GaitEngine.measure(pose(visibility = .3f),ViewAngle.SIDE,Facing.RIGHT)
        assertFalse(m.valid)
    }
    @Test fun missingPoseIsUnusable() {
        val m = GaitEngine.measure(PoseFrame(0,emptyList(),720,960),ViewAngle.SIDE,Facing.RIGHT)
        assertFalse(m.valid); assertTrue(m.flags.isEmpty())
    }
    @Test fun unknownAndInvalidCoordinatesNeverCrashOrScore() {
        val p = pose().joints.toMutableList(); p[23] = Joint(Float.NaN,.5f)
        assertFalse(GaitEngine.measure(PoseFrame(0,p,720,960),ViewAngle.SIDE,Facing.RIGHT).valid)
    }
    @Test fun anglesUseImageAspectRatio() {
        val p = pose().joints.toMutableList()
        p[11] = Joint(.59f,.25f); p[12] = Joint(.65f,.25f)
        val a = GaitEngine.measure(PoseFrame(0,p,720,960),ViewAngle.SIDE,Facing.RIGHT)
        // Same pixel geometry represented on a canvas twice as wide.
        val q = p.map { it.copy(x = it.x / 2) }
        val b = GaitEngine.measure(PoseFrame(0,q,1440,960),ViewAngle.SIDE,Facing.RIGHT)
        assertTrue(a.valid && b.valid); assertEquals(a.lean!!,b.lean!!,.001f)
    }
    @Test fun rearViewRejectsOverlappingHips() {
        assertFalse(GaitEngine.measure(pose(),ViewAngle.REAR,Facing.RIGHT).valid)
    }
    @Test fun exampleReportUsesRealAnalyzerAndIsExplicitlySynthetic() {
        val example = ExampleSession.create()
        assertTrue(example.demo); assertNotNull(example.score)
        assertTrue(example.frames.size >= 100); assertTrue(example.moments.any { it.key == "lean" })
        assertTrue(example.score!! < 100)
    }
    @Test fun temporalFlagsRequireConsecutiveVisibleFrames() {
        val m = listOf(FrameMetrics(0,true,.99f,flags=listOf("lean")),
            FrameMetrics(100,false,.2f,flags=listOf("lean")),FrameMetrics(200,true,.99f,flags=listOf("lean")))
        assertTrue(GaitEngine.sustainedKeys(m).all { it.isEmpty() })
    }
    @Test fun initialTimestampIsNormalizedToVideoTime() {
        val engine = GaitEngine(ViewAngle.SIDE,Facing.RIGHT)
        engine.add(pose(100000)); engine.add(pose(100100))
        val report = engine.finish("test",200)
        assertEquals(0L,report.frames.first().timeMs); assertEquals(100L,report.frames.last().timeMs)
    }
}
