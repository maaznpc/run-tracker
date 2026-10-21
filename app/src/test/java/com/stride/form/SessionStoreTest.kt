package com.stride.form

import org.junit.Assert.*
import org.junit.Test

class SessionStoreTest {
    @Test fun reportSurvivesExportRoundTripWithLandmarksAndEvidence() {
        val report = ExampleSession.create()
        val loaded = SessionStore.fromJson(SessionStore.toJson(report))
        assertEquals(report, loaded)
        assertTrue(loaded.frames.first().joints.size == 33)
    }
    @Test fun historySummariesDoNotLoadEveryLandmarkArray() {
        val report = ExampleSession.create()
        val summary = SessionStore.fromJson(SessionStore.toJson(report), lightweight = true)
        assertTrue(summary.frames.isEmpty()); assertTrue(summary.metrics.isEmpty())
        assertEquals(report.score, summary.score); assertEquals(report.summaries, summary.summaries)
        assertEquals(report.moments, summary.moments)
    }
    @Test(expected = IllegalArgumentException::class) fun unknownSchemaIsRejected() {
        val json = SessionStore.toJson(ExampleSession.create()).put("schemaVersion", 99)
        SessionStore.fromJson(json)
    }
}
