/*
 * Copyright 2019 David Blanc
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

const val httpmock_version = "1.1.9"

const val kotlinVersion = "1.3.61"

object BuildPlugins {

    object Versions {
        const val buildTools = "3.5.2"
        const val dokka = "0.9.18"
        const val bintray = "1.8.4"
        const val artifactory = "4.9.6"
        const val ktlint = "2.1.3"
        const val detekt = "1.0.0"
    }

    object PluginsId {
        const val kotlin = "kotlin"
        const val kotlinKapt = "kotlin-kapt"
        const val kotlinSerialization = "kotlinx-serialization"
        const val androidApplication = "com.android.application"
        const val kotlinAndroid = "kotlin-android"
        const val kotlinAndroidExtensions = "kotlin-android-extensions"
        const val ktlint = "org.jmailen.kotlinter"
        const val detekt = "io.gitlab.arturbosch.detekt"
        const val dokka = "org.jetbrains.dokka"
        const val maven = "maven"
        const val mavenPublish = "maven-publish"
        const val bintray = "com.jfrog.bintray"
        const val artifactory = "com.jfrog.artifactory"
    }

    const val androidGradlePlugin = "com.android.tools.build:gradle:${Versions.buildTools}"
    const val kotlinSerialization =
        "org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion"
    const val dokka = "org.jetbrains.dokka:dokka-android-gradle-plugin:${Versions.dokka}"
    const val bintray = "com.jfrog.bintray.gradle:gradle-bintray-plugin:${Versions.bintray}"
    const val artifactory =
        "org.jfrog.buildinfo:build-info-extractor-gradle:${Versions.artifactory}"
    const val ktlint = "org.jmailen.gradle:kotlinter-gradle:${Versions.ktlint}"
    const val detekt = "io.gitlab.arturbosch.detekt:detekt-gradle-plugin:${Versions.detekt}"


}

object AndroidSdk {
    const val min = 21
    const val compile = 29
    const val target = compile
}

object LibVersions {
    const val okhttp = "3.14.4"
    const val slf4j = "1.7.26"
    const val jackson = "2.10.0"
    const val gson = "2.8.6"
    const val kotlinxSerialization = "0.13.0"
    const val moshi = "1.8.0"
    const val jetpack = "1.0.0"
    const val ktx = "1.1.0"
    const val constraintLayout = "1.1.3"
    const val arch = "2.1.0"
    const val coroutines = "1.3.0"
    const val retrofit = "2.6.0"
}

object Libraries {
    // Common
    const val slf4jApi = "org.slf4j:slf4j-api:${LibVersions.slf4j}"
    const val slf4jSimple = "org.slf4j:slf4j-simple:${LibVersions.slf4j}"
    const val slf4jAndroid = "org.slf4j:slf4j-android:${LibVersions.slf4j}"
    const val okHttp = "com.squareup.okhttp3:okhttp:${LibVersions.okhttp}"

    // GSON adapter
    const val gson = "com.google.code.gson:gson:${LibVersions.gson}"

    // Jackson adapter
    const val jacksonCore = "com.fasterxml.jackson.core:jackson-core:${LibVersions.jackson}"
    const val jacksonKotlin =
        "com.fasterxml.jackson.module:jackson-module-kotlin:${LibVersions.jackson}"
    const val jacksonAnnotations =
        "com.fasterxml.jackson.core:jackson-annotations:${LibVersions.jackson}"
    const val jacksonDatabind = "com.fasterxml.jackson.core:jackson-databind:${LibVersions.jackson}"

    // Kotlinx serialization
    const val kotlinSerialization =
        "org.jetbrains.kotlinx:kotlinx-serialization-runtime:${LibVersions.kotlinxSerialization}"

    // Moshi
    const val moshi = "com.squareup.moshi:moshi:${LibVersions.moshi}"
    const val moshiCodegen = "com.squareup.moshi:moshi-kotlin-codegen:${LibVersions.moshi}"

    // Android dependencies for demo app
    const val appCompat = "androidx.appcompat:appcompat:${LibVersions.jetpack}"
    const val constraintLayout =
        "androidx.constraintlayout:constraintlayout:${LibVersions.constraintLayout}"
    const val ktxCore = "androidx.core:core-ktx:${LibVersions.ktx}"
    const val lifecycle = "androidx.lifecycle:lifecycle-viewmodel-ktx:${LibVersions.arch}"

    const val retrofit = "com.squareup.retrofit2:retrofit:${LibVersions.retrofit}"
    const val coroutinesCore =
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:${LibVersions.coroutines}"
    const val coroutinesAndroid =
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:${LibVersions.coroutines}"

}

object TestLibraries {
    private object Versions {
        const val junit4 = "4.12"
        const val junit5 = "5.4.2"
        const val testRunner = "1.1.0"
        const val mockk = "1.9.3"
        const val mockito = "2.27.0"
        const val mockitoKotlin = "2.1.0"
    }

    const val junit4 = "junit:junit:${Versions.junit4}"
    const val junit5api = "org.junit.jupiter:junit-jupiter-api:${Versions.junit5}"
    const val junit5params = "org.junit.jupiter:junit-jupiter-params:${Versions.junit5}"
    const val junit5engine = "org.junit.jupiter:junit-jupiter-engine:${Versions.junit5}"
    const val mockServer = "com.squareup.okhttp3:mockwebserver:${LibVersions.okhttp}"
    const val mockk = "io.mockk:mockk:${Versions.mockk}"
    const val mockitoCore = "org.mockito:mockito-core:${Versions.mockito}"
    const val mockitoKotlin = "com.nhaarman.mockitokotlin2:mockito-kotlin:${Versions.mockitoKotlin}"
    const val archCore = "androidx.arch.core:core-testing:${LibVersions.arch}"
    const val testRunner = "androidx.test:runner:${Versions.testRunner}"
}