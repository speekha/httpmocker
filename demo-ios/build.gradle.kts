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

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    kotlin("plugin.serialization")
}

kotlin {

    val frameworkName = "HttpMockerDemo"

    iosArm64 {
        binaries.framework {
            baseName = frameworkName
            isStatic = true
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = frameworkName
            isStatic = true
        }
    }

    iosX64 {
        binaries.framework {
            baseName = frameworkName
            isStatic = true
        }
    }

    sourceSets {
        iosMain {
            dependencies {
                implementation(libs.coroutines.core)
                implementation(libs.ktor.client.core)
                // Darwin is the native iOS HTTP engine for Ktor (replaces JVM-only CIO)
                implementation("io.ktor:ktor-client-darwin:${libs.versions.ktor.get()}")
                implementation(libs.ktor.content.negotiation)
                implementation(libs.ktor.serialization.json)
                implementation(libs.kotlinx.serialization)

                implementation(project(":mocker-core"))
                implementation(project(":mocker-ktor"))
                implementation(project(":kotlinx-adapter"))
                implementation(project(":custom-adapter"))
            }
        }
    }
}

// Task to assemble a universal XCFramework for all iOS slices
val xcframeworkOutputDir = layout.buildDirectory.dir("xcframeworks/HttpMockerDemo.xcframework")

tasks.register<Exec>("assembleXCFramework") {
    dependsOn(
        "linkReleaseFrameworkIosArm64",
        "linkReleaseFrameworkIosSimulatorArm64",
        "linkReleaseFrameworkIosX64"
    )

    doFirst {
        val dir = xcframeworkOutputDir.get().asFile
        if (dir.exists()) dir.deleteRecursively()
    }

    val buildDir = layout.buildDirectory.get().asFile.absolutePath
    commandLine(
        "xcodebuild", "-create-xcframework",
        "-framework", "$buildDir/bin/iosArm64/releaseFramework/HttpMockerDemo.framework",
        "-framework", "$buildDir/bin/iosSimulatorArm64/releaseFramework/HttpMockerDemo.framework",
        "-output", xcframeworkOutputDir.get().asFile.absolutePath
    )
}
