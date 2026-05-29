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
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21"
}

val coroutines_version: String by rootProject.extra
val compose_bom_version: String by rootProject.extra
val uniflow_version: String by rootProject.extra
val retrofit_version: String by rootProject.extra
val slf4j_version: String by rootProject.extra
val ktor_version: String by rootProject.extra
val kotlinx_serialization_version: String by rootProject.extra
val mockk_version: String by rootProject.extra

fun release(version: Int): Int = version

android {
    namespace = "fr.speekha.httpmocker.demo"
    compileSdk = release(36)

    sourceSets {
        getByName("main").kotlin.srcDirs("src/main/kotlin")
    }

    defaultConfig {
        applicationId = "fr.speekha.httpmocker.demo"
        minSdk = 23
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines_version")

    // AndroidX
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.google.android.material:material:1.14.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")

    // UniFlow
    implementation("org.uniflow-kt:uniflow-core:$uniflow_version")
    testImplementation("org.uniflow-kt:uniflow-test:$uniflow_version")
    implementation("org.uniflow-kt:uniflow-android:$uniflow_version")
    testImplementation("org.uniflow-kt:uniflow-android-test:$uniflow_version")

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:$compose_bom_version")
    implementation(composeBom)

    implementation("androidx.compose.runtime:runtime-livedata:1.11.2")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation(platform("androidx.compose:compose-bom:$compose_bom_version"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:$retrofit_version")

    // Koin
    implementation("io.insert-koin:koin-android:4.2.1")

    // Logging
    implementation("org.slf4j:slf4j-android:$slf4j_version")

    // OkHttp flavor
    "okhttpImplementation"(project(":mocker-okhttp"))
    "okhttpImplementation"(project(":jackson-adapter"))
    "okhttpImplementation"("com.squareup.retrofit2:converter-jackson:$retrofit_version")

    // Ktor flavor
    "ktorImplementation"(project(":mocker-ktor"))
    "ktorImplementation"(project(":kotlinx-adapter"))
    "ktorImplementation"("io.ktor:ktor-client-cio:$ktor_version")
    "ktorImplementation"("io.ktor:ktor-client-android:$ktor_version")
    "ktorImplementation"("io.ktor:ktor-client-content-negotiation:$ktor_version")
    "ktorImplementation"("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    "ktorImplementation"("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_version")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:$mockk_version")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutines_version")
    testImplementation("org.slf4j:slf4j-simple:$slf4j_version")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
