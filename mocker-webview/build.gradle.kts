/*
 * Copyright 2019-2026 David Blanc
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
    id("com.android.library")
}

android {
    namespace = "fr.speekha.httpmocker.webview"
    compileSdk = 37

    sourceSets {
        getByName("main").kotlin.directories.add("src/main/kotlin")
    }

    defaultConfig {
        minSdk = 21
    }

    buildToolsVersion = "36.0.0"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    api(project(":mocker-okhttp"))
    implementation(libs.coroutines.android)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.matching { it.name == "javaDocReleaseGeneration" }.configureEach {
    enabled = false
}

apply(from = "../gradle/publish.gradle")
