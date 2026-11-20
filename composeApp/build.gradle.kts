import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.detekt)
}

detekt {
    toolVersion = libs.versions.detekt.get()
    parallel = true
    buildUponDefaultConfig = false
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    ignoreFailures = false
}

// Один общий baseline на все задачи анализа: записи — это «правило + FQN»,
// они не зависят от source set'а и строки, поэтому commonMain-задача пишет файл,
// а остальные читают тот же самый.
val detektBaselineFile = rootProject.file("config/detekt/detekt-baseline.xml")

tasks.withType<Detekt>().configureEach {
    exclude { it.file.invariantSeparatorsPath.contains("/build/generated/") }
    baseline.set(detektBaselineFile)
}

tasks.withType<DetektCreateBaselineTask>().configureEach {
    baseline.set(detektBaselineFile)
}

tasks.named<Detekt>("detekt") {
    dependsOn(tasks.withType<Detekt>().matching { it.name != "detekt" })
}

kotlin {
    configureTargets()
    configureOptions()
    configureDependencies()
}

private fun KotlinMultiplatformExtension.configureDependencies() {
    sourceSets {
        val jvmSharedMain = create("jvmSharedMain") {
            dependsOn(commonMain.get())
        }
        jvmMain.get().dependsOn(jvmSharedMain)
        androidMain.get().dependsOn(jvmSharedMain)

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui)

            implementation(libs.kodein)
            implementation(libs.kodein.compose)

            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.runtime.compose)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            implementation(libs.multiplatformSettings.noArgs)

            implementation(libs.ktor.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }

        jvmSharedMain.dependencies {
            implementation(libs.firebase.admin)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.slf4j.simple)
        }
    }
}

private fun KotlinMultiplatformAndroidLibraryTarget.configureAndroidAppVersion() {
    namespace = "${libs.versions.namespace.get()}.composeApp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    minSdk = libs.versions.android.minSdk.get().toInt()
}

private fun KotlinMultiplatformExtension.configureTargets() {
    android {
        configureAndroidAppVersion()
        configureAndroidOptions()
    }

    jvm()
}

private fun KotlinMultiplatformExtension.configureOptions() {
    jvmToolchain(17)

    compilerOptions {
        freeCompilerArgs.addAll(
            listOf(
                "-Xno-call-assertions",
                "-Xno-receiver-assertions",
                "-Xno-param-assertions",
                "-Xexpect-actual-classes",
            )
        )
    }
}

private fun KotlinMultiplatformAndroidLibraryTarget.configureAndroidOptions() {
    experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/kotlin/**"
            excludes += "/META-INF/androidx.*.version"
            excludes += "/META-INF/com.google.*.version"
            excludes += "/META-INF/kotlinx_*.version"
        }
    }
}

compose.desktop {
    application {
        mainClass = "${libs.versions.namespace.get()}.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = libs.versions.namespace.get()
            packageVersion = libs.versions.versionName.get()
        }
    }
}
