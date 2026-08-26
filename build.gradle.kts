// Top-level build file. CoreSwap is a single-module app; the Rust engine lives in
// external/OpenSCQ30 and is built by tasks declared in app/build.gradle.kts.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.plugin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
