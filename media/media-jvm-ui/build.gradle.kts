plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.compose.compiler)
}
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }

    dependencies {
        implementation(project(":common"))
        implementation(project(":domain"))
        implementation(project(":media-jvm"))
        // UI
        implementation(libs.compose.ui)
        implementation(libs.compose.material3)

        implementation(libs.coil.compose)
        implementation(libs.coil.network.okhttp)

        implementation(platform(libs.koin.bom))
        implementation(libs.koin.jvm)
        implementation(libs.koin.compose)

        implementation(libs.kotlinx.coroutinesSwing)
    }
}