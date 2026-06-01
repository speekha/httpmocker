/*
 * Copyright 2019-2021 David Blanc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.kotlin.compose)
}

fun release(version: Int): Int = version

android {
    namespace = "fr.speekha.httpmocker.demo"
    compileSdk = release(36)

    sourceSets {
        getByName("main").kotlin.srcDirs("src/main/kotlin")
    }

    defaultConfig {
        applicationId = "fr.speekha.httpmocker.demo"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }

    buildToolsVersion = "35.0.0"

    flavorDimensions += "engine"

    productFlavors {
        create("okhttp") {
            dimension = "engine"
        }
        create("ktor") {
            dimension = "engine"
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

repositories {
    // For snapshots versions
    // maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
}

dependencies {
    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // AndroidX
    implementation(libs.bundles.androidx)

    // Lifecycle
    implementation(libs.bundles.androidx.lifecycle)

    // UniFlow
    implementation(libs.bundles.uniflow)
    testImplementation(libs.bundles.uniflow.test)

    // Jetpack Compose
    implementation(libs.bundles.androidx.compose)
    implementation(platform(libs.compose.bom))

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Retrofit
    implementation(libs.retrofit)

    // Koin
    implementation(libs.koin.android)

    // Logging
    implementation(libs.slf4j.android)

    // OkHttp flavor
    "okhttpImplementation"(project(":mocker-okhttp"))
    "okhttpImplementation"(project(":jackson-adapter"))
    "okhttpImplementation"(libs.retrofit.jackson)

    // Ktor flavor
    "ktorImplementation"(project(":mocker-ktor"))
    "ktorImplementation"(project(":kotlinx-adapter"))
    "ktorImplementation"(libs.ktor.client.android)
    "ktorImplementation"(libs.bundles.ktor)
    "ktorImplementation"(libs.kotlinx.serialization)

    // Testing
    testImplementation(libs.junit4)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.arch.core)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.slf4j.simple)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
