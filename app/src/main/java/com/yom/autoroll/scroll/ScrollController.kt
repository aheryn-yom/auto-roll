package com.yom.autoroll.scroll

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ScrollController(
    private val executor: GestureExecutor,
    private val scope: CoroutineScope,
    private val screenSize: () -> Pair<Int, Int>,
    private val onStopped: (String?) -> Unit,
) {
    private var job: Job? = null
    private var generation = 0L

    val isRunning: Boolean get() = job?.isActive == true

    fun start(config: ScrollConfig) {
        stop()
        val runId = ++generation
        job = scope.launch {
            var error: String? = null
            while (isActive && runId == generation) {
                val (width, height) = screenSize()
                if (width <= 0 || height <= 0) {
                    error = "无法获取屏幕尺寸"
                    break
                }
                val fromY = height * 0.76f
                val distance = height * config.distanceRatio
                val toY = (fromY - distance).coerceAtLeast(height * 0.12f)
                val completed = executor.swipe(width * 0.5f, fromY, toY, config.gestureDurationMs)
                if (!completed) {
                    error = "手势未能完成，请重试"
                    break
                }
                if (!isActive || runId != generation) break
                delay(config.pauseMs)
            }
            if (runId == generation) {
                job = null
                onStopped(error)
            }
        }
    }

    fun stop() {
        generation++
        job?.cancel()
        job = null
    }

    fun dispose() {
        stop()
        scope.cancel()
    }
}
