package com.yom.autoroll.scroll

import org.junit.Assert.assertEquals
import org.junit.Test

class ScrollConfigTest {
    @Test
    fun stepPresetsMatchMvpCadence() {
        assertEquals(ScrollConfig(ScrollMode.STEP, 0f, 0.25f, 700L, 2_000L),
            ScrollConfig.forRate(ScrollMode.STEP, 0f))
        assertEquals(ScrollConfig(ScrollMode.STEP, 0.5f, 0.40f, 500L, 1_200L),
            ScrollConfig.forRate(ScrollMode.STEP, 0.5f))
        assertEquals(ScrollConfig(ScrollMode.STEP, 1f, 0.60f, 350L, 600L),
            ScrollConfig.forRate(ScrollMode.STEP, 1f))
    }

    @Test
    fun smoothPresetsUseShortFrequentGestures() {
        val presets = ScrollSpeed.entries.map { ScrollConfig.forRate(ScrollMode.SMOOTH, it.rate) }
        assertEquals(listOf(0.04f, 0.07f, 0.10f), presets.map { it.distanceRatio })
        assertEquals(listOf(320L, 220L, 150L), presets.map { it.gestureDurationMs })
        assertEquals(listOf(120L, 50L, 20L), presets.map { it.pauseMs })
        assert(presets.all { it.distanceRatio in 0f..0.10f && it.gestureDurationMs > 0L })
    }

    @Test
    fun rateInterpolatesBetweenReferenceSpeedsAndIsClamped() {
        val quarter = ScrollConfig.forRate(ScrollMode.STEP, 0.25f)
        assertEquals(0.325f, quarter.distanceRatio, 0.0001f)
        assertEquals(600L, quarter.gestureDurationMs)
        assertEquals(1_600L, quarter.pauseMs)

        assertEquals(ScrollConfig.forRate(ScrollMode.STEP, 0f), ScrollConfig.forRate(ScrollMode.STEP, -1f))
        assertEquals(ScrollConfig.forRate(ScrollMode.SMOOTH, 1f), ScrollConfig.forRate(ScrollMode.SMOOTH, 2f))
    }
}
