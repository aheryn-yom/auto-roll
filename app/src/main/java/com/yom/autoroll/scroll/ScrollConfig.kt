package com.yom.autoroll.scroll

enum class ScrollMode(val label: String) {
    STEP("间歇"),
    SMOOTH("连续")
}

enum class ScrollSpeed(val label: String, val rate: Float) {
    SLOW("慢", 0f), MEDIUM("中", 0.5f), FAST("快", 1f)
}

data class ScrollConfig(
    val mode: ScrollMode,
    val rate: Float,
    val distanceRatio: Float,
    val gestureDurationMs: Long,
    val pauseMs: Long,
) {
    companion object {
        fun preset(mode: ScrollMode, speed: ScrollSpeed): ScrollConfig = forRate(mode, speed.rate)

        fun forRate(mode: ScrollMode, rate: Float): ScrollConfig {
            val normalizedRate = rate.coerceIn(0f, 1f)
            val anchors = when (mode) {
                ScrollMode.STEP -> listOf(
                    Anchor(0.25f, 700L, 2_000L),
                    Anchor(0.40f, 500L, 1_200L),
                    Anchor(0.60f, 350L, 600L),
                )
                ScrollMode.SMOOTH -> listOf(
                    Anchor(0.04f, 320L, 120L),
                    Anchor(0.07f, 220L, 50L),
                    Anchor(0.10f, 150L, 20L),
                )
            }
            val scaledRate = normalizedRate * (anchors.lastIndex)
            val leftIndex = scaledRate.toInt().coerceAtMost(anchors.lastIndex - 1)
            val fraction = if (normalizedRate == 1f) 1f else scaledRate - leftIndex
            val left = anchors[leftIndex]
            val right = anchors[leftIndex + 1]

            return ScrollConfig(
                mode = mode,
                rate = normalizedRate,
                distanceRatio = interpolate(left.distanceRatio, right.distanceRatio, fraction),
                gestureDurationMs = interpolate(left.gestureDurationMs, right.gestureDurationMs, fraction),
                pauseMs = interpolate(left.pauseMs, right.pauseMs, fraction),
            )
        }

        private fun interpolate(start: Float, end: Float, fraction: Float) =
            start + (end - start) * fraction

        private fun interpolate(start: Long, end: Long, fraction: Float) =
            (start + (end - start) * fraction).toLong()

        private data class Anchor(
            val distanceRatio: Float,
            val gestureDurationMs: Long,
            val pauseMs: Long,
        )
    }
}
