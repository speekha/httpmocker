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
    kotlin("jvm")
    kotlin("plugin.serialization")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    //    val processors = Runtime.getRuntime().availableProcessors()
    //    if (processors > 1) {
    //        maxParallelForks = processors - 1
    //    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(libs.coroutines.core)

    // Logs
    implementation(libs.slf4j.simple)

    // Local projects
    implementation(project(":mocker-okhttp"))
    implementation(project(":mocker-ktor"))
    implementation(project(":jackson-adapter"))
    implementation(project(":gson-adapter"))
    implementation(project(":moshi-adapter"))
    implementation(project(":kotlinx-adapter"))
    implementation(project(":custom-adapter"))
    implementation(project(":sax-adapter"))

    // OkHttp
    implementation(libs.okhttp)

    // Ktor
    implementation(libs.bundles.ktor)
    testImplementation(libs.kotlinx.serialization)

    // Jackson
    testImplementation(libs.bundles.jackson)

    // Junit
    testImplementation(libs.bundles.junit)

    // Mocks for Kotlin tests
    testImplementation(libs.mockk)

    // Mock web server
    testImplementation(libs.okhttp.mockwebserver)
}

apply(from = "../gradle/coverage.gradle.kts")
