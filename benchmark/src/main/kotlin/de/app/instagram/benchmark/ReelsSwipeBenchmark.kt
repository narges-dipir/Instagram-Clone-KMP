package de.app.instagram.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
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
class ReelsSwipeBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun reelsSwipeFrameTiming() {
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.None(),
            iterations = 3,
            startupMode = null,
            setupBlock = {
                pressHome()
                launchApp(device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()))

                val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
                device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), UI_WAIT_TIMEOUT_MS)
                val reelsTab = device.findObject(By.desc("Reels")) ?: device.findObject(By.text("Reels"))
                if (reelsTab != null) {
                    reelsTab.click()
                } else {
                    // Fallback for devices where semantics labels are not exposed reliably.
                    val x = device.displayWidth / 2
                    val y = (device.displayHeight * 0.96f).toInt()
                    device.click(x, y)
                }
                device.waitForIdle()
            },
        ) {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            repeat(6) {
                swipeUp(device)
                device.waitForIdle()
            }
        }
    }

    private fun swipeUp(device: UiDevice) {
        val width = device.displayWidth
        val height = device.displayHeight
        device.swipe(
            width / 2,
            (height * 0.80f).toInt(),
            width / 2,
            (height * 0.25f).toInt(),
            SWIPE_STEPS,
        )
    }

    private fun launchApp(device: UiDevice) {
        device.executeShellCommand("am start -W -n $PACKAGE_NAME/.MainActivity")
    }

    private companion object {
        private const val PACKAGE_NAME = "de.app.instagram"
        private const val UI_WAIT_TIMEOUT_MS = 5_000L
        private const val SWIPE_STEPS = 20
    }
}
