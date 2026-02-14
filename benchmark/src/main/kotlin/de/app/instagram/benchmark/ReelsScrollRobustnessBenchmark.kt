package de.app.instagram.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingGfxInfoMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMetricApi::class)
class ReelsScrollRobustnessBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun reelsScrollNormalSpeedFrameTiming() {
        runReelsScrollBenchmark(
            iterations = 3,
            swipesPerIteration = 8,
            swipeSteps = 20,
        )
    }

    @Test
    fun reelsScrollFastFlingFrameTiming() {
        runReelsScrollBenchmark(
            iterations = 3,
            swipesPerIteration = 10,
            swipeSteps = 8,
        )
    }

    private fun runReelsScrollBenchmark(
        iterations: Int,
        swipesPerIteration: Int,
        swipeSteps: Int,
    ) {
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingGfxInfoMetric()),
            compilationMode = CompilationMode.None(),
            iterations = iterations,
            startupMode = null,
            setupBlock = {
                pressHome()
                val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
                launchApp(device = device)

                check(device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), UI_WAIT_TIMEOUT_MS)) {
                    "App package was not visible after launch."
                }
                openReelsTab(device)
            },
        ) {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            check(device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), UI_WAIT_TIMEOUT_MS)) {
                "App package is not in foreground during measurement."
            }
            repeat(swipesPerIteration) {
                swipeUp(device, swipeSteps)
                device.waitForIdle()
            }
        }
    }

    private fun openReelsTab(device: UiDevice) {
        val reelsTab = device.findObject(By.desc("Reels")) ?: device.findObject(By.text("Reels"))
        if (reelsTab != null) {
            reelsTab.click()
            device.waitForIdle()
            return
        }

        // Fallback for devices where semantics labels are not exposed reliably in benchmark runs.
        val x = device.displayWidth / 2
        val y = (device.displayHeight * 0.96f).toInt()
        device.click(x, y)
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

    private fun launchApp(device: UiDevice) {
        device.executeShellCommand("am start -W -n $PACKAGE_NAME/.MainActivity")
    }

    private companion object {
        private const val PACKAGE_NAME = "de.app.instagram"
        private const val UI_WAIT_TIMEOUT_MS = 5_000L
    }
}
