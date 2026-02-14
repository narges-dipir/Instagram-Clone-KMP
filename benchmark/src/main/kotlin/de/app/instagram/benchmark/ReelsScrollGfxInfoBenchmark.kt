package de.app.instagram.benchmark

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import android.util.Log
import kotlin.math.max
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReelsScrollGfxInfoBenchmark {
    @Test
    fun normalSpeedScrollRobustness() {
        val metrics = runScrollScenario(
            swipes = 10,
            swipeSteps = 20,
        )
        printMetrics("normal", metrics)
        assertRobustEnough(metrics)
    }

    @Test
    fun fastFlingScrollRobustness() {
        val metrics = runScrollScenario(
            swipes = 14,
            swipeSteps = 8,
        )
        printMetrics("fast_fling", metrics)
        assertRobustEnough(metrics)
    }

    private fun runScrollScenario(swipes: Int, swipeSteps: Int): GfxInfoMetrics {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.pressHome()
        device.executeShellCommand("am force-stop $PACKAGE_NAME")
        device.executeShellCommand("am start -W -n $PACKAGE_NAME/$MAIN_ACTIVITY")
        check(device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), UI_WAIT_TIMEOUT_MS)) {
            "App did not reach foreground."
        }

        openReelsTab(device)
        device.executeShellCommand("dumpsys gfxinfo $PACKAGE_NAME reset")

        repeat(swipes) {
            swipeUp(device, swipeSteps)
            device.waitForIdle()
        }

        val gfx = device.executeShellCommand("dumpsys gfxinfo $PACKAGE_NAME")
        return parseMetrics(gfx)
    }

    private fun openReelsTab(device: UiDevice) {
        val reelsTab = device.findObject(By.desc("Reels")) ?: device.findObject(By.text("Reels"))
        if (reelsTab != null) {
            reelsTab.click()
        } else {
            // Fallback for devices with missing semantics in instrumentation.
            val x = device.displayWidth / 2
            val y = (device.displayHeight * 0.96f).toInt()
            device.click(x, y)
        }
        device.waitForIdle()
    }

    private fun swipeUp(device: UiDevice, steps: Int) {
        val width = device.displayWidth
        val height = device.displayHeight
        device.swipe(
            width / 2,
            (height * 0.83f).toInt(),
            width / 2,
            (height * 0.22f).toInt(),
            steps,
        )
    }

    private fun parseMetrics(gfx: String): GfxInfoMetrics {
        val totalFrames = findInt(gfx, """Total frames rendered:\s*(\d+)""")
        val jankyFrames = findInt(gfx, """Janky frames:\s*(\d+)""")
        val jankPercent = findDouble(gfx, """Janky frames:\s*\d+\s*\(([\d.]+)%\)""")
        val p50 = findInt(gfx, """50th percentile:\s*(\d+)ms""")
        val p90 = findInt(gfx, """90th percentile:\s*(\d+)ms""")
        val p95 = findInt(gfx, """95th percentile:\s*(\d+)ms""")

        return GfxInfoMetrics(
            totalFrames = totalFrames,
            jankyFrames = jankyFrames,
            jankPercent = jankPercent,
            p50Ms = p50,
            p90Ms = p90,
            p95Ms = p95,
        )
    }

    private fun findInt(text: String, pattern: String): Int {
        val match = Regex(pattern).find(text)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    }

    private fun findDouble(text: String, pattern: String): Double {
        val match = Regex(pattern).find(text)
        return match?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 0.0
    }

    private fun printMetrics(mode: String, metrics: GfxInfoMetrics) {
        Log.i(
            METRICS_TAG,
            "ROBUSTNESS_METRICS mode=$mode " +
                "total_frames=${metrics.totalFrames} " +
                "janky_frames=${metrics.jankyFrames} " +
                "jank_percent=${metrics.jankPercent} " +
                "p50_ms=${metrics.p50Ms} " +
                "p90_ms=${metrics.p90Ms} " +
                "p95_ms=${metrics.p95Ms}",
        )
    }

    private fun assertRobustEnough(metrics: GfxInfoMetrics) {
        check(metrics.totalFrames >= MIN_EXPECTED_FRAMES) {
            "Insufficient frames captured: ${metrics.totalFrames}"
        }
        val maxJankPercent = max(MAX_JANK_PERCENT, 0.0)
        check(metrics.jankPercent <= maxJankPercent) {
            "Jank too high: ${metrics.jankPercent}% > $maxJankPercent%"
        }
    }

    private data class GfxInfoMetrics(
        val totalFrames: Int,
        val jankyFrames: Int,
        val jankPercent: Double,
        val p50Ms: Int,
        val p90Ms: Int,
        val p95Ms: Int,
    )

    private companion object {
        private const val PACKAGE_NAME = "de.app.instagram"
        private const val MAIN_ACTIVITY = ".MainActivity"
        private const val UI_WAIT_TIMEOUT_MS = 5_000L
        private const val MIN_EXPECTED_FRAMES = 30
        private const val MAX_JANK_PERCENT = 40.0
        private const val METRICS_TAG = "ROBUSTNESS_METRICS"
    }
}
