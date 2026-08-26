import com.coreswap.gradle.CopyNativeLibTask
import com.coreswap.gradle.GenerateUniffiBindingsTask

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Single ABI, no flavors: CoreSwap targets one phone.
val abiAndroid = "arm64-v8a"
val abiRust = "aarch64-linux-android"
val ndkRevision = "29.0.14206865"
val gradleToCargoProfiles = mapOf(
    "debug" to "debug",
    "release" to "release-android",
)

android {
    namespace = "com.coreswap.app"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.coreswap.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "0.3.0"
        ndk {
            abiFilters.add(abiAndroid)
        }
    }

    buildTypes {
        named("debug") {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        named("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    ndkVersion = ndkRevision
    packaging {
        resources {
            excludes += "/META-INF/*"
        }
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)

    // Required by the uniffi-generated Kotlin bindings.
    implementation(libs.jna) {
        artifact {
            type = "aar"
        }
    }

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

val rustProjectDir: File = rootProject.file("external/OpenSCQ30/android")
val rustWorkspaceDir: File = rootProject.file("external/OpenSCQ30")
val cargoTargetDirectory: File = rustWorkspaceDir.resolve("target")
// AGP 9 dropped the extension's sdkDirectory; sdkComponents resolves the NDK from ndkVersion.
val ndkDir = androidComponents.sdkComponents.ndkDirectory

gradleToCargoProfiles.forEach { (gradleBuildProfile, cargoProfile) ->
    tasks.register<Exec>("cargo-build-$gradleBuildProfile") {
        description = "Building the OpenSCQ30 core for $gradleBuildProfile"
        workingDir = rustProjectDir
        doFirst {
            environment("ANDROID_NDK_HOME", ndkDir.get().asFile.absolutePath)
        }
        commandLine(
            "cargo",
            "ndk",
            "--target",
            abiRust,
            "--platform",
            "26",
            "build",
            "--profile",
            if (cargoProfile == "debug") "dev" else cargoProfile,
        )
    }

    val copyNativeLibTask = tasks.register<CopyNativeLibTask>("rust-deploy-$gradleBuildProfile") {
        dependsOn("cargo-build-$gradleBuildProfile")
        description = "Copy the rust cdylib for $gradleBuildProfile into jniLibs"
        this.inputFile = File("$cargoTargetDirectory/$abiRust/$cargoProfile/libopenscq30_android.so")
        this.androidAbi = abiAndroid
        this.outputDirectory =
            layout.buildDirectory.get().asFile.resolve("generated/native/$gradleBuildProfile/jniLibs")
    }

    val generateTask =
        tasks.register<GenerateUniffiBindingsTask>("generate-uniffi-bindings-$gradleBuildProfile") {
            dependsOn("cargo-build-$gradleBuildProfile")
            description = "Generate kotlin bindings using uniffi-bindgen"
            this.rustAbi = abiRust
            this.cargoProfile = cargoProfile
            this.rustWorkspaceDirectory = rustWorkspaceDir
            // Our uniffi.toml, not upstream's: it names the com.coreswap packages.
            this.rustProjectDirectory = layout.projectDirectory.asFile
            this.outputDirectory = layout.buildDirectory.get().asFile
                .resolve("generated/source/uniffi/$gradleBuildProfile/java")
        }

    androidComponents {
        onVariants(selector().withBuildType(gradleBuildProfile)) { variant ->
            variant.sources.java!!.addGeneratedSourceDirectory(
                generateTask,
                GenerateUniffiBindingsTask::outputDirectory,
            )
            variant.sources.jniLibs!!.addGeneratedSourceDirectory(
                copyNativeLibTask,
                CopyNativeLibTask::outputDirectory,
            )
        }
    }
}
