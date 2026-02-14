import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

plugins {
    alias(libs.plugins.androidTest)
    alias(libs.plugins.kotlinAndroid)
}

kotlin {
    jvmToolchain(11)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

android {
    namespace = "de.app.instagram.benchmark"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
    }

    targetProjectPath = ":composeApp"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    buildTypes {
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.testExt.junit)
    implementation(libs.androidx.uiautomator)
}

androidComponents {
    beforeVariants(selector().all()) { variant ->
        if (variant.buildType == "debug" || variant.buildType == "release") {
            variant.enable = false
        }
    }
}

abstract class ExportRobustnessMetricsTask @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val metricTag: Property<String>

    @TaskAction
    fun exportMetrics() {
        val reportDir = outputDir.get().asFile
        reportDir.mkdirs()

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        val latestFile = reportDir.resolve("robustness-metrics-latest.txt")
        val timestampedFile = reportDir.resolve("robustness-metrics-$timestamp.txt")

        val adbOutput = ByteArrayOutputStream()
        val execResult = runCatching {
            execOperations.exec {
                commandLine("adb", "logcat", "-d")
                standardOutput = adbOutput
                errorOutput = adbOutput
                isIgnoreExitValue = true
            }
        }

        val allLogs = if (execResult.isSuccess) adbOutput.toString(Charsets.UTF_8.name()) else ""
        val metricsLines = allLogs
            .lineSequence()
            .filter { it.contains(metricTag.get()) }
            .toList()

        val report = buildString {
            appendLine("Instagram benchmark metrics report")
            appendLine("Generated: ${LocalDateTime.now()}")
            appendLine("Gradle task: :benchmark:runBenchmarksWithMetrics")
            appendLine("Connected report: benchmark/build/reports/androidTests/connected/benchmark/index.html")
            appendLine()

            if (execResult.isFailure) {
                appendLine("adb status: FAILED")
                appendLine("reason: ${execResult.exceptionOrNull()?.message}")
                appendLine()
            }

            if (metricsLines.isEmpty()) {
                appendLine("${metricTag.get()}: none found in current logcat buffer.")
                appendLine("If benchmarks just ran, ensure tests emitted metrics and run adb logcat -d again.")
            } else {
                appendLine("${metricTag.get()}:")
                metricsLines.forEach { appendLine(it) }
            }
        }

        latestFile.writeText(report)
        timestampedFile.writeText(report)

        logger.lifecycle("Metrics exported to ${latestFile.absolutePath}")
        logger.lifecycle("Metrics archived at ${timestampedFile.absolutePath}")
    }
}

val exportRobustnessMetrics by tasks.registering(ExportRobustnessMetricsTask::class) {
    group = "verification"
    description = "Exports ROBUSTNESS_METRICS from adb logcat into a stable report file."
    outputDir.set(layout.buildDirectory.dir("reports/androidTests/connected/benchmark"))
    metricTag.set("ROBUSTNESS_METRICS")
}

tasks.register("runBenchmarksWithMetrics") {
    group = "verification"
    description = "Runs connected benchmark tests and exports a stable metrics report file."
    dependsOn("connectedBenchmarkAndroidTest")
    finalizedBy(exportRobustnessMetrics)
}

val clearBenchmarkLogcat by tasks.registering(Exec::class) {
    group = "verification"
    description = "Clears adb logcat before robustness benchmark run."
    commandLine("adb", "logcat", "-c")
}

val runInstalledRobustnessBenchmark by tasks.registering(Exec::class) {
    group = "verification"
    description = "Runs installed ReelsScrollGfxInfoBenchmark class via adb instrumentation."
    dependsOn(clearBenchmarkLogcat)
    commandLine(
        "adb",
        "shell",
        "am",
        "instrument",
        "-w",
        "-e",
        "class",
        "de.app.instagram.benchmark.ReelsScrollGfxInfoBenchmark",
        "de.app.instagram.benchmark/androidx.test.runner.AndroidJUnitRunner",
    )
}

tasks.register("runInstalledRobustnessWithMetrics") {
    group = "verification"
    description = "Runs installed robustness benchmark class via adb and exports metrics to report files."
    dependsOn(runInstalledRobustnessBenchmark)
    finalizedBy(exportRobustnessMetrics)
}
